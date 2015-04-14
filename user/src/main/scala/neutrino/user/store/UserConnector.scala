package neutrino.user.store

import neutrino.user.UserSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee


class UserCassandraManager(settings: UserSettings) extends DefaultCassandraManager {
  override def cassandraHost: String = settings.CassandraHost
  override val livePort: Int = settings.CassandraPort
}


trait UserConnector extends SimpleCassandraConnector {
  def settings: UserSettings

  override def manager = new UserCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

}
