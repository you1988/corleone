/*
 * Copyright [2015] Zalando SE
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package filters

import javax.inject.Inject


import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter


/**
 * See 
 * <ul>
 * <li>https://www.playframework.com/documentation/2.4.x/SecurityHeaders</li>
 * <li>https://www.playframework.com/documentation/2.4.x/GzipEncoding</li>
 * </ul>
 */
class Filters @Inject() (oauth2Filter: OAuth2Filter, 
                         oauth2ServiceFilter: OAuth2ServiceCallFilter, 
                         securityHeadersFilter: SecurityHeadersFilter,
                         gzipFilter: GzipFilter) extends HttpFilters {
  def filters = Seq(oauth2Filter, oauth2ServiceFilter, gzipFilter, securityHeadersFilter)
}
