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

import java.util.UUID
import javax.inject.Inject

import play.api.mvc.Results
import play.api.{Logger, Play}
import play.api.libs.ws.{WSAuthScheme, WS, WSResponse}
import play.mvc.Http.Status

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2Helper @Inject() (credentialsProvider: OAuth2CredentialsProvider ) {


  def requestAccessToken(oauth2Code: String, wasAlreadyCalledBefore: Boolean = false): WSResponse = {
    val credentials = credentialsProvider.get
    val callbackUrl = OAuth2Constants.callbackUrl
    val payload = s"grant_type=authorization_code&code=$oauth2Code&realm=employees&redirect_uri=$callbackUrl"

    val futureResponse = WS.url(OAuth2Constants.accessTokenUrl)
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"))
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .withRequestTimeout(OAuth2Constants.requestTimeout)
      .post(payload)

    val response =  Await.result(futureResponse, Duration(5L, SECONDS))
    
    // if response was not successful, the reason might be stale credentials. So we invalidate the cache and try it again
    if(response.status != Status.OK && ! wasAlreadyCalledBefore) {
      credentialsProvider.invalidateCache()
      requestAccessToken(oauth2Code, true)
    } 
    else response
  }


  def refreshAccessToken(refreshToken: String, wasAlreadyCalledBefore: Boolean = false): WSResponse = {

    val credentials = credentialsProvider.get
    val payload = s"grant_type=refresh_token&refresh_token=$refreshToken&realm=employees"

    val futureResponse = WS.url(OAuth2Constants.accessTokenUrl)
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"))
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .withRequestTimeout(OAuth2Constants.requestTimeout)
      .post(payload)

    val response = Await.result(futureResponse, Duration(5L, SECONDS))

    // if response was not successful, the reason might be stale credentials. So we invalidate the cache and try it again
    if(response.status != Status.OK && ! wasAlreadyCalledBefore) {
      credentialsProvider.invalidateCache()
      refreshAccessToken(refreshToken, true)
    }
    else response
  }


  def requestTokenInfo(accessToken: String): Future[WSResponse] = {
    WS.url(OAuth2Constants.tokenInfoUrl)(Play.current)
      .withQueryString(("access_token", accessToken))
      .get()

  }


  def redirectToAuthorizationServer(originalRequestUrl: String) = Future {
    val oauthCredentials = credentialsProvider.get
    val state = UUID.randomUUID().toString
    val clientId = oauthCredentials.clientId
    
    val redirectToAuthTemplate = OAuth2Constants.authorizationUrl + 
                                            "?client_id=%s&redirect_uri=%s&state=%s&realm=employees&response_type=code"
    
    val redirectUrl = redirectToAuthTemplate.format(clientId, OAuth2Constants.callbackUrl, state)

    Logger.debug(s"redirecting user to [redirectUrl=$redirectUrl]")
    Results.Redirect(redirectUrl).withSession((OAuth2Constants.SESSION_KEY_STATE, state),
      (OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, originalRequestUrl))
  }

  def extractConfiguredValueList(configKey: String): Set[String] = {
    val configListOption = Play.current.configuration.getStringList(configKey)

    if(configListOption.isEmpty) {
      Set.empty
    }
    else {
      val excludedPathsList = configListOption.get
      
      // unfortunately, excludedPathsList has type java.util.List
      val entrySetBuilder = Set.newBuilder[String]
      val iterator = excludedPathsList.iterator()
      while(iterator.hasNext) entrySetBuilder += iterator.next()

      entrySetBuilder.result
    }
  }
}