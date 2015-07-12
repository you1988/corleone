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

package controllers

import security.OAuth2Constants
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import utils.OAuth2TestCredentials
import scala.language.implicitConversions


class OAuth2TestControllerSpec extends PlaySpec with OAuth2TestCredentials with OneServerPerSuite {
  
  override def testPort = port
  implicit override lazy val app = fakeApp
  
  
  "OAUth2Controller" should {
    
    "reply with 400 if no redirect URL is supplied via session" in  {
        val Some(result) = route(
            FakeRequest(
                GET,
                s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE&code=$OAUTH2_CALLBACK_CODE",
                FakeHeaders(),
                AnyContentAsEmpty
            )
            .withSession((OAuth2Constants.SESSION_KEY_STATE, OAUTH2_CALLBACK_STATE))
          )
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("no redirect URL")
    }
    
    
    "reply with 400 if no state was supplied via session" in {
      val Some(result) = route(
          FakeRequest(
            GET,
            s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE&code=$OAUTH2_CALLBACK_CODE",
            FakeHeaders(),
            AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL ))
      )
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must be ("Could not find OAUTH2 state from session")
    }
    
    
    "detect possible CSRF attack" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE&code=$OAUTH2_CALLBACK_CODE",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL), 
                       (OAuth2Constants.SESSION_KEY_STATE, "is different to supplied state"))
      )

      status(result) mustBe CONFLICT
    }

    
    "report 400, if callback was performed without having attached `state`" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?code=$OAUTH2_CALLBACK_CODE",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL),
                       (OAuth2Constants.SESSION_KEY_STATE, OAUTH2_CALLBACK_STATE))
      )

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Did not receive any OAUTH2 state from callback")
    }
    
    
    "redirect user to authorization service, if callback contains error and if it is the user's first try" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?error=$OAUTH2_CALLBACK_ERROR&error_description=$OAUTH2_CALLBACK_ERROR_DESCRIPTION",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL))
      )

      status(result) mustBe SEE_OTHER
      session(result).get(OAuth2Constants.SESSION_KEY_RETRY_AFTER_AUTH_ERROR) mustNot be (None) 
    }

    
    "report error, if callback contains error and if it is NOT the user's first try" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?error=$OAUTH2_CALLBACK_ERROR&error_description=$OAUTH2_CALLBACK_ERROR_DESCRIPTION",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession( (OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL),
                        (OAuth2Constants.SESSION_KEY_RETRY_AFTER_AUTH_ERROR, "true"))
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include (s"[error=$OAUTH2_CALLBACK_ERROR, errorDescription=$OAUTH2_CALLBACK_ERROR_DESCRIPTION]")
    }

    
    "report 400, if callback was performed without having attached `code`" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL),
                       (OAuth2Constants.SESSION_KEY_STATE, OAUTH2_CALLBACK_STATE))
      )

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Did not receive OAUTH2 code from callback")
    }


    "request access token and refresh token and redirect the user to its original request" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE&code=$OAUTH2_CALLBACK_CODE",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL),
                       (OAuth2Constants.SESSION_KEY_STATE, OAUTH2_CALLBACK_STATE))
      )

      status(result) mustBe SEE_OTHER
      headers(result).get("Location") mustBe Some(REDIRECT_URL)
      session(result).get(OAuth2Constants.SESSION_KEY_ACCESS_TOKEN) mustBe Some(ACCESS_TOKEN)
      session(result).get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN) mustBe Some(REFRESH_TOKEN)
    }
    
  }
}


class OAuth2ControllerSpec2Test extends PlaySpec with OAuth2TestCredentials with OneServerPerSuite{

  override def testPort = port
  implicit override lazy val app = fakeApp

  override def routes =  {
    case ("POST", "/access_token")  => Action { Results.Ok("{\"access_token\":\"" + ACCESS_TOKEN+ "\",\"scope\":\"uid cn\",\"token_type\":\"Bearer\",\"expires_in\":3599}") }
  }
  
  
  "OAUth2Controller" should {
    
    "store only the access token in session if no refresh token could be received" in {
        val Some(result) = route(
          FakeRequest(
            GET,
            s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE&code=$OAUTH2_CALLBACK_CODE",
            FakeHeaders(),
            AnyContentAsEmpty
          )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL),
                       (OAuth2Constants.SESSION_KEY_STATE, OAUTH2_CALLBACK_STATE))
        )
  
        status(result) mustBe SEE_OTHER
        headers(result).get("Location") mustBe Some(REDIRECT_URL)
        session(result).get(OAuth2Constants.SESSION_KEY_ACCESS_TOKEN) mustBe Some(ACCESS_TOKEN)
        session(result).get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN) mustBe None
      }
    }
  }


class OAuth2ControllerSpec3Test extends PlaySpec with OAuth2TestCredentials with OneServerPerSuite {

  override def testPort = port
  implicit override lazy val app = fakeApp

  override def routes = {
    case ("POST", "/access_token") => Action {
      Results.InternalServerError("does not work")
    }
  }


  "OAUth2Controller" should {
    "respond with InternalServerError, if communication to access token endpoint fails " in {
      val Some(result) = route(
        FakeRequest(
          GET,
          s"/oauth_callback?state=$OAUTH2_CALLBACK_STATE&code=$OAUTH2_CALLBACK_CODE",
          FakeHeaders(),
          AnyContentAsEmpty
        )
          .withSession((OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL, REDIRECT_URL),
            (OAuth2Constants.SESSION_KEY_STATE, OAUTH2_CALLBACK_STATE))
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("access token request was not successful")
    }
  }
}