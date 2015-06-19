package neutrino.service

import scala.util.{Failure => TFailure, Success => TSuccess, Try}
import scala.util.control.NonFatal
import scala.concurrent._, duration._
import scala.collection.Set

import java.net.InetSocketAddress

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
import com.goshoplane.neutrino.feed._
import com.goshoplane.creed.search._

import goshoplane.commons.core.protocols.Implicits._

import neutrino._, core._
import shopplan._, shopplan.protocols._
import user._, user.protocols._
import bucket._, bucket.protocols._
import feed._, feed.protocols._
import search._, search.protocols._

import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.Thrift
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.bijection._, twitter_util.UtilBijections._
import com.twitter.bijection.Conversion.asMethod

import com.typesafe.config.{Config, ConfigFactory}

import org.apache.thrift.protocol.TBinaryProtocol


class NeutrinoService(implicit inj: Injector) extends Neutrino[TwitterFuture] {

  import NeutrinoService._

  implicit val system = inject [ActorSystem]

  val log = Logging.getLogger(system, this)

  import system._ // importing execution context from implicit dispatcher

  val settings = NeutrinoSettings(system)

  val User     = system.actorOf(UserAccountSupervisor.props,  "user")
  val Bucket   = system.actorOf(BucketSupervisor.props,       "bucket")
  val Feed     = system.actorOf(FeedSupervisor.props,         "feed")
  val Search   = system.actorOf(SearchSupervisor.props,       "search")


  val ShopPlan = system.actorOf(ShopPlanSupervisor.props(BucketActorRef(Bucket), UserActorRef(User)), "shopplan")

  implicit val defaultTimeout = Timeout(10 seconds)


  ///////////////
  // User APIs //
  ///////////////

  def createUser(userInfo: UserInfo) = {
    val userIdF = User ?= CreateOrUpdateUser(userInfo)

    awaitResult(userIdF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while creating/updating user for " +
                        s"facebook user id = ${userInfo.facebookInfo.map(_.userId)}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })

  }


  def updateUser(userId: UserId, userInfo: UserInfo) = {
    val successF = User ?= UpdateUser(userId, userInfo)

    awaitResult(successF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while updating user info for " +
                        s"user id = ${userId.uuid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def getUserDetail(userId: UserId) = {
    val infoF = User ?= GetUserDetail(userId)

    awaitResult(infoF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting user detail for " +
                        s"user id = ${userId.uuid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def getFriendsForInvite(userId: UserId, filter: FriendListFilter) = {
    val friendsF = User ?= GetFriendsForInvite(userId, filter)

    awaitResult(friendsF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting friends to invite for " +
                        s"user id = ${userId.uuid} " +
                        s"and filter = ${filter.location}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  /////////////////
  // Bucket APIs //
  /////////////////

  def getBucketStores(userId: UserId, fields: Seq[BucketStoreField]) = {
    val storesF = Bucket ?= GetBucketStores(userId, fields)

    awaitResult(storesF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting bucket stores for " +
                        s"user id = ${userId.uuid} " +
                        s"and fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def cudBucket(userId: UserId, cud: CUDBucket) = {
    val successF = Bucket ?= ModifyBucket(userId, cud)

    awaitResult(successF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while cud operation on bucket for " +
                        s"user id = ${userId.uuid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }

  ///////////////////
  // ShopPlan APIs //
  ///////////////////

  def getShopPlanStores(shopplanId: ShopPlanId, fields: Seq[ShopPlanStoreField]) = {
    val storesF = ShopPlan ?= GetShopPlanStores(shopplanId, fields)

    awaitResult(storesF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting shop plan stores for " +
                        s"shop plan id = ${shopplanId.createdBy.uuid}.${shopplanId.suid} " +
                        s"fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def getOwnShopPlans(userId: UserId, fields: Seq[ShopPlanField]) = {
    val shopplansF = ShopPlan ?= GetOwnShopPlans(userId, fields)

    awaitResult(shopplansF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting user's own shop plan for " +
                        s"user id = ${userId.uuid} " +
                        s"and fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def getInvitedShopPlans(userId: UserId, fields: Seq[ShopPlanField]) = {
    val shopplansF = ShopPlan ?= GetInvitedShopPlans(userId, fields)

    awaitResult(shopplansF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting invited shop plans for " +
                        s"user id = ${userId.uuid} " +
                        s"and fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def getShopPlan(shopplanId: ShopPlanId, fields: Seq[ShopPlanField]) = {
    val shopplanF = ShopPlan ?= GetShopPlan(shopplanId, fields)

    awaitResult(shopplanF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting shop plan detail for " +
                        s"shop plan id = ${shopplanId.createdBy.uuid}.${shopplanId.suid} " +
                        s"and fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def createShopPlan(userId: UserId, cud: CUDShopPlan) = {
    val shopplanIdF = ShopPlan ?= CreateShopPlan(userId, cud)

    awaitResult(shopplanIdF, 5 seconds, {
      case NonFatal(ex) =>
        val statement = s"Error while creating shop plan for " +
                        s"user id = ${userId.uuid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def cudShopPlan(shopplanId: ShopPlanId, cud: CUDShopPlan) = {
    val successF = ShopPlan ?= ModifyShopPlan(shopplanId, cud)

    awaitResult(successF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while performing cud operation on shop plan for " +
                        s"shop plan id = ${shopplanId.createdBy.uuid}.${shopplanId.suid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def endShopPlan(shopplanId: ShopPlanId) = {
    val successF = ShopPlan ?= EndShopPlan(shopplanId)

    awaitResult(successF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while ending shop plan for " +
                        s"shop plan id = ${shopplanId.createdBy.uuid}.${shopplanId.suid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  ///////////////
  // Feed APIs //
  ///////////////

  def getCommonFeed(filter: FeedFilter) = {
    val feedF = Feed ?= GetCommonFeed(filter)

    awaitResult(feedF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting common feed for " +
                        s"location = ${filter.location} " +
                        s"page  = ${filter.page}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  def getUserFeed(userId: UserId, filter: FeedFilter) = {
    val feedF = Feed ?= GetUserFeed(userId, filter)

    awaitResult(feedF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting user feed for " +
                        s"user id = ${userId.uuid} " +
                        s"and location = ${filter.location} " +
                        s"and page = ${filter.page}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
    })
  }


  /////////////////
  // Search APIs //
  /////////////////

  def search(request: CatalogueSearchRequest) = {
    val resultF = Search ?= SearchCatalogue(request)

    awaitResult(resultF, 1 seconds, {
      case NonFatal(ex) =>
        val statement = s"Error while searching for " +
                        s"search id = ${request.searchId.userId.uuid}.${request.searchId.sruid}"

        log.error(ex, statement)
        TFailure(NeutrinoException(statement))
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

    val protocol = new TBinaryProtocol.Factory()
    val service  = new Neutrino$FinagleService(inject [NeutrinoService], protocol)
    val address  = new InetSocketAddress(settings.NeutrinoPort)

    ServerBuilder()
      .codec(ThriftServerFramedCodec())
      .name(settings.ServiceName)
      .bindTo(address)
      .build(service)
  }
}