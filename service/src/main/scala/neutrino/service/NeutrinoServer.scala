package neutrino.service

import scaldi.akka.AkkaInjectable._

import com.typesafe.config.ConfigFactory

import com.twitter.finagle.Thrift
import com.twitter.util.Await

import akka.actor.ActorSystem

import neutrino.service.injectors._
import neutrino.core.injectors._

/**
 * Neutrino Server that starts the NeutrinoService
 *
 * {{{
 *   service/target/start neutrino.service.NeutrinoServer
 * }}}
 */
object NeutrinoServer {

  def main(args: Array[String]) {

    val config = ConfigFactory.load("neutrino")

    implicit val appModule = new NeutrinoServiceModule :: new AkkaModule(config)

    implicit val system = inject [ActorSystem]

    val serviceId = NeutrinoSettings(system).ServiceId
    val datacenterId = NeutrinoSettings(system).DatacenterId

    val service = NeutrinoService.start
    Await.ready(service)
  }

}