package filters

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{Action, Results, Handler, AnyContentAsEmpty}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import security.OAuth2Constants
import utils.OAuth2TestCredentials

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


trait OAuth2FilterTestMethods extends PlaySpec with OAuth2TestCredentials{

  val TARGET_PATH = "/target"
  val TARGET_RESPONSE = "WORKS :-)"

  override def enableOAuth2 = true
  
  
  def checkTargetPath(result: Future[play.api.mvc.Result]) = {
    val targetPathInSessionOption = session(result).get(OAuth2Constants.SESSION_KEY_ORIGINAL_REQUEST_URL)
    targetPathInSessionOption mustNot be(None)
    targetPathInSessionOption mustBe Some(TARGET_PATH)
  }


  def checkRedirectLocation(result: Future[play.api.mvc.Result]) = {
    val redirectLocationOption = redirectLocation(result)
    redirectLocationOption mustNot be(None)
    val location = redirectLocationOption.get

    location.startsWith(s"$authorizationEndpoint") mustBe true
    location.contains(s"redirect_uri=$callbackUrl")
    location contains(s"client_id=$OAUTH2_CLIENT_ID") mustBe true
    location contains("realm=employees") mustBe true
    location. contains("response_type=code") mustBe true
    location matches(".*&state=[a-z0-9-]+.*") mustBe true
  }
}



class OAuth2FilterSpec extends PlaySpec with OAuth2FilterTestMethods with OneServerPerSuite {

  override def testPort = port
  implicit override lazy val app = fakeApp
  
  
  override def routes: PartialFunction[Tuple2[String, String], Handler ] = {
    case ("GET", TARGET_PATH) => Action { Results.Ok(TARGET_RESPONSE)}
  }
  
  override  def configuration: Map[String, _] =   Map (
    "oauth2.enabled"              -> enableOAuth2,
    "oauth2.callback.url"         -> callbackUrl,
    "oauth2.access.token.url"     -> accessTokenEndpoint,
    "oauth2.authorization.url"    -> authorizationEndpoint,
    "oauth2.token.info.url"       ->tokenInfoEndpoint,
    "oauth2.credentials.filePath" -> credentialsFile.toURI.getPath,
    "oauth2.excluded.paths"       -> List("/webjars", 
                                          "/assets", 
                                          "/z/oauth2/authorize") // no redirect for mocked auth endpoint 
  )
  
  
  "OAuth2Filter" should {
    
    "redirect a request if no access token is available" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          TARGET_PATH,
          FakeHeaders(),
          AnyContentAsEmpty
        )
      )
      
      status(result) mustBe SEE_OTHER
      checkTargetPath(result)
      session(result).get(OAuth2Constants.SESSION_KEY_STATE) mustNot be(None)
      checkRedirectLocation(result)
    }

    "should redirect a request if access token is invalid or user does not have sufficient permission" in {
      val Some(result) = route(
        FakeRequest(
          GET,
          TARGET_PATH,
          FakeHeaders(),
          AnyContentAsEmpty
        )
        .withSession( (OAuth2Constants.SESSION_KEY_ACCESS_TOKEN,  "DOES_NOT_EXIST"),
                      (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, "DOES_NOT_EXIST")))

        status(result) mustBe SEE_OTHER
        checkTargetPath(result)
        session(result).get(OAuth2Constants.SESSION_KEY_STATE) mustNot be(None)
        checkRedirectLocation(result)
    }
  }
  
}


class OAuth2FilterSpec2 extends PlaySpec with OAuth2FilterTestMethods with OneServerPerSuite {

  override def testPort = port
  implicit override lazy val app = fakeApp

  
  override  def configuration: Map[String, _] =   Map (
    "oauth2.enabled"              -> enableOAuth2,
    "oauth2.callback.url"         -> callbackUrl,
    "oauth2.access.token.url"     -> accessTokenEndpoint,
    "oauth2.authorization.url"    -> authorizationEndpoint,
    "oauth2.token.info.url"       ->tokenInfoEndpoint,
    "oauth2.credentials.filePath" -> credentialsFile.toURI.getPath,
    "oauth2.excluded.paths"       -> List("/webjars", 
    "/assets",
    "/z/oauth2/authorize",
    "/oauth2/tokeninfo",
    "/access_token", 
     "/oauth2/tokeninfo")
  )
  

  var wasTokenInfoRequested = false
  var wasTokenRefreshed = false
  
  val NEW_ACCESS_TOKEN = "NEW_ACCESS_TOKEN"
  val NEW_REFRESH_TOKEN = "NEW_REFRESH_TOKEN"
  val STANDARD_REFRESH_RESPONSE = "{\"access_token\":\"" + NEW_ACCESS_TOKEN + "\",\"grant_type\":\"refresh_token\",\"expires_in\":399}"
  val STANDARD_TOKEN_INFO_RESPONSE = "{\"access_token\":\"d1f2bf18-16f9-4da5-8b0b-75d1a702122d\",\"uid\":\"bfriedrich\",\"grant_type\":\"password\",\"scope\":[\"uid\",\"cn\"],\"realm\":\"employees\",\"cn\":\"\",\"token_type\":\"Bearer\",\"expires_in\":100}"
  
  var accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
  var tokenInfoResponse = STANDARD_TOKEN_INFO_RESPONSE
  var isTokenRefreshRequestSuccessful = true
  
  
  override def routes: PartialFunction[Tuple2[String, String], Handler ] = {
    case ("GET", TARGET_PATH) => Action { 
      Results.Ok(TARGET_RESPONSE) 
    }
    case ("GET", "/oauth2/tokeninfo") => Action { 
      wasTokenInfoRequested = true
      Results.Ok(tokenInfoResponse) 
    }
    case ("POST", "/access_token") => Action {
        wasTokenRefreshed = true
        if(isTokenRefreshRequestSuccessful) Results.Ok(accessTokenRefreshResponse)
        else Results.BadRequest("no refresh this time")
    }
  }

  
  "OAuth2Filter" should {

    "redirect a request if access token is almost expired and no refresh token is available" in {
      wasTokenInfoRequested = false
      wasTokenRefreshed = false
      accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
      tokenInfoResponse = STANDARD_TOKEN_INFO_RESPONSE
      isTokenRefreshRequestSuccessful = true
      
      val Some(result) = route(
        FakeRequest(
          GET,
          TARGET_PATH,
          FakeHeaders(),
          AnyContentAsEmpty
        ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, ACCESS_TOKEN)))

      
      wasTokenInfoRequested mustBe true
      status(result) mustBe SEE_OTHER
      checkTargetPath(result)
      session(result).get(OAuth2Constants.SESSION_KEY_STATE) mustNot be(None)
    }
  }
  
  
  "refresh access token, if access token is almost expired" in {
    wasTokenInfoRequested = false
    wasTokenRefreshed = false
    accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
    tokenInfoResponse = STANDARD_TOKEN_INFO_RESPONSE
    isTokenRefreshRequestSuccessful = true
    
    val Some(result) = route(
      FakeRequest(
        GET,
        TARGET_PATH,
        FakeHeaders(),
        AnyContentAsEmpty
      ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, ACCESS_TOKEN),
                    (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))
    
    wasTokenInfoRequested mustBe true
    wasTokenRefreshed mustBe true
    status(result) mustBe OK
    session(result).get(OAuth2Constants.SESSION_KEY_ACCESS_TOKEN) mustBe(Some(NEW_ACCESS_TOKEN))
    session(result).get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN) mustBe(Some(REFRESH_TOKEN))
  }

  
  "use new refresh token, if refresh request returns a new one" in {
     wasTokenInfoRequested = false
     wasTokenRefreshed = false
     tokenInfoResponse = STANDARD_TOKEN_INFO_RESPONSE
     accessTokenRefreshResponse = "{\"refresh_token\":\""+ NEW_REFRESH_TOKEN + "\",\"access_token\":\"" + NEW_ACCESS_TOKEN + "\",\"grant_type\":\"refresh_token\",\"expires_in\":399}"
     isTokenRefreshRequestSuccessful = true
    
     val Some(result) = route(
        FakeRequest(
          GET,
          TARGET_PATH,
          FakeHeaders(),
          AnyContentAsEmpty
        ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, ACCESS_TOKEN),
            (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))

     wasTokenInfoRequested mustBe true
     wasTokenRefreshed mustBe true
     status(result) mustBe OK
     session(result).get(OAuth2Constants.SESSION_KEY_ACCESS_TOKEN) mustBe(Some(NEW_ACCESS_TOKEN))
     session(result).get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN) mustBe(Some(NEW_REFRESH_TOKEN))

  }
  
  "not do anything, if everything is fine" in {
    wasTokenInfoRequested = false
    wasTokenRefreshed = false
    accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
    tokenInfoResponse = "{\"access_token\":\"d1f2bf18-16f9-4da5-8b0b-75d1a702122d\",\"uid\":\"bfriedrich\",\"grant_type\":\"password\",\"scope\":[\"uid\",\"cn\"],\"realm\":\"employees\",\"cn\":\"\",\"token_type\":\"Bearer\",\"expires_in\":399}"
    isTokenRefreshRequestSuccessful = true
    
    val Some(result) = route(
      FakeRequest(
        GET,
        TARGET_PATH,
        FakeHeaders(),
        AnyContentAsEmpty
      ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN,  ACCESS_TOKEN),
                    (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))
    
    wasTokenInfoRequested mustBe true
    wasTokenRefreshed mustBe false
    status(result) mustBe OK
    session(result).get(OAuth2Constants.SESSION_KEY_ACCESS_TOKEN) mustBe(Some(ACCESS_TOKEN))
    session(result).get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN) mustBe(Some(REFRESH_TOKEN))
  }
  
  
  "redirect a request, if refresh request returns with no access token" in {
    wasTokenInfoRequested = false
    wasTokenRefreshed = false
    accessTokenRefreshResponse = "{\"grant_type\":\"refresh_token\",\"expires_in\":399}"
    tokenInfoResponse = STANDARD_TOKEN_INFO_RESPONSE
    isTokenRefreshRequestSuccessful = true
    
    val Some(result) = route(
      FakeRequest(
        GET,
        TARGET_PATH,
        FakeHeaders(),
        AnyContentAsEmpty
      ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN, ACCESS_TOKEN),
                    (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))

    wasTokenInfoRequested mustBe true
    wasTokenRefreshed mustBe true
    status(result) mustBe SEE_OTHER
    checkTargetPath(result)
    session(result).get(OAuth2Constants.SESSION_KEY_STATE) mustNot be(None)
    checkRedirectLocation(result)
  }


  "redirect request, if refresh token request is not successful" in {
    wasTokenInfoRequested = false
    wasTokenRefreshed = false
    accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
    tokenInfoResponse = STANDARD_TOKEN_INFO_RESPONSE
    isTokenRefreshRequestSuccessful = false

    val Some(result) = route(
      FakeRequest(
        GET,
        TARGET_PATH,
        FakeHeaders(),
        AnyContentAsEmpty
      ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN,  ACCESS_TOKEN),
          (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))

    wasTokenInfoRequested mustBe true
    wasTokenRefreshed mustBe true
    status(result) mustBe SEE_OTHER
    checkTargetPath(result)
    session(result).get(OAuth2Constants.SESSION_KEY_STATE) mustNot be(None)
    checkRedirectLocation(result)
  }


  "try to refresh access tokeb, if no expiry time can be retrieved from token info request" in {
    wasTokenInfoRequested = false
    wasTokenRefreshed = false
    accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
    tokenInfoResponse = "{\"access_token\":\"d1f2bf18-16f9-4da5-8b0b-75d1a702122d\",\"uid\":\"bfriedrich\",\"grant_type\":\"password\",\"scope\":[\"uid\",\"cn\"],\"realm\":\"employees\",\"cn\":\"\",\"token_type\":\"Bearer\"}"
    isTokenRefreshRequestSuccessful = true

    val Some(result) = route(
      FakeRequest(
        GET,
        TARGET_PATH,
        FakeHeaders(),
        AnyContentAsEmpty
      ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN,  ACCESS_TOKEN),
                     (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))

    wasTokenInfoRequested mustBe true
    wasTokenRefreshed mustBe true
    status(result) mustBe OK
    session(result).get(OAuth2Constants.SESSION_KEY_ACCESS_TOKEN) mustBe(Some(NEW_ACCESS_TOKEN))
    session(result).get(OAuth2Constants.SESSION_KEY_REFRESH_TOKEN) mustBe(Some(REFRESH_TOKEN))
  }

  "redirect request, if access is not granted with current access token" in {
    wasTokenInfoRequested = false
    wasTokenRefreshed = false
    accessTokenRefreshResponse = STANDARD_REFRESH_RESPONSE
    tokenInfoResponse = "{\"access_token\":\"d1f2bf18-16f9-4da5-8b0b-75d1a702122d\",\"uid\":\"bfriedrich\",\"grant_type\":\"password\",\"scope\":[\"uid\",\"cn\"],\"realm\":\"employeesXXX\",\"cn\":\"\",\"token_type\":\"Bearer\",\"expires_in\":399}"
    isTokenRefreshRequestSuccessful = true

    val Some(result) = route(
      FakeRequest(
        GET,
        TARGET_PATH,
        FakeHeaders(),
        AnyContentAsEmpty
      ).withSession((OAuth2Constants.SESSION_KEY_ACCESS_TOKEN,  ACCESS_TOKEN),
          (OAuth2Constants.SESSION_KEY_REFRESH_TOKEN, REFRESH_TOKEN)))

    wasTokenInfoRequested mustBe true
    wasTokenRefreshed mustBe false
    status(result) mustBe SEE_OTHER
    checkTargetPath(result)
    session(result).get(OAuth2Constants.SESSION_KEY_STATE) mustNot be(None)
    checkRedirectLocation(result)
  }
}
