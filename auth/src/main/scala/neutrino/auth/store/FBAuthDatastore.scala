package neutrino.auth.store

import scala.concurrent.duration._
import scala.concurrent.{Future, Await}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import neutrino.core.user._
import neutrino.core.auth._
import neutrino.auth._


class FBAuthDatastore(val settings: AuthSettings) extends AuthConnector {

  object AuthByEmail extends ConcreteAuthByEmail(settings)
  object AuthByFBUId extends ConcreteAuthByFBUId(settings)

  /**
   * To initialize cassandra tables
   */
  def init(): Boolean = {
    val creation =
      for {
        _ <- AuthByEmail.create.ifNotExists.future()
        _ <- AuthByFBUId.create.ifNotExists.future()
      } yield true

    Await.result(creation, 2 seconds)
  }

  def getUserId(fbUserId: FBUserId, email: Email) = {
    AuthByEmail.getUserIdByEmail(email).one().filter(!_.isEmpty).recoverWith {
      case _: NoSuchElementException =>
        AuthByFBUId.getUserIdByFBUId(fbUserId).one()
    } map(_.map(UserId(_)))
  }

  def addFBAuthenticationInfo(userId: UserId, fbAuthInfo: FBAuthInfo, emailInfo: EmailAuthInfo) =
    Batch.logged
      .add(AuthByFBUId.insertFBAuthInfo(userId, fbAuthInfo))
      .add(AuthByEmail.insertEmailAuthInfo(userId, emailInfo))
      .future().map(_ => true)

}