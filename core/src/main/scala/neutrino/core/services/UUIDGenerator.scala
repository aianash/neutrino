package neutrino.core.services

import scala.util.{Try, Success, Failure}

import akka.actor.{Actor, Props, ActorLogging}

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap

import neutrino.core.protocols.Replyable

/**
 * A strip off of Twitter Snowflake (id generator)
 *
 * Idea is to have an inbuilt ID generator actor instead of a
 * separate cluster of id generators.
 *
 * TO FIX - later should be able to instantiate load balanced id generator
 * actor with different worker ids... Redesign possible
 *
 */
class UUIDGenerator(serviceId: Long, datacenterId: Long) extends Actor with ActorLogging {
  import UUIDGenerator._

  private[this] val sequences = new Object2LongOpenHashMap[String]
  sequences.defaultReturnValue(0L)

  val rdepoch = 1395645822638L

  private[this] val serviceIdBits = 5L
  private[this] val datacenterIdBits = 5L
  private[this] val maxserviceId = -1L ^ (-1L << serviceIdBits)
  private[this] val maxDatacenterId = -1L ^ (-1L << datacenterIdBits)
  private[this] val sequenceBits = 12L

  private[this] val serviceIdShift = sequenceBits
  private[this] val datacenterIdShift = sequenceBits + serviceIdBits
  private[this] val timestampLeftShift = sequenceBits + serviceIdBits + datacenterIdBits
  private[this] val sequenceMask = -1L ^ (-1L << sequenceBits)

  private[this] var lastTimestamp = -1L

  private def nextId(idFor: String) = synchronized {
    Try {
      var timestamp = System.currentTimeMillis
      if(timestamp < lastTimestamp) {
        throw new Exception("clock is running backward, so wont be generating ids for %d millis".format(
          lastTimestamp - timestamp))
      }

      var sequence = sequences.getLong(idFor)

      if(lastTimestamp == timestamp){
        sequence = (sequence + 1L) & sequenceMask
        if(sequence == 0L){
          timestamp = tilNextMillis(lastTimestamp)
        }
        sequences.put(idFor, sequence)
      }

      lastTimestamp = timestamp
      ((timestamp - rdepoch) << timestampLeftShift) |
        (datacenterId << datacenterIdShift) |
        (serviceId << serviceIdShift) |
        sequence
    } match {
      case Success(v) => Some(v)
      case Failure(ex) =>
        ex.printStackTrace;
        None
    }
  }

  protected def tilNextMillis(lastTimestamp: Long): Long = {
    var timestamp = System.currentTimeMillis
    while (timestamp <= lastTimestamp) {
      timestamp = System.currentTimeMillis
    }
    timestamp
  }

  def receive = {
    case NextId(idFor) =>
      sender() ! nextId(idFor)
  }
}


case class NextId(idFor: String) extends Replyable[Long]

object UUIDGenerator {


  def props(serviceId: Long, datacenterId: Long) =
    Props(classOf[UUIDGenerator], serviceId, datacenterId)
}