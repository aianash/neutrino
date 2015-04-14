package neutrino.bucket

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class BucketSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "neutrino.bucket")
    config
  }

  val CassandraHost     = config.getString("neutrino.bucket.cassandraHost")
  val CassandraPort     = config.getInt("neutrino.bucket.cassandraPort")
  val CassandraKeyspace = config.getString("neutrino.bucket.cassandraKeyspace")
  val ServiceId         = config.getLong("neutrino.service.id")
  val DatacenterId      = config.getLong("neutrino.datacenter.id")
}


object BucketSettings extends ExtensionId[BucketSettings]
  with ExtensionIdProvider {

  override def lookup = BucketSettings

  override def createExtension(system: ExtendedActorSystem) =
    new BucketSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}