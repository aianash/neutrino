package neutrino.core.auth

import neutrino.core.user._

sealed trait SocialUserId
sealed trait SocialAuthToken

case class FBUserId(id: Long) extends SocialUserId
case class FBAuthToken(value: String) extends SocialAuthToken

case class GoogleUserId(id: String) extends SocialUserId
case class GoogleAuthToken(value: String) extends SocialAuthToken
case class GoogleIdToken(value: String)

// linked profiles
case class ExternalAccountInfo (
  fbUserId        : Option[FBUserId],
  fbAuthToken     : Option[FBAuthToken],
  googleUserId    : Option[GoogleUserId],
  googleAuthToken : Option[GoogleAuthToken]
  )

sealed trait SocialAuthInfo

// Fb auth info
case class FBAuthInfo (
  fbUserId : FBUserId,
  authToken    : FBAuthToken
  ) extends SocialAuthInfo

// Google auth info
case class GoogleAuthInfo (
  googleUserId : GoogleUserId,
  authToken    : GoogleAuthToken,
  idToken      : GoogleIdToken
  ) extends SocialAuthInfo

// Email auth info
case class EmailAuthInfo (
  email        : Email,
  fbUserId     : Option[FBUserId],
  googleUserId : Option[GoogleUserId]
)