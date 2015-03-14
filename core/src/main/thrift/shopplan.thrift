include 'common.thrift'

namespace java com.goshoplane.neutrino.shopplan
namespace js neutrino.shopplan

typedef i16 DestinationOrder

struct ShopPlanId {
  1: common.UserId createdBy;
  2: i64 suid;
}

struct Store {
  1: common.StoreId storeId;
  2: string name;
  3: list<common.SerializedCatalogueItem> collection;
  4: optional common.PostalAddress address;
}

struct DestinationId {
  1: ShopPlanId shopplanId;
  2: i64 duid;
}

struct Destination {
  1: DestinationId destId;
  2: DestinationOrder order;
  3: optional list<Store> stores;
  4: optional common.PostalAddress address;
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

struct ShopPlan {
  1: ShopPlanId shopplanId;
  2: optional list<Destination> destinations;
  3: optional list<Friend> friends;
}