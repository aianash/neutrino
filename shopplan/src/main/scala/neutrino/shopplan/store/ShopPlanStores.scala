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

import goshoplane.commons.core.factories._


class ShopPlanStores(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanStores, ShopPlanStore] with  ShopPlanConnector {

  override def tableName = "shopplan_stores"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object stuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending

  object storeType extends StringColumn(this)
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

  // avatar
  object small extends OptionalStringColumn(this)
  object medium extends OptionalStringColumn(this)
  object large extends OptionalStringColumn(this)

  object email extends OptionalStringColumn(this)
  object phoneNums extends SetColumn[ShopPlanStores, ShopPlanStore, String](this)

  // cuids of the shopplan stores
  object cuids extends SetColumn[ShopPlanStores, ShopPlanStore, Long](this)

  override def fromRow(row: Row) = {

    val storeId       = StoreId(stuid = stuid(row))
    val gpsLoc        = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    val addressO      = Common.address(gpsLoc, addressTitle(row), addressShort(row), addressFull(row), pincode(row), country(row), city(row))
    val nameO         = Common.storeName(fullname(row), handle(row))
    val avatarO       = Common.storeAvatar(small(row), medium(row), large(row))
    val phoneO        = Common.phoneContact(phoneNums(row).toSeq)
    val itemTypesO    = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some
    val itemIds       = cuids.optional(row).map(_.map(cuid => CatalogueItemId(storeId = storeId, cuid = cuid)).toSeq)
    val destinationId =
      DestinationId(
        shopplanId = ShopPlanId(suid = suid(row), createdBy = UserId(uuid = uuid(row))),
        dtuid      = dtuid(row)
      )

    ShopPlanStore(
      storeId   = storeId,
      destId    = destinationId,
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = Common.storeInfo(nameO, itemTypesO, addressO, avatarO, email(row), phoneO).getOrElse(StoreInfo()),
      itemIds   = itemIds
    )

  }


  /**
   * Insert ShopPlan store into the table
   */
  def insertStore(store: ShopPlanStore) =
    insert
      .value(_.uuid,          store.destId.shopplanId.createdBy.uuid)
      .value(_.suid,          store.destId.shopplanId.suid)
      .value(_.stuid,         store.storeId.stuid)
      .value(_.storeType,     store.storeType.name)
      .value(_.dtuid,         store.destId.dtuid)
      .value(_.fullname,      store.info.name.flatMap(_.full))
      .value(_.handle,        store.info.name.flatMap(_.handle))
      .value(_.itemTypes,     store.info.itemTypes.map(_.map(_.name).toSet).getOrElse(Set.empty[String]))
      .value(_.lat,           store.info.address.flatMap(_.gpsLoc.map(_.lat)))
      .value(_.lng,           store.info.address.flatMap(_.gpsLoc.map(_.lng)))
      .value(_.addressTitle,  store.info.address.flatMap(_.title))
      .value(_.addressShort,  store.info.address.flatMap(_.short))
      .value(_.addressFull,   store.info.address.flatMap(_.full))
      .value(_.pincode,       store.info.address.flatMap(_.pincode))
      .value(_.country,       store.info.address.flatMap(_.country))
      .value(_.city,          store.info.address.flatMap(_.city))
      .value(_.small,         store.info.avatar.flatMap(_.small))
      .value(_.medium,        store.info.avatar.flatMap(_.medium))
      .value(_.large,         store.info.avatar.flatMap(_.large))
      .value(_.email,         store.info.email)
      .value(_.phoneNums,     store.info.phone.map(_.numbers.toSet).getOrElse(Set.empty[String]))
      .value(_.cuids,         store.itemIds.map(_.map(_.cuid).toSet).getOrElse(Set.empty[Long]))



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


  import ShopPlanStoreField._

  private def fieldToSelectors(fields: Seq[ShopPlanStoreField]) = {
    fields.flatMap {
      case Name               => Seq("fullname", "handle")
      case Address            => Seq("lat", "lng", "addressTitle", "addressShort", "addressFull", "pincode", "country", "city")
      case ItemTypes          => Seq("itemTypes")
      case Avatar             => Seq("small", "medium", "large")
      case Contacts           => Seq("email", "phoneNums")
      case CatalogueItemIds   => Seq("cuids")
      case _                  => Seq.empty[String]
    } ++ Seq("uuid", "stuid", "suid", "storeType", "dtuid")
  }

}