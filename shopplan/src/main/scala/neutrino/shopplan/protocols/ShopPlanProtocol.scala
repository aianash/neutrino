package neutrino.shopplan.protocols

import scala.collection.Set

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import neutrino.core.protocols._

sealed trait ShopPlanSupervisorMessages extends Serializable

case class NewShopPlanFor(userId: UserId)
  extends ShopPlanSupervisorMessages with Replyable[ShopPlan]

case class GetOwnShopPlans(userId: UserId, fields: Seq[ShopPlanField])
  extends ShopPlanSupervisorMessages with Replyable[Seq[ShopPlan]]

case class GetInvitedShopPlans(userId: UserId, fields: Seq[ShopPlanField])
  extends ShopPlanSupervisorMessages with Replyable[Seq[ShopPlan]]

case class CreateShopPlan(userId: UserId, cud: CUDShopPlan)
  extends ShopPlanSupervisorMessages with Replyable[ShopPlanId]




sealed trait ShopPlanMessages extends Serializable {
  def shopplanId: ShopPlanId
}

case class GetShopPlanStores(shopplanId: ShopPlanId, fields: Seq[ShopPlanStoreField])
  extends ShopPlanMessages with Replyable[Seq[ShopPlanStore]]

case class GetShopPlan(shopplanId: ShopPlanId, fields: Seq[ShopPlanField])
  extends ShopPlanMessages with Replyable[ShopPlan]

case class AddNewShopPlan(shopplanId: ShopPlanId, cud: CUDShopPlan)
  extends ShopPlanMessages with Replyable[ShopPlanId]

case class ModifyShopPlan(shopplanId: ShopPlanId, cud: CUDShopPlan)
  extends ShopPlanMessages with Replyable[Boolean]

case class EndShopPlan(shopplanId: ShopPlanId)
  extends ShopPlanMessages with Replyable[Boolean]