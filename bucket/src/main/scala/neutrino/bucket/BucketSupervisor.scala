package neutrino.bucket

import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.util.control.NonFatal

import akka.actor.{Props, Actor, ActorLogging}
import akka.pattern.pipe

import goshoplane.commons.core.protocols.Implicits._
import goshoplane.commons.catalogue.CatalogueItem

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.cassie.service._, Cassie._

import store.BucketDatastore

import org.apache.thrift.protocol.TBinaryProtocol
import com.twitter.finagle.Thrift
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodecFactory

import com.twitter.bijection._, Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._



class BucketSupervisor extends Actor with ActorLogging {

  private val settings = BucketSettings(context.system)

  import context.dispatcher
  import settings._
  import protocols._

  private val bucketDatastore = new BucketDatastore(settings)
  bucketDatastore.init()

  // create a cassie client
  // [TO THINK] should there is one clien in whole neutrino
  val cassie = {
    val protocol = new TBinaryProtocol.Factory()
    val client = ClientBuilder().codec(new ThriftClientFramedCodecFactory(None, false, protocol))
      .dest(CassieHost + ":" + CassiePort).hostConnectionLimit(2).build()

    new Cassie$FinagleClient(client, protocol)
  }


  def receive = {

    case GetBucketStores(userId, fields) =>
      bucketDatastore.getBucketStores(userId, fields) pipeTo sender()



    case GetGivenBucketStores(userId, storeIds, fields, remove) =>
      (for {
        stores <- bucketDatastore.getGivenBucketStores(userId, storeIds, fields)
        _      <- bucketDatastore.deleteBucketStores(userId, storeIds) if remove
      } yield stores)
        .andThen {
          case Success(stores) if remove =>
          case Failure(NonFatal(ex)) =>
            log.error(ex, "Caught error {} while getting " +
                          (if(remove) "and removing " else " ") +
                          "bucket stores for " +
                          "user id = {} and storeIds = [{}] and fields = [{}]",
                          ex.getMessage,
                          userId.uuid,
                          storeIds.map(_.stuid).mkString(", "),
                          fields.mkString(", "))

        } pipeTo sender()



    case ModifyBucket(userId, cud) =>
      import StoreInfoField._

      val successOF =
        for {
          storeIds <- cud.adds.map(_.map(_.storeId).distinct)
          itemIds  <- cud.adds.map(_.distinct)
        } yield {
          // 1. Get details for stores and items from cassie
          val storesF = cassie.getStores(storeIds, Seq(Name, ItemTypes, Address, Avatar, Contacts))
          val itemsF  = cassie.getCatalogueItems(itemIds, CatalogeItemDetailType.Summary)

          val successFF =
            for {
              stores <- storesF
              items  <- itemsF
            } yield {
              val jsonItems = items.flatMap(CatalogueItem.asJsonItem(_))
              val grpdItems = jsonItems.groupBy(_.itemId.storeId.stuid)  // 2. group by items based on stores

              val bucketStores = stores.map { store =>                   // 3. create bucket stores with corrspding items
                BucketStore(
                  storeId        = store.storeId,
                  storeType      = store.storeType,
                  info           = store.info,
                  catalogueItems = grpdItems.get(store.storeId.stuid)
                )
              }

              bucketDatastore.insertBucketStores(userId, bucketStores) // 4. insert stores and items into storage
                .andThen {
                  case Failure(NonFatal(ex)) =>
                    log.error(ex, "Caught Error {} while inserting bucket stores for userId = {} and itemIds = [{}]",
                                  ex.getMessage,
                                  userId.uuid,
                                  bucketStores
                                    .flatMap(_.catalogueItems).flatMap(_.map(_.itemId))
                                    .map(id => s"${id.storeId.stuid}.${id.cuid}").mkString(", ")
                             )
                }
            }

          successFF.as[Future[Future[Boolean]]].flatMap(identity)
        }

      successOF getOrElse(Future.successful(true)) pipeTo sender()     // 5. send back the result

  }

}

object BucketSupervisor {
  def props = Props(classOf[BucketSupervisor])
}