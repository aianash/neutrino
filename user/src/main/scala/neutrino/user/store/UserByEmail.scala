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


sealed class UserByEmail extends CassandraTable[UserByEmail, UserId] {

  override def tableName = "user_by_email"

  // ids
  object email extends StringColumn(this) with PartitionKey[String]
  object userId extends LongColumn(this)

  def fromRow(row: Row) = UserId(userId(row))

}

abstract class ConcreteUserByEmail(val settings: UserSettings) extends UserByEmail with UserConnector {

  def insertEmail(userId: UserId, email: Email) =
    insert.value(_.email, email.email)
      .value(_.userId, userId.uuid)
      .future()

  def getUserIdBy(email: String) = select.where(_.email eqs email)

}