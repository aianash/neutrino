package neutrino.user.store

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.Implicits._

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import neutrino.user.UserSettings

class UserFriends(val settings: UserSettings)
  extends CassandraTable[UserFriends, Friend] with UserConnector {

  override def tableName = "user_friends"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]
  object fruid extends LongColumn(this) with PrimaryKey[Long]

  // name
  object first extends OptionalStringColumn(this)
  object last extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  // avatar
  object avatarsmall extends OptionalStringColumn(this)
  object avatarmedium extends OptionalStringColumn(this)
  object avatarlarge extends OptionalStringColumn(this)


  def fromRow(row: Row) = {

    val usernameO = UserName(
      first   = first(row),
      last    = last(row),
      handle  = handle(row)).some

    val avatarO = UserAvatar(
      small  = avatarsmall(row),
      medium = avatarmedium(row),
      large  = avatarlarge(row)).some

    Friend(
      id           = UserId(uuid = fruid(row)),
      name         = usernameO,
      avatar       = avatarO)

  }


  def insertFriends(userId: UserId, friends: Seq[Friend]) = {
    val batch = BatchStatement()

    friends foreach { friend =>
      batch add
        insert.value(_.uuid,          userId.uuid)
              .value(_.fruid,         friend.id.uuid)
              .value(_.first,         friend.name.flatMap(_.first))
              .value(_.last,          friend.name.flatMap(_.last))
              .value(_.handle,        friend.name.flatMap(_.handle))
              .value(_.avatarsmall,   friend.avatar.flatMap(_.small))
              .value(_.avatarmedium,  friend.avatar.flatMap(_.medium))
              .value(_.avatarlarge,   friend.avatar.flatMap(_.large))
    }

    batch
  }


  def getFriendsBy(userId: UserId) = select.where(_.uuid eqs userId.uuid)
}