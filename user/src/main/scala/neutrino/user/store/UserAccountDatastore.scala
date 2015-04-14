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

  def init()(implicit executor: ExecutionContext) {
    val creation =
      for {
        _ <- UserInfos.create.future()
        _ <- UserFriends.create.future()
      } yield true

    Await.ready(creation, 2 seconds)
  }


  def getUserInfo(userId: UserId)(implicit executor: ExecutionContext) =
    UserInfos.getInfoBy(userId).fetch()

  def getUserFriends(userId: UserId)(implicit executor: ExecutionContext) =
    UserFriends.getFriendsBy(userId).fetch()

  def createUser(userId: UserId, info: UserInfo)(implicit executor: ExecutionContext) =
    UserInfos.insertUserInfo(userId, info).future()

  def updateUser(userId: UserId, info: UserInfo)(implicit executor: ExecutionContext) =
    UserInfos.updateUserInfo(userId, info).future().map(_ => true)
}