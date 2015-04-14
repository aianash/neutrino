package neutrino.shopplan

import scala.concurrent.Future

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe

import scalaz._

import com.goshoplane.neutrino.shopplan._
import com.goshoplane.common._

import store.ShopPlanDatastore

class ShopPlanner(shopplanDatastore: ShopPlanDatastore) extends Actor {
  import neutrino.shopplan.protocols._
  import context.dispatcher

  def receive = {

    case AddNewShopPlan(shopplanId, cud) =>
      // assign destination id to shopplan stores

      var shopPlan = ShopPlan(
        shopplanId    = shopplanId,
        title         = cud.meta.flatMap(_.title),
        isInvitation  = false
      )


      shopPlan = cud.invites     .map(c => shopPlan.copy(invites = c.adds))     .getOrElse(shopPlan)
      shopPlan = cud.destinations.map(c => shopPlan.copy(destinations = c.adds)).getOrElse(shopPlan)
      shopPlan = cud.stores      .map(c => shopPlan.copy(stores = c.adds))      .getOrElse(shopPlan)

      shopplanDatastore.addNewShopPlan(shopPlan) pipeTo sender()



    // [IMP] Right now it assumes that cud's shopplan stores
    // are already assigned to proper destinations
    // and thus no re-allocations are required.
    // But this will change soon
    case ModifyShopPlan(shopplanId, cud) =>
      shopplanDatastore.updateShopPlan(shopplanId, cud) pipeTo sender()

  }

}


object ShopPlanner {

  def actorNameFor(id: ShopPlanId) = id.createdBy.uuid + "-" + id.suid

  def props(shopplanDatastore: ShopPlanDatastore) = Props(classOf[ShopPlanner], shopplanDatastore)
}