include 'common.thrift'
include 'shopplan.thrift'

namespace java com.goshoplane.neutrino.service
namespace js neutrino.service

struct AddDestinationReq {
  1: shopplan.DestinationId id;
  2: common.GPSLocation location;
  3: shopplan.DestinationOrder order;
}

service Neutrino {

  #/** ShopPlan CRUD apis */
  shopplan.ShopPlan newShopPlanFor(1:common.UserId userId);
  shopplan.ShopPlan getShopPlan(1:shopplan.ShopPlanId shopplanId);

  bool addStores(1:shopplan.ShopPlanId shopplanId, 2:list<common.StoreId> storeId);
  bool removeStores(1:shopplan.ShopPlanId shopplanId, 2:list<common.StoreId> storeId);

  bool addItems(1:shopplan.ShopPlanId shopplanId, 2:list<common.CatalogueItemId> itemId);
  bool removeItems(1:shopplan.ShopPlanId shopplanId, 2:list<common.CatalogueItemId> itemId);

  bool inviteUsers(1:shopplan.ShopPlanId shopplanId, 2:list<common.UserId> userId);
  bool removeUsersFromInvites(1:shopplan.ShopPlanId shopplanId, 2:list<common.UserId> userId);
  list<shopplan.Friend> getInvitedUsers(1:shopplan.ShopPlanId shopplanId);

  list<common.GPSLocation> getMapLocations(1:shopplan.ShopPlanId shopplanId);
  list<common.GPSLocation> getDestinations(1:shopplan.ShopPlanId shopplanId);
  bool addDestinations(1:list<AddDestinationReq> destReqs);
  bool removeDestinations(1:list<shopplan.DestinationId> destinations);
  bool updateDestinationLoc(1:shopplan.DestinationId destId, 2:common.GPSLocation location);

  #/** Search APIs */

  #/** Messaging APIs */
}