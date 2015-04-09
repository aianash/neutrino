include 'common.thrift'
include 'shopplan.thrift'
include 'feed.thrift'

namespace java com.goshoplane.neutrino.service
namespace js neutrino.service

typedef string FBToken

exception NeutrinoException {
  1: string message;
}

struct FacebookInfo {
  1: common.UserId userId;
  2: FBToken token;
}

struct UserInfo {
  1: optional common.UserId userId;
  2: optional common.UserName names;
  3: optional common.Locale locale;
  4: optional common.Gender gender;
  5: optional FacebookInfo facebookInfo;
  6: optional common.Email email;
  7: optional common.Timezone timezone;
  8: optional common.UserAvatar avatar;
  9: optional bool isNew;
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

struct CUDDStores {
  1: optional list<shopplan.DStore> adds;
  2: optional list<common.StoreId> removes;
}

struct CUDShopPlan {
  1: optional CUDDestination destinations;
  2: optional CUDInvites invites;
  3: optional CUDDStores dstores;
}

service Neutrino {

  #/** User APIs */
  UserInfo createOrUpdateUser(1:UserInfo userInfo) throws (1:NeutrinoException nex);
  UserInfo getUserDetail(1:common.UserId userId) throws (1:NeutrinoException nex);
  set<shopplan.Friend> getFriendsForInvite(1:common.UserId userId, 2:FriendListFilter filter) throws (1:NeutrinoException nex);


  #/** Bucket APIs */
  set<shopplan.DStore> getBucketStoreLocations(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);
  set<shopplan.DStore> getShopPlanStoreLocations(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);


  #/** ShopPlan APIs */
  list<shopplan.ShopPlan> getShopPlans(1:common.UserId userId) throws (1:NeutrinoException nex);
  shopplan.ShopPlan getShopPlan(1:shopplan.ShopPlanId shopplanId, 2:list<string> fields) throws (1:NeutrinoException nex);
  shopplan.ShopPlan createShopPlan(1:common.UserId userId, 2:CUDShopPlan cud) throws (1:NeutrinoException nex);
  shopplan.ShopPlan cudShopPlan(1:shopplan.ShopPlanId shopplanId, 2:CUDShopPlan cud) throws (1:NeutrinoException nex);
  shopplan.ShopPlan endShopPlan(1:shopplan.ShopPlanId shopplanId) throws (1:NeutrinoException nex);


  #/** Feed APIs */
  feed.Feed getCommonFeed(1:feed.FeedFilter filter) throws (1:NeutrinoException nex);
  feed.Feed getUserFeed(1:common.UserId userId, 2:feed.FeedFilter filter) throws (1:NeutrinoException nex);

}