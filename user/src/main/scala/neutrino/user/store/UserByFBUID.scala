package neutrino.user.store

import scala.collection.mutable.{Seq => MutableSeq}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.Implicits._

import com.datastax.driver.core.querybuilder.Assignment

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import neutrino.user.UserSettings

class UserByFBUID(val settings: UserSettings)
  extends CassandraTable[UserByFBUID, (UserId, FacebookInfo)] with UserConnector {

  override def tableName = "users_by_fbuid"

  // ids
  object fbuid extends LongColumn(this) with PartitionKey[Long]
  object uuid extends LongColumn(this)
  object fbtoken extends OptionalStringColumn(this)


  def fromRow(row: Row) =
    (UserId(uuid(row)), FacebookInfo(userId = UserId(fbuid(row)), token = fbtoken(row)))


  def insertFacebookInfo(userId: UserId, info: FacebookInfo) =
    insert
      .value(_.fbuid,         info.userId.uuid)
      .value(_.uuid,          userId.uuid)
      .value(_.fbtoken,       info.token)


  def getUserIdBy(fbUserId: UserId) = select.where(_.fbuid eqs fbUserId.uuid)


  def updateFacebookInfo(userId: UserId, info: FacebookInfo) = {
    var updateQ =
      update.where( _.fbuid eqs info.userId.uuid)
            .modify(_.uuid setTo userId.uuid)

    info.token.foreach { token =>
      updateQ = updateQ.and(_.fbtoken setTo token.some)
    }

    updateQ
  }

}