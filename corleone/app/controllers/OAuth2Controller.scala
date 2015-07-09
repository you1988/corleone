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

package controllers

import java.util.{Date, UUID}

import com.google.inject.Inject
import play.api._
import play.api.http.Status
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.mvc._
import security.{OAuth2CredentialsProvider, OAuth2Helper, OAuth2Constants, OAuth2Credentials}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.Play.current


/**
 * This controller provides a callback endpoint for handling the flow as described in 
 * http://tools.ietf.org/html/rfc6749#section-4.1.3
 */
class OAuth2Controller @Inject() (oauth2: OAuth2Helper, credentialsProvider: OAuth2CredentialsProvider) extends Controller {

  /**
   * This callback is usually called by the authorization server when the user has authenticated successfully
   * (see http://tools.ietf.org/html/rfc6749#section-4.1).
   * According to http://tools.ietf.org/html/rfc6749#section-4.1.2, we can extract the supplied state as well as the
   * code from the query parameters. We use the received code to perform an access token request 
   * (http://tools.ietf.org/html/rfc6749#section-4.1.3). If the request is successful, we save the access token to
   * the session and redirect the user to her actual target URL. Note: Also the redirect goes through OAuth2Filter
   * where the access token is validated for each request.
   */
  def callback = Action{ request =>
    handleCallback(request)
  }
 
  
  // Hack in order to allow return of result instances. Otherwise, Scala can not derive the return type leading to a
  // compilation error
  def handleCallback(request: Request[AnyContent]): Result = {

    val originalRequest = request.session.get(OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL).get
    
    // check if Authorization server reported an error
    val possibleErrorOption = request.getQueryString("error")
    if (! possibleErrorOption.isEmpty) {
      val error = possibleErrorOption.get
      val errorDescription = request.getQueryString("error_description").get
      
      val isRetryAfterAuthError = ! request.session.get("oauth2_retry_after_auth_error").isEmpty
      return if (isRetryAfterAuthError){
        handleError(s"authorization server reported error via callback: [error=$error, errorDescription=$errorDescription]",
                    INTERNAL_SERVER_ERROR)
      }
      else {
        Logger.warn(s"authorization server reported error via callback: [error=$error, errorDescription=$errorDescription]" +
                      "-> retrying again while invalidating credentials cache and letting the user authenticate again")
        credentialsProvider.invalidateCache()
        Redirect(originalRequest).withNewSession.withSession(("oauth2_retry_after_auth_error", "true"))
      }
    }
    
    
    
    
    val stateReceivedByCallbackOption = request.getQueryString("state")
    val stateInSessionOption = request.session.get(OAuth2Constants.SESSION_KEY_STATE)

    if (stateReceivedByCallbackOption.isEmpty )
      return handleError("Did not receive any OAUTH2 state from callback", BAD_REQUEST)
    
    
    if (stateInSessionOption.isEmpty)
      return handleError("Could not find OAUTH2 state from session", BAD_REQUEST)
    
    
    val stateReceivedByCallback = stateReceivedByCallbackOption.get
    val stateInSession = stateInSessionOption.get
    
    // check for possible CSRF attack
    if (stateReceivedByCallback != stateInSession)
      handleError("OAUTH2 state in session does not match state received from callback", CONFLICT)
    
      val codeReceivedByCallbackOption = request.getQueryString("code")
      if(codeReceivedByCallbackOption.isEmpty) return handleError("Did not receive OAUTH2 code from callback", BAD_REQUEST)
    
    
      // finally, request access token with the code delivered via the callback performed by the authorization server
      val response = oauth2.requestAccessToken(codeReceivedByCallbackOption.get)
      if (response.status == OK) {
        val json = response.json
        val accessTokenOption = (json \ "access_token").asOpt[String]
        
        if (accessTokenOption.isEmpty)
          return handleError("Did not receive any access token from access token request", INTERNAL_SERVER_ERROR)
        
        val accessToken = accessTokenOption.get
        
        val refreshTokenOption = (json \ "refresh_token").asOpt[String]
        
        // redirect user back to his actual target URL together with the access token (and refresh token)
        if (refreshTokenOption.isEmpty) {
          Logger.warn("Did not receive any refresh token from access token request response -> access token can not be refreshed")

          Redirect(originalRequest)
            .withNewSession
            .withSession( (OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, accessToken))
        }
        else {
          val refreshToken = refreshTokenOption.get
          Redirect(originalRequest)
            .withNewSession
            .withSession( (OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, accessToken),
                          (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, refreshToken) )
        }
      }
      else {
        val error = response.body
        InternalServerError(s"access token request was not successful: $error")
      }
  }
  
  
  private def handleError(message: String, errorStatus: Int): Result = {
    Logger.warn(message)
    
    if(errorStatus == BAD_REQUEST) {
      return BadRequest(message).withNewSession
    }
    else if (errorStatus == CONFLICT) {
      return Conflict(message).withNewSession
    }
    
    InternalServerError(message).withNewSession
  }
  
}
