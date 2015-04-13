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


class ShopPlanDestinations(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanDestinations, Destination] with ShopPlanConnector {

  override def tableName = "shopplan_destinations"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object dtuid extends LongColumn(this) with PrimaryKey[Long]

  // address
  object lat extends OptionalDoubleColumn(this)
  object lng extends OptionalDoubleColumn(this)
  object addressTitle extends OptionalStringColumn(this)
  object addressShort extends OptionalStringColumn(this)
  object addressFull extends OptionalStringColumn(this)
  object pincode extends OptionalStringColumn(this)
  object country extends OptionalStringColumn(this)
  object city extends OptionalStringColumn(this)

  // numShops
  object numShops extends OptionalIntColumn(this)


  override def fromRow(row: Row) = {

    val createdBy   = UserId(uuid = uuid(row))
    val shopplanId  = ShopPlanId(createdBy = createdBy, suid = suid(row))

    // creating address
    val gpsLoc = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    var addressO = gpsLoc.map(v => PostalAddress(gpsLoc = v.some))
    addressO = addressTitle(row).map {t => addressO.getOrElse(PostalAddress()).copy(title =    t.some) }
    addressO = addressShort(row).map {s => addressO.getOrElse(PostalAddress()).copy(short =    s.some) }
    addressO = addressFull(row) .map {f => addressO.getOrElse(PostalAddress()).copy(full =     f.some) }
    addressO = pincode(row)     .map {p => addressO.getOrElse(PostalAddress()).copy(pincode =  p.some) }
    addressO = country(row)     .map {c => addressO.getOrElse(PostalAddress()).copy(country =  c.some) }
    addressO = city(row)        .map {c => addressO.getOrElse(PostalAddress()).copy(city =     c.some) }


    Destination(
      destId    = DestinationId(shopplanId = shopplanId, dtuid = dtuid(row)),
      address   = addressO.getOrElse(PostalAddress()),
      numShops  = numShops(row)
    )
  }


  /**
   *
   */
  def insertDestination(destination: Destination) =
    insert
      .value(_.uuid,            destination.destId.shopplanId.createdBy.uuid)
      .value(_.suid,            destination.destId.shopplanId.suid)
      .value(_.dtuid,           destination.destId.dtuid)
      .value(_.lat,             destination.address.gpsLoc.map(_.lat))
      .value(_.lng,             destination.address.gpsLoc.map(_.lng))
      .value(_.addressTitle,    destination.address.title)
      .value(_.addressShort,    destination.address.short)
      .value(_.pincode,         destination.address.pincode)
      .value(_.country,         destination.address.country)
      .value(_.city,            destination.address.city)
      .value(_.numShops,        destination.numShops)


 /**
   *
   */
  def getDestinationsBy(userId: UserId) = select.where(_.uuid eqs userId.uuid)


 /**
   *
   */
  def getDestinationsBy(shopplanId: ShopPlanId) =
    select.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


  /**
   *
   */
  def updateDestinationBy(destination: Destination) =
    update.where( _.uuid         eqs     destination.destId.shopplanId.createdBy.uuid)
          .and(   _.suid         eqs     destination.destId.shopplanId.suid)
          .and(   _.dtuid        eqs     destination.destId.dtuid)

          .modify(_.lat          setTo   destination.address.gpsLoc.map(_.lat))
          .and(   _.lng          setTo   destination.address.gpsLoc.map(_.lng))
          .and(   _.addressTitle setTo   destination.address.title)
          .and(   _.addressShort setTo   destination.address.short)
          .and(   _.pincode      setTo   destination.address.pincode)
          .and(   _.country      setTo   destination.address.country)
          .and(   _.city         setTo   destination.address.city)
          .and(   _.numShops     setTo   destination.numShops)



  /**
   *
   */
  def deleteDestinationsBy(shopplanId: ShopPlanId) =
    delete.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


 /**
   *
   */
  def deleteDestinationsBy(destId: DestinationId) =
    delete.where(_.uuid   eqs destId.shopplanId.createdBy.uuid)
          .and(  _.suid   eqs destId.shopplanId.suid)
          .and(  _.dtuid  eqs destId.dtuid)

}