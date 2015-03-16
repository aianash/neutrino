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

  val NrOfRetrieverInstances = config.getInt("neutrino.shopplan.nrOfRetrieverInstances")
  val NrOfPersisterInstances = config.getInt("neutrino.shopplan.nrOfPersisterInstances")
  val ServiceId     = config.getLong("neutrino.service.id")
  val DatacenterId  = config.getLong("neutrino.datacenter.id")
}


object ShopPlanSettings extends ExtensionId[ShopPlanSettings]
  with ExtensionIdProvider {

  override def lookup = ShopPlanSettings

  override def createExtension(system: ExtendedActorSystem) =
    new ShopPlanSettings(system.settings.config)

  override def get(system: ActorSystem): ShopPlanSettings =
    super.get(system)
}