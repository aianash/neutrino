include 'common.thrift'

namespace java com.goshoplane.neutrino.feed
namespace js neutrino.feed

typedef i32    PageIndex
typedef i32    StreamIndex

# [IMP] Feed and Post structure will be extended
# at later stage in the project for Social Networking

struct PostId {
  1: i64 ptuid;
}


struct Offer {
  1: string title;
  2: string subtitle;
}


struct OfferPost {
  1: PostId postId;
  2: StreamIndex index;
  3: common.StoreId storeId;
  4: common.StoreName storeName;
  5: common.PostalAddress storeAddress;
  6: Offer offer;
}


struct PosterImage {
  1: common.Url link;
}


struct PosterAd {
  1: i64 paduid;
  2: PosterImage image;
}


struct PosterAdPost {
  1: PostId postId;
  2: StreamIndex index;
  3: PosterAd poster;
}


struct Feed {
  1: list<OfferPost> offerPosts;
  2: list<PosterAdPost> posterAdPosts;
  3: PageIndex page;
}


struct FeedFilter {
  1: optional common.PostalAddress location;
  2: optional PageIndex page;
}