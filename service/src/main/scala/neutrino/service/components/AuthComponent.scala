package neutrino.service.components

import akka.actor.ActorSystem

import commons.microservice.Component

import neutrino.auth._

case object AuthComponent extends Component {
  val name = "authentication-supervisor"
  val runOnRole = "authentication-supervisor"
  def start(system: ActorSystem) = {
    system.actorOf(AuthenticationSupervisor.props, name)
  }
}