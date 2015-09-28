package neutrino.auth

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class AuthSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino.auth")
    config
  }

  val CassandraHost     = config.getString("neutrino.auth.cassandraHost")
  val CassandraPort     = config.getInt("neutrino.auth.cassandraPort")
  val CassandraKeyspace = config.getString("neutrino.auth.cassandraKeyspace")
  val ServiceId         = config.getLong("neutrino.service.id")
  val DatacenterId      = config.getLong("neutrino.datacenter.id")
  val FBAppSecret       = config.getString("neutrino.auth.fb.app-secret")
  val FBAppId           = config.getLong("neutrino.auth.fb.app-id")
  val GoogleClientId    = config.getString("neutrino.auth.google.client-id")
}


object AuthSettings extends ExtensionId[AuthSettings]
  with ExtensionIdProvider {

  override def lookup = AuthSettings

  override def createExtension(system: ExtendedActorSystem) =
    new AuthSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}