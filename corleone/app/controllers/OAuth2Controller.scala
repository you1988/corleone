package controllers

import java.util.{Date, UUID}

import play.api._
import play.api.http.Status
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.mvc._
import security.{OAuth2Helper, OAuth2Credentials}

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
   handleCallback(request)
  }
 
  
  // weird hack in order to allow return of result instances
  def handleCallback(request: Request[AnyContent]): Result = {
    val stateReceivedByCallbackOption = request.getQueryString("state")
    val stateInSessionOption = request.session.get("oauth2_state")

    if (stateReceivedByCallbackOption.isEmpty )
      BadRequest("Did not receive any OAUTH2 state from callback")
    else if(stateInSessionOption.isEmpty)
      BadRequest("Could not find OAUTH2 state from session")
    else {
      val stateReceivedByCallback = stateReceivedByCallbackOption.get
      val stateInSession = stateInSessionOption.get

      // check for possible CSRF attack
      if (stateReceivedByCallback != stateInSession)
        Conflict("OAUTH2 state in session does not match state received from callback")
      else {
        val codeReceivedByCallbackOption = request.getQueryString("code")
        if(codeReceivedByCallbackOption.isEmpty) return BadRequest("Did not receive OAUTH2 code from callback")


        val response = OAuth2Helper.requestAccessToken(codeReceivedByCallbackOption.get)
        if (response.status == OK) {
          val json = response.json
          val accessToken = (json \ "access_token").asOpt[String].get // TODO error handling
          val refreshToken = (json \ "refresh_token").asOpt[String].get // TODO error handling


          val originalRequest = request.session.get("oauth2_original_request_url").get

          Redirect(originalRequest)
            .withNewSession
            .withSession( ("oauth2_access_token", accessToken),
              ("oauth2_refresh_token", refreshToken) )
        }
        else {
          val error = response.body
          InternalServerError(s"access token request was not successful: $error")
        }
      }




    }
  }
}
