package neutrino.user.store

import scala.concurrent.duration._
import scala.concurrent.{Future, Await}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import neutrino.user._
import neutrino.core.user._
import neutrino.core.user.auth._


sealed class UserAccountDatastore(val settings: UserSettings) extends UserConnector {

  object UserInfo extends ConcreteUserInfo(settings)
  object UserByFBUId extends ConcreteUserByFBUId(settings)
  object UserByGoogleUId extends ConcreteUserByGoogleUId(settings)
  object UserByEmail extends ConcreteUserByEmail(settings)

  def init(): Unit = {
    val creation = Future {
      for {
        _ <- UserInfo.create.execute()
        _ <- UserByFBUId.create.execute()
        _ <- UserByGoogleUId.create.execute()
        _ <- UserByEmail.create.execute()
      } yield true
    }

    Await.ready(creation, 2 seconds)
  }

  def checkIfExistingUser(user: User): Future[Option[UserId]] = ???

  def getUserInfo(userId: UserId) = UserInfo.getByUserId(userId).map(_.get)

  def createUserWithFB(userId: UserId, user: User, fbInfo: FBAuth) = {
    val emailF =
      user.email.map(UserByEmail.insertEmail(userId, _).map(_ => true))
        .getOrElse(Future.successful(true))

    for {
      ui    <- UserInfo.insertUserInfo(userId, user)
      fbi   <- UserByFBUId.insertUserByFBUId(userId, fbInfo)
      email <- emailF
    } yield email
  }

  def createUserWithGoogle(userId: UserId, user: User, googleInfo: GoogleAuth) = {
    val emailF =
      user.email.map(UserByEmail.insertEmail(userId, _).map(_ => true))
        .getOrElse(Future.successful(true))

    for {
      ui        <- UserInfo.insertUserInfo(userId, user)
      googlei   <- UserByGoogleUId.insertUserByGoogleUId(userId, googleInfo)
      email     <- emailF
    } yield email
  }

  def updateUserInfo(user: User): Future[UserId] = ???

  def updateFBInfo(info: FBAuth): Future[FBUserId] = ???

  def updateGoogleInfo(info: GoogleAuth): Future[GoogleUserId] = ???

}