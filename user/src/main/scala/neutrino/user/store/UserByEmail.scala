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

class UserByEmail(val settings: UserSettings)
  extends CassandraTable[UserByEmail, UserId] with UserConnector {

  override def tableName = "users_by_email"

  // ids
  object email extends StringColumn(this) with PartitionKey[String]
  object uuid extends LongColumn(this)


  def fromRow(row: Row) = UserId(uuid(row))


  def insertEmail(userId: UserId, email: String) =
    insert
      .value(_.email,         email)
      .value(_.uuid,          userId.uuid)


  def getUserIdBy(email: String) = select.where(_.email eqs email)


  def deleteEmail(email: String) =
    delete.where(_.email eqs email)
}