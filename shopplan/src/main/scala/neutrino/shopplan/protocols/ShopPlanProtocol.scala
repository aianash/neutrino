package neutrino.shopplan.protocols

import scala.collection.Set

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import neutrino.core.protocols._

sealed trait ShopPlanSupervisorMessages extends Serializable

case class NewShopPlanFor(userId: UserId)
  extends ShopPlanSupervisorMessages with Replyable[ShopPlan]

case class AddDestinations(destReqs: Set[AddDestinationReq])
  extends ShopPlanSupervisorMessages with Replyable[Boolean]

case class RemoveDestinations(destIds: Set[DestinationId])
  extends ShopPlanSupervisorMessages with Replyable[Boolean]







sealed trait ShopPlanMessages extends Serializable {
  def shopplanId: ShopPlanId
}

case class AddStores(shopplanId: ShopPlanId, storeIds: Set[StoreId])
  extends ShopPlanMessages with Replyable[Boolean]

case class GetShopPlanFor(shopplanId: ShopPlanId)
  extends ShopPlanMessages with Replyable[ShopPlan]

case class RemoveStores(shopplanId: ShopPlanId, storeIds: Set[StoreId])
  extends ShopPlanMessages with Replyable[Boolean]


case class AddItems(shopplanId: ShopPlanId, itemIds: Set[CatalogueItemId])
  extends ShopPlanMessages with Replyable[Boolean]

case class RemoveItems(shopplanId: ShopPlanId, itemIds: Set[CatalogueItemId])
  extends ShopPlanMessages with Replyable[Boolean]

case class InviteUsers(shopplanId: ShopPlanId, userIds: Set[UserId])
  extends ShopPlanMessages with Replyable[Boolean]

case class RemoveUsersFromInvites(shopplanId: ShopPlanId, userIds: Set[UserId])
  extends ShopPlanMessages with Replyable[Boolean]

case class GetInvitedUsers(shopplanId: ShopPlanId)
  extends ShopPlanMessages with Replyable[Set[Friend]]

case class GetMapLocations(shopplanId: ShopPlanId)
  extends ShopPlanMessages with Replyable[Set[GPSLocation]]

case class GetDestinations(shopplanId: ShopPlanId)
  extends ShopPlanMessages with Replyable[Set[GPSLocation]]

case class AddDestination(destReq: AddDestinationReq) extends ShopPlanMessages with Replyable[Boolean] {
  val shopplanId = destReq.id.shopplanId
}

case class RemoveDestination(destId: DestinationId) extends ShopPlanMessages with Replyable[Boolean] {
  val shopplanId = destId.shopplanId
}

case class UpdateDestination(destId: DestinationId, location: GPSLocation)
  extends ShopPlanMessages with Replyable[Boolean] {
  val shopplanId = destId.shopplanId
}
