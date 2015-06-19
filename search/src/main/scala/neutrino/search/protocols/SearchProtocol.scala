package neutrino.search.protocols

import goshoplane.commons.core.protocols._

import com.goshoplane.neutrino.service._
import com.goshoplane.creed.search._


sealed trait SearchMessages

case class SearchCatalogue(request: CatalogueSearchRequest)
  extends SearchMessages with Replyable[SearchResult]