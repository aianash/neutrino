package neutrino.user.store

import scala.collection.mutable.{Seq => MutableSeq}

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.Implicits._

import com.datastax.driver.core.querybuilder.Assignment

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import neutrino.user.UserSettings

class UserInfos(val settings: UserSettings)
  extends CassandraTable[UserInfos, UserInfo] with UserConnector {

  override def tableName = "user_infos"

  // ids
  object uuid extends LongColumn(this) with PartitionKey[Long]

  // name
  object first extends OptionalStringColumn(this)
  object last extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  // gender
  object gender extends OptionalStringColumn(this)

  // fb info
  object fbuid extends OptionalLongColumn(this)
  object fbtoken extends OptionalStringColumn(this)

  // email
  object email extends OptionalStringColumn(this)

  // locale
  object locale extends OptionalStringColumn(this)
  object timezone extends OptionalStringColumn(this)

  // avatar
  object avatarsmall extends OptionalStringColumn(this)
  object avatarmedium extends OptionalStringColumn(this)
  object avatarlarge extends OptionalStringColumn(this)

  // is new
  object isNew extends OptionalBooleanColumn(this)


  def fromRow(row: Row) = {

    val usernameO = UserName(
      first   = first(row),
      last    = last(row),
      handle  = handle(row)).some

    val genderO = gender(row).flatMap(Gender.valueOf(_))

    val fbInfoO = fbuid(row).map(u => FacebookInfo(userId = UserId(u), token = fbtoken(row)))

    val avatarO = UserAvatar(
      small  = avatarsmall(row),
      medium = avatarmedium(row),
      large  = avatarlarge(row)).some

    UserInfo(
      name         = usernameO,
      gender       = genderO,
      facebookInfo = fbInfoO,
      email        = email(row),
      locale       = locale(row).flatMap(Locale.valueOf(_)),
      timezone     = timezone(row),
      avatar       = avatarO,
      isNew        = isNew(row))

  }

  def insertUserInfo(userId: UserId, info: UserInfo) =
    insert
      .value(_.uuid,          userId.uuid)
      .value(_.first,         info.name.flatMap(_.first))
      .value(_.last,          info.name.flatMap(_.last))
      .value(_.handle,        info.name.flatMap(_.handle))
      .value(_.gender,        info.gender.map(_.name))
      .value(_.fbuid,         info.facebookInfo.map(_.userId.uuid))
      .value(_.fbtoken,       info.facebookInfo.flatMap(_.token))
      .value(_.email,         info.email)
      .value(_.locale,        info.locale.map(_.name))
      .value(_.timezone,      info.timezone)
      .value(_.avatarsmall,   info.avatar.flatMap(_.small))
      .value(_.avatarmedium,  info.avatar.flatMap(_.medium))
      .value(_.avatarlarge,   info.avatar.flatMap(_.large))
      .value(_.isNew,         info.isNew)



  def getInfoBy(userId: UserId) = select.where(_.uuid eqs userId.uuid)


  def updateUserInfo(userId: UserId, info: UserInfo) = {
    val updateQ = update.where(_.uuid eqs userId.uuid)

    val setTos = MutableSeq.empty[UserInfos => Assignment]

    info.name.flatMap(_.first)          .foreach {f =>    setTos :+ { (_:UserInfos).first         setTo f.some} }
    info.name.flatMap(_.last)           .foreach {l =>    setTos :+ { (_:UserInfos).last          setTo l.some} }
    info.name.flatMap(_.handle)         .foreach {h =>    setTos :+ { (_:UserInfos).handle        setTo h.some} }

    info.gender                         .foreach {g =>    setTos :+ { (_:UserInfos).gender        setTo g.name.some} }
    info.facebookInfo.map(_.userId)     .foreach {uid =>  setTos :+ { (_:UserInfos).fbuid         setTo uid.uuid.some} }
    info.facebookInfo.flatMap(_.token)  .foreach {t =>    setTos :+ { (_:UserInfos).fbtoken       setTo t.some} }
    info.email                          .foreach {e =>    setTos :+ { (_:UserInfos).email         setTo e.some} }
    info.locale                         .foreach {l =>    setTos :+ { (_:UserInfos).locale        setTo l.name.some} }
    info.timezone                       .foreach {t =>    setTos :+ { (_:UserInfos).timezone      setTo t.some} }
    info.avatar.flatMap(_.small)        .foreach {as =>   setTos :+ { (_:UserInfos).avatarsmall   setTo as.some} }
    info.avatar.flatMap(_.large)        .foreach {al =>   setTos :+ { (_:UserInfos).avatarlarge   setTo al.some} }
    info.avatar.flatMap(_.medium)       .foreach {am =>   setTos :+ { (_:UserInfos).avatarmedium  setTo am.some} }
    info.isNew                          .foreach {in =>   setTos :+ { (_:UserInfos).isNew         setTo in.some} }

    setTos.tail.foldLeft(updateQ.modify(setTos.head)) { (updateQ, assignment) => updateQ.and(assignment) }
  }

}