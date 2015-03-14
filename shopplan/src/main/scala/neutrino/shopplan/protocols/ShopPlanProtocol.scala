package neutrino.shopplan.protocols

import scala.collection.Set

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

trait ShopPlanSupervisorMessages extends Serializable
case class NewShopPlanFor(userId: UserId) extends ShopPlanSupervisorMessages
case class AddDestinations(destReqs: Set[AddDestinationReq]) extends ShopPlanSupervisorMessages
case class RemoveDestinations(destIds: Set[DestinationId]) extends ShopPlanSupervisorMessages




sealed trait ShopPlanMessages extends Serializable {
  def shopplanId: ShopPlanId
}

case class AddStores(shopplanId: ShopPlanId, storeIds: Set[StoreId]) extends ShopPlanMessages
case class GetShopPlanFor(shopplanId: ShopPlanId) extends ShopPlanMessages
case class RemoveStores(shopplanId: ShopPlanId, storeIds: Set[StoreId]) extends ShopPlanMessages
case class AddItems(shopplanId: ShopPlanId, itemIds: Set[CatalogueItemId]) extends ShopPlanMessages
case class RemoveItems(shopplanId: ShopPlanId, itemIds: Set[CatalogueItemId]) extends ShopPlanMessages
case class InviteUsers(shopplanId: ShopPlanId, userIds: Set[UserId]) extends ShopPlanMessages
case class RemoveUsersFromInvites(shopplanId: ShopPlanId, userIds: Set[UserId]) extends ShopPlanMessages
case class GetInvitedUsers(shopplanId: ShopPlanId) extends ShopPlanMessages
case class GetMapLocations(shopplanId: ShopPlanId) extends ShopPlanMessages
case class GetDestinations(shopplanId: ShopPlanId) extends ShopPlanMessages
case class AddDestination(destReq: AddDestinationReq) extends ShopPlanMessages {
  val shopplanId = destReq.id.shopplanId
}

case class RemoveDestination(destId: DestinationId) extends ShopPlanMessages {
  val shopplanId = destId.shopplanId
}

case class UpdateDestination(destId: DestinationId, location: GPSLocation) extends ShopPlanMessages {
  val shopplanId = destId.shopplanId
}
