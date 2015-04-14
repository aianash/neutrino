package neutrino.bucket.protocols

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import neutrino.core.protocols.Replyable

sealed trait BucketMessages

case class GetBucketStores(userId: UserId, fields: Seq[BucketStoreField])
  extends BucketMessages with Replyable[Seq[BucketStore]]

case class ModifyBucket(userId: UserId, cud: CUDBucket)
  extends BucketMessages with Replyable[Boolean]