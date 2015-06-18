package neutrino.shopplan

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import scala.util.control.NonFatal
import scala.collection.mutable.{HashMap => MutableHashMap}

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.pattern.pipe
import akka.util.Timeout

import scalaz._, Scalaz._

import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._
import com.goshoplane.common._

import goshoplane.commons.core.protocols.Implicits._

import store.ShopPlanDatastore

import neutrino.core._
import neutrino.user.protocols._
import neutrino.bucket.protocols._

import com.google.maps.{GeocodingApi, GeoApiContext, PendingResult}
import com.google.maps.model.{LatLng, AddressComponentType, GeocodingResult, AddressType}


class ShopPlanner(
  shopplanId: ShopPlanId,
  shopplanDatastore: ShopPlanDatastore,
  _User  : ActorRef @@ UserActorRef,
  _Bucket: ActorRef @@ BucketActorRef) extends Actor with ActorLogging {

  private val settings = ShopPlanSettings(context.system)

  import neutrino.shopplan.protocols._
  import context.dispatcher
  import settings._


  private val Bucket = Tag.unwrap(_Bucket)
  private val User   = Tag.unwrap(_User)

  // Watching these actors for any termination.
  // This supervisor is heavily dependent
  // on these actors for most of its operations.
  context watch Bucket
  context watch User


  private val geoApiContext = new GeoApiContext()
                                .setApiKey(GeoApiKey)
                                .setQueryRateLimit(500, 0)
                                .setConnectTimeout(1, TimeUnit.SECONDS)
                                .setReadTimeout(1, TimeUnit.SECONDS)
                                .setWriteTimeout(1, TimeUnit.SECONDS);



  def receive = {
    case AddNewShopPlan(`shopplanId`, cud) => addNewShopPlan(shopplanId, cud) pipeTo sender()
    case ModifyShopPlan(`shopplanId`, cud) => sender() ! false // [TO DO]

    // Handling of termination messages
    // for dependent actors
  }




  ////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////// Private methods ////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////


  /**
   * Add a new shopplan in storage for
   * a given shopplan id
   */
  private def addNewShopPlan(shopplanId: ShopPlanId, cud: CUDShopPlan): Future[Boolean] = {

    // 1. From the invites list (which is assumed to contain only userId of friend)
    //    create shop plan's Invite list with full user detail
    val invitesF =
      cud.invites.flatMap(_.adds)
         .map(makeInvites(shopplanId, _))
         .getOrElse(Future.successful(Seq.empty[Invite]))


    // filter out destinations which in case
    // dont have gps location
    val destinations =
      cud.destinations
         .flatMap(_.adds).getOrElse(Seq.empty[Destination])
         .filter(!_.address.gpsLoc.isEmpty)
         .map(destination => // update the destinations with new shopplan id
            destination.copy(destId = destination.destId.copy(shopplanId = shopplanId)))


    // 2. Resolve destinations' gps location to full address
    val destinationsF = resolveDestinations(destinations)

    val destinationNumShops = MutableHashMap.empty[DestinationId, Int]

    // 3. Convert itemIds to ShopPlanStores with items
    //    with nearest destinations assigned
    val storesF =
      cud.items.flatMap(_.adds)
         .map(makeShopPlanStores(shopplanId.createdBy, _, destinations, destinationNumShops))
         .getOrElse(Future.successful(Seq.empty[ShopPlanStore]))


    // 4. Create ShopPlan instance
    val shopplanF =
      for {
        invites      <- invitesF
        stores       <- storesF
        destinations <- destinationsF
      } yield ShopPlan(
                shopplanId    = shopplanId,
                title         = cud.meta.flatMap(_.title),
                stores        = stores.some,
                destinations  = destinations.map(d => d.copy(numShops = destinationNumShops.get(d.destId))).some,
                invites       = invites.some,
                isInvitation  = false
              )

    // 5. Persist ShopPlan to data store
    //    and return successF (Boolean)
    shopplanF.flatMap(shopplanDatastore.addNewShopPlan(_))
             .map(_ => true) // [TO FIX] read result set from
                             // cassandra and then mark it true
             .andThen {
                case Failure(NonFatal(ex)) =>
                  log.error(ex, "Caught error [{}] while adding new shop plan's info for " +
                                "shopplan id = {} and createdBy = {}",
                                ex.getMessage,
                                shopplanId.suid,
                                shopplanId.createdBy.uuid)
             }
  }



  def resolveDestinations(destinations: Seq[Destination]) = {
    Future.sequence(destinations.map({ destination =>
      resolveLatLng(destination.address)
        .map { address => destination.copy(address = address) }
    }))
  }



  /**
   * Create Invites from friend ids for user
   */
  def makeInvites(shopplanId: ShopPlanId, friendIds: Seq[UserId]) = {
    implicit val timeout = Timeout(1 seconds) // used for all ask request

    val friendsF = User ?= GetFriendsDetails(shopplanId.createdBy, friendIds)

    friendsF.map { friends =>
      friends.map { friend =>
        Invite(
          friendId   = friend.id,
          shopplanId = shopplanId,
          name       = friend.name,
          avatar     = friend.avatar)
      }
    }
  }



  /**
   * Create ShopPlanStores from itemIds using Bucket Stores for user
   * Also shop plan stores are assigned destination based on nearness
   */
  def makeShopPlanStores(
    userId: UserId,
    itemIds: Seq[CatalogueItemId],
    destinations: Seq[Destination],
    destinationNumShops: MutableHashMap[DestinationId, Int]) = {

    import BucketStoreField._

    implicit val timeout = Timeout(1 seconds) // used for all ask request

    // partial function for calculating nearest destinations
    val nearestDestination = chooseNearestDestination(destinations, _: GPSLocation)

    val storeIds = itemIds.map(_.storeId).distinct
    val cuids    = itemIds.map(_.cuid).toSet // toSet for efficient lookup
    val fields   = Seq(Name, Address, ItemTypes, CatalogueItems)

    // Get details of stores and its items
    //
    // [NOTE] It is assumed that all stores are from user's Bucket
    // therefore gettings stores complete information using Bucket
    // instead of Cassie
    val bStoresF = Bucket ?= GetGivenBucketStores(userId, storeIds, fields, remove = true)

    bStoresF map { bStores => // Convert Bucket store to ShopPlanStore with assigned
                              // nearest destination
      bStores.flatMap { bStore =>
        bStore.info.address.flatMap(_.gpsLoc).map { gpsLoc => // continue only if gpsLoc is present

          // Remove items from bucket stores that are not
          // present in given items list
          val itemsO =
            bStore.catalogueItems.map(_.filter(ser => cuids.contains(ser.itemId.cuid)))

          val destId = nearestDestination(gpsLoc) // get nearest destination
          destinationNumShops.put(destId, destinationNumShops.getOrElse(destId, 0) + 1)

          ShopPlanStore(
            storeId        = bStore.storeId,
            destId         = destId,
            storeType      = bStore.storeType,
            info           = bStore.info,
            catalogueItems = itemsO,
            itemIds        = itemsO.map(_.map(_.itemId))
          )
        }
      }
    }

  }



  /**
   * Resolve the Lat Lng of address with complete address information
   * using google geo coding api
   *
   * [TO DO] Move this logic to Commons for universal conversion
   */
  private def resolveLatLng(address: PostalAddress) =
    address.gpsLoc.map { gpsLoc =>   // make sure address has gpsLoc

      val request = GeocodingApi.newRequest(geoApiContext) // request
        .latlng(new LatLng(gpsLoc.lat, gpsLoc.lng))

      val p = Promise[PostalAddress]()

      request.setCallback(new PendingResult.Callback[Array[GeocodingResult]]() {

        override def onResult(result: Array[GeocodingResult]) {
          result.find(_.types.exists(_ == AddressType.STREET_ADDRESS)).map { googleAddress =>
            import AddressComponentType._
            import googleAddress._

            // Set country, city and postal code
            var newAddress =
              addressComponents.foldLeft(address) { (newAddress, component) =>
                if(component.types.exists(_ == COUNTRY))
                  newAddress.copy(country = component.longName.some)
                else if(component.types.exists(_ == LOCALITY))
                  newAddress.copy(city = component.longName.some)
                else if(component.types.exists(_ == POSTAL_CODE))
                  newAddress.copy(pincode = component.longName.some)
                else newAddress
              }

            // set full address
            newAddress = newAddress.copy(full = formattedAddress.some)

            val levels = Array.ofDim[String](4)
            addressComponents.foreach { component =>
              if(component.types.exists(_ == ROUTE))
                levels(0) = component.longName
              else if(component.types.exists(_ == SUBLOCALITY_LEVEL_2))
                levels(1) = component.longName
              else if(component.types.exists(_ == SUBLOCALITY_LEVEL_1))
                levels(2) = component.longName
              else if(component.types.exists(_ == LOCALITY))
                levels(3) = component.longName
            }

            // set short address
            newAddress = newAddress.copy(short = levels.mkString(", ").some)
            // set title address
            newAddress.copy(title = levels.take(3).mkString(", ").some)

          } match {
            case None => p failure (new Exception("No street address present in google"))
            case Some(newAddress) => p success newAddress
          }

        }

        override def onFailure(e: Throwable) {
          p failure e
        }

      })

      p.future

    }.getOrElse(Future.successful(address)) // else pass back the original





  private val R = 6371000 // radius of earth in metres

  // haversine’ formula to calculate the great-circle distance between two points
  // – that is, the shortest distance over the earth’s surface
  // – giving an ‘as-the-crow-flies’ distance between the points
  private def haversine(from: GPSLocation, to: GPSLocation) = {
    import scala.math._

    val latFr = toRadians(from.lat)
    val latTo = toRadians(to.lat)

    val deltaLat = toRadians(from.lat - to.lat)
    val deltaLng = toRadians(from.lng - to.lng)

    val a = pow(sin(deltaLat / 2), 2) +
            cos(latFr) * cos(latTo) *
            pow(sin(deltaLng / 2), 2)

    val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))

    R * c
  }



  // Returns the neartest destination for the target gps location
  private def chooseNearestDestination(destinations: Seq[Destination], target: GPSLocation) =
    destinations.tail.foldLeft((0.0, destinations.head))({

      case ((minD, currDest), destination) =>
        val newMinD =
          destination.address.gpsLoc
            .map(from => haversine(from, target))
            .getOrElse(minD)

        if(newMinD < minD) (newMinD, destination)
        else (minD, currDest)

    })._2.destId


}




// Companion object for ShopPlanner
object ShopPlanner {

  def actorNameFor(id: ShopPlanId) = id.createdBy.uuid + "-" + id.suid

  def props(shopplanId: ShopPlanId,
            shopplanDatastore: ShopPlanDatastore,
            user  : ActorRef @@ UserActorRef,
            bucket: ActorRef @@ BucketActorRef) =
    Props(classOf[ShopPlanner], shopplanId, shopplanDatastore, user, bucket)
}