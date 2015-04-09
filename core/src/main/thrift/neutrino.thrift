include 'common.thrift'
include 'shopplan.thrift'
include 'feed.thrift'

namespace java com.goshoplane.neutrino.service
namespace js neutrino.service

exception NeutrinoException {
  1: string message;
}

struct FriendListFilter {
  1: optional common.PostalAddress location;
}

struct CUDDestination {
  1: optional list<shopplan.Destination> creates;
  2: optional list<shopplan.Destination> updates;
  3: optional list<shopplan.DestinationId> deletes;
}

struct CUDInvites {
  1: optional list<shopplan.Friend> adds;
  2: optional list<common.UserId> removes;
}

struct CUDShopPlanStore {
  1: optional list<shopplan.ShopPlanStore> adds;
  2: optional list<common.StoreId> removes;
}

struct CUDShopPlan {
  1: optional CUDDestination destinations;
  2: optional CUDInvites invites;
  3: optional CUDShopPlanStore shopplanStores;
}

service Neutrino {

  #/** User APIs */
  common.UserId createUser(1:common.UserInfo userInfo) throws (1:NeutrinoException nex);
  bool updateUser(1:common.UserId userId, 2:common.UserInfo userInfo) throws (1:NeutrinoException nex);
  common.UserInfo getUserDetail(1:common.UserId userId) throws (1:NeutrinoException nex);
  list<shopplan.Friend> getFriendsForInvite(1:common.UserId userId, 2:FriendListFilter filter) throws (1:NeutrinoException nex);


  #/** Bucket APIs */
  list<shopplan.BucketStore> getBucketStores(1:common.UserId userId, 2:list<shopplan.BucketStoreField> fields) throws (1:NeutrinoException nex);

  #/** ShopPlan APIs */
  list<shopplan.ShopPlanStore> getShopPlanStores(1:shopplan.ShopPlanId shopplanId, 2:list<shopplan.ShopPlanStoreField> fields) throws (1:NeutrinoException nex);
  list<shopplan.ShopPlan> getShopPlans(1:common.UserId userId, 2:list<shopplan.ShopPlanField> fields) throws (1:NeutrinoException nex);
  shopplan.ShopPlan getShopPlan(1:shopplan.ShopPlanId shopplanId, 2:list<shopplan.ShopPlanField> fields) throws (1:NeutrinoException nex);

  shopplan.ShopPlanId createShopPlan(1:common.UserId userId, 2:CUDShopPlan cud) throws (1:NeutrinoException nex);
  bool cudShopPlan(1:shopplan.ShopPlanId shopplanId, 2:CUDShopPlan cud) throws (1:NeutrinoException nex);
  bool endShopPlan(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);


  #/** Feed APIs */
  feed.Feed getCommonFeed(1:feed.FeedFilter filter) throws (1:NeutrinoException nex);
  feed.Feed getUserFeed(1:common.UserId userId, 2:feed.FeedFilter filter) throws (1:NeutrinoException nex);

}