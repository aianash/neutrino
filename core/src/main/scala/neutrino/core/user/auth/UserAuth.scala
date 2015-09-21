package neutrino.core.user.auth

import neutrino.core.user._

case class FBUserId(id: Long)
case class FBAuthToken(value: String)
case class GoogleUserId(id: Long)
case class GoogleAuthToken(value: String)

// user auth info
case class UserAuth(
  userId          : UserId,
  fbUserId        : Option[FBUserId],
  fbAuthToken     : Option[FBAuthToken],
  googleUserId    : Option[GoogleUserId],
  googleAuthToken : Option[GoogleAuthToken]
  )

// Fb auth info
case class FBAuth(
  fbUserId : FBUserId,
  token    : FBAuthToken,
  userId   : UserId
  )

// Google auth info
case class GoogleAuth(
  googleUserId : GoogleUserId,
  token        : GoogleAuthToken,
  userId       : UserId
  )

