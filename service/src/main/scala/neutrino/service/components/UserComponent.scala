package neutrino.service.components

import akka.actor.ActorSystem

import commons.microservice._
import neutrino.user.UserAccountSupervisor

case object UserComponent extends Component {
  val identifiedBy = "user"
  def start(system: ActorSystem) {
    system.actorOf(UserAccountSupervisor.props, "user")
  }
}