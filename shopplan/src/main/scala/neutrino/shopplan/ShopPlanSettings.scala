package neutrino.shopplan

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class ShopPlanSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino.shopplan")
    config
  }

  val CassandraHost     = config.getString("neutrino.shopplan.cassandraHost")
  val CassandraPort     = config.getInt("neutrino.shopplan.cassandraPort")
  val CassandraKeyspace = config.getString("neutrino.shopplan.cassandraKeyspace")
  val ServiceId         = config.getLong("neutrino.service.id")
  val DatacenterId      = config.getLong("neutrino.datacenter.id")
  val GeoApiKey         = config.getString("neutrino.geo.api-key")
}


object ShopPlanSettings extends ExtensionId[ShopPlanSettings]
  with ExtensionIdProvider {

  override def lookup = ShopPlanSettings

  override def createExtension(system: ExtendedActorSystem) =
    new ShopPlanSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}