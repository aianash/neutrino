package neutrino.auth.store

import scala.concurrent.Future
import scala.collection.mutable.{Seq => MutableSeq}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._
import com.websudos.phantom.builder.query._
import com.websudos.phantom.builder.clauses.UpdateClause

import neutrino.core.auth._
import neutrino.core.user._
import neutrino.auth.AuthSettings


sealed class EmailAuthInfos extends CassandraTable[ConcreteEmailAuthInfos, (UserId, EmailAuthInfo)] {

  override def tableName = "email_auth_infos"

  // ids
  object email extends StringColumn(this) with PartitionKey[String]
  object userId extends LongColumn(this)
  object fbUserId extends OptionalLongColumn(this)
  object googleUserId extends OptionalStringColumn(this)

  def fromRow(row: Row) = {
    val fbUserIdO = fbUserId(row).map(FBUserId(_))
    val googleUserIdO = googleUserId(row).map(GoogleUserId(_))

    (UserId(userId(row)), EmailAuthInfo(Email(email(row)), fbUserIdO, googleUserIdO))
  }

}

abstract class ConcreteEmailAuthInfos(val settings: AuthSettings) extends EmailAuthInfos {

  def insertEmailAuthInfo(userId: UserId, info: EmailAuthInfo)(implicit keySpace: KeySpace) =
    insert.value(_.email, info.email.email)
      .value(_.userId, userId.uuid)
      .value(_.fbUserId, info.fbUserId.map(_.id))
      .value(_.googleUserId, info.googleUserId.map(_.id))

  def getEmailAuthInfoByEmail(email: Email)(implicit keySpace: KeySpace) =
    select.where(_.email eqs email.email)

  def getUserIdByEmail(email: Email)(implicit keySpace: KeySpace) =
    select(_.userId).where(_.email eqs email.email)

  def updateEmailAuthInfo(userId: UserId, info: EmailAuthInfo)(implicit keySpace: KeySpace) = {
    val updateWhere = update.where(_.email eqs info.email.email)
    var setTos = MutableSeq.empty[ConcreteEmailAuthInfos => UpdateClause.Condition]
    setTos = setTos :+ { (_ : ConcreteEmailAuthInfos).userId setTo userId.uuid}

    info.fbUserId.map(_.id)      .foreach { fid => setTos = setTos :+ { (_ : ConcreteEmailAuthInfos).fbUserId setTo fid.some }}
    info.googleUserId.map(_.id)  .foreach { gid => setTos = setTos :+ { (_ : ConcreteEmailAuthInfos).googleUserId setTo gid.some }}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) } some
    }
  }

}