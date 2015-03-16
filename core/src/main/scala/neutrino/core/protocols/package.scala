package neutrino.core

import scala.concurrent.Future
import scala.reflect.ClassTag

import akka.util.Timeout
import akka.actor.{ActorRef, ActorSelection}

package object protocols {

  // Replyable actorRef pattern - credits - http://www.warski.org/blog/2013/05/typed-ask-for-akka/
  trait Replyable[T]

  implicit class ReplyActorRef(actorRef: ActorRef) {
    def ?=[T](message: Replyable[T])(implicit timeout: Timeout, tag: ClassTag[T]): Future[T] =
      akka.pattern.ask(actorRef, message).mapTo[T]
  }

  implicit class ReplyActorSelection(actorSel: ActorSelection) {
    def ?=[T](message: Replyable[T])(implicit timeout: Timeout, tag: ClassTag[T]): Future[T] =
      akka.pattern.ask(actorSel, message).mapTo[T]
  }
}