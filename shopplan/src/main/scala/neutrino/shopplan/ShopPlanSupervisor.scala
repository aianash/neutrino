package neutrino.shopplan

import akka.actor.{Actor, ActorLogging, Props}

import neutrino.shopplan.protocols._

class ShopPlanSupervisor extends Actor with ActorLogging {

  def receive = {
    case NewShopPlanFor(userId) =>

    case AddDestinations(destReqs) =>

    case RemoveDestinations(destIds) =>

    case req: ShopPlanMessages =>
      req.shopplanId


  }

}

object ShopPlanSupervisor {
  def props = Props(new ShopPlanSupervisor)
}