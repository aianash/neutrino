package neutrino.user.protocols

import goshoplane.commons.core.protocols.Replyable

import neutrino.core.user._
import neutrino.core.auth._

sealed trait UserAccountMessages

// create
case class InsertUserInfo(userId: UserId, user: User, info: ExternalAccountInfo) extends UserAccountMessages with Replyable[Boolean]

// update
case class UpdateUserInfo(userId: UserId, userInfo: User) extends UserAccountMessages with Replyable[Boolean]
case class UpdateExternalAccountInfo(userId: UserId, info: ExternalAccountInfo) extends UserAccountMessages with Replyable[Boolean]

// read
case class GetUserInfo(userId: UserId) extends UserAccountMessages with Replyable[User]
case class GetExternalAccountInfo(userId: UserId) extends UserAccountMessages with Replyable[ExternalAccountInfo]