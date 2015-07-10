package controllers

import org.scalatest.TestData
import security.OAuth2Constants
import org.scalatestplus.play.PlaySpec
import play.api.test
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import scala.reflect.io.File
import scala.language.implicitConversions


class OAuth2ControllerSpec extends PlaySpec with OneServerPerSuite{

  val OAUTH2_CALLBACK_CODE = "anyCode"
  val OAUTH2_CALLBACK_STATE = "anyState"
  val OAUTH2_CALLBACK_ERROR = "anyError"
  val OAUTH2_CALLBACK_ERROR_DESCRIPTION = "very bad problem"
  val REDIRECT_URL = "https://localhost:9000"
  val ACCESS_TOKEN = "0989cd01-333d-4220-a699-539b452d019c"
  val REFRESH_TOKEN = "e8e099cf-2bc7-43c4-9e80-0c8ba66e4141"

  val credentialsFile = File.makeTemp().toAbsolute
  credentialsFile.writeAll("{\"client_id\":\"my_client_id\",\"client_secret\":\"my_client_secret\"}")

  implicit override lazy val app: test.FakeApplication = test.FakeApplication(
      additionalConfiguration = Map (
        "oauth2.enabled"              -> false,
        "oauth2.callback.url"         -> s"http://localhost:$port/oauth_callback",
        "oauth2.access.token.url"     -> s"http://localhost:$port/access_token",
        "oauth2.authorization.url"    -> s"http://localhost:$port/z/oauth2/authorize",
        "oauth2.token.info.url"       -> s"http://localhost:$port/oauth2/tokeninfo",
        "oauth2.credentials.filePath" -> credentialsFile.toURI.getPath
      ),
      withRoutes = {
            case ("POST", "/access_token") => Action { Results.Ok("{\"access_token\":\"" + ACCESS_TOKEN+ "\",\"refresh_token\":\"" + REFRESH_TOKEN+ "\",\"scope\":\"uid cn\",\"token_type\":\"Bearer\",\"expires_in\":3599}") }
      })


  
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


/**
 * Lazy solution to test one specification with different route mock implementation
 */
class OAuth2ControllerSpec2 extends PlaySpec with OneServerPerSuite{

  val OAUTH2_CALLBACK_CODE = "anyCode"
  val OAUTH2_CALLBACK_STATE = "anyState"

  val OAUTH2_CALLBACK_ERROR = "anyError"
  val OAUTH2_CALLBACK_ERROR_DESCRIPTION = "very bad problem"

  val REDIRECT_URL = "https://localhost:9000"
  val ACCESS_TOKEN = "0989cd01-333d-4220-a699-539b452d019c"
  val REFRESH_TOKEN = "e8e099cf-2bc7-43c4-9e80-0c8ba66e4141"

  val credentialsFile = File.makeTemp().toAbsolute
  credentialsFile.writeAll("{\"client_id\":\"my_client_id\",\"client_secret\":\"my_client_secret\"}")


  implicit override lazy val app: test.FakeApplication = test.FakeApplication(
    additionalConfiguration = Map (
      "oauth2.enabled"              -> false,
      "oauth2.callback.url"         -> s"http://localhost:$port/oauth_callback",
      "oauth2.access.token.url"     -> s"http://localhost:$port/access_token",
      "oauth2.authorization.url"    -> s"http://localhost:$port/z/oauth2/authorize",
      "oauth2.token.info.url"       -> s"http://localhost:$port/oauth2/tokeninfo",
      "oauth2.credentials.filePath" -> credentialsFile.toURI.getPath
    ),
    withRoutes = {
      case ("POST", "/access_token")  => Action { Results.Ok("{\"access_token\":\"" + ACCESS_TOKEN+ "\",\"scope\":\"uid cn\",\"token_type\":\"Bearer\",\"expires_in\":3599}") }
    })

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