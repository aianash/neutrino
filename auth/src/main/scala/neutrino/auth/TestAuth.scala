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

    val system = ActorSystem("test")
    val as = AuthenticationSupervisor.props
    val supervisor = system.actorOf(as)

    val authToken = "CAALYIU6LOQsBAKUq42VDuRglLPBu6LFnmKmqIuR5gvwvmiZCD1cjyDDVO9vh7VGxxvMYImU4xtwDFItjVZBYszBz4s53An2dwRrRxwinmIGwdHV0swx9QFVJisaV3UJhcYfi28DDZCoJvC8X5lS0hiTyZBozNDdeuXuWad46oHM7UJf1GxZBaPrjvkve6wodb8m5CHaIdIor0pchVansu"
    val fbUserId = new FBUserId(947459488606587L)
    val appId = "800587516688651"

    val authInfo = new FBAuthInfo(fbUserId, FBAuthToken(authToken))
    implicit val timeout = Timeout(2 seconds)
    val result = supervisor ? AuthenticateUser(authInfo, None)
    // val authenticator = new FacebookAuthenticator

    // authenticator.getUsersFacebookInfo(authInfo)

  }

}