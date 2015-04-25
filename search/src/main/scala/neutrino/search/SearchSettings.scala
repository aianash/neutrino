package neutrino.search

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class SearchSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino.search")
    config
  }

  val ServiceId    = config.getLong("neutrino.service.id")
  val DatacenterId = config.getLong("neutrino.datacenter.id")
  val CreedHost    = config.getString("neutrino.search.creed.host")
  val CreedPort    = config.getInt("neutrino.search.creed.port")
  val CassieHost   = config.getString("neutrino.cassie.host")
  val CassiePort   = config.getInt("neutrino.cassie.port")
}


object SearchSettings extends ExtensionId[SearchSettings]
  with ExtensionIdProvider {

  override def lookup = SearchSettings

  override def createExtension(system: ExtendedActorSystem) =
    new SearchSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}