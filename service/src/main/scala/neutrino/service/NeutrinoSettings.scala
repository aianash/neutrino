package neutrino.service

import scala.concurrent.duration._

import akka.actor.{ActorSystem, Extension, ExtensionId, ExtensionIdProvider, ExtendedActorSystem}

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Neutrino specific settings
 */
class NeutrinoSettings(cfg: Config) extends Extension {

  // validate neutrino config
  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino")
    config
  }

  val ActorSystem   = config.getString("neutrino.actorSystem")
  val ServiceId     = config.getLong("neutrino.service.id")
  val DatacenterId  = config.getLong("neutrino.datacenter.id")

  val NeutrinoEndpoint = config.getString("neutrino.endpoint") + ":" + config.getInt("neutrino.port")
}


object NeutrinoSettings extends ExtensionId[NeutrinoSettings] with ExtensionIdProvider {
  override def lookup = NeutrinoSettings

  override def createExtension(system: ExtendedActorSystem) =
    new NeutrinoSettings(system.settings.config)

  override def get(system: ActorSystem): NeutrinoSettings = super.get(system);
}