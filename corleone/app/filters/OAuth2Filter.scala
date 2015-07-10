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

import play.api.{Logger,  Play}
import security.{OAuth2Helper, OAuth2Constants}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.http._


/**
 * The OAuth2Filter filters all requests for valid OAuth2 credentials, if the the filter is enabled and the requested 
 * server path is not excluded from the check (to save performance for example). Note that the filter validates the
 * access token against the token info endpoint for each (enabled) request. If the access token is almost expired,
 * it tries to refresh this token.
 */
class OAuth2Filter @Inject() (oauth2: OAuth2Helper) extends Filter
{
  val NO_TOKEN :String = "NO_TOKEN"
  val EXCLUDED_REQUEST_PATHS: Set[String] = Set("/oauth_callback", " /heartbeat")
  val customExcludedRequestPaths = {
     val excludedPathsList = Play.current.configuration.getStringList("oauth2.excluded.paths").get
    
    // unfortunately, excludedPathsList has type java.util.List
    val entrySetBuilder = collection.mutable.Set.newBuilder[String]
    val iterator = excludedPathsList.iterator()
    while(iterator.hasNext) entrySetBuilder += iterator.next()
    
    entrySetBuilder.result()
  }
  
  
  override def apply(nextFilter: RequestHeader => Future[Result]) 
                    (requestHeader: RequestHeader): Future[Result] = {
    
    if(! OAuth2Constants.isOAuth2Enabled) return nextFilter.apply(requestHeader)

    if (Logger.isDebugEnabled) Logger.debug("Entered OAuth2Filter for path " + requestHeader.path)
    
    // go on with following filter actions, if path is not excluded from OAUTH2 security check
    if(EXCLUDED_REQUEST_PATHS.contains(requestHeader.path)) return nextFilter.apply(requestHeader)
    if(! customExcludedRequestPaths.find(entry => requestHeader.path.startsWith(entry)).isEmpty) return nextFilter.apply(requestHeader)
    
    if (Logger.isDebugEnabled) Logger.debug("OAuth2Filter is executed for path " + requestHeader.path)
    
    val accessTokenOption = requestHeader.session.get("oauth2_access_token")
    val accessToken = accessTokenOption.getOrElse(NO_TOKEN)
    
    if (accessToken == NO_TOKEN) {
      Logger.info("No token was supplied -> redirecting user to authorization server")
      oauth2.redirectToAuthorizationServer(requestHeader.path)
    }
    else {
      val tokenInfoResponse = oauth2.requestTokenInfo(accessToken)
      
      if (isAccessGranted(tokenInfoResponse)){
        Logger.info("supplied access token IS valid")

        if(isAccessTokenAlmostExpired(tokenInfoResponse)) {

          Logger.debug("access token IS almost expired -> trying to refresh access token")
          
          val refreshTokenOption = requestHeader.session.get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN)

          if ( refreshTokenOption.isEmpty ) {
            Logger.warn("could not find refresh token in current session -> redirecting user to authorization server")
            return oauth2.redirectToAuthorizationServer(requestHeader.path)
          }
          
          val refreshToken = refreshTokenOption.get
          val accessTokenRefreshResponse = oauth2.refreshAccessToken(refreshToken)
          
          if(accessTokenRefreshResponse.status == Status.OK) {
              
              Logger.debug("access token refresh request WAS successful")
                          
              val newAccessTokenOption = (accessTokenRefreshResponse.json \ "access_token").asOpt[String]
              if(newAccessTokenOption.isEmpty) {
                Logger.warn("did not receive new access token from token refresh request -> redirecting user to authorization server")
              }
                
              val newAccessToken = newAccessTokenOption.get
              val newRefreshToken = (accessTokenRefreshResponse.json \ "refresh_token").asOpt[String]
            
              // NOTE: an access token refresh request might deliver a new refresh token. if so, we must use the
              // new one for the next refresh
              return nextFilter(requestHeader).map{ result =>
                if(newRefreshToken.isEmpty) result.withSession(("oauth2_access_token", newAccessToken))
                else result.withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, newAccessToken), 
                                        (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, newRefreshToken.get))
              }
          }
          else {
            val error = accessTokenRefreshResponse.body
            Logger.info(s"access token refresh request was NOT successful -> redirecting user to authorization server [error=$error]")
            return oauth2.redirectToAuthorizationServer(requestHeader.path)
          }
        }
        else {
          Logger.debug("access token is NOT almost expired")
        }
        
        // apply filter following in filter chain
        nextFilter.apply(requestHeader)
      }
      else{
        Logger.info("supplied access token is NOT valid -> redirecting user to authorization server" )
        oauth2.redirectToAuthorizationServer(requestHeader.path)
      }
    }
  }

  
  def isAccessGranted(response: WSResponse): Boolean = {
    // token is still valid. now. let's check if user has sufficient scopes 
    if (response.status == Status.OK){
      // at the moment, we can only check if the user is an employee. In the future, it will be possible to assign
      // more scopes to an user
      val realmOption = (response.json \ "realm").asOpt[String]
      realmOption match {
        case Some(realm) => realm == "employees"
        case _           => false
      }
    }
    else false
  }
  
  
  def isAccessTokenAlmostExpired(tokenInfoResponse: WSResponse): Boolean = {
    val tokenExpiryTimeInSecondsOption = (tokenInfoResponse.json \ "expires_in").asOpt[Int]

    tokenExpiryTimeInSecondsOption match {
      case Some(expiryTime) => expiryTime <= OAuth2Constants.expiryTimeLimitForTokenRefreshInSeconds
      case _ => {
        Logger.warn("Did not receive token expiry time from token info request -> access token is considered as expired")
        false
      }
    }
  }
}

