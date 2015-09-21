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


sealed class UserAuthInfo extends CassandraTable[ConcreteUserAuthInfo, UserAuth] {

  override def tableName = "user_auth_info"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]

  // facebook auth info
  object fbUserId extends OptionalLongColumn(this)
  object fbAuthToken extends OptionalStringColumn(this)

  // google auth info
  object googleUserId extends OptionalLongColumn(this)
  object googleAuthToken extends OptionalStringColumn(this)

  def fromRow(row: Row) = {
    val userId           = UserId(uuid(row))
    val fbUserIdO        = fbUserId(row).map(FBUserId(_))
    val fbAuthTokenO     = fbAuthToken(row).map(FBAuthToken(_))
    val googleUserIdO    = googleUserId(row).map(GoogleUserId(_))
    val googleAuthTokenO = googleAuthToken(row).map(GoogleAuthToken(_))

    UserAuth(userId, fbUserIdO, fbAuthTokenO, googleUserIdO, googleAuthTokenO)
  }

}


abstract class ConcreteUserAuthInfo(val settings: UserSettings) extends UserAuthInfo with UserConnector {

  def insertUserAuthInfo(info: UserAuth) = {
    insert.value(_.uuid, info.userId.uuid)
      .value(_.fbUserId, info.fbUserId.map(_.id))
      .value(_.fbAuthToken, info.fbAuthToken.map(_.value))
      .value(_.googleUserId, info.googleUserId.map(_.id))
      .value(_.googleAuthToken, info.googleAuthToken.map(_.value))
  }

  def getByUserId(id: UserId): Future[Option[UserAuth]] = {
    select.where(_.uuid eqs id.uuid).one()
  }

  def updateUserAuthInfo(info: UserAuth) = {
    val updateWhere = update.where(_.uuid eqs info.userId.uuid)
    var setTos = MutableSeq.empty[ConcreteUserAuthInfo => UpdateClause.Condition]

    info.fbUserId.map(_.id)             .foreach { fid => setTos = setTos :+ { (_ : ConcreteUserAuthInfo).fbUserId setTo fid.some }}
    info.fbAuthToken.map(_.value)       .foreach { ft  => setTos = setTos :+ { (_ : ConcreteUserAuthInfo).fbAuthToken setTo ft.some }}
    info.googleUserId.map(_.id)         .foreach { gid => setTos = setTos :+ { (_ : ConcreteUserAuthInfo).googleUserId setTo gid.some }}
    info.googleAuthToken.map(_.value)   .foreach { gt  => setTos = setTos :+ { (_ : ConcreteUserAuthInfo).googleAuthToken setTo gt.some }}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) } some
    }
  }
}