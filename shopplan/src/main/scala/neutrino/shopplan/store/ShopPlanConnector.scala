package neutrino.shopplan.store

import neutrino.shopplan.ShopPlanSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee

import com.datastax.driver.core.Session

class ShopPlanCassandraManager(settings: ShopPlanSettings) extends DefaultCassandraManager {
  override def cassandraHost: String = settings.CassandraHost
  override val livePort: Int = settings.CassandraPort
}


trait ShopPlanConnector extends SimpleCassandraConnector {
  def settings: ShopPlanSettings

  override val manager = new ShopPlanCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

  override implicit lazy val session: Session = {
    manager.initIfNotInited(keySpace)
    manager.session
  }
}
