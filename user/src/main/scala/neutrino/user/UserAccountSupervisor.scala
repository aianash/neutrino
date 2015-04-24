package neutrino.user

import scala.concurrent._, duration._

import akka.actor.{Props, Actor, ActorLogging}
import akka.util.Timeout
import akka.pattern.pipe

import goshoplane.commons.core.protocols.Implicits._

import neutrino.core.services._
import neutrino.user.store._

import com.goshoplane.common._

class UserAccountSupervisor extends Actor with ActorLogging {

  private val settings = UserSettings(context.system)

  import settings._
  import protocols._
  import context.dispatcher

  private val userAccDatastore = new UserAccountDatastore(settings)
  userAccDatastore.init()

  private val UUID = context.actorOf(UUIDGenerator.props(ServiceId, DatacenterId))
  context watch UUID

  def receive = {

    case IsExistingFBUser(fbUserId) =>
      userAccDatastore.getUserId(fbUserId) pipeTo sender()



    case CreateUser(userInfo) =>
      implicit val timeout = Timeout(1 seconds)

      val userIdF =
        for {
          uuid   <- UUID ?= NextId("user")
          userId <- Future.successful(UserId(uuid = uuid))
          _ <- userAccDatastore.createUser(userId, userInfo)
        } yield userId

      userIdF pipeTo sender()



    case UpdateUser(userId, userInfo) =>
      userAccDatastore.updateUser(userId, userInfo) pipeTo sender()



    case GetUserDetail(userId) =>
      userAccDatastore.getUserInfo(userId) pipeTo sender()



    case GetFriendsForInvite(userId, filter) =>
      userAccDatastore.getUserFriends(userId) pipeTo sender()


    case GetFriendsDetails(userId, friendIds) =>
      userAccDatastore.getUserFriends(userId, friendIds) pipeTo sender()

  }

}

object UserAccountSupervisor {
  def props = Props(classOf[UserAccountSupervisor])
}