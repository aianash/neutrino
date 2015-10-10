package neutrino.auth

import scala.concurrent.duration._

import akka.util.Timeout
import akka.pattern.ask
import akka.actor.{Actor, ActorSystem}

import neutrino.core.auth._
import neutrino.auth.handler._

object TestAuth {

  import protocols._

  def main(args: Array[String]) {
    // fbAuthTest
    googleAuthTest

  }

  def googleAuthTest = {
    val idToken = GoogleIdToken("")
    val authToken = GoogleAuthToken("")
    val userId = GoogleUserId("")
    val authInfo = GoogleAuthInfo(userId, authToken, idToken)

    val system = ActorSystem("test")
    val as = AuthenticationSupervisor.props
    val supervisor = system.actorOf(as)
    implicit val timeout = Timeout(2 seconds)
    val result = supervisor ? AuthenticateUser(authInfo, None)
  }

  def fbAuthTest = {
    val system = ActorSystem("test")
    val as = AuthenticationSupervisor.props
    val supervisor = system.actorOf(as)

    val authToken = ""
    val fbUserId = new FBUserId(9L)
    val appId = ""

    val authInfo = new FBAuthInfo(fbUserId, FBAuthToken(authToken))
    implicit val timeout = Timeout(2 seconds)
    val result = supervisor ? AuthenticateUser(authInfo, None)
  }

}