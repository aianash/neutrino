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

/**
 * Store objec wih link to a destination
 */
struct DStore {
  1: common.StoreId storeId;
  2: common.StoreName name;
  3: common.PostalAddress address;
  4: optional DestinationId destId;
  5: optional list<common.ItemType> itemTypes;
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

struct ShopPlan {
  1: ShopPlanId shopplanId;
  2: optional Title title;
  3: optional list<DStore> dstores;
  4: optional list<Destination> destinations;
  5: optional list<Friend> invites;
  6: bool isInvitation;
}