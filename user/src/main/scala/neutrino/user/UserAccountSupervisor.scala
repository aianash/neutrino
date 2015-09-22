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
import neutrino.core.user._


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

    case CreateOrUpdateUserWithFB(userInfo, fbInfo) =>
      implicit val timeout = Timeout(1 seconds)

      userAccDatastore.checkIfExistingUser(userInfo).andThen {
        case Failure(NonFatal(ex)) =>
          log.error(ex, "Caught error [{}] while checking if user existed for user info = {}",
            ex.getMessage, userInfo)
      } flatMap { userIdO =>
        userIdO match {
          case Some(userId) => // update the existing user
            (for {
              _ <- userAccDatastore.updateUserInfo(userInfo)
              _ <- userAccDatastore.updateFBInfo(fbInfo)
            } yield userId).andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{}] while updating user info for user id = {}",
                  ex.getMessage, userId)
            }

          case None => // create a new user
            (for {
              uuid   <- (UUID ?= NextId("user")).map(_.get)
              userId <- Future.successful(UserId(uuid))
              _      <- userAccDatastore.createUserWithFB(userId, userInfo, fbInfo)
            } yield userId).andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{}] while creating account for user info = {}",
                  ex.getMessage, userInfo)
            }
        }
      }

    case CreateOrUpdateUserWithGoogle(userInfo, googleInfo) =>
      implicit val timeout = Timeout(1 seconds)

      userAccDatastore.checkIfExistingUser(userInfo).andThen {
        case Failure(NonFatal(ex)) =>
          log.error(ex, "Caught error [{}] while checking if user existed for user info = {}",
            ex.getMessage, userInfo)
      } flatMap { userIdO =>
        userIdO match {
          case Some(userId) => // update existing user
            (for {
              _ <- userAccDatastore.updateUserInfo(userInfo)
              _ <- userAccDatastore.updateGoogleInfo(googleInfo)
            } yield userId).andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{ex}] while updating user info for user id = {}",
                  ex.getMessage, userId)
            }

          case None => // create new user
            (for {
              uuid   <- (UUID ?= NextId("user")).map(_.get)
              userId <- Future.successful(UserId(uuid))
              _      <- userAccDatastore.createUserWithGoogle(userId, userInfo, googleInfo)
            } yield userId).andThen {
              case Failure(NonFatal(ex)) =>
                log.error(ex, "Caught error [{}] while creating account for user info = {}",
                  ex.getMessage, userInfo)
            }
        }
      }

    case UpdateUserInfo(info) => ???

    case UpdateFBInfo(info) => ???

    case UpdateGoogleInfo(info) => ???

    case GetUserDetail(userId) =>
      userAccDatastore.getUserInfo(userId) pipeTo sender()

  }

}

object UserAccountSupervisor {
  def props = Props(classOf[UserAccountSupervisor])
}