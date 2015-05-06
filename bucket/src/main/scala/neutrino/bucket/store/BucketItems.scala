package neutrino.bucket.store

import java.nio.ByteBuffer

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context
import com.websudos.phantom.query.SelectQuery

import com.datastax.driver.core.querybuilder.QueryBuilder

import neutrino.bucket.BucketSettings

import goshoplane.commons.core.factories._


class BucketItems(val settings: BucketSettings)
  extends CassandraTable[BucketItems, (BucketStore, JsonCatalogueItem)]
  with BucketConnector {

  override def tableName = "bucket_items"

  object uuid extends LongColumn(this) with PartitionKey[Long]
  object stuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object cuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object storeType extends StringColumn(this)

  // name
  object fullname extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  // item types
  object itemTypes extends SetColumn[BucketItems, (BucketStore, JsonCatalogueItem), String](this)

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
  object phoneNums extends SetColumn[BucketItems, (BucketStore, JsonCatalogueItem), String](this)

  // serializer identifiers
  object versionId extends StringColumn(this)
  object json extends StringColumn(this)


  override def fromRow(row: Row) = {
    val storeId    = StoreId(stuid = stuid(row))
    val gpsLoc     = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    val addressO   = Common.address(gpsLoc, addressTitle(row), addressShort(row), addressFull(row), pincode(row), country(row), city(row))
    val nameO      = Common.storeName(fullname(row), handle(row))
    val avatarO    = Common.storeAvatar(small(row), medium(row), large(row))
    val itemTypesO = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some

    val store = BucketStore(
      storeId   = storeId,
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = Common.storeInfo(nameO, itemTypesO, addressO, avatarO, email(row), None).getOrElse(StoreInfo()) // [NOTE] intentionally left phone number
    )

    val item = JsonCatalogueItem(
      itemId       = CatalogueItemId(storeId = storeId, cuid = cuid(row)),
      versionId    = versionId(row),
      json         = json.optional(row).getOrElse("") // If full detail is not
                                                      // required then send empty string
    )

    (store, item)
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

    val storeId = StoreId(stuid = stuid(row))

    val store = BucketStore(
      storeId   = storeId,
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = info
    )

    val jsonText = fields.find(_ == CatalogueItems).map { _ => json(row) } getOrElse("")

    val item = JsonCatalogueItem(
      itemId       = CatalogueItemId(storeId = storeId, cuid = cuid(row)),
      versionId    = versionId(row),
      json         = jsonText
    )

    (store, item)
  }


  def insertStoreItems(userId: UserId, stores: Seq[BucketStore]) = {
    val batch = BatchStatement()

    stores.foreach { store =>
      store.catalogueItems.toSeq.flatten foreach { item =>
        val insertQ =
          insert
            .value(_.uuid,          userId.uuid)
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
            .value(_.stuid,     item.itemId.storeId.stuid)
            .value(_.cuid,      item.itemId.cuid)
            .value(_.versionId, item.versionId)
            .value(_.json,      item.json)

        batch add insertQ
      }
    }

    batch
  }


  def getBucketItemsBy(userId: UserId, fields: Seq[BucketStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow(fields))

    select.where(_.uuid eqs userId.uuid)
  }


  def getBucketItemsBy(userId: UserId, storeId: StoreId, fields: Seq[BucketStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow(fields))

    select.where(_.uuid  eqs userId.uuid)
          .and(  _.stuid eqs storeId.stuid)
  }


  def deleteBucketItemsBy(userId: UserId, storeId: StoreId) =
    delete
      .where(_.uuid  eqs userId.uuid)
      .and(  _.stuid eqs storeId.stuid)


  ////////////////////////////// Private methods ///////////////////////////////

  import BucketStoreField._

  private def fieldToSelectors(fields: Seq[BucketStoreField]) =
    fields.flatMap {
      case Name             => Seq("fullname", "handle")
      case Address          => Seq("lat", "lng", "addressTitle", "addressShort", "addressFull", "pincode", "country", "city")
      case ItemTypes        => Seq("itemTypes")
      case Avatar           => Seq("small", "medium", "large")
      case Contacts         => Seq("email", "phoneNums")
      case CatalogueItems   => Seq("json")
      case _                => Seq.empty[String]
    } ++ Seq("uuid", "stuid", "storeType", "cuid", "versionId")    // always all ids, since if this table is accessed
                                                                   // then definately for some catalogue information
                                                                   // i.e. atleast ids

}

