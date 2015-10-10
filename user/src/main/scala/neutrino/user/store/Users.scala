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


sealed class Users extends CassandraTable[ConcreteUsers, User] {

  override def tableName = "users"

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
    val userId = UserId(uuid(row))

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

    val info = UserInfo(
      usernameO,
      displayNameO,
      emailO,
      mobileO,
      avatarO,
      genderO,
      localeO,
      locationO,
      userType
      )

    User(userId, info)
  }

}


abstract class ConcreteUsers(val settings: UserSettings) extends Users {

  def insertUser(user: User)(implicit keySpace: KeySpace) =
    insert.value(_.uuid, user.id.uuid)
      .value(_.username, user.info.username.map(_.username))
      .value(_.first, user.info.name.map(_.first))
      .value(_.last, user.info.name.map(_.last))
      .value(_.email, user.info.email.map(_.email))
      .value(_.mobile, user.info.mobile.map(_.mobile))
      .value(_.gender, user.info.gender.map(_.value))
      .value(_.locale, user.info.locale.map(_.value))
      .value(_.city, user.info.location.map(_.city))
      .value(_.country, user.info.location.map(_.country))
      .value(_.usertype, user.info.usertype.value)
      .value(_.avatarsmall, user.info.avatar.map(_.small))
      .value(_.avatarmedium, user.info.avatar.map(_.medium))
      .value(_.avatarlarge, user.info.avatar.map(_.large))

  def getUserByUserId(id: UserId)(implicit keySpace: KeySpace) =
    select.where(_.uuid eqs id.uuid)

  def updateUser(user: User)(implicit keySpace: KeySpace) = {
    val updateWhere = update.where(_.uuid eqs user.id.uuid)
    var setTos = MutableSeq.empty[ConcreteUsers => UpdateClause.Condition]

    user.info.name.map(_.first)          .foreach { f  => setTos = setTos :+ { (_ : ConcreteUsers).first        setTo f.some }}
    user.info.name.map(_.last)           .foreach { l  => setTos = setTos :+ { (_ : ConcreteUsers).last         setTo l.some }}
    user.info.username.map(_.username)   .foreach { h  => setTos = setTos :+ { (_ : ConcreteUsers).username     setTo h.some }}
    user.info.email.map(_.email)         .foreach { e  => setTos = setTos :+ { (_ : ConcreteUsers).email        setTo e.some }}
    user.info.mobile.map(_.mobile)       .foreach { m  => setTos = setTos :+ { (_ : ConcreteUsers).mobile       setTo m.some }}
    user.info.gender.map(_.value)        .foreach { g  => setTos = setTos :+ { (_ : ConcreteUsers).gender       setTo g.some }}
    user.info.locale.map(_.value)        .foreach { l  => setTos = setTos :+ { (_ : ConcreteUsers).locale       setTo l.some }}
    user.info.avatar.map(_.small)        .foreach { as => setTos = setTos :+ { (_ : ConcreteUsers).avatarsmall  setTo as.some }}
    user.info.avatar.map(_.large)        .foreach { al => setTos = setTos :+ { (_ : ConcreteUsers).avatarlarge  setTo al.some }}
    user.info.avatar.map(_.medium)       .foreach { am => setTos = setTos :+ { (_ : ConcreteUsers).avatarmedium setTo am.some }}
    user.info.location.map(_.city)       .foreach { c  => setTos = setTos :+ { (_ : ConcreteUsers).city         setTo c.some }}
    user.info.location.map(_.country)    .foreach { c  => setTos = setTos :+ { (_ : ConcreteUsers).country      setTo c.some }}

    setTos match {
      case MutableSeq() => None
      case MutableSeq(x) => updateWhere.modify(x).some
      case MutableSeq(head, tail @ _*) =>
        (tail.foldLeft(updateWhere.modify(head)) { (updateWhere, cqlQuery) => updateWhere.and(cqlQuery) }).some
    }

  }

}