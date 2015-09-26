package neutrino.user.store

import scala.concurrent.Future
import scala.collection.mutable.{Seq => MutableSeq}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._
import com.websudos.phantom.builder.query.CQLQuery
import com.websudos.phantom.builder.clauses.UpdateClause

import neutrino.core.user._
import neutrino.user._


sealed class UserInfos extends CassandraTable[ConcreteUserInfos, User] {

  override def tableName = "user_infos"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]

  // username
  object username extends OptionalStringColumn(this)

  // name
  object first extends OptionalStringColumn(this)
  object last extends OptionalStringColumn(this)

  // gender
  object gender extends OptionalStringColumn(this)

  // email
  object email extends OptionalStringColumn(this)

  // mobile
  object mobile extends OptionalStringColumn(this)

  // locale
  object locale extends OptionalStringColumn(this)

  // location
  object city extends OptionalStringColumn(this)
  object country extends OptionalStringColumn(this)

  // avatar
  object avatarsmall extends OptionalStringColumn(this)
  object avatarmedium extends OptionalStringColumn(this)
  object avatarlarge extends OptionalStringColumn(this)

  // usertype
  object usertype extends StringColumn(this)


  def fromRow(row: Row) = {
    val usernameO = username(row).map(d => Username(d))

    val displayNameO = 
      for {
        first <- first(row)
        last  <- last(row)
      } yield DisplayName(first, last)

    val avatarO = 
      for {
        small  <- avatarsmall(row)
        medium <- avatarmedium(row)
        large  <- avatarlarge(row)
      } yield UserAvatar(small, medium, large)

    val genderO = gender(row).map(Gender(_))

    val localeO = gender(row).map(d => Locale(d))

    val locationO = for {
      city    <- city(row)
      country <- country(row)
    } yield Location(city, country)

    val emailO = email(row).map(e => Email(e))

    val mobileO = mobile(row).map(m => Mobile(m))

    val userType = UserType(usertype(row))

    User(usernameO, displayNameO, emailO, mobileO, avatarO, genderO, localeO, locationO, userType)
  }

}


abstract class ConcreteUserInfos(val settings: UserSettings) extends UserInfos {

  def insertUserInfo(userId: UserId, user: User)(implicit keySpace: KeySpace) =
    insert.value(_.uuid, userId.uuid)
      .value(_.username, user.username.map(_.username))
      .value(_.first, user.name.map(_.first))
      .value(_.last, user.name.map(_.last))
      .value(_.email, user.email.map(_.email))
      .value(_.mobile, user.mobile.map(_.mobile))
      .value(_.gender, user.gender.map(_.value))
      .value(_.locale, user.locale.map(_.value))
      .value(_.city, user.location.map(_.city))
      .value(_.country, user.location.map(_.country))
      .value(_.usertype, user.usertype.value)
      .value(_.avatarsmall, user.avatar.map(_.small))
      .value(_.avatarmedium, user.avatar.map(_.medium))
      .value(_.avatarlarge, user.avatar.map(_.large))

  def getByUserId(id: UserId)(implicit keySpace: KeySpace) =
    select.where(_.uuid eqs id.uuid)

  def updateUserInfo(userId: UserId, user: User)(implicit keySpace: KeySpace) = {
    val updateWhere = update.where(_.uuid eqs userId.uuid)
    var setTos = MutableSeq.empty[ConcreteUserInfos => UpdateClause.Condition]

    user.name.map(_.first)          .foreach { f  => setTos = setTos :+ { (_ : ConcreteUserInfos).first        setTo f.some }}
    user.name.map(_.last)           .foreach { l  => setTos = setTos :+ { (_ : ConcreteUserInfos).last         setTo l.some }}
    user.username.map(_.username)   .foreach { h  => setTos = setTos :+ { (_ : ConcreteUserInfos).username     setTo h.some }}
    user.email.map(_.email)         .foreach { e  => setTos = setTos :+ { (_ : ConcreteUserInfos).email        setTo e.some }}
    user.mobile.map(_.mobile)       .foreach { m  => setTos = setTos :+ { (_ : ConcreteUserInfos).mobile       setTo m.some }}
    user.gender.map(_.value)        .foreach { g  => setTos = setTos :+ { (_ : ConcreteUserInfos).gender       setTo g.some }}
    user.locale.map(_.value)        .foreach { l  => setTos = setTos :+ { (_ : ConcreteUserInfos).locale       setTo l.some }}
    user.avatar.map(_.small)        .foreach { as => setTos = setTos :+ { (_ : ConcreteUserInfos).avatarsmall  setTo as.some }}
    user.avatar.map(_.large)        .foreach { al => setTos = setTos :+ { (_ : ConcreteUserInfos).avatarlarge  setTo al.some }}
    user.avatar.map(_.medium)       .foreach { am => setTos = setTos :+ { (_ : ConcreteUserInfos).avatarmedium setTo am.some }}
    user.location.map(_.city)       .foreach { c  => setTos = setTos :+ { (_ : ConcreteUserInfos).city         setTo c.some }}
    user.location.map(_.country)    .foreach { c  => setTos = setTos :+ { (_ : ConcreteUserInfos).country      setTo c.some }}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        (tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) }).some
    }

  }

}