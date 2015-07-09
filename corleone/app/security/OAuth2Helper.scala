package security

import java.util.UUID

import com.google.inject.Inject
import controllers.Assets
import play.api.mvc.Results
import play.api.{Logger, Play}
import play.api.libs.ws.{WSAuthScheme, WS, WSResponse}
import play.mvc.Http.Status

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
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
      credentialsProvider.invalidateCache
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
      credentialsProvider.invalidateCache
      refreshAccessToken(refreshToken, true)
    }
    else response
  }


  def requestTokenInfo(accessToken: String): WSResponse = {
    val futureTokenInfoResponse = WS.url(OAuth2Constants.tokenInfoUrl)(Play.current)
      .withQueryString(("access_token", accessToken))
      .get()

    Await.result(futureTokenInfoResponse, Duration(5L, SECONDS))
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
}