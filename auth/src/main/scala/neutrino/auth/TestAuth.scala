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

    val authToken = "CAALYIU6LOQsBAIfQaIPVpabZCyiYWZC7oByu5QoUg8xb6ydZAjkdk9uEO9ZBothGg7mHVa8uMfWpc6jZCceH2yhZC26DN1VHV0G9i26fQS4n2FyNANXIJDO7j5VoDOZBC85X8jc314z6yTDCGZAPJhxDjG5uPkUcTcxSVPl9rbTIAPeRVZBuXOntKBeRH7wsbbBbofSJTxGqjoOZCMethVM8pV"
    val fbUserId = new FBUserId(947459488606587L)
    val appId = "800587516688651"

    val authInfo = new FBAuthInfo(fbUserId, FBAuthToken(authToken))
    implicit val timeout = Timeout(2 seconds)
    val result = supervisor ? AuthenticateUser(authInfo, None)
    // val authenticator = new FacebookAuthenticator

    // authenticator.getUsersFacebookInfo(authInfo)

  }

}