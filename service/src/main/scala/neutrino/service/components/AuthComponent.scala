package neutrino.service.components

import akka.actor.ActorSystem

import commons.microservice.Component

import neutrino.auth._

case object AuthComponent extends Component {
  val name = "neutrino-auth"
  val runOnRole = "neutrino-auth"
  def start(system: ActorSystem) = {
    system.actorOf(AuthenticationSupervisor.props, name)
  }
}