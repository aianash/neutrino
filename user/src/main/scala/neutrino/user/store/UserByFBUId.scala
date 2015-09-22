package neutrino.user.store

import scala.concurrent.Future
import scala.collection.mutable.{Seq => MutableSeq}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._
import com.websudos.phantom.builder.query.CQLQuery
import com.websudos.phantom.builder.clauses.UpdateClause

import neutrino.core.user.auth._
import neutrino.core.user._
import neutrino.user.UserSettings


sealed class UserByFBUId extends CassandraTable[ConcreteUserByFBUId, (UserId, FBAuth)] {

  override def tableName = "fb_auth_info"

  object fbUserId extends LongColumn(this) with PartitionKey[Long]
  object token extends StringColumn(this)
  object userId extends LongColumn(this)

  def fromRow(row: Row) = {
    val fbUserIdV = FBUserId(fbUserId(row))
    val tokenV    = FBAuthToken(token(row))
    val userIdV   = UserId(userId(row))
    // suffix V represents that string represents a variable

    (userIdV, FBAuth(fbUserIdV, tokenV))
  }

}


abstract class ConcreteUserByFBUId(val settings: UserSettings) extends UserByFBUId with UserConnector {

  def insertUserByFBUId(userId: UserId, info: FBAuth) = {
    insert.value(_.fbUserId, info.fbUserId.id)
      .value(_.token, info.token.value)
      .value(_.userId, userId.uuid)
      .future()
  }

  def getByFBUId(id: FBUserId): Future[Option[(UserId, FBAuth)]] = {
    select.where(_.fbUserId eqs id.id).one()
  }

  def updateUserByFBUId(userId: UserId, info: FBAuth) = {
    val updateWhere = update.where(_.fbUserId eqs info.fbUserId.id)
    var setTos = MutableSeq.empty[ConcreteUserByFBUId => UpdateClause.Condition]

    setTos = setTos :+ { (_ : ConcreteUserByFBUId).token setTo info.token.value}
    setTos = setTos :+ { (_ : ConcreteUserByFBUId).userId setTo userId.uuid}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).future()
      case MutableSeq(head, tail @ _*) =>
        (tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) }).future()
    }
  }

}