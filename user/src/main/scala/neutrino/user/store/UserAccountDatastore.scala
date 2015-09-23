package neutrino.user.store

import scala.concurrent.duration._
import scala.concurrent.{Future, Await}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import neutrino.user._
import neutrino.core.user._
import neutrino.core.auth._


sealed class UserAccountDatastore(val settings: UserSettings) extends UserConnector {

  object UserInfo extends ConcreteUserInfo(settings)
  object ExternalAccountInfo extends ConcreteExternalAccountInfo(settings)

  /**
   * To initialize cassandra tables
   */
  def init(): Boolean = {
    val creation =
      for {
        _ <- UserInfo.create.ifNotExists.future()
        _ <- ExternalAccountInfo.create.ifNotExists.future()
      } yield true

    Await.result(creation, 2 seconds)
  }

  /**
   * Function to insert user info
   * @param userId UserId
   * @param user User
   */
  def insertUserInfo(userId: UserId, user: User) =
    UserInfo.insertUserInfo(userId, user).future().map(_ => true)

  /**
   * Function to insert external account info
   * @param userId UserId
   * @param info ExternalAccountInfo
   */
  def insertExternalAccountInfo(userId: UserId, info: ExternalAccount) =
    ExternalAccountInfo.insertExternalAccountInfo(userId, info).future().map(_ => true)

  /**
   * Function to get user info for given user id
   * @param userId UserId
   * @return Future[Option[User]]
   */
  def getUserInfo(userId: UserId) = UserInfo.getByUserId(userId).one()

  /**
   * Function to get external account info for a given user id
   * @param userId UserId
   * @return Future[Option[ExternalAccount]]
   */
  def getExternalAccountInfo(userId: UserId) = ExternalAccountInfo.getByUserId(userId).one()

  /**
   * Function to update user info
   * @param userId UserId
   * @param user User
   */
  def updateUserInfo(userId: UserId, user: User) =
    UserInfo.updateUserInfo(userId, user).map(_.future().map(_ => true)).getOrElse(Future.successful(true))

  /**
   * Function to update external account info
   * @param userId UserId
   * @param user User
   */
  def updateExternalAccountInfo(userId: UserId, info: ExternalAccount) =
    ExternalAccountInfo.updateExternalAccountInfo(userId, info).map(_.future().map(_ => true)).getOrElse(Future.successful(true))

}