package neutrino.user.protocols

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._


import neutrino.core.protocols.Replyable

sealed trait UserAccountMessages

case class CreateUser(userInfo: UserInfo) extends UserAccountMessages with Replyable[UserId]
case class UpdateUser(userId: UserId, userInfo: UserInfo) extends UserAccountMessages with Replyable[Boolean]
case class GetUserDetail(userId: UserId) extends UserAccountMessages with Replyable[UserInfo]
case class GetFriendsForInvite(userId: UserId, filter: FriendListFilter) extends UserAccountMessages with Replyable[Seq[Friend]]