package neutrino.user.test

import java.io.File

import akka.actor._

import com.typesafe.config.ConfigFactory

import neutrino.user.store._
import neutrino.core.user._
import neutrino.core.auth._
import neutrino.user._
import neutrino.user.protocols._

object TestUserInfo {

  val info =
    UserInfo(
      Some(Username("neerajgangwar")),
      Some(DisplayName("Neeraj Updated", "Gangwar")),
      Some(Email("y.neeraj2008@gmail.com")),
      Some(Mobile("9739666202")),
      None,
      None,
      None,
      None,
      UserType("registered")
      )

  val user = User(UserId(13433112313L), info)

  def main(args: Array[String]) {
    val system = ActorSystem("test")
    val supervisor = system.actorOf(UserAccountSupervisor.props)

    supervisor ! UpdateUser(user)
  }

}