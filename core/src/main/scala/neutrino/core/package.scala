package neutrino

import scalaz._, Scalaz._

import akka.actor.ActorRef

package object core {

  // Tags for marking ActorRefs
  // while passing around to provide
  // type safety
  //
  // ex:
  // def func(Bucket: ActorRef @@ BucketActorRef, User: ActorRef @@ UserActorRef) {..}
  //
  // val Bucket = context.actorOf(....)
  // val User   = context.actorOf(....)
  // func(BucketActorRef(Bucket), UserActorRef(User))
  sealed trait BucketActorRef
  sealed trait UserActorRef

  def BucketActorRef(a: ActorRef): ActorRef @@ BucketActorRef = Tag[ActorRef, BucketActorRef](a)
  def UserActorRef(a: ActorRef): ActorRef @@ UserActorRef     = Tag[ActorRef, UserActorRef](a)

}