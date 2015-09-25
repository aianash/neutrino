package neutrino.auth.handler

import scala.concurrent.{Future, ExecutionContext}

import neutrino.core.auth._
import neutrino.core.user._

import neutrino.auth._
import neutrino.auth.store._

sealed trait ValidationResult
case object Validated extends ValidationResult
case object NotValidated extends ValidationResult

trait SocialLoginHandler {

  def validate(implicit ec: ExecutionContext): Future[ValidationResult]
  def getUserId: Future[Option[UserId]]
  def getUserInfo(implicit ec: ExecutionContext): Future[User]
  def updateAuthTable(userId: UserId, userInfo: User): Future[Boolean]

}

class SocialLoginHandlerFactory(settings: AuthSettings) {

  private val fbAuthDatastore = new FBAuthDatastore(settings)
  fbAuthDatastore.init()

  def instanceFor(authInfo: AuthInfo) = authInfo match {
    case fbAuthInfo: FBAuthInfo         => new FBLoginHandler(fbAuthInfo, fbAuthDatastore)
    // case googleAuthInfo: GoogleAuthInfo => new GoogleLoginHandler(googleAuthInfo)
  }

}