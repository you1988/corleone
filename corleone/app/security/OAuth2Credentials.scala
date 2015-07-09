package security

import com.google.inject.Inject
import play.api.cache._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api._
import play.api.mvc._
import play.api.Application

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._

/**
 * DO NOT CACHE!!!
 *
 */


class OAuth2CredentialsProvider @Inject() (application: Application, cache: CacheApi) {
  
   private val CACHE_KEY = "oauth2.cache.credentials"

   def get: OAuth2Credentials = {
     
     val cachedCredentials = cache.get[OAuth2Credentials](CACHE_KEY)
     if(cachedCredentials.isEmpty) {
       val credentials = scala.io.Source.fromFile(OAuth2Constants.credentialsFilePath).mkString
       val json = Json.parse(credentials)

       val (clientId, clientSecret)  = ( (json \ "client_id")    .asOpt[String].get,
                                         (json \ "client_secret").asOpt[String].get  )
       
       val oauth2Credentials = new OAuth2Credentials(clientId, clientSecret)
       cache.set(CACHE_KEY, oauth2Credentials, Duration(OAuth2Constants.credentialsCacheExpiryTime, SECONDS))
       oauth2Credentials
     }
     else{
       cachedCredentials.get
     }
   }
  
  def invalidateCache() = {
    cache.remove(CACHE_KEY)
  }
}


class OAuth2Credentials(val clientId: String, val clientSecret: String)
