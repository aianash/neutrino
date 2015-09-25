package neutrino.auth.protocols

import neutrino.core.auth._
import neutrino.core.user._

sealed trait AuthenticationProtocols
case class AuthenticateUser(authInfo: AuthInfo, suggestedUserId: Option[UserId]) extends AuthenticationProtocols