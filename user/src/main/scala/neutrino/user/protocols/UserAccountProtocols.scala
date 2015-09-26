package neutrino.user.protocols

import goshoplane.commons.core.protocols.Replyable

import neutrino.core.user._
import neutrino.core.auth._

sealed trait UserAccountMessages

// create
case class InsertUser(user: User, info: ExternalAccountInfo) extends UserAccountMessages with Replyable[Boolean]

// update
case class UpdateUser(user: User) extends UserAccountMessages with Replyable[Boolean]
case class UpdateExternalAccountInfo(userId: UserId, info: ExternalAccountInfo) extends UserAccountMessages with Replyable[Boolean]

// read
case class GetUser(userId: UserId) extends UserAccountMessages with Replyable[User]
case class GetExternalAccountInfo(userId: UserId) extends UserAccountMessages with Replyable[ExternalAccountInfo]