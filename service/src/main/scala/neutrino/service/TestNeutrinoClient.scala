package neutrino.service

import com.twitter.finagle.Thrift
import com.twitter.util.{Future => TwitterFuture, Await}

import com.twitter.finagle.Thrift
import com.twitter.util.{Future => TwitterFuture, Await}

import org.apache.thrift.protocol.TBinaryProtocol
import com.twitter.finagle.Thrift
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodecFactory

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.creed.search._


/**
 * [TO DO] Proper test
 * {{{
 *   service/target/start neutrino.service.TestNeutrinoClient
 * }}}
 */
object TestNeutrinoClient {

  def main(args: Array[String]) {

    val protocol = new TBinaryProtocol.Factory()
    val client = ClientBuilder().codec(new ThriftClientFramedCodecFactory(None, false, protocol))
      .dest("localhost:2424").hostConnectionLimit(2).build()

    val neutrino = new Neutrino$FinagleClient(client, protocol)

    val userId  = UserId(1L)
    val seachId = CatalogueSearchId(userId, 1)
    val param   = QueryParam(value = Some("levis men's jeans"))
    val query   = CatalogueSearchQuery(params = Map("brand" -> param), queryText = "Men Black Jeans")

    val startTime = System.currentTimeMillis
    val resultF   = neutrino.search(CatalogueSearchRequest(seachId, query, 1, 100))

    resultF onSuccess { response =>
      println(response)
      println("Received result in " + (System.currentTimeMillis - startTime))
      response.result.flatMap(_.items).map(_.json).foreach(println(_))
    }

    resultF onFailure {
      case ex: Exception => ex.printStackTrace
    }

  }
}