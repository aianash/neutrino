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

class ShopPlanItems(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanItems, SerializedCatalogueItem]
  with ShopPlanConnector {

  override def tableName = "shopplan_items"

  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object stuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object cuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending

  // serializer identifiers
  object sid extends StringColumn(this)
  object stype extends StringColumn(this)
  object stream extends BlobColumn(this)


  override def fromRow(row: Row) = {
    val serializerType  = SerializerType.valueOf(stype(row)).getOrElse(SerializerType.Msgpck)

    SerializedCatalogueItem(
      itemId       = CatalogueItemId(storeId = StoreId(stuid = stuid(row)), cuid = cuid(row)),
      serializerId = SerializerId(sid = sid(row), stype = serializerType),
      stream       = stream(row)
    )
  }


  def insertItem(shopplanId: ShopPlanId, item: SerializedCatalogueItem) =
    insert
      .value(_.uuid,    shopplanId.createdBy.uuid)
      .value(_.suid,    shopplanId.suid)
      .value(_.stuid,   item.itemId.storeId.stuid)
      .value(_.cuid,    item.itemId.cuid)
      .value(_.sid,     item.serializerId.sid)
      .value(_.stype,   item.serializerId.stype.name)
      .value(_.stream,  item.stream)



}