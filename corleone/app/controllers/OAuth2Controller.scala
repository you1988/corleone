package controllers

import java.util.{Date, UUID}

import play.api._
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.mvc._
import security.OAuth2Credentials

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.Play.current

/**
 * This controller provides a callback endpoint for handling the flow as described in 
 * http://tools.ietf.org/html/rfc6749#section-4.1.3
 */
class OAuth2Controller extends Controller {

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

    val stateReceivedByCallback = request.getQueryString("state").get // TODO error handling
    val stateInSession = request.session.get("oauth2_state").get // TODO error handling
    
    // check for possible CSRF attack
    if (stateReceivedByCallback != stateInSession) 
        // TODO correct status code?
        NotAcceptable("OAUTH2 state in session does not match state received from callback")
    else {
      val codeReceivedByCallback = request.getQueryString("code").get // TODO error handling
      val credentials = new OAuth2Credentials(Play.current)
      val redirectUri = Play.current.configuration.getString("oauth2.callback.url").get // TODO error handling
      val payload = s"grant_type=authorization_code&code=$codeReceivedByCallback&realm=employees&redirect_uri=$redirectUri"

      val accessTokenUrl = Play.current.configuration.getString("oauth2.access.token.url").get // TODO error handling
      
      val futureResponse = WS.url(accessTokenUrl)
        .withHeaders(("Content-Type","application/x-www-form-urlencoded"))
        .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
        .withRequestTimeout(5000L)
        .post(payload)

      // TODO error handling
      val response = Await.result(futureResponse, Duration(5L, SECONDS))

      // TODO check error code
      
      val json = response.json
      val accessToken = (json \ "access_token").asOpt[String].get // TODO error handling
      val originalRequest = request.session.get("oauth2_original_request_url").get
      
      // FIXME consider expiryTimeInSeconds
      Redirect(originalRequest)
        .withNewSession
        .withSession( ("oauth2_access_token", accessToken),
                      //("oauth2_token_expiry_time", expiryTimeInSeconds),
                      ("oauth2_token_receive_time_in_ms", String.valueOf(new Date().getTime))
        )
    }
  }
  
}
