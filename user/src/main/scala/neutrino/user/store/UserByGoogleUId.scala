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


sealed class UserByGoogleUId extends CassandraTable[ConcreteUserByGoogleUId, (UserId, GoogleAuth)] {

  override val tableName = "google_auth_info"

  object googleUserId extends LongColumn(this) with PartitionKey[Long]
  object token extends StringColumn(this)
  object userId extends LongColumn(this)

  def fromRow(row: Row) = {
    val googleUserIdV = GoogleUserId(googleUserId(row))
    val tokenV        = GoogleAuthToken(token(row))
    val userIdV       = UserId(userId(row))
    // suffix V represents that string represents a variable

    (userIdV, GoogleAuth(googleUserIdV, tokenV))
  }

}


abstract class ConcreteUserByGoogleUId(val settings: UserSettings) extends UserByGoogleUId with UserConnector {

  def insertUserByGoogleUId(userId: UserId, info: GoogleAuth) = {
    insert.value(_.googleUserId, info.googleUserId.id)
      .value(_.token, info.token.value)
      .value(_.userId, userId.uuid)
      .future()
  }

  def getByGoogleUId(id: GoogleUserId): Future[Option[(UserId, GoogleAuth)]] = {
    select.where(_.googleUserId eqs id.id).one()
  }

  def updateUserByGoogleUId(userId: UserId, info: GoogleAuth) = {
    val updateWhere = update.where(_.googleUserId eqs info.googleUserId.id)
    var setTos = MutableSeq.empty[ConcreteUserByGoogleUId => UpdateClause.Condition]

    setTos = setTos :+ { (_ : ConcreteUserByGoogleUId).token setTo info.token.value}
    setTos = setTos :+ { (_ : ConcreteUserByGoogleUId).userId setTo userId.uuid}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        (tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) }).future()
    }
  }

}