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
case class UserType(value: String)


case class User(
  userId   : UserId,
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