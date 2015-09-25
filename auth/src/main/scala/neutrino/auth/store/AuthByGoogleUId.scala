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


sealed class AuthByGoogleUId extends CassandraTable[ConcreteAuthByGoogleUId, (UserId, GoogleAuthInfo)] {

  override val tableName = "google_auth_info"

  object googleUserId extends LongColumn(this) with PartitionKey[Long]
  object googleToken extends StringColumn(this)
  object userId extends LongColumn(this)

  def fromRow(row: Row) = {
    val googleUserIdV = GoogleUserId(googleUserId(row))
    val googleTokenV  = GoogleAuthToken(googleToken(row))
    val userIdV       = UserId(userId(row))
    // suffix V represents that string represents a variable

    (userIdV, GoogleAuthInfo(googleUserIdV, googleTokenV))
  }

}


abstract class ConcreteAuthByGoogleUId(val settings: AuthSettings) extends AuthByGoogleUId {

  def insertGoogleAuthInfo(userId: UserId, info: GoogleAuthInfo)(implicit keySpace: KeySpace) =
    insert.value(_.googleUserId, info.googleUserId.id)
      .value(_.googleToken, info.token.value)
      .value(_.userId, userId.uuid)

  def getGoogleAuthInfoByGoogleUId(id: GoogleUserId)(implicit keySpace: KeySpace) =
    select.where(_.googleUserId eqs id.id)

  def updateAuthByGoogleUId(userId: UserId, info: GoogleAuthInfo)(implicit keySpace: KeySpace) = {
    val updateWhere = update.where(_.googleUserId eqs info.googleUserId.id)
    var setTos = MutableSeq.empty[ConcreteAuthByGoogleUId => UpdateClause.Condition]

    setTos = setTos :+ { (_ : ConcreteAuthByGoogleUId).googleToken setTo info.token.value}
    setTos = setTos :+ { (_ : ConcreteAuthByGoogleUId).userId setTo userId.uuid}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) }.some
    }
  }

}