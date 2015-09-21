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


sealed class FBAuthInfo extends CassandraTable[ConcreteFBAuthInfo, FBAuth] {

  override def tableName = "fb_auth_info"

  object fbUserId extends LongColumn(this) with PartitionKey[Long]
  object token extends StringColumn(this)
  object userId extends LongColumn(this)

  def fromRow(row: Row) = {
    val fbUserIdV = FBUserId(fbUserId(row))
    val tokenV    = FBAuthToken(token(row))
    val userIdV   = UserId(userId(row))
    // suffix V represents that string represents a variable

    FBAuth(fbUserIdV, tokenV, userIdV)
  }

}


abstract class ConcreteFBAuthInfo(val settings: UserSettings) extends FBAuthInfo with UserConnector {

  def insertFBAuthInfo(info: FBAuth) = {
    insert.value(_.fbUserId, info.fbUserId.id)
      .value(_.token, info.token.value)
      .value(_.userId, info.userId.uuid)
  }

  def getByFBUserId(id: FBUserId): Future[Option[FBAuth]] = {
    select.where(_.fbUserId eqs id.id).one()
  }

  def updateFBAuthInfo(info: FBAuth) = {
    val updateWhere = update.where(_.fbUserId eqs info.fbUserId.id)
    var setTos = MutableSeq.empty[ConcreteFBAuthInfo => UpdateClause.Condition]

    setTos = setTos :+ { (_ : ConcreteFBAuthInfo).token setTo info.token.value}
    setTos = setTos :+ { (_ : ConcreteFBAuthInfo).userId setTo info.userId.uuid}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) } some
    }
  }

}