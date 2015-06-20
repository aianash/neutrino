package neutrino.scripts

import scala.util.Random
import scala.concurrent.duration.FiniteDuration

import com.twitter.finagle.Thrift
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodecFactory
import com.twitter.util.{Future => TwitterFuture, Await}

import org.apache.thrift.protocol.TBinaryProtocol

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._
import com.goshoplane.creed.search._

import scalaz._, Scalaz._


/**
 * [TO DO] Proper test
 * {{{
 *   service/target/start neutrino.service.TestNeutrinoClient
 * }}}
 */
object NeutrinoClient {

  object User {

    val name         = UserName(first = "Kumar".some, last = "Ishan".some, handle = "kumarishan".some)
    val locale       = Locale.EnUs
    val gender       = Gender.Male
    val fbuid        = 1028289992817277L
    val facebookInfo = FacebookInfo(userId = UserId(fbuid), token = "KSJD03039JKSJKSJKLS0P2".some)
    val email        = "kumar.ishan89@gmail.com"
    val timezone     = "Asia/Kolkata"
    val avatar       = UserAvatar(small = "small".some, medium = "medium".some, large = "large".some)
    val isNew        = true

    val info = UserInfo(
      name         = name.some,
      locale       = locale.some,
      gender       = gender.some,
      facebookInfo = facebookInfo.some,
      email        = email.some,
      timezone     = timezone.some,
      avatar       = avatar.some,
      isNew        = isNew.some
    )

  }

  object Search {
    val param = QueryParam(value = Some("levis men's jeans"))
    val query = CatalogueSearchQuery(params = Map("brand" -> param), queryText = "")

    def searchId(userId: UserId) = CatalogueSearchId(userId, System.currentTimeMillis)
    def searchRequest(userId: UserId) = CatalogueSearchRequest(searchId(userId), query, 1, 100)

    def print(response: SearchResult) = {
      println("Search Result := ")
      response.result.foreach(printStore(_))
    }

    def printStore(store: SearchResultStore) {
      import store._
      println(
        s"""|stuid     = ${storeId.stuid}
            |storeType = ${storeType}
            |info =
            |  name      = ${info.name}
            |  itemTypes = ${info.itemTypes}
            |  address   = ${info.address}
            |  avatar    = ${info.avatar}
            |  email     = ${info.email}
            |  phone     = ${info.phone}
        """.stripMargin
      )

      store.items.foreach(Items.print)
    }

  }


  object Items {

    def print(items: Seq[JsonCatalogueItem]): Unit =
      items.foreach { item => print(item); println("\n") }

    def print(item: JsonCatalogueItem) {
      import item._
      println(
        s"""|stuid     = ${itemId.storeId.stuid}
            |cuid      = ${itemId.cuid}
            |versionId = ${versionId}
            |json      = ${json}
        """.stripMargin
      )
    }

  }


  object Bucket {
    import BucketStoreField._

    val fields = Seq(Name, Address, ItemTypes, CatalogueItemIds)

    def print(store: BucketStore) = {
      import store._
      println(
        s"""|stuid     = ${storeId.stuid}
            |storeType = ${storeType}
            |info =
            |  name      = ${info.name}
            |  itemTypes = ${info.itemTypes}
            |  address   = ${info.address}
            |  avatar    = ${info.avatar}
            |  email     = ${info.email}
            |  phone     = ${info.phone}
            |items =
        """.stripMargin
      )

      store.catalogueItems.foreach(Items.print)
    }

  }


  object ShopPlanUtils {
    def print(shopplan: ShopPlan) = {
      println(
        s"""|createdBy    = ${shopplan.shopplanId.createdBy.uuid}
            |suid         = ${shopplan.shopplanId.suid}
            |destinations =
        """.stripMargin
      )

      shopplan.destinations.foreach { destinations =>
        destinations.foreach(printDestination(_))
      }

      println("stores =")
      shopplan.stores.foreach { stores =>
        stores.foreach { x => println("\nShopPlan Store ="); printStore(x) }
      }
    }


    def printDestination(destination: Destination) =
      println(
        s"""|createdBy = ${destination.destId.shopplanId.createdBy.uuid}
            |suid      = ${destination.destId.shopplanId.suid}
            |dtuid     = ${destination.destId.dtuid}
            |address   =
            |  gpsLoc   = ${destination.address.gpsLoc}
            |  title    = ${destination.address.title}
            |  short    = ${destination.address.short}
            |  full     = ${destination.address.full}
            |  pincode  = ${destination.address.pincode}
            |  country  = ${destination.address.country}
            |  city     = ${destination.address.city}
            |numShops  = ${destination.numShops}
        """.stripMargin
      )


    def printStore(store: ShopPlanStore) = {
      println(
        s"""|stuid       = ${store.storeId.stuid}
            |createdBy   = ${store.destId.shopplanId.createdBy.uuid}
            |suid        = ${store.destId.shopplanId.suid}
            |dtuid       = ${store.destId.dtuid}
            |storeType   = ${store.storeType}
            |info =
            |  name      = ${store.info.name}
            |  itemTypes = ${store.info.itemTypes}
            |  address   = ${store.info.address}
            |  avatar    = ${store.info.avatar}
            |  email     = ${store.info.email}
            |  phone     = ${store.info.phone}
            |catalogueItems =
        """.stripMargin

      )
      store.catalogueItems.foreach(Items.print)

      println("catalogueItemIds = ")
      store.itemIds.foreach { itemIds =>
        itemIds.foreach(id => println(s"${id.storeId.stuid}.${id.cuid}"))
      }
    }
  }


  def main(args: Array[String]) {

    val Neutrino = {
      val protocol = new TBinaryProtocol.Factory()
      val client = ClientBuilder().codec(new ThriftClientFramedCodecFactory(None, false, protocol))
        .dest("localhost:2424").hostConnectionLimit(2).build()
      new Neutrino$FinagleClient(client, protocol)
    }

    val apiLatencies = new scala.collection.mutable.HashMap[String, FiniteDuration]()

    val aelapsed = lapse.Stopwatch.start()
    Neutrino.createUser(User.info) foreach { userId =>
      apiLatencies.put("Create user in = ", aelapsed())
      println(s"\n\nCreated user ${userId.uuid} in ${aelapsed()}")

      val belapsed = lapse.Stopwatch.start()
      Neutrino.search(Search.searchRequest(userId)).foreach { response =>
        apiLatencies.put("Search finished in = ", belapsed())
        println(s"\n\nSearch finished in ${belapsed()}")
        Search.print(response)

        val adds = response.result.flatMap(_.items).map(_.itemId).distinct
        val celapsed = lapse.Stopwatch.start()
        Neutrino.cudBucket(userId, CUDBucket(adds.some)) foreach { success =>
          apiLatencies.put("CUD Bucket in = ", celapsed())
          println(s"\n\nCUD bucket finished ${success}ly in ${celapsed()}")

          val delapsed = lapse.Stopwatch.start()
          Neutrino.getBucketStores(userId, Bucket.fields) foreach { stores =>
            apiLatencies.put("Got Bucket stores in = ", delapsed())
            println(s"\n\nGot Bucket Stores in ${delapsed()} and stores = ")
            stores.foreach(Bucket.print)

            // Values for cud shop plan

            var destIdSeq = System.currentTimeMillis
            val destinations = stores.take(2).flatMap(_.info.address).foldLeft(Seq.empty[Destination]) { (destinations, address) =>
              Destination(
                destId  = DestinationId(shopplanId = ShopPlanId(createdBy = userId, suid = -1L),
                                        dtuid = math.abs(destIdSeq + Random.nextLong)),
                address = PostalAddress(gpsLoc = address.gpsLoc)
              ) +: destinations
            }

            val cudDestinations = CUDDestinations(adds = destinations.some)
            val cudInvites      = CUDInvites()
            val cudItems        = CUDShopPlanItems(stores.flatMap(_.catalogueItems).flatMap(_.map(_.itemId)).some)
            val cudMeta         = CUDShopPlanMeta(title = "Your new shop plan".some)

            val cud = CUDShopPlan(meta          = cudMeta.some,
                                  destinations  = cudDestinations.some,
                                  invites       = cudInvites.some,
                                  items         = cudItems.some)

            val eelapsed = lapse.Stopwatch.start()
            Neutrino.createShopPlan(userId, cud) foreach { shopplanId =>
              apiLatencies.put("Create Shop plan in = ", eelapsed())
              println(s"\n\nCreated shop plan in ${eelapsed()} and shop plan id = " + shopplanId.suid)

              import ShopPlanField._
              val fields = Seq(Title, Stores, CatalogueItems, Destinations, Invites)

              val nelapsed = lapse.Stopwatch.start()
              Neutrino.getShopPlan(shopplanId, fields) foreach { shopPlan =>
                apiLatencies.put("Got shop plan in = ", nelapsed())
                println(s"\n\nGot shop plan in ${nelapsed()} and shop plan id = " + shopplanId.suid)
                ShopPlanUtils.print(shopPlan)
              }

              val oelapsed = lapse.Stopwatch.start()
              Neutrino.getOwnShopPlans(userId, Seq(Title, Stores, Destinations, Invites, CatalogueItemIds)) foreach { shopPlans =>
                apiLatencies.put("Got own shop plans in = ", oelapsed())
                println(s"\n\nGot own shop plans in ${oelapsed()} and shop plans are = ")
                shopPlans.foreach(ShopPlanUtils.print(_))

                apiLatencies.toSeq.foreach(s => println(s._1 + " " + s._2))
              }
            }
          }
        }
      }
    }


  }
}