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
  
  private val configuration = Play.current.configuration
  
  val accessTokenUrl = configuration.getString("oauth2.access.token.url").get
  val tokenInfoUrl = configuration.getString("oauth2.token.info.url").get
  val callbackUrl = configuration.getString("oauth2.callback.url").get
  val authorizationUrl = configuration.getString("oauth2.authorization.url").get
  val requestTimeout =configuration.getLong("oauth2.request.timeout").get
  val expiryTimeLimitForTokenRefreshInSeconds = configuration.getInt("oauth2.token.refresh.expiry.limit").get
  val isOAuth2Enabled = configuration.getBoolean("oauth2.enabled").get
  
  
  val SESSION_KEY_ACCESS_TOKEN = "oauth2_access_token"
  val SESSION_KEY_REFRESH_TOKEN = "oauth2_refresh_token"
  val SESSION_KEY_STATE = "oauth2_state"
  val SESSION_KEY_ORIGINAL_REQUEST_URL = "oauth2_original_request_url"
  
  
  def requestAccessToken(oauth2Code: String): WSResponse = {
    val credentials = OAuth2Credentials.get(Play.current)
    val payload = s"grant_type=authorization_code&code=$oauth2Code&realm=employees&redirect_uri=$callbackUrl"
    
    val futureResponse = WS.url(accessTokenUrl)
      .withHeaders(("Content-Type","application/x-www-form-urlencoded"))
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .withRequestTimeout(requestTimeout)
      .post(payload)
    
    Await.result(futureResponse, Duration(5L, SECONDS))
  }
  
  
  def refreshAccessToken(refreshToken: String): WSResponse = {
    
    val credentials = OAuth2Credentials.get(Play.current)
    val payload = s"grant_type=refresh_token&refresh_token=$refreshToken&realm=employees"

    val futureResponse = WS.url(accessTokenUrl)
      .withHeaders(("Content-Type","application/x-www-form-urlencoded"))
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .withRequestTimeout(requestTimeout)
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
    Results.Redirect(redirectUrl).withSession(("oauth2_state",state), 
                                              ("oauth2_original_request_url", originalRequestUrl))
  }
}
