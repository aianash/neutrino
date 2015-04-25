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

/**
 * ShopPlan keyspace for
 *
 */
class ShopPlanMeta(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanMeta, ShopPlan] with ShopPlanConnector {

  override def tableName = "shopplan_meta"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object cyuid extends LongColumn(this) // This field is same as uuid in not an invited plan, otherwise different

  // title
  object title extends OptionalStringColumn(this)

  // is invitation
  object isInvitation extends BooleanColumn(this)

  override def fromRow(row: Row) =
    ShopPlan(
      shopplanId   = ShopPlanId(createdBy = UserId(uuid = cyuid(row)), suid = suid(row)),
      title        = title(row),
      isInvitation = isInvitation(row)
    )


  /**
   *
   */
  def insertShopPlan(userId: UserId, shopPlan: ShopPlan) =
    insert
      .value(_.uuid,          userId.uuid)
      .value(_.cyuid,         shopPlan.shopplanId.createdBy.uuid)
      .value(_.title,         shopPlan.title)
      .value(_.isInvitation,  shopPlan.isInvitation)


  /**
   *
   */
  def getShopPlanBy(userId: UserId) = select.where(_.uuid eqs userId.uuid)


  /**
   *
   */
  def getShopPlanBy(shopplanId: ShopPlanId) = select.where(_.uuid eqs shopplanId.createdBy.uuid)


  /**
   *
   */
  def updateTitleBy(shopplanId: ShopPlanId, title: String) =
    update.where( _.uuid  eqs   shopplanId.createdBy.uuid)
          .and(   _.suid  eqs   shopplanId.suid)
          .modify(_.title setTo title.some)


  /**
   *
   */
  def deleteShopPlansBy(shopplanId: ShopPlanId) =
    delete.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


  /**
   *
   */
  def deleteShopPlanBy(userId: UserId, shopplanId: ShopPlanId) =
    delete.where(_.uuid eqs userId.uuid)
          .and(  _.suid eqs shopplanId.suid)

}


class ShopPlanMetaByInvitation(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanMetaByInvitation, ShopPlan] with ShopPlanConnector {

  override def tableName = "shopplan_meta_by_invites"

  object uuid extends LongColumn(this) with PartitionKey[Long]
  object isInvitation extends BooleanColumn(this) with PrimaryKey[Boolean] //with ClusteringOrder[Long] with Ascending
  object suid extends LongColumn(this) with PrimaryKey[Long]

  object cyuid extends LongColumn(this)

  // title
  object title extends OptionalStringColumn(this)

  override def fromRow(row: Row) =
    ShopPlan(
      shopplanId   = ShopPlanId(createdBy = UserId(uuid = cyuid(row)), suid = suid(row)),
      title        = title(row),
      isInvitation = isInvitation(row)
    )


  /**
   *
   */
  def insertShopPlan(userId: UserId, shopPlan: ShopPlan) =
    insert
      .value(_.uuid,          userId.uuid)
      .value(_.cyuid,         shopPlan.shopplanId.createdBy.uuid)
      .value(_.title,         shopPlan.title)
      .value(_.isInvitation,  shopPlan.isInvitation)


  /**
   *
   */
  def getOwnShopPlansBy(userId: UserId) =
    select.where(_.uuid         eqs userId.uuid)
          .and(  _.isInvitation eqs false)


  /**
   *
   */
  def getInvitedShopPlansBy(userId: UserId) =
    select.where(_.uuid         eqs userId.uuid)
          .and(  _.isInvitation eqs true)


  /**
   *
   */
  def updateTitleBy(shopplanId: ShopPlanId, title: String) =
    update.where( _.uuid  eqs     shopplanId.createdBy.uuid)
          .and(   _.suid  eqs     shopplanId.suid)
          .modify(_.title setTo   title.some)


  /**
   *
   */
  def deleteShopPlansBy(shopplanId: ShopPlanId) =
    delete.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


  /**
   *
   */
  def deleteInvitedShopPlanBy(userId: UserId, shopplanId: ShopPlanId) =
    delete.where(_.uuid         eqs userId.uuid)
          .and(  _.isInvitation eqs true)
          .and(  _.suid         eqs shopplanId.suid)
}
