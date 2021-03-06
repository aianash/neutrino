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
  def getUserInfo(implicit ec: ExecutionContext): Future[UserInfo]
  def addAuthInfo(userId: UserId, userInfo: UserInfo): Future[Boolean]
  def updateAuthInfo(userId: UserId, userInfo: UserInfo): Future[Boolean]
  def getExternalAccountInfo: ExternalAccountInfo

}

class SocialLoginHandlerFactory(settings: AuthSettings) {

  private val fbAuthDatastore = new FBAuthDatastore(settings)
  fbAuthDatastore.init()

  private val googleAuthDatastore = new GoogleAuthDatastore(settings)
  googleAuthDatastore.init()

  def instanceFor(authInfo: SocialAuthInfo) = authInfo match {
    case fbAuthInfo: FBAuthInfo         => new FBLoginHandler(fbAuthInfo, fbAuthDatastore, settings)
    case googleAuthInfo: GoogleAuthInfo => new GoogleLoginHandler(googleAuthInfo, googleAuthDatastore, settings)
  }

}