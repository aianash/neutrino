package neutrino.user.store

import neutrino.user.UserSettings

import com.websudos.phantom.iteratee.Iteratee
import com.websudos.phantom.connectors.{SimpleConnector, ContactPoint, KeySpace}

trait UserConnector extends SimpleConnector {

  def settings: UserSettings

  implicit val keySpace = KeySpace(settings.CassandraKeyspace)

  val connector = ContactPoint(settings.CassandraHost, settings.CassandraPort)

}
