package neutrino.auth

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout

import goshoplane.commons.core.protocols.Implicits._
import goshoplane.commons.core.services.{UUIDGenerator, NextId}

import neutrino.core.user._
import neutrino.core.auth._
import neutrino.user._
import neutrino.user.protocols._
import neutrino.auth._
import neutrino.auth.store._

object AuthStatus {
  case class Success(userId: UserId, userType: UserType)
  case object Failure
}

class AuthenticationSupervisor extends Actor with ActorLogging {

  import handler._
  import protocols._
  import context.dispatcher

  private val settings       = AuthSettings(context.system)
  private val handlerFactory = new SocialLoginHandlerFactory(settings)
  private val uuid           = context.actorOf(UUIDGenerator.props(settings.ServiceId, settings.DatacenterId))
  private val userAccount    = context.actorOf(UserAccountSupervisor.props)
  context watch uuid

  def receive = {

    case AuthenticateUser(socialAuthInfo, suggestedUserIdO) =>
      val handler = handlerFactory.instanceFor(socialAuthInfo)
      handler.validate foreach {
        case Validated =>
          handler
            .getUserId
            .andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{}] while getting user id", ex)
                sender() ! AuthStatus.Failure
            }
            .foreach {
              case Some(userId) => sender() ! AuthStatus.Success(userId, UserType.REGISTERED) 
              case None =>
                val uuidF =
                  suggestedUserIdO
                    .map(x => Future(x.uuid))
                    .getOrElse {
                      implicit val timeout = Timeout(2 seconds)
                      (uuid ?= NextId("user")).map(_.get)
                    }
                (for {
                  userId   <- uuidF.map(UserId(_))
                  userInfo <- handler.getUserInfo
                  success  <- handler.updateAuthTable(userId, userInfo) if success
                } yield User(userId, userInfo))
                  .andThen {
                    case Failure(NonFatal(ex)) =>
                      log.error(ex, "Caught error [{}] while creating new user", ex)
                      sender() ! AuthStatus.Failure
                  }
                  .foreach { case user: User =>
                    userAccount ! InsertUser(user, handler.getExternalAccountInfo)
                    sender() ! AuthStatus.Success(user.id, UserType.REGISTERED)
                  }
            }

        case NotValidated => sender() ! AuthStatus.Failure
      }

  }

}


object AuthenticationSupervisor {

  def props = Props(classOf[AuthenticationSupervisor])

}