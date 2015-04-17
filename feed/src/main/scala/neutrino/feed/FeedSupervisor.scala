package neutrino.feed

import akka.actor.{Props, Actor, ActorLogging}

import protocols._

import com.goshoplane.neutrino.feed._

import goshoplane.commons.core.protocols.Implicits._

/**
 * [IMP] Complete implementation only after
 * Social network until then its hardcoded
 * feed here
 */
class FeedSupervisor extends Actor with ActorLogging {

  def receive = {
    case GetCommonFeed(filter) => sender() ! HardcodedCommonFeed

    case GetUserFeed(userId, filter) => sender() ! HardcodedCommonFeed
  }

}


object FeedSupervisor {
  def props = Props(classOf[FeedSupervisor])
}


object HardcodedCommonFeed extends Feed {

  def offerPosts = Seq.empty[OfferPost]
  def posterAdPosts = Seq.empty[PosterAdPost]
  def page = 1

}