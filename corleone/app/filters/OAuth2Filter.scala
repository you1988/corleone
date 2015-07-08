package filters

import java.util.UUID

import play.api.{Logger,  Play}
import security.{OAuth2Helper, OAuth2Credentials}
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
  val EXPIRY_TIME_LIMIT_FOR_REFRESH_IN_S = 300 // trigger access token refresh, if access token expires in 5 minutes
  
  
  override def apply(nextFilter: RequestHeader => Future[Result]) 
                    (requestHeader: RequestHeader): Future[Result] = {
    
    Logger.debug("Entered OAuth2Filter for path " + requestHeader.path)

    
    // go on with following filter actions, if path is excluded from OAUTH2 security check
    if(EXCLUDED_REQUEST_PATHS.contains(requestHeader.path)) return nextFilter.apply(requestHeader)

    Logger.debug("OAuth2Filter is executed for path " + requestHeader.path)
    
    val accessTokenOption = requestHeader.session.get("oauth2_access_token")
    val accessToken = accessTokenOption.getOrElse(NO_TOKEN)
    
    if (accessToken == NO_TOKEN) {
      Logger.info("No token was supplied -> redirecting user to authorization server")
      OAuth2Helper.redirectToAuthorizationServer(requestHeader.path)
    }
    else {
      val tokenInfoResponse = OAuth2Helper.requestTokenInfo(accessToken)
      
      if (isAccessTokenValid(tokenInfoResponse)){
        Logger.info("supplied access token IS valid")

        if(isAccessTokenAlmostExpired(tokenInfoResponse)) {

          Logger.debug("access token IS almost expired -> trying to refresh access token")
          
          val refreshTokenOption = requestHeader.session.get("oauth2_refresh_token")

          if ( refreshTokenOption.isEmpty ) {
            Logger.warn("could not find refresh token in current session -> redirecting user to authorization server")
            return OAuth2Helper.redirectToAuthorizationServer(requestHeader.path)
          }
          
          val refreshToken = refreshTokenOption.get
          val accessTokenRefreshResponse = OAuth2Helper.refreshAccessToken(refreshToken)
          
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
                else result.withSession(("oauth2_access_token", newAccessToken), 
                                        ("oauth2_refresh_token", newRefreshToken.get))
              }
          }
          else {
            val error = accessTokenRefreshResponse.body
            Logger.info(s"access token refresh request was NOT successful -> redirecting user to authorization server [error=$error]")
            return OAuth2Helper.redirectToAuthorizationServer(requestHeader.path)
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
        OAuth2Helper.redirectToAuthorizationServer(requestHeader.path)
      }
    }
  }

  
  def isAccessTokenValid(response: WSResponse): Boolean = {
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
    // TODO error handling
    val tokenExpiryTimeInSeconds= (tokenInfoResponse.json \ "expires_in").asOpt[Int].get
    Logger.debug(s"token expires in $tokenExpiryTimeInSeconds seconds")
    return tokenExpiryTimeInSeconds <= EXPIRY_TIME_LIMIT_FOR_REFRESH_IN_S
  }
}

