package security

import java.util.UUID

import play.api.mvc.Results
import play.api.{Logger, Play}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS}
import play.libs.Json

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

object OAuth2Helper {
  
  // TODO error handling
  val accessTokenUrl = Play.current.configuration.getString("oauth2.access.token.url").get
  val tokenInfoUrl = Play.current.configuration.getString("oauth2.token.info.url").get
  val callbackUrl = Play.current.configuration.getString("oauth2.callback.url").get
  val authorizationUrl = Play.current.configuration.getString("oauth2.authorization.url").get

  
  val REQUEST_TIMEOUT = 5000L
  
  
  def requestAccessToken(oauth2Code: String): WSResponse = {
    val credentials = OAuth2Credentials.get(Play.current)
    val redirectUri = Play.current.configuration.getString("oauth2.callback.url").get // TODO error handling
    val payload = s"grant_type=authorization_code&code=$oauth2Code&realm=employees&redirect_uri=$redirectUri"


    val futureResponse = WS.url(accessTokenUrl)
      .withHeaders(("Content-Type","application/x-www-form-urlencoded"))
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .withRequestTimeout(REQUEST_TIMEOUT) // TODO mak configurable
      .post(payload)

    // TODO error handling
    Await.result(futureResponse, Duration(5L, SECONDS))
  }
  
  
  
  def refreshAccessToken(refreshToken: String): WSResponse = {
    
    val credentials = OAuth2Credentials.get(Play.current)
    val payload = s"grant_type=refresh_token&refresh_token=$refreshToken&realm=employees"

    val futureResponse = WS.url(accessTokenUrl)
      .withHeaders(("Content-Type","application/x-www-form-urlencoded"))
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .withRequestTimeout(REQUEST_TIMEOUT) // TODO mak configurable
      .post(payload)

    Await.result(futureResponse, Duration(5L, SECONDS))
  }

  
  def requestTokenInfo(accessToken: String ): WSResponse = {
    val futureTokenInfoResponse = WS.url(tokenInfoUrl)(Play.current)
        .withQueryString(("access_token", accessToken))
        .get()

    Await.result(futureTokenInfoResponse, Duration(5L, SECONDS))
  }


  def redirectToAuthorizationServer(originalRequestUrl: String) =  Future {
    val oauthCredentials = OAuth2Credentials.get(Play.current)
    val state = UUID.randomUUID().toString  // random confirmation string
    val clientId = oauthCredentials.clientId
    val redirectUrl = authorizationUrl.format(clientId, callbackUrl, state)
    
    Logger.debug(s"redirecting user to [redirectUrl=$redirectUrl]")
    Results.Redirect(redirectUrl).withSession(("oauth2_state",state), ("oauth2_original_request_url", originalRequestUrl))
  }
}
