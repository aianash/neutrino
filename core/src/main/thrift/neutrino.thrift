include 'common.thrift'
include 'shopplan.thrift'

namespace java com.goshoplane.neutrino.service
namespace js neutrino.service


exception NeutrinoException {
  1: string message;
}

struct AddDestinationReq {
  1: shopplan.DestinationId id;
  2: common.GPSLocation location;
  3: shopplan.DestinationOrder order;
}

service Neutrino {

  #/** ShopPlan CRUD apis */
  shopplan.ShopPlan newShopPlanFor(1:common.UserId userId) throws (1:NeutrinoException nex);
  shopplan.ShopPlan getShopPlan(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);

  bool addStores(1:shopplan.ShopPlanId shopplanId, 2:set<common.StoreId> storeIds) throws (1:NeutrinoException  nex);
  bool removeStores(1:shopplan.ShopPlanId shopplanId, 2:set<common.StoreId> storeIds) throws (1:NeutrinoException nex);

  bool addItems(1:shopplan.ShopPlanId shopplanId, 2:set<common.CatalogueItemId> itemIds) throws (1:NeutrinoException nex);
  bool removeItems(1:shopplan.ShopPlanId shopplanId, 2:set<common.CatalogueItemId> itemIds) throws (1:NeutrinoException nex);

  bool inviteUsers(1:shopplan.ShopPlanId shopplanId, 2:set<common.UserId> userIds) throws (1:NeutrinoException nex);
  bool removeUsersFromInvites(1:shopplan.ShopPlanId shopplanId, 2:set<common.UserId> userIds) throws (1:NeutrinoException nex);
  set<shopplan.Friend> getInvitedUsers(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);

  set<common.GPSLocation> getMapLocations(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);

  set<common.GPSLocation> getDestinationLocs(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);
  bool addDestinations(1:set<AddDestinationReq> destReqs) throws (1:NeutrinoException nex);
  bool removeDestinations(1:set<shopplan.DestinationId> destIds) throws (1:NeutrinoException nex);
  bool updateDestinationLoc(1:shopplan.DestinationId destId, 2:common.GPSLocation location) throws (1:NeutrinoException nex);

  #/** Search APIs */

  #/** Messaging APIs */
}