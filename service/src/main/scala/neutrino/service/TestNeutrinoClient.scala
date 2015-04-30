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
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._
import com.goshoplane.creed.search._

import scalaz._, Scalaz._


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

    val userId  = UserId(19288829L)
    val seachId = CatalogueSearchId(userId, 1)
    val param   = QueryParam(value = Some("levis men's jeans"))
    val query   = CatalogueSearchQuery(params = Map("brand" -> param), queryText = "Men Black Jeans")

    val startTime = System.currentTimeMillis
    val resultF   = neutrino.search(CatalogueSearchRequest(seachId, query, 1, 100))

    resultF foreach { response =>

      println("\n\nReceived search result in " + (System.currentTimeMillis - startTime))
      println("Search Result := ")
      response.result.flatMap(_.items).map(_.json).foreach(json => println("\n" + json))

      val adds     = response.result.flatMap(_.items).map(_.itemId)
      val successF = neutrino.cudBucket(userId, CUDBucket(adds.some))
      successF foreach { success =>
        import BucketStoreField._
        println("\n\nBucket modified = " + success);

        println("====================================================")
        println("Get Bucket Stores")
        val storesF = neutrino.getBucketStores(userId, Seq(Name, Address, ItemTypes, CatalogueItemIds))
        storesF foreach { stores =>
          println("Got Bucket Stores")
          stores.foreach(println(_))

          println("Add items to shopplan")
        }
      }
    }

  }
}