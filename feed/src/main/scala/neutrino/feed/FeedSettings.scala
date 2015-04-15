package neutrino.feed

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class FeedSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino.feed")
    config
  }

  val CassandraHost     = config.getString("neutrino.feed.cassandraHost")
  val CassandraPort     = config.getInt("neutrino.feed.cassandraPort")
  val CassandraKeyspace = config.getString("neutrino.feed.cassandraKeyspace")
  val ServiceId         = config.getLong("neutrino.service.id")
  val DatacenterId      = config.getLong("neutrino.datacenter.id")
}


object FeedSettings extends ExtensionId[FeedSettings]
  with ExtensionIdProvider {

  override def lookup = FeedSettings

  override def createExtension(system: ExtendedActorSystem) =
    new FeedSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}