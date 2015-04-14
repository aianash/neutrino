namespace java com.goshoplane.common
namespace js goshoplane.common

typedef string Url
typedef string City
typedef string Country
typedef string Pincode
typedef string AddressTitle
typedef string AddressShort
typedef string AddressFull
typedef double Latitude
typedef double Longitude
typedef string Email
typedef string Timezone
typedef string FBToken


enum Locale {
  EN_US = 1;
}

enum Gender {
  FEMALE = 1;
  MALE = 2;
}

struct UserId {
  1: i64 uuid;
}


struct UserName {
  1: optional string first;
  2: optional string last;
  3: optional string handle;
}

struct UserAvatar {
  1: optional Url small;
  2: optional Url medium;
  3: optional Url large;
}


struct FacebookInfo {
  1: UserId userId;
  2: optional FBToken token;
}

struct UserInfo {
  1: optional UserName name;
  2: optional Locale locale;
  3: optional Gender gender;
  4: optional FacebookInfo facebookInfo;
  5: optional Email email;
  6: optional Timezone timezone;
  7: optional UserAvatar avatar;
  8: optional bool isNew;
}

enum ItemType {
  ApparelMen = 1;
}

struct StoreId {
  1: i64 stuid;
}

struct StoreName {
  1: optional string full;
  2: optional string handle;
}

struct CatalogueItemId {
  1: StoreId storeId;
  2: i64 cuid;
}

enum SerializerType {
  MSGPCK = 1;
  JSON   = 2;
  Kryo   = 3;
}

struct SerializerId {
  1: string sid;
  2: SerializerType stype;
}

struct SerializedCatalogueItem {
  1: CatalogueItemId itemId;
  2: SerializerId serializerId;
  3: binary stream;
}

struct GPSLocation {
  1: Latitude lat;
  2: Longitude lng;
}

struct PostalAddress {
  1: optional GPSLocation gpsLoc;
  2: optional AddressTitle title;
  3: optional AddressShort short;
  4: optional AddressFull full;
  5: optional Pincode pincode;
  6: optional Country country;
  7: optional City city;
}