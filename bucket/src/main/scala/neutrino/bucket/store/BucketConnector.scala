package neutrino.bucket.store

import neutrino.bucket.BucketSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee


class BucketCassandraManager(settings: BucketSettings) extends DefaultCassandraManager {
  override def cassandraHost = settings.CassandraHost
  override val livePort      = settings.CassandraPort
}


trait BucketConnector extends SimpleCassandraConnector {
  def settings: BucketSettings

  override def manager = new BucketCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

}
