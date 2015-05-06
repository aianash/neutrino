package neutrino.bucket.store

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context
import com.websudos.phantom.query.SelectQuery

import neutrino.bucket.BucketSettings

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
  object phoneNums extends SetColumn[BucketStores, BucketStore, String](this)



  override def fromRow(row: Row) = {
    val gpsLoc     = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    val addressO   = Common.address(gpsLoc, addressTitle(row), addressShort(row), addressFull(row), pincode(row), country(row), city(row))
    val nameO      = Common.storeName(fullname(row), handle(row))
    val avatarO    = Common.storeAvatar(small(row), medium(row), large(row))
    val phoneO     = None // Common.phoneContact(phoneNums(row).toSeq)
    val itemTypesO = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some

    BucketStore(
      storeId   = StoreId(stuid = stuid(row)),
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = Common.storeInfo(nameO, itemTypesO, addressO, avatarO, email(row), phoneO).getOrElse(StoreInfo())
    )
  }


  def fromRow(fields: Seq[BucketStoreField])(row: Row) = {
    import BucketStoreField._

    val info =
      fields.foldLeft(StoreInfo()) { (info, field) =>
        field match {
          case Name            => info.copy(name      = Common.storeName(fullname(row), handle(row)))
          case ItemTypes       => info.copy(itemTypes = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some)
          case Avatar          => info.copy(avatar    = Common.storeAvatar(small(row), medium(row), large(row)))
          case Contacts        => info.copy(phone     = Common.phoneContact(phoneNums(row).toSeq), email = email(row))
          case Address         =>
            val gpsLoc = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
            info.copy(address = Common.address(gpsLoc, addressTitle(row), addressShort(row), addressFull(row), pincode(row), country(row), city(row)))
          case _               => info
        }
      }

    BucketStore(
      storeId   = StoreId(stuid = stuid(row)),
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = info
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
      .value(_.phoneNums,     store.info.phone.map(_.numbers.toSet).getOrElse(Set.empty[String]))


  def getBucketStoresBy(userId: UserId, fields: Seq[BucketStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow(fields))

    select.where(_.uuid eqs userId.uuid)
  }


  def getBucketStoresBy(userId: UserId, storeIds: Seq[StoreId], fields: Seq[BucketStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow(fields))

    select.where(_.uuid eqs userId.uuid)
          .and(  _.stuid in storeIds.map(_.stuid).toList)
  }


  def deleteBucketStoreBy(userId: UserId, storeId: StoreId) =
    delete
      .where(_.uuid  eqs userId.uuid)
      .and(  _.stuid eqs storeId.stuid)


  ////////////////////////////// Private methods ///////////////////////////////

  private def fieldToSelectors(fields: Seq[BucketStoreField]) = {
    import BucketStoreField._

    fields.flatMap {
      case Name            => Seq("fullname", "handle")
      case Address         => Seq("lat", "lng", "addressTitle", "addressShort", "addressFull", "pincode", "country", "city")
      case ItemTypes       => Seq("itemTypes")
      case Avatar          => Seq("small", "medium", "large")
      case Contacts        => Seq("email", "phoneNums")
      case _               => Seq.empty[String]
    } ++ Seq("uuid", "stuid", "storeType")
  }

}

