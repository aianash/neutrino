package neutrino.shopplan.store

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import neutrino.shopplan.ShopPlanSettings

import com.goshoplane.common._
import com.goshoplane.neutrino.shopplan._
import com.goshoplane.neutrino.service._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context


sealed class ShopPlanDatastore(val settings: ShopPlanSettings)
  extends ShopPlanConnector {

  object ShopPlanMeta extends ShopPlanMeta(settings)
  object ShopPlanMetaByInvitation extends ShopPlanMetaByInvitation(settings)
  object ShopPlanStores extends ShopPlanStores(settings)
  object ShopPlanDestinations extends ShopPlanDestinations(settings)
  object ShopPlanInvites extends ShopPlanInvites(settings)


  def init()(implicit executor: ExecutionContext) {
    val creation =
      for {
        _ <- ShopPlanMeta.create.future()
        _ <- ShopPlanMetaByInvitation.create.future()
        _ <- ShopPlanStores.create.future()
        _ <- ShopPlanDestinations.create.future()
        _ <- ShopPlanInvites.create.future()
      } yield true

    Await.ready(creation, 2 seconds)
  }



  /**
   * Get User's own shop plans with specified fields
   */
  def getOwnShopPlans(userId: UserId, fields: Seq[ShopPlanField])(implicit executor: ExecutionContext) = {
    import ShopPlanField._

    val shopplansF = ShopPlanMetaByInvitation.getOwnShopPlansBy(userId).fetch()

    // Get stores
    val storesF =
      fields.find(_ == Stores)
        .map { _ => ShopPlanStores.getStoresBy(userId, toStoreField(fields)).fetch().map(_.some) }
        .getOrElse(Future.successful(None))

    // Get Destinations
    val destinationsF =
      fields.find(_ == Destinations)
        .map { _ => ShopPlanDestinations.getDestinationsBy(userId).fetch().map(_.some) }
        .getOrElse(Future.successful(None))

    // Get invites
    val invitesF =
      fields.find(_ == Invites)
        .map { _ => ShopPlanInvites.getInvitesBy(userId).fetch().map(_.some) }
        .getOrElse(Future.successful(None))


    // Create Future[Seq[ShopPlan]]
    for {
      shopplans     <- shopplansF
      stores        <- storesF
      destinations  <- destinationsF
      invites       <- invitesF
    } yield {

      val storesGrpd       = stores      .map(_.groupBy(_.destId.shopplanId.suid))
      val destinationsGrpd = destinations.map(_.groupBy(_.destId.shopplanId.suid))
      val invitesGrpd      = invites     .map(_.groupBy(_.shopplanId.suid))

      shopplans.map { shopPlan =>
        val suid = shopPlan.shopplanId.suid

        shopPlan.copy(
          stores       = storesGrpd      .flatMap(_.get(suid)),
          destinations = destinationsGrpd.flatMap(_.get(suid)),
          invites      = invitesGrpd     .flatMap(_.get(suid))
        )
      }
    }

  }



  /**
   * [IMP] Its slowest API right now, to be improved
   */
  def getInvitedShopPlans(userId: UserId, fields: Seq[ShopPlanField])(implicit executor: ExecutionContext) =
    ShopPlanMetaByInvitation.getInvitedShopPlansBy(userId).fetch().flatMap { shopplans =>

      val shopplansSeqF =
        shopplans.map { shopplan =>
          val shopplanId = shopplan.shopplanId

          val (storesF, destinationsF, invitesF) = getShopPlanFields(shopplanId, fields)

          for {
            stores <- storesF
            destinations <- destinationsF
            invites <- invitesF
          } yield shopplan.copy(
              stores = stores,
              destinations = destinations,
              invites = invites
            )
        }

      Future.sequence(shopplansSeqF)
    }



  /**
   * Get ShopPlan for shopplan id
   */
  def getShopPlan(shopplanId: ShopPlanId, fields: Seq[ShopPlanField])(implicit executor: ExecutionContext) = {
    import ShopPlanField._

    val shopplanF = ShopPlanMeta.getShopPlanBy(shopplanId).one()
    val (storesF, destinationsF, invitesF) = getShopPlanFields(shopplanId, fields)

    // Filling ShopPlan with details
    for {
      shopplan      <- shopplanF
      stores        <- storesF
      destinations  <- destinationsF
      invites       <- invitesF
    } yield shopplan.map(_.copy(
              stores       = stores,
              destinations = destinations,
              invites      = invites
            ))

  }



  /**
   * Get ShopPlan's stores for shopplan id
   */
  def getStores(shopplanId: ShopPlanId, fields: Seq[ShopPlanStoreField])(implicit executor: ExecutionContext) =
    ShopPlanStores.getStoresBy(shopplanId, fields).fetch()



  def getDestinations(shopplanId: ShopPlanId)(implicit executor: ExecutionContext) =
    ShopPlanDestinations.getDestinationsBy(shopplanId).fetch()



  /**
   * Add a new shopplan to database
   */
  def addNewShopPlan(shopplan: ShopPlan)(implicit executor: ExecutionContext) = {
    val userId = shopplan.shopplanId.createdBy
    val batch = BatchStatement()

    batch add ShopPlanMeta            .insertShopPlan(userId, shopplan)
    batch add ShopPlanMetaByInvitation.insertShopPlan(userId, shopplan)

    // Once shop plan's meta has been inserted
    // Insert data to other tables

    shopplan.stores      .foreach(_.foreach(store =>       batch add ShopPlanStores.insertStore(store)))
    shopplan.destinations.foreach(_.foreach(destination => batch add ShopPlanDestinations.insertDestination(destination)))

    shopplan.invites.foreach(_.foreach { invite =>
      val friendId = invite.friendId

      batch add ShopPlanInvites.insertInvite(invite)
      batch add ShopPlanMeta            .insertShopPlan(friendId, shopplan.copy(isInvitation = true))
      batch add ShopPlanMetaByInvitation.insertShopPlan(friendId, shopplan.copy(isInvitation = true))
    })

    batch.future()
  }



  def updateShopPlan(shopplanId: ShopPlanId, cud: CUDShopPlan)(implicit executor: ExecutionContext) = {
    val batch = BatchStatement()

    cud.meta.foreach(_.title.foreach { title =>
      batch add ShopPlanMeta            .updateTitleBy(shopplanId, title)
      batch add ShopPlanMetaByInvitation.updateTitleBy(shopplanId, title)
    })

    cud.destinations.foreach { destinations =>
      destinations.adds     .toSeq.flatten.foreach { batch add ShopPlanDestinations.insertDestination(_) }
      destinations.updates  .toSeq.flatten.foreach { batch add ShopPlanDestinations.updateDestinationBy(_) }
      destinations.removals .toSeq.flatten.foreach { batch add ShopPlanDestinations.deleteDestinationsBy(_) }
    }

    cud.stores.foreach { stores =>
      stores.adds    .toSeq.flatten.foreach { batch add ShopPlanStores.insertStore(_) }
      stores.removals.toSeq.flatten.foreach { batch add ShopPlanStores.deleteStoreBy(shopplanId, _) }
    }

    cud.invites.foreach { invites =>
      invites.adds    .toSeq.flatten.foreach { batch add ShopPlanInvites.insertInvite(_) }
      invites.removals.toSeq.flatten.foreach { batch add ShopPlanInvites.deleteInviteBy(shopplanId, _) }
    }

    batch.future()
  }



  def deleteShopPlan(shopplanId: ShopPlanId)(implicit executor: ExecutionContext) = {
    val batch = BatchStatement()

    val batchDeletesF =
      batch add ShopPlanMeta            .deleteShopPlansBy(shopplanId)
      batch add ShopPlanMetaByInvitation.deleteShopPlansBy(shopplanId)
      batch add ShopPlanStores          .deleteStoresBy(shopplanId)
      batch add ShopPlanDestinations    .deleteDestinationsBy(shopplanId)


    ShopPlanInvites.getInvitesBy(shopplanId).fetch() flatMap { invites =>
      val batch = BatchStatement()

      batch add ShopPlanInvites.deleteInvitesBy(shopplanId)

      invites.foreach { invite =>
        batch add ShopPlanMeta            .deleteShopPlanBy(invite.friendId, shopplanId)
        batch add ShopPlanMetaByInvitation.deleteInvitedShopPlanBy(invite.friendId, shopplanId)
      }

      batch.future()
    }
  }


  /////////////////// Private methods /////////////////////////////


  private def getShopPlanFields(shopplanId: ShopPlanId, fields: Seq[ShopPlanField])(implicit executor: ExecutionContext) = {
    import ShopPlanField._

    val storesF =
      fields.find(_ == Stores)
        .map { _ => ShopPlanStores.getStoresBy(shopplanId, toStoreField(fields)).fetch().map(_.some) }
        .getOrElse(Future.successful(None))

    val destinationsF =
      fields.find(_ == Destinations)
        .map { _ => ShopPlanDestinations.getDestinationsBy(shopplanId).fetch().map(_.some) }
        .getOrElse(Future.successful(None))

    // Get invites
    val invitesF =
      fields.find(_ == Invites)
        .map { _ => ShopPlanInvites.getInvitesBy(shopplanId).fetch().map(_.some) }
        .getOrElse(Future.successful(None))

    (storesF, destinationsF, invitesF)
  }



  /**
   * Create Seq[ShopPlanStoreField] from Seq[ShopPlanField]
   */
  private def toStoreField(fields: Seq[ShopPlanField]) = {
    import ShopPlanStoreField._

    val storeFields = Seq(Name, Address, ItemTypes)
    if(fields.contains(CatalogueItems)) storeFields :+ CatalogueItems
    else storeFields
  }

}
