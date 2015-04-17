package neutrino.bucket.store

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import neutrino.bucket.BucketSettings

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._



sealed class BucketDatastore(val settings: BucketSettings)
  extends BucketConnector {

  object BucketItems extends BucketItems(settings)
  object BucketStores extends BucketStores(settings)

  def init()(implicit executor: ExecutionContext) {
    val creationF =
      for {
        _ <- BucketItems.create.future()
        _ <- BucketStores.create.future()
      } yield true

    Await.ready(creationF, 2 seconds)
  }


  def getBucketStores(userId: UserId, fields: Seq[BucketStoreField])(implicit executor: ExecutionContext) =
    fields.find(_ != BucketStoreField.CatalogueItems)
          .map { _ => BucketStores.getBucketStoresBy(userId, fields).fetch() }
          .getOrElse {
            for {
              storeItems <- BucketItems.getBucketItemsBy(userId, fields).fetch()
            } yield
              storeItems.foldLeft(MutableMap.empty[StoreId, BucketStore])({ (map, storeItem) =>
                val (store, item) = storeItem
                val newStore =
                  map.get(store.storeId)
                     .map(store => store.copy(catalogueItems = store.catalogueItems.map(_ + item)))
                     .getOrElse(store.copy(catalogueItems = Set(item).some))

                map += (store.storeId -> newStore)
              }).values.toSeq
          }


  def cudBucketStores(userId: UserId, cud: CUDBucket)(implicit executor: ExecutionContext) =
    cud.adds.map { stores =>
      val itemsF  = BucketItems .insertStoreItems(userId, stores).future().map(_ => true)
      val storesF = BucketStores.insertStores(userId, stores)    .future().map(_ => true)

      for {
        _       <- itemsF
        success <- storesF
      } yield success

    } getOrElse Future.successful(true)

}