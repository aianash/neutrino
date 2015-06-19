package neutrino.user.store

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import neutrino.user.UserSettings

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context

sealed class UserAccountDatastore(val settings: UserSettings)
  extends UserConnector {

  object UserInfos extends UserInfos(settings)
  object UserFriends extends UserFriends(settings)
  object UserByFBUID extends UserByFBUID(settings)
  object UserByEmail extends UserByEmail(settings)


  def init()(implicit executor: ExecutionContext) {
    val creation =
      for {
        _ <- UserInfos.create.future()
        _ <- UserFriends.create.future()
        _ <- UserByFBUID.create.future()
        _ <- UserByEmail.create.future()
      } yield true

    Await.ready(creation, 2 seconds)
  }


  def checkIfExistingUser(userInfo: UserInfo)(implicit executor: ExecutionContext) = {
    val thruEmailF = userInfo.email.map(UserByEmail.getUserIdBy(_).one()).getOrElse(Future.successful(None))
    val thruFbF    = userInfo.facebookInfo.map(info =>
                      UserByFBUID.getUserIdBy(info.userId).one().map(_.map(_._1))).getOrElse(Future.successful(None))

    for {
      thruEmail <- thruEmailF
      thruFb    <- thruFbF
    } yield thruEmail orElse thruFb

  }


  def getUserInfo(userId: UserId)(implicit executor: ExecutionContext) =
    UserInfos.getInfoBy(userId).one()


  def getUserFriends(userId: UserId)(implicit executor: ExecutionContext) =
    UserFriends.getFriendsBy(userId).fetch()


  def getUserFriends(userId: UserId, friendIds: Seq[UserId])(implicit executor: ExecutionContext) =
    UserFriends.getFriendsBy(userId, friendIds).fetch()


  def createUser(userId: UserId, info: UserInfo)(implicit executor: ExecutionContext) = {
    val emailF = info.email.map(UserByEmail.insertEmail(userId, _).future().map(_ => true))
                    .getOrElse(Future.successful(true))

    val infoF  = UserInfos.insertUserInfo(userId, info).future().map(_ => true)

    val fbF    = info.facebookInfo.map(UserByFBUID.insertFacebookInfo(userId, _).future().map(_ => true))
                    .getOrElse(Future.successful(true))

    for {
      email <- emailF
      info  <- infoF
      fb    <- fbF
    } yield email && info && fb
  }


  def updateUser(userId: UserId, info: UserInfo)(implicit executor: ExecutionContext) = {
    val emailF =
      info.email.map { email => // if email provided in info
        UserInfos.getInfoBy(userId).one().map(_.get) flatMap { info => // get email from UserInfods
          // If UserInfos have email
          info.email map { oldEmail =>
            // Check if emails (provided vs UserInfos') are equal
            if(email equals oldEmail) Future.successful(true) // if they are equal then no operation to
                                                              // be done on storage
            else { // otherwise if not equal, delete and insert new one
              val batch = BatchStatement()
              batch add UserByEmail.deleteEmail(oldEmail)
              batch add UserByEmail.insertEmail(userId, email)
              batch.future().map(_ => true)
            }
          } getOrElse(UserByEmail.insertEmail(userId, email).future().map(_ => true)) // If no email in UserInfos
                                                                                      // [NOTE] info will be update
                                                                                      // with update to UserInfos below
        }
      } getOrElse(Future.successful(true)) // No operation to perform if no email in
                                           // provided info

    val infoF = UserInfos.updateUserInfo(userId, info).map(_.future().map(_ => true))
                         .getOrElse(Future.successful(true))

    val fbF   = info.facebookInfo.map(UserByFBUID.updateFacebookInfo(userId, _).future().map(_ => true))
                    .getOrElse(Future.successful(true))

    for {
      email <- emailF
      info  <- infoF
      fb    <- fbF
    } yield email && info && fb
  }

}