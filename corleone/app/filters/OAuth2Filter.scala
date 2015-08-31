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

import play.api.{Logger, Play}
import security.{OAuth2Helper, OAuth2Constants}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.http._


/**
 * The OAuth2Filter filters all non-service requests for valid OAuth2 credentials, if the the filter is enabled and the requested 
 * server path is not excluded from the check (to save performance for example). Note that the filter validates the
 * access token against the token info endpoint for each (enabled) request. If the access token is almost expired,
 * it tries to refresh this token.
 */
class OAuth2Filter @Inject()(oauth2: OAuth2Helper) extends Filter {
  val NO_TOKEN: String = "NO_TOKEN"
  val EXCLUDED_REQUEST_PATHS: Set[String] = Set("/oauth_callback", " /heartbeat", "/api")
  val customExcludedRequestPaths = oauth2.extractConfiguredValueList("oauth2.excluded.paths") ++
    oauth2.extractConfiguredValueList("oauth2.service.paths")


  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {

    if (OAuth2Constants.isOAuth2Enabled) {
      if (Logger.isDebugEnabled) Logger.debug("Entered OAuth2Filter for path " + requestHeader.path)

      // go on with following filter actions, if path is not excluded from OAUTH2 security check
      if (!EXCLUDED_REQUEST_PATHS.find(entry => requestHeader.path.startsWith(entry)).isEmpty) {
        nextFilter.apply(requestHeader)
      }
      else if (!customExcludedRequestPaths.find(entry => requestHeader.path.startsWith(entry)).isEmpty) {
        nextFilter.apply(requestHeader)
      }
      else {
        if (Logger.isDebugEnabled) Logger.debug("OAuth2Filter is executed for path " + requestHeader.path)

        val accessTokenOption = requestHeader.session.get("oauth2_access_token")
        val accessToken = accessTokenOption.getOrElse(NO_TOKEN)

        if (accessToken == NO_TOKEN) {
          Logger.info("No token was supplied -> redirecting user to authorization server")
          oauth2.redirectToAuthorizationServer(requestHeader.path)
        }
        else {
          val tokenInfoResponseFuture = oauth2.requestTokenInfo(accessToken)
          tokenInfoResponseFuture.flatMap { tokenInfoResponse =>
            processTokenInfo(nextFilter, requestHeader, tokenInfoResponse)
          }
        }
      }
    }
    else {
      nextFilter.apply(requestHeader)
    }
  }


  private def isAccessGranted(response: WSResponse): Boolean = {
    // token is still valid. now. let's check if user has sufficient scopes 
    if (response.status == Status.OK) {
      // at the moment, we can only check if the user is an employee. In the future, it will be possible to assign
      // more scopes to an user
      val realmOption = (response.json \ "realm").asOpt[String]
      realmOption match {
        case Some(realm) => realm == "employees"
        case _ => false
      }
    }
    else {
      Logger.warn("request to token info endpoint was not successful -> access is not granted")
      false
    }
  }


  private def isAccessTokenAlmostExpired(tokenInfoResponse: WSResponse): Boolean = {
    val tokenExpiryTimeInSecondsOption = (tokenInfoResponse.json \ "expires_in").asOpt[Int]

    tokenExpiryTimeInSecondsOption match {
      case Some(expiryTime) => expiryTime <= OAuth2Constants.expiryTimeLimitForTokenRefreshInSeconds
      case _ => {
        Logger.warn("Did not receive token expiry time from token info request -> access token is considered as expired")
        true
      }
    }
  }


  private def refreshAccessToken(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader, refreshToken: String): Future[Result] = {
    val accessTokenRefreshResponseFuture = oauth2.refreshAccessToken(refreshToken)

    accessTokenRefreshResponseFuture.flatMap { accessTokenRefreshResponse =>
      if (accessTokenRefreshResponse.status == Status.OK) {

        Logger.debug("access token refresh request WAS successful")

        val newAccessTokenOption = (accessTokenRefreshResponse.json \ "access_token").asOpt[String]
        if (newAccessTokenOption.isEmpty) {
          Logger.warn("did not receive new access token from token refresh request -> redirecting user to authorization server")
          oauth2.redirectToAuthorizationServer(requestHeader.path)
        }
        else {
          val newAccessToken = newAccessTokenOption.get
          val newRefreshTokenOption = (accessTokenRefreshResponse.json \ "refresh_token").asOpt[String]

          // NOTE: an access token refresh request might deliver a new refresh token. if so, we must use the
          // new one for the next refresh
          val otherSessionData = requestHeader.session.data.filterKeys(k =>
                k != OAuth2Constants.SESSION_KEY_ACCESS_TOKEN &&
                k != OAuth2Constants.SESSION_KEY_REFRESH_TOKEN)
            .toList

          val cookies = requestHeader.cookies.filterNot(cookie =>
              cookie.name != OAuth2Constants.SESSION_KEY_ACCESS_TOKEN &&
              cookie.name != OAuth2Constants.SESSION_KEY_ACCESS_TOKEN)
            .toList


          nextFilter(requestHeader).map { result =>
            newRefreshTokenOption.map{ newRefreshToken =>
                val sessionData: List[(String, String)] = otherSessionData :::
                  List((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, newAccessToken),
                    (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, newRefreshToken))
  
                result.withSession(sessionData: _*)
                  .withCookies(cookies: _*)
                  .withHeaders(requestHeader.headers.headers: _*)
            }.getOrElse {
              val sessionData: List[(String, String)] = otherSessionData :::
                List((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, newAccessToken),
                  (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, refreshToken))

              result.withSession(sessionData: _*)
                .withCookies(cookies: _*)
                .withHeaders(requestHeader.headers.headers: _*)
            }
          }
        }
      }
      else {
        val error = accessTokenRefreshResponse.body
        Logger.info(s"access token refresh request was NOT successful -> redirecting user to authorization server [error=$error]")
        oauth2.redirectToAuthorizationServer(requestHeader.path)
      }
    }
  }


  private def processTokenInfo(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader, tokenInfoResponse: WSResponse): Future[Result] = {
    if (isAccessGranted(tokenInfoResponse)) {
      Logger.info("supplied access token IS valid")

      if (isAccessTokenAlmostExpired(tokenInfoResponse)) {

        Logger.debug("access token IS almost expired -> trying to refresh access token")

        val refreshTokenOption = requestHeader.session.get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN)

        refreshTokenOption match {
          case Some(refreshToken) => refreshAccessToken(nextFilter, requestHeader, refreshToken)
          case None => {
            Logger.warn("could not find refresh token in current session -> redirecting user to authorization server")
            oauth2.redirectToAuthorizationServer(requestHeader.path)
          }
        }
      }
      else {
        Logger.debug("access token is NOT almost expired")

        // apply filter following in filter chain
        nextFilter(requestHeader).map { result =>
          result.withSession(requestHeader.session)
            .withCookies(requestHeader.cookies.toList: _*)
            //.withHeaders(requestHeader.headers.headers: _*)
        }
      }
    }
    else {
      Logger.info("supplied access token is NOT valid -> redirecting user to authorization server")
      oauth2.redirectToAuthorizationServer(requestHeader.path)
    }
  }
}

