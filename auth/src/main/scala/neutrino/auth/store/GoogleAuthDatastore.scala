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

class GoogleAuthDatastore(val settings: AuthSettings) extends AuthConnector {

  object EmailAuthInfos extends ConcreteEmailAuthInfos(settings)
  object GoogleAuthInfos extends ConcreteGoogleAuthInfos(settings)


  /**
   * To initialize cassandra tables
   */
  def init(): Boolean = {
    val creation =
      for {
        _ <- EmailAuthInfos.create.ifNotExists.future()
        _ <- GoogleAuthInfos.create.ifNotExists.future()
      } yield true

    Await.result(creation, 2 seconds)
  }

  def getUserId(googleUserId: GoogleUserId, email: Email) = {
    EmailAuthInfos.getUserIdByEmail(email).one().filter(!_.isEmpty).recoverWith {
      case _: NoSuchElementException =>
        GoogleAuthInfos.getUserIdByGoogleUId(googleUserId).one()
    } map(_.map(UserId(_)))
  }

  def addAuthInfo(userId: UserId, authInfo: GoogleAuthInfo, emailInfo: EmailAuthInfo) =
    Batch.logged
      .add(GoogleAuthInfos.insertGoogleAuthInfo(userId, authInfo))
      .add(EmailAuthInfos.insertEmailAuthInfo(userId, emailInfo))
      .future().map(_ => true)

  def updateAuthInfo(userId: UserId, authInfo: GoogleAuthInfo, emailInfo: EmailAuthInfo) = {
    val googleUpdateQO = GoogleAuthInfos.updateGoogleAuthInfo(userId, authInfo)
    val emailUpdateQO  = EmailAuthInfos.updateEmailAuthInfo(userId, emailInfo)
    val batch = List(googleUpdateQO, emailUpdateQO).flatten.foldLeft(Batch.logged) { (b, q) => b.add(q) }
    batch.future().map(_ => true)
  }

}