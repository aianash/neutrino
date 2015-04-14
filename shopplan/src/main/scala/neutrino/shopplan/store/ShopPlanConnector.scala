package neutrino.shopplan.store

import neutrino.shopplan.ShopPlanSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee


class ShopPlanCassandraManager(settings: ShopPlanSettings) extends DefaultCassandraManager {
  override def cassandraHost: String = settings.CassandraHost
  override val livePort: Int = settings.CassandraPort
}


trait ShopPlanConnector extends SimpleCassandraConnector {
  def settings: ShopPlanSettings

  override def manager = new ShopPlanCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

}
