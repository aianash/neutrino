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
  val creationStatus = userAccDatastore.init()
  if(!creationStatus) {
    log.error("Caught error while initializing UserAccountDatastore")
    context.stop(self)
  }

  private val UUID = context.actorOf(UUIDGenerator.props(ServiceId, DatacenterId))
  context watch UUID

  def receive = {

    case InsertUser(user, extAccinfo) =>
      for {
        u <- userAccDatastore.insertUser(user)
        e <- userAccDatastore.insertExternalAccountInfo(user.id, extAccinfo)
      } yield true

    case UpdateUser(user) =>
      userAccDatastore.updateUser(user)

    case UpdateExternalAccountInfo(userId, info) =>
      userAccDatastore.updateExternalAccountInfo(userId, info)

    case GetUser(userId) =>
      userAccDatastore.getUser(userId) pipeTo sender()

    case GetExternalAccountInfo(userId) =>
      userAccDatastore.getExternalAccountInfo(userId) pipeTo sender()

  }

}

object UserAccountSupervisor {
  def props = Props(classOf[UserAccountSupervisor])
}