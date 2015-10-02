package neutrino.auth.handler

import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

import play.api.libs.json._

import com.google.api.client.googleapis.auth.oauth2._, GoogleIdToken.Payload
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.oauth2._, Oauth2.Builder
import com.google.api.services.oauth2.model.Userinfoplus

import goshoplane.commons.core.protocols.Implicits._
import goshoplane.commons.core.services.{UUIDGenerator, NextId}

import neutrino.core.auth._
import neutrino.core.user._
import neutrino.auth.store._
import neutrino.auth._


class GoogleLoginHandler(authInfo: GoogleAuthInfo, datastore: GoogleAuthDatastore, settings: AuthSettings) extends SocialLoginHandler {

  private val httpTransport = new NetHttpTransport
  private val jsonFactory   = new JacksonFactory
  private val clientId      = List(settings.GoogleClientId).asJava

  private var isValidated = false
  private var email       = Email("")

  def validate(implicit ec: ExecutionContext) = Future {
    val verifier = new GoogleIdTokenVerifier
        .Builder(httpTransport, jsonFactory)
        .setAudience(clientId)
        .build

    val idToken = verifier.verify(authInfo.idToken.value)

    if(idToken == null) NotValidated
    else {
      val payload = idToken.getPayload
      val userId  = payload.getSubject
      if(clientId.contains(payload.getAuthorizedParty) && userId == authInfo.googleUserId.id) {
        isValidated = true
        email = Email(payload.getEmail)
        Validated
      }
      else NotValidated
    }
  }  recover {
    case NonFatal(ex) => NotValidated
  }

  def getUserId =
    if(isValidated) datastore.getUserId(authInfo.googleUserId, email)
    else throw new Exception("User is not validated")

  def getUserInfo(implicit ec: ExecutionContext) = Future {
    val credential = new GoogleCredential().setAccessToken(authInfo.authToken.value)
    val oauth2 = new Oauth2
        .Builder(new NetHttpTransport(), new JacksonFactory(), credential)
        .setApplicationName("Oauth2").build()

    val response = oauth2.userinfo().get.execute()
    val userInfo = buildUserInfoFromResponse(response)
    userInfo
  }

  def addAuthInfo(userId: UserId, userInfo: UserInfo) = {
    val emailInfo = EmailAuthInfo(email, None, Some(authInfo.googleUserId))
    datastore.addAuthInfo(userId, authInfo, emailInfo)
  }

  def updateAuthInfo(userId: UserId, userInfo: UserInfo) = {
    val emailInfo = EmailAuthInfo(email, None, Some(authInfo.googleUserId))
    datastore.updateAuthInfo(userId, authInfo, emailInfo)
  }

  def getExternalAccountInfo: ExternalAccountInfo =
    ExternalAccountInfo(None, None, Some(authInfo.googleUserId), Some(authInfo.authToken))

  private def buildUserInfoFromResponse(response: Userinfoplus) = {
    val displayName = Some(DisplayName(response.getGivenName, response.getFamilyName))
    val email       = Some(Email(response.getEmail))
    val gender      = Some(Gender(response.getGender))
    val locale      = Some(Locale(response.getLocale))
    val avatar      = Some(UserAvatar(response.getPicture, response.getPicture, response.getPicture))

    UserInfo(None, displayName, email, None, avatar, gender, locale, None, UserType.REGISTERED)
  }
}