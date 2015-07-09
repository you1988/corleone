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

package security

import com.google.inject.Inject
import play.api.cache._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api._
import play.api.mvc._
import play.api.Application

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._

/**
 * Provides OAUTH2 credentials to its client.
 * <b>DO NOT CACHE THE RETURNED CREDENTIALS!!!</b> The provider already takes for caching.
 */
class OAuth2CredentialsProvider @Inject() (application: Application, cache: CacheApi) {
  
   private val CACHE_KEY = "oauth2.cache.credentials"

  /**
   * Provides access to the OAuth2 credentials stored in the file specified in 
   * 'oauth2.credentials.filePath'.
   * <b>DO NOT CACHE THE RETURNED CREDENTIALS!!!</b>. The reason is that they are rotated periodically 
   * and the communication with stale credentials would fail. This method already takes care for 
   * some caching. Other components of the OAUTH2 mechanism might invalidate this cache. 
   * 
   * @return OAUTH2 credentials
   */
   def get: OAuth2Credentials = {
     
      /*
       * We try to reduce IO by avoiding reading the file for each single request. The OAuth2 mechanism is aware
       * of this cache and invalidates it if it might be necessary (for example, if a request fails, it might be because
       * of stale credentials).
       */
    
     val cachedCredentials = cache.get[OAuth2Credentials](CACHE_KEY)
     if(cachedCredentials.isEmpty) {
       val credentials = scala.io.Source.fromFile(OAuth2Constants.credentialsFilePath).mkString
       val json = Json.parse(credentials)

       val (clientId, clientSecret)  = ( (json \ "client_id")    .asOpt[String].get,
                                         (json \ "client_secret").asOpt[String].get  )
       
       val oauth2Credentials = new OAuth2Credentials(clientId, clientSecret)
       cache.set(CACHE_KEY, oauth2Credentials, Duration(OAuth2Constants.credentialsCacheExpiryTime, SECONDS))
       oauth2Credentials
     }
     else{
       cachedCredentials.get
     }
   }
  
  def invalidateCache() = {
    cache.remove(CACHE_KEY)
  }
}

class OAuth2Credentials(val clientId: String, val clientSecret: String)
