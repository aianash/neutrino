package neutrino.user

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class UserSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino.user")
    config
  }

  val CassandraHost     = config.getString("neutrino.user.cassandraHost")
  val CassandraPort     = config.getInt("neutrino.user.cassandraPort")
  val CassandraKeyspace = config.getString("neutrino.user.cassandraKeyspace")
  val ServiceId         = config.getLong("neutrino.service.id")
  val DatacenterId      = config.getLong("neutrino.datacenter.id")
}


object UserSettings extends ExtensionId[UserSettings]
  with ExtensionIdProvider {

  override def lookup = UserSettings

  override def createExtension(system: ExtendedActorSystem) =
    new UserSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}