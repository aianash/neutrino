package neutrino.auth.restfb

import scala.collection.JavaConverters._

import com.restfb.JsonMapper

import play.api.libs.json._

import neutrino.core.auth._
import neutrino.core.user._

import neutrino.auth.handler._


class CustomJsonMapper extends JsonMapper {

  private val USERINFO  = classOf[User]


  def toJavaObject[T](response: String, clazz: Class[T]): T = clazz match {
    case USERINFO  => buildUserInfoFromJson(response).asInstanceOf[T]
    case _         => ???.asInstanceOf[T]
  }

  def toJavaList[T](response: String, clazz: Class[T]): java.util.List[T] = {
    val json = Json.parse(response).as[List[JsValue]]
    val list = json.map(x => toJavaObject(Json.stringify(x), clazz))
    list.asJava
  }

  def toJson(obj: Object) = ""

  def toJson(obj: Object, ignoreNullValuedProperties: Boolean) = ""

  private def buildUserInfoFromJson(response: String) = {
    val mejson      = Json.parse(response)
    val picturejson = Json.parse(response)

    val displayname =
      for {
        first <- (mejson \ "first_name").asOpt[String]
        last  <- (mejson \ "last_name").asOpt[String]
      } yield DisplayName(first, last)

    val email  = (mejson \ "email").asOpt[String].map(Email(_))
    val mobile = (mejson \ "mobile").asOpt[String].map(Mobile(_))
    val gender = (mejson \ "gender").asOpt[String].map(Gender(_))
    val locale = (mejson \ "locale").asOpt[String].map(Locale(_))

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