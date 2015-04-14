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

class ShopPlanInvites(val settings: ShopPlanSettings)
  extends CassandraTable[ShopPlanInvites, Invite] with ShopPlanConnector {

  override def tableName = "shopplan_invites"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]
  object suid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending
  object fruid extends LongColumn(this) with PrimaryKey[Long]

  // name
  object firstname extends OptionalStringColumn(this)
  object lastname extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  // avatar
  object small extends OptionalStringColumn(this)
  object medium extends OptionalStringColumn(this)
  object large extends OptionalStringColumn(this)

  // invite status
  object invitestatus extends OptionalStringColumn(this)


  override def fromRow(row: Row) = {
    val name = UserName(
        first   = firstname(row),
        last    = lastname(row),
        handle  = handle(row)
      )

    val avatar = UserAvatar(
        small   = small(row),
        medium  = medium(row),
        large   = large(row)
      )

    Invite(
      friendId      = UserId(uuid = fruid(row)),
      shopplanId    = ShopPlanId(createdBy = UserId(uuid = uuid(row)), suid = suid(row)),
      name          = name.some,
      avatar        = avatar.some,
      inviteStatus  = invitestatus(row).flatMap(InviteStatus.valueOf(_))
    )
  }


  def insertInvite(invite: Invite) =
    insert
      .value(_.uuid,          invite.shopplanId.createdBy.uuid)
      .value(_.suid,          invite.shopplanId.suid)
      .value(_.fruid,         invite.friendId.uuid)
      .value(_.firstname,     invite.name.flatMap(_.first))
      .value(_.lastname,      invite.name.flatMap(_.last))
      .value(_.handle,        invite.name.flatMap(_.handle))
      .value(_.small,         invite.avatar.flatMap(_.small))
      .value(_.medium,        invite.avatar.flatMap(_.medium))
      .value(_.large,         invite.avatar.flatMap(_.large))
      .value(_.invitestatus,  invite.inviteStatus.map(_.name))


  /**
   *
   */
  def getInvitesBy(userId: UserId) = select.where(_.uuid eqs userId.uuid)


  /**
   *
   */
  def getInvitesBy(shopplanId: ShopPlanId) =
    select.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


  /**
   *
   */
  def deleteInvitesBy(shopplanId: ShopPlanId) =
    delete.where(_.uuid eqs shopplanId.createdBy.uuid)
          .and(  _.suid eqs shopplanId.suid)


  /**
   *
   */
  def deleteInviteBy(shopplanId: ShopPlanId, friendId: UserId) =
    delete.where(_.uuid   eqs shopplanId.createdBy.uuid)
          .and(  _.suid   eqs shopplanId.suid)
          .and(  _.fruid  eqs friendId.uuid)
}
