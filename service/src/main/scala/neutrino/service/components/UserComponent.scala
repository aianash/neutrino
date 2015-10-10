package neutrino.service.components

import akka.actor.ActorSystem

import commons.microservice.Component

import neutrino.user._

case object UserComponent extends Component {
  val name = "user-account-supervisor"
  val runOnRole = "neutrino-user"
  def start(system: ActorSystem) = {
    system.actorOf(UserAccountSupervisor.props, name)
  }
}