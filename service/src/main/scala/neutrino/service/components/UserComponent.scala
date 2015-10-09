package neutrino.service.components

import akka.actor.ActorSystem

import commons.microservice.Component

import neutrino.user._

case object UserComponent extends Component {
  val name = "user-account-supervisor"
  val runOnRole = "user-account-supervisor"
  def start(system: ActorSystem) = {
    system.actorOf(UserAccountSupervisor.props, name)
  }
}