package neutrino.service

import scala.util.{Failure => TFailure, Success => TSuccess, Try}
import scala.concurrent._, duration._

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import scaldi.Injector
import scaldi.akka.AkkaInjectable._

import akka.actor.{Actor, Props}
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.event.Logging

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.neutrino.shopplan._

import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.Thrift
import com.twitter.bijection._, twitter_util.UtilBijections._
import com.twitter.bijection.Conversion.asMethod

import com.typesafe.config.{Config, ConfigFactory}

import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.Thrift

import com.typesafe.config.{Config, ConfigFactory}


class NeutrinoService(implicit inj: Injector) extends Neutrino[TwitterFuture] {

  import NeutrinoService._

  implicit val system = inject [ActorSystem]

  val log = Logging.getLogger(system, this)

  import system._ // importing execution context from implicit dispatcher

  val settings = NeutrinoSettings(system)



  def newShopPlanFor(userId: UserId) = {
    val shopplanId = ShopPlanId(userId, 2L);

    TwitterFuture.value(ShopPlan(shopplanId))
  }

  def getShopPlan(shopplanId: ShopPlanId) = {
    TwitterFuture.value(ShopPlan(shopplanId))
  }

  def addStores(shopplanId: ShopPlanId, storeIds: Seq[StoreId]) = {
    TwitterFuture.value(true)
  }

  def removeStores(shopplanId: ShopPlanId, storeIds: Seq[StoreId]) = {
    TwitterFuture.value(true)
  }

  def addItems(shopplanId: ShopPlanId, itemIds: Seq[CatalogueItemId]) = {
    TwitterFuture.value(true)
  }

  def removeItems(shopplanId: ShopPlanId, itemIds: Seq[CatalogueItemId]) = {
    TwitterFuture.value(true)
  }

  def inviteUsers(shopplanId: ShopPlanId, userIds: Seq[UserId]) = {
    TwitterFuture.value(true)
  }

  def removeUsersFromInvites(shopplanId: ShopPlanId, userIds: Seq[UserId]) = {
    TwitterFuture.value(true)
  }

  def getInvitedUsers(shopplanId: ShopPlanId) = {
    TwitterFuture.value(List.empty[Friend])
  }

  def getMapLocations(shopplanId: ShopPlanId) = {
    TwitterFuture.value(List.empty[GPSLocation])
  }

  def getDestinations(shopplanId: ShopPlanId) = {
    TwitterFuture.value(List.empty[GPSLocation])
  }

  def addDestinations(destReqs: Seq[AddDestinationReq]) = {
    TwitterFuture.value(true)
  }

  def removeDestinations(destinations: Seq[DestinationId]) = {
    TwitterFuture.value(true)
  }

  def updateDestinationLoc(destId: DestinationId, location: GPSLocation) = {
    TwitterFuture.value(true)
  }

}


object NeutrinoService {

  def start(implicit inj: Injector) = {
    val settings = NeutrinoSettings(inject [ActorSystem])
    Thrift.serveIface(settings.NeutrinoEndpoint, inject [NeutrinoService])
  }
}