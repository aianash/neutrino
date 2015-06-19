package neutrino.feed.protocols

import com.goshoplane.common._
import com.goshoplane.neutrino.feed._

import goshoplane.commons.core.protocols.Replyable

sealed trait FeedMessages

case class GetCommonFeed(filter: FeedFilter) extends FeedMessages with Replyable[Feed]
case class GetUserFeed(userId: UserId, filter: FeedFilter) extends FeedMessages with Replyable[Feed]
