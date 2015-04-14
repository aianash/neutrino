package neutrino.bucket

import akka.actor.{Props, Actor, ActorLogging}
import akka.pattern.pipe

import protocols._

import store.BucketDatastore

class BucketSupervisor extends Actor with ActorLogging {

  private val settings = BucketSettings(context.system)

  import context.dispatcher
  import settings._

  private val bucketDatastore = new BucketDatastore(settings)
  bucketDatastore.init()

  def receive = {
    case GetBucketStores(userId, fields) =>
      bucketDatastore.getBucketStores(userId, fields) pipeTo sender()


    case ModifyBucket(userId, cud) =>
      bucketDatastore.cudBucketStores(userId, cud) pipeTo sender()

  }
}

object BucketSupervisor {
  def props = Props(classOf[BucketSupervisor])
}