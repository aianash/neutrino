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

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context


sealed class BucketDatastore(val settings: BucketSettings)
  extends BucketConnector {

  import BucketStoreField._

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
    if(fields.forall(field => field != CatalogueItems && field != CatalogueItemIds))
      BucketStores.getBucketStoresBy(userId, fields).fetch()
    else {
      for {
        storeItems <- BucketItems.getBucketItemsBy(userId, fields).fetch()
      } yield
        storeItems.foldLeft(MutableMap.empty[StoreId, BucketStore])({ (map, storeItem) =>
          val (store, item) = storeItem
          val newStore =
            map.get(store.storeId)
               .map(store => store.copy(catalogueItems = store.catalogueItems.map(_ :+ item )))
               .getOrElse(store.copy(catalogueItems = Seq(item).some))

          map += (store.storeId -> newStore)
        }).values.toSeq
    }



  def getGivenBucketStores(userId: UserId, storeIds: Seq[StoreId], fields: Seq[BucketStoreField])(implicit executor: ExecutionContext) =
    if(fields.forall(field => field != CatalogueItems && field != CatalogueItemIds))
      BucketStores.getBucketStoresBy(userId, storeIds, fields).fetch()
    else {
      for {
        storeItems <- BucketItems.getBucketItemsBy(userId, storeIds, fields).fetch()
      } yield
        storeItems.foldLeft(MutableMap.empty[StoreId, BucketStore])({ (map, storeItem) =>
          val (store, item) = storeItem
          val newStore =
            map.get(store.storeId)
               .map(store => store.copy(catalogueItems = store.catalogueItems.map(_ :+ item )))
               .getOrElse(store.copy(catalogueItems = Seq(item).some))

          map += (store.storeId -> newStore)
        }).values.toSeq
    }


  def insertBucketStores(userId: UserId, stores: Seq[BucketStore])(implicit executor: ExecutionContext) = {
    val batch = BucketItems.insertStoreItems(userId, stores)
    stores foreach {store =>
      batch add BucketStores.insertStore(userId, store)
    }

    batch.future().map(_ => true)
  }

}