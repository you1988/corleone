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

package filters

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{Action, Results, Handler, AnyContentAsEmpty}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import utils.OAuth2TestCredentials

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class OAuth2ServiceCallFilterSpec extends PlaySpec with OAuth2TestCredentials with OneServerPerSuite {

  override def testPort = port
  implicit override lazy val app = fakeApp
  override def enableOAuth2: Boolean = true
    
  val SERVICE_RESPONSE  = "works"
  val SERVICE_PATH = "/api/translations"
  val CUSTOM_SERVICE_PATH = "/test-service"
  
  var wasTokenInfoRequested = false
  var isAccessGranted = true
  var isTokenInfoRequestSuccessful = true
  
  
  val STANDARD_TOKEN_INFO_RESPONSE = "{\"access_token\":\"" +ACCESS_TOKEN +"\",\"uid\":\"bfriedrich\",\"grant_type\":\"password\",\"scope\":[\"uid\",\"cn\"],\"realm\":\"services\",\"cn\":\"\",\"token_type\":\"Bearer\",\"expires_in\":100}"
  val ACCESS_NOT_GRANTED_TOKEN_INFO_RESPONE = "{\"access_token\":\"" +ACCESS_TOKEN +"\",\"uid\":\"bfriedrich\",\"grant_type\":\"password\",\"scope\":[\"uid\",\"cn\"],\"realm\":\"nobody\",\"cn\":\"\",\"token_type\":\"Bearer\",\"expires_in\":100}"

  override  def configuration: Map[String, _] =   Map (
    "oauth2.enabled"              -> enableOAuth2,
    "oauth2.callback.url"         -> callbackUrl,
    "oauth2.access.token.url"     -> accessTokenEndpoint,
    "oauth2.authorization.url"    -> authorizationEndpoint,
    "oauth2.token.info.url"       ->tokenInfoEndpoint,
    "oauth2.credentials.filePath" -> credentialsFile.toURI.getPath,
    "oauth2.excluded.paths"       -> List("/oauth2/tokeninfo"),
    "oauth2.service.paths"        -> List(CUSTOM_SERVICE_PATH)
  )
  
  
  override def routes: PartialFunction[Tuple2[String, String], Handler ] = {
    case ("GET", SERVICE_PATH) => Action {
      Results.Ok(SERVICE_RESPONSE)
    }
    case ("GET", "/oauth2/tokeninfo") => Action {
      wasTokenInfoRequested = true
      
      if(isAccessGranted)
        if(isTokenInfoRequestSuccessful)
          Results.Ok(STANDARD_TOKEN_INFO_RESPONSE)
        else
          Results.InternalServerError("why not?")
      else
        Results.Ok(ACCESS_NOT_GRANTED_TOKEN_INFO_RESPONE)
    }
    case("GET", CUSTOM_SERVICE_PATH) => Action (Results.Ok(SERVICE_RESPONSE))

  }
  
  
  "OAuth2ServiceCallFilter" should {
    "respond with BAD_REQUEST, if no Authorization header is supplied" in {
      wasTokenInfoRequested = false
      isAccessGranted = true
      isTokenInfoRequestSuccessful = true
      
      val Some(result) = route(
        FakeRequest(
          GET,
          SERVICE_PATH,
          FakeHeaders(),
          AnyContentAsEmpty
        )
      )
      
      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "no authorization header provided in request"
      wasTokenInfoRequested mustBe false
    }
    
    
    "respond with BAD_REQUEST, if Authorization header does not contain any access token" in {
      wasTokenInfoRequested = false
      isAccessGranted = true
      isTokenInfoRequestSuccessful = true
      val headers = List(("Authorization","something"))
      
      val Some(result) = route(
        FakeRequest(
          GET,
          SERVICE_PATH,
          FakeHeaders(headers),
          AnyContentAsEmpty
        )
      )

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Authorization header does not contain any access token"
      wasTokenInfoRequested mustBe false
    }


    "allow request, if everything is fine" in {
      wasTokenInfoRequested = false
      isAccessGranted = true
      isTokenInfoRequestSuccessful = true
      val headers = List(("Authorization","Bearer " + ACCESS_TOKEN))

      val Some(result) = route(
        FakeRequest(
          GET,
          SERVICE_PATH,
          FakeHeaders(headers),
          AnyContentAsEmpty
        )
      )
      
      status(result) mustBe OK
      contentAsString(result) mustBe SERVICE_RESPONSE
      wasTokenInfoRequested mustBe true
    }
    
    
    "respond with FORBIDDEN , if access is not granted" in {
      wasTokenInfoRequested = false
      isAccessGranted = false
      isTokenInfoRequestSuccessful = true
      val headers = List(("Authorization","Bearer " + ACCESS_TOKEN))
      val Some(result) = route(
        FakeRequest(
          GET,
          SERVICE_PATH,
          FakeHeaders(headers),
          AnyContentAsEmpty
        )
      )
      
      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "access is not granted"
      wasTokenInfoRequested mustBe true
    }

    
    "respond with FORBIDDEN , if token info endpoint does not respond" in {
      wasTokenInfoRequested = false
      isAccessGranted = true
      isTokenInfoRequestSuccessful = false
      val headers = List(("Authorization","Bearer " + ACCESS_TOKEN))
      val Some(result) = route(
        FakeRequest(
          GET,
          SERVICE_PATH,
          FakeHeaders(headers),
          AnyContentAsEmpty
        )
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "access is not granted"
      wasTokenInfoRequested mustBe true
    }
    
    
    "serve custom service paths" in {
      wasTokenInfoRequested = false
      isAccessGranted = true
      isTokenInfoRequestSuccessful = true
      val headers = List(("Authorization","Bearer " + ACCESS_TOKEN))

      val Some(result) = route(
        FakeRequest(
          GET,
          CUSTOM_SERVICE_PATH,
          FakeHeaders(headers),
          AnyContentAsEmpty
        )
      )

      status(result) mustBe OK
      contentAsString(result) mustBe SERVICE_RESPONSE
      wasTokenInfoRequested mustBe true
    }
  }
}
