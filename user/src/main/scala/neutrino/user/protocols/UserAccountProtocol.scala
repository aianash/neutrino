package neutrino.user.protocols

import goshoplane.commons.core.protocols.Replyable

import neutrino.core.user._
import neutrino.core.user.auth._

sealed trait UserAccountMessages

// create
case class CreateOrUpdateUserWithFB(userInfo: User, fbInfo: FBAuth) extends UserAccountMessages with Replyable[UserId]
case class CreateOrUpdateUserWithGoogle(userInfo: User, googleInfo: GoogleAuth) extends UserAccountMessages with Replyable[UserId]

// update
case class UpdateUserInfo(userInfo: User) extends UserAccountMessages with Replyable[Boolean]
case class UpdateGoogleInfo(googleInfo: GoogleAuth) extends UserAccountMessages with Replyable[Boolean]
case class UpdateFBInfo(gbInfo: FBAuth) extends UserAccountMessages with Replyable[Boolean]

// read
case class GetUserDetail(userId: UserId) extends UserAccountMessages with Replyable[User]