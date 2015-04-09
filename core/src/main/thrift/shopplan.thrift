include 'common.thrift'

namespace java com.goshoplane.neutrino.shopplan
namespace js neutrino.shopplan

typedef i16 DestinationOrder
typedef string Title
typedef i64 Timestamp

struct ShopPlanId {
  1: common.UserId createdBy;
  2: i64 suid;
}

struct DestinationId {
  1: ShopPlanId shopplanId;
  2: i64 dtuid;
}

enum BucketStoreField {
  NAME            = 1;
  ADDRESS         = 2;
  ITEM_TYPES      = 3;
  CATALOGUE_ITEMS = 4;
}

struct BucketStore {
  1: common.StoreId storeId;
  2: optional common.StoreName name;
  3: optional common.PostalAddress address;
  4: optional list<common.ItemType> itemTypes;
  5: optional list<common.SerializedCatalogueItem> catalogueItems;
}

enum ShopPlanStoreField {
  NAME            = 1;
  ADDRESS         = 2;
  ITEM_TYPES      = 3;
  CATALOGUE_ITEMS = 4;
}

struct ShopPlanStore {
  1: common.StoreId storeId;
  2: DestinationId destId;
  3: optional common.StoreName name;
  4: optional common.PostalAddress address;
  5: optional list<common.ItemType> itemTypes;
  6: optional list<common.SerializedCatalogueItem> catalogueItems;
}

struct Destination {
  1: DestinationId destId;
  2: common.PostalAddress address;
  3: optional i32 numShops;
}

enum InviteStatus {
  PENDING = 1;
  INVITED = 2;
  ACCEPTED = 3;
}

struct Friend {
  1: common.UserId id;
  2: optional common.UserName name;
  3: optional common.UserAvatar avatar;
  4: optional InviteStatus inviteStatus;
}

enum ShopPlanField {
  TITLE           = 1;
  STORES          = 2;
  CATALOGUE_ITEMS = 3;
  DESTINATIONS    = 4;
  INVITES         = 5;
}

struct ShopPlan {
  1: ShopPlanId shopplanId;
  2: optional Title title;
  3: optional list<ShopPlanStore> shopplanStores;
  4: optional list<Destination> destinations;
  5: optional list<Friend> invites;
  6: bool isInvitation;
}