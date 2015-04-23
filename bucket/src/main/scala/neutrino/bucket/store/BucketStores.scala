package neutrino.bucket.store

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._


import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context

import neutrino.bucket.BucketSettings
import com.websudos.phantom.query.SelectQuery

import com.datastax.driver.core.querybuilder.QueryBuilder

import goshoplane.commons.core.factories._

class BucketStores(val settings: BucketSettings)
  extends CassandraTable[BucketStores, BucketStore]
  with BucketConnector {

  override def tableName = "bucket_stores"

  object uuid extends LongColumn(this) with PartitionKey[Long]
  object stuid extends LongColumn(this) with PrimaryKey[Long]
  object storeType extends StringColumn(this)

  // name
  object fullname extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  object itemTypes extends SetColumn[BucketStores, BucketStore, String](this)

  // address
  object lat extends OptionalDoubleColumn(this)
  object lng extends OptionalDoubleColumn(this)
  object addressTitle extends OptionalStringColumn(this)
  object addressShort extends OptionalStringColumn(this)
  object addressFull extends OptionalStringColumn(this)
  object pincode extends OptionalStringColumn(this)
  object country extends OptionalStringColumn(this)
  object city extends OptionalStringColumn(this)

  // avatar
  object small extends OptionalStringColumn(this)
  object medium extends OptionalStringColumn(this)
  object large extends OptionalStringColumn(this)

  object email extends OptionalStringColumn(this)



  override def fromRow(row: Row) = {
    val gpsLoc     = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    val addressO   = Common.address(gpsLoc, addressTitle(row), addressShort(row), addressFull(row), pincode(row), country(row), city(row))
    val nameO      = Common.storeName(fullname(row), handle(row))
    val avatarO    = Common.storeAvatar(small(row), medium(row), large(row))
    val itemTypesO = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some

    BucketStore(
      storeId   = StoreId(stuid = stuid(row)),
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = Common.storeInfo(nameO, itemTypesO, addressO, avatarO, email(row), None).getOrElse(StoreInfo()) // [NOTE] intentionally left phone number
    )
  }

  def insertStore(userId: UserId, store: BucketStore) =
    insert
      .value(_.uuid,          userId.uuid)
      .value(_.stuid,         store.storeId.stuid)
      .value(_.storeType,     store.storeType.name)
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


  def getBucketStoresBy(userId: UserId, fields: Seq[BucketStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow)

    select.where(_.uuid eqs userId.uuid)
  }



  ////////////////////////////// Private methods ///////////////////////////////

  private def fieldToSelectors(fields: Seq[BucketStoreField]) = {
    fields.flatMap {
      case BucketStoreField.Name            => Seq("fullname", "handle")
      case BucketStoreField.Address         => Seq("lat", "lng", "addressTitle", "addressShort", "addressFull", "pincode", "country", "city")
      case BucketStoreField.ItemTypes       => Seq("itemTypes")
      case _                                => Seq.empty[String]
    } ++ Seq("uuid", "stuid")
  }

}

