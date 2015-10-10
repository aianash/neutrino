package neutrino.auth.store

import scala.concurrent.Future
import scala.collection.mutable.{Seq => MutableSeq}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._
import com.websudos.phantom.builder.query.CQLQuery
import com.websudos.phantom.builder.clauses.UpdateClause

import neutrino.core.auth._
import neutrino.core.user._
import neutrino.auth.AuthSettings


sealed class GoogleAuthInfos extends CassandraTable[ConcreteGoogleAuthInfos, (UserId, GoogleAuthInfo)] {

  override val tableName = "google_auth_infos"

  object googleUserId extends StringColumn(this) with PartitionKey[String]
  object googleAuthToken extends StringColumn(this)
  object googleIdToken extends StringColumn(this)
  object userId extends LongColumn(this)

  def fromRow(row: Row) = {
    val googleUserIdV    = GoogleUserId(googleUserId(row))
    val googleAuthTokenV = GoogleAuthToken(googleAuthToken(row))
    val userIdV          = UserId(userId(row))
    val googleIdTokenV   = GoogleIdToken(googleIdToken(row))
    // suffix V represents that string represents a variable

    (userIdV, GoogleAuthInfo(googleUserIdV, googleAuthTokenV, googleIdTokenV))
  }

}


abstract class ConcreteGoogleAuthInfos(val settings: AuthSettings) extends GoogleAuthInfos {

  def insertGoogleAuthInfo(userId: UserId, info: GoogleAuthInfo)(implicit keySpace: KeySpace) =
    insert.value(_.googleUserId, info.googleUserId.id)
      .value(_.googleAuthToken, info.authToken.value)
      .value(_.googleIdToken, info.idToken.value)
      .value(_.userId, userId.uuid)

  def getGoogleAuthInfoByGoogleUId(id: GoogleUserId)(implicit keySpace: KeySpace) =
    select.where(_.googleUserId eqs id.id)

  def getUserIdByGoogleUId(id: GoogleUserId)(implicit keySpace: KeySpace) =
    select(_.userId).where(_.googleUserId eqs id.id)

  def updateGoogleAuthInfo(userId: UserId, info: GoogleAuthInfo)(implicit keySpace: KeySpace) = {
    val updateWhere = update.where(_.googleUserId eqs info.googleUserId.id)
    var setTos = MutableSeq.empty[ConcreteGoogleAuthInfos => UpdateClause.Condition]

    setTos = setTos :+ { (_ : ConcreteGoogleAuthInfos).googleAuthToken setTo info.authToken.value}
    setTos = setTos :+ { (_ : ConcreteGoogleAuthInfos).userId setTo userId.uuid}
    setTos = setTos :+ { (_ : ConcreteGoogleAuthInfos).googleIdToken setTo info.idToken.value}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) }.some
    }
  }

}