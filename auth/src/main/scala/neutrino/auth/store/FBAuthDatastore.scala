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

  object EmailAuthInfos extends ConcreteEmailAuthInfos(settings)
  object FBAuthInfos extends ConcreteFBAuthInfos(settings)


  /**
   * To initialize cassandra tables
   */
  def init(): Boolean = {
    val creation =
      for {
        _ <- EmailAuthInfos.create.ifNotExists.future()
        _ <- FBAuthInfos.create.ifNotExists.future()
      } yield true

    Await.result(creation, 2 seconds)
  }

  def getUserId(fbUserId: FBUserId, email: Email) = {
    EmailAuthInfos.getUserIdByEmail(email).one().filter(!_.isEmpty).recoverWith {
      case _: NoSuchElementException =>
        FBAuthInfos.getUserIdByFBUId(fbUserId).one()
    } map(_.map(UserId(_)))
  }

  def addAuthInfo(userId: UserId, fbAuthInfo: FBAuthInfo, emailInfo: EmailAuthInfo) =
    Batch.logged
      .add(FBAuthInfos.insertFBAuthInfo(userId, fbAuthInfo))
      .add(EmailAuthInfos.insertEmailAuthInfo(userId, emailInfo))
      .future().map(_ => true)

  def updateAuthInfo(userId: UserId, authInfo: FBAuthInfo, emailInfo: EmailAuthInfo) = {
    val fbUpdateQ = FBAuthInfos.updateFBAuthInfo(userId, authInfo)
    val emailUpdateQ = EmailAuthInfos.updateEmailAuthInfo(userId, emailInfo)
    val batch = List(fbUpdateQ, emailUpdateQ).flatten.foldLeft(Batch.logged) { (b, q) => b.add(q) }
    batch.future().map(_ => true)
  }

}