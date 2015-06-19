package neutrino.user

import scala.concurrent._, duration._
import scala.util.Failure
import scala.util.control.NonFatal

import akka.actor.{Props, Actor, ActorLogging}
import akka.util.Timeout
import akka.pattern.pipe

import goshoplane.commons.core.protocols.Implicits._
import goshoplane.commons.core.services.{UUIDGenerator, NextId}

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

    case CreateOrUpdateUser(userInfo) =>
      implicit val timeout = Timeout(1 seconds)

      userAccDatastore.checkIfExistingUser(userInfo)
      .andThen {
        case Failure(NonFatal(ex)) =>
          log.error(ex, "Caught error while checking if existing user for user info = {}",
                        ex.getMessage,
                        userInfo)
      } flatMap { userIdO =>
        userIdO match {
          case Some(userId) => // Update the existing user with the provided information
            userAccDatastore.updateUser(userId, userInfo).map(_ => userId)
            .andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{}] while update existing user id = {}",
                              ex.getMessage,
                              userId.uuid)
            }
          case None => // Create the new user with the provided information
            (for {
              uuid   <- (UUID ?= NextId("user")).map(_.get)
              userId <- Future.successful(UserId(uuid = uuid))
              _      <- userAccDatastore.createUser(userId, userInfo)
            } yield userId).andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{}] while creating new user for userInfo = {}",
                              ex.getMessage,
                              userInfo)
            }
        }
      } pipeTo sender()



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