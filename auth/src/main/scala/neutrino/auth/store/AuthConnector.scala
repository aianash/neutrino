package neutrino.auth.store

import neutrino.auth.AuthSettings

import com.websudos.phantom.connectors.{SimpleConnector, ContactPoint, KeySpace}

trait AuthConnector extends SimpleConnector {

  def settings: AuthSettings

  implicit val keySpace = KeySpace(settings.CassandraKeyspace)

  val connector = ContactPoint(settings.CassandraHost, settings.CassandraPort)

}
