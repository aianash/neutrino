package neutrino.service

import com.twitter.finagle.Thrift
import com.twitter.util.{Future => TwitterFuture, Await}

import com.goshoplane.common._
import com.goshoplane.neutrino.service._


/**
 * [TO DO] Proper test
 * {{{
 *   service/target/start neutrino.service.TestNeutrinoClient
 * }}}
 */
object TestNeutrinoClient {

  def main(args: Array[String]) {
    val client = Thrift.newIface[Neutrino.FutureIface]("localhost:2424")

    // val f = client.newShopPlanFor(UserId(1L))

    // Await.ready(f)

    // f onSuccess { response =>
    //   println(response)
    // }

    // f onFailure {
    //   case ex: Exception => ex.printStackTrace
    // }

  }
}