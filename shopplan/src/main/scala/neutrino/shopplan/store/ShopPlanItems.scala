package neutrino.shopplan.store

import java.nio.ByteBuffer

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context
import com.websudos.phantom.query.SelectQuery

import neutrino.shopplan.ShopPlanSettings

import com.datastax.driver.core.querybuilder.QueryBuilder

import goshoplane.commons.core.factories._
import goshoplane.commons.catalogue.CatalogueItem




class ShopPlanItems(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanItems, JsonCatalogueItem]
  with ShopPlanConnector {

  override def tableName = "shopplan_items"

  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object stuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object cuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending

  // serializer identifiers
  object versionId extends StringColumn(this)
  object json extends StringColumn(this)


  override def fromRow(row: Row) =
    JsonCatalogueItem(
      itemId    = CatalogueItemId(storeId = StoreId(stuid = stuid(row)), cuid = cuid(row)),
      versionId = versionId(row),
      json      = json(row)
    )


  def insertItem(shopplanId: ShopPlanId, item: JsonCatalogueItem) =
    insert
      .value(_.uuid,      shopplanId.createdBy.uuid)
      .value(_.suid,      shopplanId.suid)
      .value(_.stuid,     item.itemId.storeId.stuid)
      .value(_.cuid,      item.itemId.cuid)
      .value(_.versionId, item.versionId)
      .value(_.json,      item.json)


  def getItemsBy(shopplanId: ShopPlanId) =
    select
      .where(_.uuid eqs shopplanId.createdBy.uuid)
      .and(  _.suid eqs shopplanId.suid)


  def deleteItemsBy(shopplanId: ShopPlanId) =
    delete
      .where(_.uuid eqs shopplanId.createdBy.uuid)
      .and(  _.suid eqs shopplanId.suid)
}