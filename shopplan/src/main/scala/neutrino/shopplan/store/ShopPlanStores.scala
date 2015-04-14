package neutrino.shopplan.store

import java.nio.ByteBuffer

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

import neutrino.shopplan.ShopPlanSettings

import com.goshoplane.neutrino.shopplan._
import com.goshoplane.common._

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery

import com.datastax.driver.core.querybuilder.QueryBuilder

import play.api.libs.json._


class ShopPlanStores(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanStores, ShopPlanStore] with  ShopPlanConnector {

  override def tableName = "shopplan_stores"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object stuid extends LongColumn(this) with PrimaryKey[Long]

  object dtuid extends LongColumn(this)

  // name
  object fullname extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  // address
  object lat extends OptionalDoubleColumn(this)
  object lng extends OptionalDoubleColumn(this)
  object addressTitle extends OptionalStringColumn(this)
  object addressShort extends OptionalStringColumn(this)
  object addressFull extends OptionalStringColumn(this)
  object pincode extends OptionalStringColumn(this)
  object country extends OptionalStringColumn(this)
  object city extends OptionalStringColumn(this)

  // item types
  object itemTypes extends SetColumn[ShopPlanStores, ShopPlanStore, String](this)

  // catalogue item details
  object catalogueItems extends CatalogueItemsJsonSetColumn[ShopPlanStores, ShopPlanStore](this)



  override def fromRow(row: Row) = {

    // creating address
    val gpsLoc = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    var addressO = gpsLoc.map(v => PostalAddress(gpsLoc = v.some))
    addressO = addressTitle(row).map {t => addressO.getOrElse(PostalAddress()).copy(title   =  t.some) }
    addressO = addressShort(row).map {s => addressO.getOrElse(PostalAddress()).copy(short   =  s.some) }
    addressO = addressFull(row) .map {f => addressO.getOrElse(PostalAddress()).copy(full    =  f.some) }
    addressO = pincode(row)     .map {p => addressO.getOrElse(PostalAddress()).copy(pincode =  p.some) }
    addressO = country(row)     .map {c => addressO.getOrElse(PostalAddress()).copy(country =  c.some) }
    addressO = city(row)        .map {c => addressO.getOrElse(PostalAddress()).copy(city    =  c.some) }

    var storeNameO = StoreName().some
    storeNameO = fullname(row).flatMap(f => storeNameO.map(_.copy(full = f.some)))
    storeNameO = handle(row)  .flatMap(h => storeNameO.map(_.copy(handle = h.some)))

    val destinationId =
      DestinationId(
        shopplanId = ShopPlanId(suid = suid(row), createdBy = UserId(uuid = uuid(row))),
        dtuid      = dtuid(row)
      )

    ShopPlanStore(
      storeId        = StoreId(stuid = stuid(row)),
      destId         = destinationId,
      name           = storeNameO,
      address        = addressO,
      itemTypes      = itemTypes(row).flatMap(name => ItemType.valueOf(name)).some,
      catalogueItems = catalogueItems(row).some
    )

  }


  /**
   * Insert ShopPlan store into the table
   */
  def insertStore(store: ShopPlanStore) =
    insert
      .value(_.uuid,            store.destId.shopplanId.createdBy.uuid)
      .value(_.suid,            store.destId.shopplanId.suid)
      .value(_.stuid,           store.storeId.stuid)
      .value(_.dtuid,           store.destId.dtuid)
      .value(_.fullname,        store.name.flatMap(_.full))
      .value(_.handle,          store.name.flatMap(_.handle))
      .value(_.lat,             store.address.flatMap(_.gpsLoc.map(_.lat)))
      .value(_.lng,             store.address.flatMap(_.gpsLoc.map(_.lng)))
      .value(_.addressTitle,    store.address.flatMap(_.title))
      .value(_.addressShort,    store.address.flatMap(_.short))
      .value(_.pincode,         store.address.flatMap(_.pincode))
      .value(_.country,         store.address.flatMap(_.country))
      .value(_.city,            store.address.flatMap(_.city))
      .value(_.itemTypes,       store.itemTypes.map(_.map(_.name).toSet).getOrElse(Set.empty[String])) // [IMP] Check it camel case only
      .value(_.catalogueItems,  store.catalogueItems.map(_.toSet).getOrElse(Set.empty[SerializedCatalogueItem]))


  /**
   *
   */
  def getStoresBy(userId: UserId, fields: Seq[ShopPlanStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow)

    select.where(_.uuid eqs userId.uuid)
  }


  /**
   *
   */
  def getStoresBy(shopplanId: ShopPlanId, fields: Seq[ShopPlanStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow)

    select.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)
  }


  /**
   *
   */
  def deleteStoresBy(shopplanId: ShopPlanId) =
    delete.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


  /**
   *
   */
  def deleteStoreBy(shopplanId: ShopPlanId, storeId: StoreId) =
    delete.where(_.uuid  eqs shopplanId.createdBy.uuid)
          .and(  _.suid  eqs shopplanId.suid)
          .and(  _.stuid eqs storeId.stuid)



  //////////////////////////// Private methods ////////////////////////////


  private def fieldToSelectors(fields: Seq[ShopPlanStoreField]) = {
    fields.flatMap {
      case ShopPlanStoreField.Name            => Seq("fullname", "handle")
      case ShopPlanStoreField.Address         => Seq("lat", "lng", "addressTitle", "addressShort", "pincode", "country", "city")
      case ShopPlanStoreField.ItemTypes       => Seq("itemTypes")
      case ShopPlanStoreField.CatalogueItems  => Seq("catalogueItems")
      case _                                  => Seq.empty[String]
    } ++ Seq("uuid", "stuid", "suid", "dtuid")
  }

}