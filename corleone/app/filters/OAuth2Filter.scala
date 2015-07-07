package filters

import java.util.UUID

import play.api.{Logger,  Play}
import security.OAuth2Credentials
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS}
import play.api._
import play.api.mvc._
import scala.concurrent.duration._
import play.api.http._

class OAuth2Filter extends Filter
{
  val NO_TOKEN :String = "NO_TOKEN" 
  
  val EXCLUDED_REQUEST_PATHS: Set[String] = Set("/oauth_callback", " /heartbeat")
  
  override def apply(nextFilter: RequestHeader => Future[Result]) 
                    (requestHeader: RequestHeader): Future[Result] = {
    
    Logger.info("OAuth2 Filter is executed for path " + requestHeader.path)

    
    // go on with following filter actions, if path is excluded from OAUTH2 security check
    if(EXCLUDED_REQUEST_PATHS.contains(requestHeader.path)) return nextFilter.apply(requestHeader)
    
    
    val accessTokenOption = requestHeader.session.get("oauth2_access_token")
    
    val accessToken = accessTokenOption match {
      case Some(token) => token
      case None => NO_TOKEN
    }
    
    if (accessToken == NO_TOKEN) {
      Logger.info("No token was supplied -> redirect user to authorization server")
      redirectToAuthorizationServer(requestHeader.path)
    }
    else {
      val response = requestTokenInfo(accessToken)
      Logger.info(response.body)
      
      if (isAccessTokenValid(response)){
        Logger.info("supplied access token IS valid")

        // apply filter following in filter chain
        nextFilter.apply(requestHeader)
      }
      else{
        Logger.info("supplied access token is NOT valid -> redirect user to authorization server" )
        redirectToAuthorizationServer(requestHeader.path)
      }
    }
  }

  
  def requestTokenInfo(accessToken: String ): WSResponse = {
    val tokenInfoUrl = Play.current.configuration.getString("oauth2.token.info.url").get // TODO error handling
    val futureTokenInfoResponse = WS.url(tokenInfoUrl)(Play.current)
        .withQueryString(("access_token", accessToken))
        .get()

    Await.result(futureTokenInfoResponse, Duration(5L, SECONDS))
  }
  
  
  def isAccessTokenValid(response: WSResponse): Boolean = {
    // token is still valid. now. let's check if user has sufficient scopes 
    if (response.status == Status.OK){
      // at the moment, we can only check if the user is an employee. In the future, it will be possible to assign
      // more scopes to an user
      (response.json \ "realm").asOpt[String].get == "employees" // TODO error handling
    }
    else false
  }
  
  
  def redirectToAuthorizationServer(originalRequestUrl: String) =  Future {

    val oauthCredentials = new OAuth2Credentials(Play.current)

    val callbackUrl =  Play.current.configuration.getString("oauth2.callback.url").get
    val state = UUID.randomUUID().toString  // random confirmation string
    val redirectUrl = getAuthorizationUrl(oauthCredentials, callbackUrl, state)


    Logger.info(s"start page was opened [redirectUrl=$redirectUrl]")
    Results.Redirect(redirectUrl).withSession(("oauth2_state",state), ("oauth2_original_request_url", originalRequestUrl))  
  }

  def getAuthorizationUrl(oauthCredentials:OAuth2Credentials, redirectUri: String, state: String): String = {
    
    // TODO error handling
    val accessTokenUrl = Play.current.configuration.getString("oauth2.access.token.url").get
    val authorizationUrl = Play.current.configuration.getString("oauth2.authorization.url").get
    val clientId = oauthCredentials.clientId
    authorizationUrl format(clientId, redirectUri, state)
  }


}



