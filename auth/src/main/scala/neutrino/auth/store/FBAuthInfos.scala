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


sealed class FBAuthInfos extends CassandraTable[ConcreteFBAuthInfos, (UserId, FBAuthInfo)] {

  override def tableName = "fb_auth_infos"

  object fbUserId extends LongColumn(this) with PartitionKey[Long]
  object fbToken extends StringColumn(this)
  object userId extends LongColumn(this)

  def fromRow(row: Row) = {
    val fbUserIdV = FBUserId(fbUserId(row))
    val fbTokenV  = FBAuthToken(fbToken(row))
    val userIdV   = UserId(userId(row))
    // suffix V represents that string represents a variable

    (userIdV, FBAuthInfo(fbUserIdV, fbTokenV))
  }

}


abstract class ConcreteFBAuthInfos(val settings: AuthSettings) extends FBAuthInfos {

  def insertFBAuthInfo(userId: UserId, info: FBAuthInfo)(implicit keySpace: KeySpace) =
    insert.value(_.fbUserId, info.fbUserId.id)
      .value(_.fbToken, info.token.value)
      .value(_.userId, userId.uuid)

  def getFBAuthInfoByFBUId(id: FBUserId)(implicit keySpace: KeySpace) =
    select.where(_.fbUserId eqs id.id)

  def getUserIdByFBUId(id: FBUserId)(implicit keySpace: KeySpace) =
    select(_.userId).where(_.fbUserId eqs id.id)

  def updateFBAuthInfos(userId: UserId, info: FBAuthInfo)(implicit keySpace: KeySpace) = {
    val updateWhere = update.where(_.fbUserId eqs info.fbUserId.id)
    var setTos = MutableSeq.empty[ConcreteFBAuthInfos => UpdateClause.Condition]

    setTos = setTos :+ { (_ : ConcreteFBAuthInfos).fbToken setTo info.token.value}
    setTos = setTos :+ { (_ : ConcreteFBAuthInfos).userId setTo userId.uuid}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x)
      case MutableSeq(head, tail @ _*) =>
        (tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) })
    }
  }

}