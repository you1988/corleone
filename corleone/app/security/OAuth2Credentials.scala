package security

import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api._
import play.api.mvc._
import play.api.Application

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
 * DO NOT CACHE!!!
 *
 */
class OAuth2Credentials(application: Application) {
  
  // TODO error handling
  private val credentialsFilePath = application.configuration.getString("oauth2.credentials.filePath").get
  private val credentials = scala.io.Source.fromFile(credentialsFilePath).mkString
  private val json = Json.parse(credentials)

  val (clientId, clientSecret)  = ( (json \ "client_id")    .asOpt[String].get, 
                                    (json \ "client_secret").asOpt[String].get  )
}

