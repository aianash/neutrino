package neutrino.core.auth

import neutrino.core.user._

sealed trait AuthStatus

object AuthStatus {
  case class Success(userId: UserId, userType: UserType, isNewUser: Boolean) extends AuthStatus

  sealed trait Failure extends AuthStatus
  object Failure {
    case object InternalServerError extends Failure
    case class InvalidCredentials(msg: String) extends Failure
  }
}