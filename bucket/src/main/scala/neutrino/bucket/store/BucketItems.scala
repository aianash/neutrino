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


class BucketItems(val settings: BucketSettings)
  extends CassandraTable[BucketItems, (BucketStore, SerializedCatalogueItem)]
  with BucketConnector {

  override def tableName = "bucket_items"

  object uuid extends LongColumn(this) with PartitionKey[Long]
  object stuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object cuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending

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
  object itemTypes extends SetColumn[BucketItems, (BucketStore, SerializedCatalogueItem), String](this)

  // serializer identifiers
  object sid extends StringColumn(this)
  object stype extends StringColumn(this)
  object stream extends BlobColumn(this)


  override def fromRow(row: Row) = {
    val gpsLoc = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    var addressO = gpsLoc.map(v => PostalAddress(gpsLoc = v.some))
    addressO = addressTitle(row).map {t => addressO.getOrElse(PostalAddress()).copy(title   =  t.some) }
    addressO = addressShort(row).map {s => addressO.getOrElse(PostalAddress()).copy(short   =  s.some) }
    addressO = addressFull(row) .map {f => addressO.getOrElse(PostalAddress()).copy(full    =  f.some) }
    addressO = pincode(row)     .map {p => addressO.getOrElse(PostalAddress()).copy(pincode =  p.some) }
    addressO = country(row)     .map {c => addressO.getOrElse(PostalAddress()).copy(country =  c.some) }
    addressO = city(row)        .map {c => addressO.getOrElse(PostalAddress()).copy(city    =  c.some) }

    val storeId = StoreId(stuid = stuid(row))

    var storeNameO = StoreName().some
    storeNameO = fullname(row).flatMap(f => storeNameO.map(_.copy(full = f.some)))
    storeNameO = handle(row)  .flatMap(h => storeNameO.map(_.copy(handle = h.some)))


    val store = BucketStore(
      storeId   = storeId,
      name      = storeNameO,
      address   = addressO,
      itemTypes = itemTypes(row).flatMap(name => ItemType.valueOf(name)).some
    )


    val serializerType  = SerializerType.valueOf(stype(row)).getOrElse(SerializerType.Msgpck)

    val item = SerializedCatalogueItem(
      itemId       = CatalogueItemId(storeId = storeId, cuid = cuid(row)),
      serializerId = SerializerId(sid = sid(row), stype = serializerType),
      stream       = stream(row)
    )

    (store, item)
  }

  def insertStoreItems(userId: UserId, stores: Seq[BucketStore]) = {
    val batch = BatchStatement()

    stores.foreach { store =>
      val partialQ =
        insert
          .value(_.uuid,            userId.uuid)
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


      store.catalogueItems.toSeq.flatten foreach { item =>
        val insertQ =
          partialQ
            .value(_.stuid,   item.itemId.storeId.stuid)
            .value(_.cuid,    item.itemId.cuid)
            .value(_.sid,     item.serializerId.sid)
            .value(_.stype,   item.serializerId.stype.name)
            .value(_.stream,  item.stream)

        batch add insertQ
      }
    }

    batch
  }


  def getBucketItemsBy(userId: UserId, fields: Seq[BucketStoreField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow)

    select.where(_.uuid eqs userId.uuid)
  }



  ////////////////////////////// Private methods ///////////////////////////////

  private def fieldToSelectors(fields: Seq[BucketStoreField]) = {
    fields.flatMap {
      case BucketStoreField.Name            => Seq("fullname", "handle")
      case BucketStoreField.Address         => Seq("lat", "lng", "addressTitle", "addressShort", "pincode", "country", "city")
      case BucketStoreField.ItemTypes       => Seq("itemTypes")
      case _                                => Seq.empty[String]
    } ++ Seq("uuid", "stuid", "cuid", "sid", "stype", "stream")
  }

}

