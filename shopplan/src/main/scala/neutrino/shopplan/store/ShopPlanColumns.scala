package neutrino.shopplan.store

import java.nio.ByteBuffer

import com.goshoplane.common._

import com.websudos.phantom.Implicits._

import play.api.libs.json._


class CatalogueItemsJsonSetColumn[T <: CassandraTable[T, R], R](table: T)
  extends JsonSetColumn[T, R, SerializedCatalogueItem](table) {

  override def fromJson(str: String) = {
    val json   = Json.parse(str)

    val cuid   = (json \ "cuid").as[Long]
    val stuid  = (json \ "stuid").as[Long]
    val sid    = (json \ "sid").as[String]
    val `type` = SerializerType.valueOf((json \ "type").as[String]).getOrElse(SerializerType.Msgpck)
    val stream = (json \ "stream").as[String]

    SerializedCatalogueItem(
      itemId       = CatalogueItemId(cuid = cuid, storeId = StoreId(stuid = stuid)),
      serializerId = SerializerId(sid = sid, `type` = `type`),
      stream       = ByteBuffer.wrap(stream.getBytes())
    )
  }

  override def toJson(item: SerializedCatalogueItem) = {
    val b = Array.ofDim[Byte](item.stream.remaining())
    item.stream.get(b)

    val stream = new String(b, "UTF-8")
    val json = Json.obj(
      "cuid"    -> item.itemId.cuid,
      "stuid"   -> item.itemId.storeId.stuid,
      "sid"     -> item.serializerId.sid,
      "type"    -> item.serializerId.`type`.name,
      "stream"  -> stream
    )

    Json.stringify(json)
  }

}