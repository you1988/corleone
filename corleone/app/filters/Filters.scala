package filters

import javax.inject.Inject


import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter


/**
 * see 
 * <ul>
 * <li>https://www.playframework.com/documentation/2.4.x/SecurityHeaders</li>
 * <li>https://www.playframework.com/documentation/2.4.x/GzipEncoding</li>
 * </ul>
 */
class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter, gzipFilter: GzipFilter) extends HttpFilters {
  def filters = Seq(gzipFilter, securityHeadersFilter)
}
