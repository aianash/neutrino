package neutrino.core.user

case class UserId(uuid: Long)
case class Username(username: String)
case class DisplayName(first: String, last: String)
case class Email(email: String)
case class Mobile(mobile: String)
case class UserAvatar(small: String, medium: String, large: String)
case class Gender(value: String)
case class Locale(value: String)
case class Location(city: String, country: String)

//user type
sealed trait UserType {
  def value: String
}

object UserType {

  def apply(userType: String) = userType match {
    case "registered" => REGISTERED
    case "guest"      => GUEST
  }

  case object REGISTERED extends UserType {
    val value = "registered"
    override def toString = value
  }

  case object GUEST extends UserType {
    val value = "guest"
    override def toString = value
  }

}

case class User(
  username : Option[Username],
  name     : Option[DisplayName],
  email    : Option[Email],
  mobile   : Option[Mobile],
  avatar   : Option[UserAvatar],
  gender   : Option[Gender],
  locale   : Option[Locale],
  location : Option[Location],
  usertype : UserType
  )