package neutrino.search

import scala.concurrent.Future

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.util.Timeout

import org.apache.thrift.protocol.TBinaryProtocol
import com.twitter.finagle.Thrift
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodecFactory

import com.twitter.bijection._, Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._

import goshoplane.commons.core.protocols.Implicits._
import goshoplane.commons.catalogue.CatalogueItem

import com.goshoplane.common._
import com.goshoplane.neutrino.service._
import com.goshoplane.creed.search._
import com.goshoplane.creed.service._
import com.goshoplane.cassie.service._, Cassie._


class SearchSupervisor extends Actor with ActorLogging {
  val settings = SearchSettings(context.system)

  import protocols._
  import settings._
  import context.dispatcher


  val creed = {
    val protocol = new TBinaryProtocol.Factory()
    val client = ClientBuilder().codec(new ThriftClientFramedCodecFactory(None, false, protocol))
      .dest(CreedHost + ":" + CreedPort).hostConnectionLimit(2).build()

    new Creed$FinagleClient(client, protocol)
  }

  val cassie = {
    val protocol = new TBinaryProtocol.Factory()
    val client = ClientBuilder().codec(new ThriftClientFramedCodecFactory(None, false, protocol))
      .dest(CassieHost + ":" + CassiePort).hostConnectionLimit(2).build()

    new Cassie$FinagleClient(client, protocol)
  }


  def receive = {

    case SearchCatalogue(request) =>
      val creedResultF = creed.searchCatalogue(request)

      // Fetch details in parallel

      // 1. Get Item details
      val itemsF  =
        creedResultF.flatMap { cr =>
          cassie.getCatalogueItems(cr.results.map(_.itemId), CatalogeItemDetailType.Summary)
        }

      import StoreInfoField._

      val infoFields = Seq(Name, ItemTypes, Address, Avatar, Contacts)

      // 2. Get Store details
      val storesF =
        creedResultF.flatMap { cr =>
          cassie.getStores(cr.results.map(_.itemId.storeId).distinct, infoFields)
        }

      val resultF =
        for {
          items   <- itemsF
          stores  <- storesF
        } yield {

          val grpdItems = items.groupBy(_.itemId.storeId.stuid)

          stores.flatMap { store =>
            grpdItems.get(store.storeId.stuid).map { items =>
              val jsonItems =
                items.flatMap(CatalogueItem.asJsonItem(_))

              SearchResultStore(
                storeId   = store.storeId,
                storeType = store.storeType,
                info      = store.info,
                items     = jsonItems
              )
            }
          }
        }

      resultF
        .map(result => SearchResult(searchId = request.searchId, result = result))
        .as[Future[SearchResult]] pipeTo sender()

  }


}


object SearchSupervisor {

  def props = Props(classOf[SearchSupervisor])
}