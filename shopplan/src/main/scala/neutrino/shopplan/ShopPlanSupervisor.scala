package neutrino.shopplan

import scala.concurrent._, duration._

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import akka.actor.{PoisonPill, Terminated}
import akka.util.Timeout
import akka.pattern.pipe

import com.goshoplane.neutrino.shopplan._

import neutrino.core.protocols._
import neutrino.core.services._
import neutrino.shopplan.store._

import scalaz._

/**
 * Supervises ShopPlanner and other adjacent services like
 * UUID generator and shop plan persister
 */
class ShopPlanSupervisor extends Actor with ActorLogging {

  import neutrino.shopplan.protocols._

  import context.dispatcher

  private[this] val settings = ShopPlanSettings(context.system)

  private[this] val store: ShopPlanStore = new CassandraShopPlanStore

  private[this] val uuid =
    context.actorOf(UUIDGenerator.props(settings.ServiceId, settings.DatacenterId))

  // [EXPERIMENTAL] In order to throttle read and write requests for ShopPlan related data
  // we have separate endpoints for write(persister) and read(retriever)
  private[this] val persister =
    Tag[ActorRef, Persister](
      context.actorOf(ShopPlanPersister.props(store, settings.NrOfPersisterInstances)))

  private[this] val retriever =
    Tag[ActorRef, Retriever](
      context.actorOf(ShopPlanRetriever.props(store, settings.NrOfRetrieverInstances)))


  context watch uuid
  context watch persister
  context watch retriever


  def receive = {

    case NewShopPlanFor(userId) =>
      implicit val timeout = Timeout(1 seconds)

      val shopPlanF =
        for {
          suid        <- uuid ?= NextId("shopplan")
          shopplanId  <- Future.successful(ShopPlanId(suid = suid, createdBy = userId))
          _           <- persister ?= CreateNewShopPlan(shopplanId)
        } yield ShopPlan(shopplanId)

      shopPlanF pipeTo sender()



    case AddDestinations(destReqs) =>
      implicit val timeout = Timeout(1 seconds)

      val successesF =
        destReqs map { addReq =>
          val planner = getOrCreateShopPlanner(addReq.id.shopplanId)
          planner ?= AddDestination(addReq)
        }

      Future.fold(successesF)(true) {_ && _} pipeTo sender()



    case RemoveDestinations(destIds) =>
      implicit val timeout = Timeout(1 seconds)

      val successesF =
        destIds map { destId =>
          val planner = getOrCreateShopPlanner(destId.shopplanId)
          planner ?= RemoveDestination(destId)
        }

      Future.fold(successesF)(true) {_ && _} pipeTo sender()



    /**
     * ShopPlan messages are forwarded
     */
    case req: ShopPlanMessages =>
      getOrCreateShopPlanner(req.shopplanId) forward req



    case Shutdown =>
      context.children foreach { child =>
        child ! PoisonPill
      }

      context become shuttingDown
  }


  def shuttingDown: Receive = {
    case m: ShopPlanSupervisorMessages =>
      sender() ! ServiceUnavailable("ShopPlanSupervisor in shutdown mode")

    case m: ShopPlanMessages =>
      sender() ! ServiceUnavailable("ShopPlanSupervisor in shutdown mode")

    case Terminated(child) =>
      if(context.children.isEmpty) context stop self
  }


  private def getOrCreateShopPlanner(id: ShopPlanId) = {
    val name = ShopPlanner.actorNameFor(id)

    context.child(name) getOrElse {
      val planner = context.actorOf(ShopPlanner.props(persister, retriever), name)
      context watch planner
      planner
    }
  }

}



object ShopPlanSupervisor {
  def props = Props(new ShopPlanSupervisor)
}