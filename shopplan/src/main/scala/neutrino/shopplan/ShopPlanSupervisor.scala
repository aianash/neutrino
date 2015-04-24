package neutrino.shopplan

import scala.concurrent._, duration._
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import akka.actor.{PoisonPill, Terminated}
import akka.util.Timeout
import akka.pattern.pipe

import com.goshoplane.neutrino.shopplan._

import goshoplane.commons.core.protocols._, Implicits._

import neutrino.core._
import neutrino.core.services._
import neutrino.shopplan.store._

import scalaz._, Scalaz._

/**
 * Supervises lifecycle of ShopPlanner instances and other adjacent services like
 */
class ShopPlanSupervisor(
  _Bucket: ActorRef @@ BucketActorRef,
  _User  : ActorRef @@ UserActorRef) extends Actor with ActorLogging {


  private val settings = ShopPlanSettings(context.system)

  import neutrino.shopplan.protocols._
  import context.dispatcher
  import settings._

  private val UUID   = context.actorOf(UUIDGenerator.props(ServiceId, DatacenterId))
  private val Bucket = Tag.unwrap(_Bucket)
  private val User   = Tag.unwrap(_User)


  // Watching these actors for any termination.
  // This supervisor is heavily dependent
  // on these actors for most of its operations.
  context watch UUID
  context watch Bucket
  context watch User


  // [TO IMPROVE] In order to throttle read and write requests for ShopPlan related data
  // we could create separate endpoints for write(persister) and read(retriever)
  private val shopplanDatastore = new ShopPlanDatastore(settings)
  shopplanDatastore.init()


  def receive = {

    case GetOwnShopPlans(userId, fields) =>
      shopplanDatastore.getOwnShopPlans(userId, fields) pipeTo sender()



    case GetInvitedShopPlans(userId, fields) =>
      shopplanDatastore.getInvitedShopPlans(userId, fields) pipeTo sender()



    case GetShopPlan(shopplanId, fields) =>
      shopplanDatastore.getShopPlan(shopplanId, fields) pipeTo sender()



    case GetShopPlanStores(shopplanId, fields) =>
      shopplanDatastore.getStores(shopplanId, fields) pipeTo sender()



    case CreateShopPlan(userId, cud) =>
      implicit val timeout = Timeout(1 seconds)

      val shopplanIdF =
        for {
          suid        <- UUID ?= NextId("shopplan")
          shopplanId  <- Future.successful(ShopPlanId(suid = suid, createdBy = userId))
          _           <- getOrCreateShopPlanner(shopplanId) ?= AddNewShopPlan(shopplanId, cud)
        } yield shopplanId

      shopplanIdF pipeTo sender()



    case msg @ ModifyShopPlan(shopplanId, _) =>
      implicit val timeout = Timeout(1 seconds)

      (getOrCreateShopPlanner(shopplanId) ?= msg) pipeTo sender()



    case EndShopPlan(shopplanId) =>
      context stop getOrCreateShopPlanner(shopplanId)
      shopplanDatastore.deleteShopPlan(shopplanId) pipeTo sender()

  }



  def shuttingDown: Receive = {
    case m: ShopPlanSupervisorMessages =>
      sender() ! ServiceUnavailable("ShopPlanSupervisor in shutdown mode")

    case m: ShopPlanMessages =>
      sender() ! ServiceUnavailable("ShopPlanSupervisor in shutdown mode")

    case Terminated(child) =>
      if(context.children.isEmpty) context stop self
  }


  private def getOrCreateShopPlanner(shopplanId: ShopPlanId) = {
    val name = ShopPlanner.actorNameFor(shopplanId)

    context.child(name) getOrElse {
      val planner = context.actorOf(ShopPlanner.props(shopplanId, shopplanDatastore, _User, _Bucket), name)
      context watch planner
      planner
    }
  }

}



object ShopPlanSupervisor {
  def props(bucket: ActorRef @@ BucketActorRef,
            user  : ActorRef @@ UserActorRef) =
    Props(classOf[ShopPlanSupervisor], bucket, user)
}