package neutrino.auth

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.util.Timeout

import goshoplane.commons.core.protocols.Implicits._
import goshoplane.commons.core.services.{UUIDGenerator, NextId}

import neutrino.core.user._
import neutrino.core.auth._
import neutrino.user._
import neutrino.user.protocols._
import neutrino.auth._
import neutrino.auth.store._


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
      handler.validate.flatMap {
        case NotValidated => Future.successful(AuthStatus.Failure.InvalidCredentials("Invalid credentials"))
        case Validated    =>
          handler.getUserId.flatMap(
            _.EITHER {
              userId =>
                updateExistingUser(handler, userId)
                  .map(_ => AuthStatus.Success(userId, UserType.REGISTERED, false))
            } OR {
              for {
                userId <- chooseUserId(suggestedUserIdO)
                _      <- createNewUser(handler, userId)
              } yield AuthStatus.Success(userId, UserType.REGISTERED, true)
            }
          )
      }.recover {
        case NonFatal(ex) =>
          log.error(ex, "Error occurred while authenitcating user")
          AuthStatus.Failure.InternalServerError
      } pipeTo sender()

  }

  implicit class OptionEitherOr[T](opt: Option[T]) {
    def EITHER[R](either: T => R) =
      new {
        def OR(or: => R) =
          opt match {
            case Some(a) => either(a)
            case None => or
          }
      }
  }

  private def updateExistingUser(handler: SocialLoginHandler, userId: UserId) = {
    (for {
      userInfo <- handler.getUserInfo
      success  <- handler.updateAuthInfo(userId, userInfo) if success
    } yield {
      userAccount ! UpdateExternalAccountInfo(userId, handler.getExternalAccountInfo)
      success
    }).recoverWith {
      case NonFatal(ex) => Future.failed[AuthStatus](new Exception("Error while updating user", ex))
    }
  }

  private def chooseUserId(suggestedUserIdO: Option[UserId]) =
    suggestedUserIdO
      .map(Future.successful(_))
      .getOrElse {
        implicit val timeout = Timeout(2 seconds)
        (uuid ?= NextId("user")).map(id => UserId(id.get))
      }

  private def createNewUser(handler: SocialLoginHandler, userId: UserId) = {
    (for {
      userInfo <- handler.getUserInfo
      success  <- handler.addAuthInfo(userId, userInfo) if success
    } yield User(userId, userInfo))
    .andThen {
      case Success(user) =>
        userAccount ! InsertUser(user, handler.getExternalAccountInfo)
    }
    .recoverWith {
      case NonFatal(ex) => Future.failed[AuthStatus](new Exception("Error while creating user", ex))
    }
  }

}


object AuthenticationSupervisor {

  def props = Props(classOf[AuthenticationSupervisor])

}