package neutrino.service

import scala.util.{Failure => TFailure, Success => TSuccess, Try}
import scala.concurrent._, duration._
import scala.collection.Set

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import scaldi.Injector
import scaldi.akka.AkkaInjectable._

import akka.actor.{Actor, Props}
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.event.Logging
import akka.pattern.{ask, pipe}

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._

import neutrino.shopplan._, protocols._

import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.Thrift
import com.twitter.bijection._, twitter_util.UtilBijections._
import com.twitter.bijection.Conversion.asMethod

import com.typesafe.config.{Config, ConfigFactory}



class NeutrinoService(implicit inj: Injector) extends Neutrino[TwitterFuture] {

  import NeutrinoService._

  implicit val system = inject [ActorSystem]

  val log = Logging.getLogger(system, this)

  import system._ // importing execution context from implicit dispatcher

  val settings = NeutrinoSettings(system)

  val ShopPlan = system.actorOf(ShopPlanSupervisor.props, "shopplan")


  implicit val defaultTimeout = Timeout(1 seconds)


  def newShopPlanFor(userId: UserId) = {

    val shopPlanF = (ShopPlan ? NewShopPlanFor(userId)).mapTo[ShopPlan]

    awaitResult(shopPlanF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Couldn't create new shop plan for " + userId))
    })
  }



  def getShopPlan(shopplanId: ShopPlanId) = {
    val shopPlanF = (ShopPlan ? GetShopPlanFor(shopplanId)).mapTo[ShopPlan]

    awaitResult(shopPlanF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while getting shop plan for " + shopplanId))
    })
  }



  def addStores(shopplanId: ShopPlanId, storeIds: scala.collection.Set[StoreId]) = {
    val successF = (ShopPlan ? AddStores(shopplanId, storeIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while adding stores to plan" + shopplanId))
    })
  }



  def removeStores(shopplanId: ShopPlanId, storeIds: Set[StoreId]) = {
    val successF = (ShopPlan ? RemoveStores(shopplanId, storeIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while removing stores from shop plan " + shopplanId))
    })
  }



  def addItems(shopplanId: ShopPlanId, itemIds: Set[CatalogueItemId]) = {
    val successF = (ShopPlan ? AddItems(shopplanId, itemIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while adding item to stores"))
    })
  }



  def removeItems(shopplanId: ShopPlanId, itemIds: Set[CatalogueItemId]) = {
    val successF = (ShopPlan ? RemoveItems(shopplanId, itemIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while removing items from from shop plan " + shopplanId))
    })
  }



  def inviteUsers(shopplanId: ShopPlanId, userIds: Set[UserId]) = {
    val successF = (ShopPlan ? InviteUsers(shopplanId, userIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while sending invites for shop plan " + shopplanId))
    })
  }



  def removeUsersFromInvites(shopplanId: ShopPlanId, userIds: Set[UserId]) = {
    val successF = (ShopPlan ? RemoveUsersFromInvites(shopplanId, userIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while removing invites for shop plan " + shopplanId))
    })
  }



  def getInvitedUsers(shopplanId: ShopPlanId) = {
    val usersF = (ShopPlan ? GetInvitedUsers(shopplanId)).mapTo[Set[Friend]]

    awaitResult(usersF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while fetching invited users"))
    })
  }



  def getMapLocations(shopplanId: ShopPlanId) = {
    val locsF = (ShopPlan ? GetMapLocations(shopplanId)).mapTo[Set[GPSLocation]]

    awaitResult(locsF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while fetching invited users"))
    })
  }



  def getDestinationLocs(shopplanId: ShopPlanId) = {
    val locsF = (ShopPlan ? GetDestinations(shopplanId)).mapTo[Set[GPSLocation]]

    awaitResult(locsF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while fetching invited users"))
    })
  }



  def addDestinations(destReqs: Set[AddDestinationReq]) = {
    val boolF = (ShopPlan ? AddDestinations(destReqs)).mapTo[Boolean]

    awaitResult(boolF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while fetching invited users"))
    })
  }


  def removeDestinations(destIds: Set[DestinationId]) = {
    val successF = (ShopPlan ? RemoveDestinations(destIds)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while fetching invited users"))
    })
  }


  def updateDestinationLoc(destId: DestinationId, location: GPSLocation) = {
    val successF = (ShopPlan ? UpdateDestination(destId, location)).mapTo[Boolean]

    awaitResult(successF, defaultTimeout.duration, {
      case ex: Throwable =>
        TFailure(NeutrinoException("Error while fetching invited users"))
    })

  }


  /**
   * A helper method to await on Scala Future and encapsulate the result into TwitterFuture
   */
  private def awaitResult[T, U >: T](future: Future[T], timeout: Duration, ex: PartialFunction[Throwable, Try[U]]): TwitterFuture[U] = {
    TwitterFuture.value(Try {
      Await.result(future, timeout)
    } recoverWith(ex) get)
  }

}


object NeutrinoService {

  def start(implicit inj: Injector) = {
    val settings = NeutrinoSettings(inject [ActorSystem])
    Thrift.serveIface(settings.NeutrinoEndpoint, inject [NeutrinoService])
  }
}