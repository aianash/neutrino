package neutrino.service

import scala.collection.IndexedSeq

import akka.actor.ActorSystem

import commons.microservice._

import neutrino.service.components._


object NeutrinoService {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("neutrino")
    Microservice(system).start(IndexedSeq(UserComponent))
  }

}