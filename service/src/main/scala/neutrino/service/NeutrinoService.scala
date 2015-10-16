package neutrino.service

import scala.collection.IndexedSeq

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import commons.microservice._

import neutrino.service.components._

object NeutrinoService {

  def main(args: Array[String]) {
    val config = ConfigFactory.load("neutrino")
    val system = ActorSystem(config.getString("neutrino.actorSystem"), config)
    Microservice(system).start(IndexedSeq(AuthComponent, UserComponent))
  }

}