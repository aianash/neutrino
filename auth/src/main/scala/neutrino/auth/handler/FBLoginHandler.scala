package neutrino.auth.handler

import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

import play.api.libs.json._

import com.restfb._, exception._
import com.restfb.batch._, BatchRequest._

import neutrino.core.auth._
import neutrino.core.user._
import neutrino.auth.store._
import neutrino.auth.restfb._


case class InvalidCredentialsException(message: String) extends Exception(message)


class FBLoginHandler(authInfo: FBAuthInfo, fbAuthDatastore: FBAuthDatastore) extends SocialLoginHandler {

  private var isValidated = false;
  private var email = Email("");

  private val fbClient = new DefaultFacebookClient(authInfo.token.value, "b59466a3500f21168131758c3dba4ce1", Version.VERSION_2_3)

  def validate(implicit ec: ExecutionContext) = Future {
    val me       = List(new BatchRequestBuilder("me?fields=id,email").build()).asJava
    val resp     = fbClient.executeBatch(me)
    val mejson   = Json.parse(resp.get(0).getBody)
    val fbUserId = (mejson \ "id").as[String].toLong

    if(fbUserId != authInfo.fbUserId.id) NotValidated
    else {
      isValidated = true
      email = Email((mejson \ "email").as[String])
      Validated
    }
  } recover {
    case NonFatal(ex) => NotValidated
  }

  def getUserId =
    fbAuthDatastore.getUserId(authInfo.fbUserId, email)

  def getUserInfo(implicit ec: ExecutionContext) = Future {
    val me = new BatchRequestBuilder("me").build()
    val picture = new BatchRequestBuilder("me/picture?redirect=false&type=normal").build()
    val responses = fbClient.executeBatch(me, picture)
    val userInfo = buildUserInfoFromResponse(responses)
    userInfo
  }

  def updateAuthTable(userId: UserId, userInfo: User) = {
    val email = userInfo.email.get
    val emailInfo = EmailAuthInfo(email, Some(authInfo.fbUserId), None)
    fbAuthDatastore.addFBAuthenticationInfo(userId, authInfo, emailInfo)
  }

  /**
   * Function to read facebook response and creates User
   * @type {[type]}
   */
  private def buildUserInfoFromResponse(responses: java.util.List[BatchResponse]) = {
    val mejson      = Json.parse(responses.get(0).getBody)
    val picturejson = Json.parse(responses.get(1).getBody)

    val displayname =
      for {
        first <- (mejson \ "first_name").asOpt[String]
        last  <- (mejson \ "last_name").asOpt[String]
      } yield DisplayName(first, last)

    val email  = (mejson \ "email").asOpt[String].map(Email(_))
    val mobile = (mejson \ "mobile").asOpt[String].map(Mobile(_))
    val gender = (mejson \ "gender").asOpt[String].map(Gender(_))
    val locale = (mejson \ "locale").asOpt[String].map(Locale(_))

    println((mejson \ "location" \ "name"))
    val location =
      for {
        city    <- (mejson \ "location" \ "name").asOpt[String].map(_.split(",")(0).trim)
        country <- (mejson \ "location" \ "name").asOpt[String].map(_.split(",")(1).trim)
      } yield Location(city, country)

    val usertype = UserType("registered")
    val avatar =
      for {
        small  <- (picturejson \ "data" \ "url").asOpt[String]
        medium <- (picturejson \ "data" \ "url").asOpt[String]
        large  <- (picturejson \ "data" \ "url").asOpt[String]
      } yield UserAvatar(small, medium, large)

    User(None, displayname, email, mobile, avatar, gender, locale, location, usertype)
  }

}