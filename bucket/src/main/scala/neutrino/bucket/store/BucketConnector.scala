package neutrino.bucket.store

import neutrino.bucket.BucketSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee

import com.datastax.driver.core.Session

class BucketCassandraManager(settings: BucketSettings) extends DefaultCassandraManager {
  override def cassandraHost = settings.CassandraHost
  override val livePort      = settings.CassandraPort
}


trait BucketConnector extends SimpleCassandraConnector {
  def settings: BucketSettings

  override val manager = new BucketCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

  override implicit lazy val session: Session = {
    manager.initIfNotInited(keySpace)
    manager.session
  }
}
