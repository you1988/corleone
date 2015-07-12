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

package security

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.cache.CacheApi
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.concurrent.duration._

class OAuth2CredentialsSpec  extends PlaySpec with OAuth2TestCredentials with MockitoSugar with OneAppPerSuite{

  val CACHE_KEY = "oauth2.cache.credentials"
  implicit override lazy val app = fakeApp
  
  "credentials provider" should {
    "retrieve credentials from credentials file" in {
    
      // force file read
      val cacheMock = mock[CacheApi]
      when(cacheMock.get[OAuth2Credentials](CACHE_KEY)).thenReturn(None)

      val provider = new OAuth2CredentialsProvider(app, cacheMock)
      val credentials = provider.get
      credentials.clientId mustBe (OAUTH2_CLIENT_ID)
      credentials.clientSecret mustBe (OAUTH2_CLIENT_SECRET)
      
      verify(cacheMock, times(1)).get[OAuth2Credentials](CACHE_KEY)
      verify(cacheMock, times(1)).set(
          org.mockito.Matchers.eq(CACHE_KEY),
          anyObject[OAuth2Credentials],
          org.mockito.Matchers.eq(Duration(OAuth2Constants.credentialsCacheExpiryTime, SECONDS)))
      verifyNoMoreInteractions(cacheMock)
    
    }

    "retrieve credentials from cache" in {
      val cacheMock = mock[CacheApi]
      val credentialsInCache = new OAuth2Credentials(OAUTH2_CLIENT_ID, OAUTH2_CLIENT_SECRET)
      when(cacheMock.get[OAuth2Credentials](CACHE_KEY)).thenReturn(Some(credentialsInCache))

      val provider = new OAuth2CredentialsProvider(app, cacheMock)

      val credentials = provider.get
      credentials.clientId mustBe (OAUTH2_CLIENT_ID)
      credentials.clientSecret mustBe (OAUTH2_CLIENT_SECRET)

      verify(cacheMock, times(1)).get[OAuth2Credentials](CACHE_KEY)
      verify(cacheMock, times(0)).set(
        org.mockito.Matchers.eq(CACHE_KEY),
        anyObject[OAuth2Credentials],
        org.mockito.Matchers.eq(Duration(OAuth2Constants.credentialsCacheExpiryTime, SECONDS)))
      verifyNoMoreInteractions(cacheMock)
    }
    
    "provide newest credentials when credentials file changes and cache has expired (or has been invalidated)" in {
      // force permanent file read
      val cacheMock = mock[CacheApi]
      when(cacheMock.get[OAuth2Credentials](CACHE_KEY)).thenReturn(None)
      val provider = new OAuth2CredentialsProvider(app, cacheMock)
      
      val credentials = provider.get
      credentials.clientId mustBe (OAUTH2_CLIENT_ID)
      credentials.clientSecret mustBe (OAUTH2_CLIENT_SECRET)

      val newClientId = OAUTH2_CLIENT_ID + "xxx"
      val newClientSecret = OAUTH2_CLIENT_SECRET+ "xxx"
      credentialsFile.writeAll("{\"client_id\":\"" + newClientId + "\",\"client_secret\":\"" + newClientSecret + "\"}")
      
      val newCredentials = provider.get
      newCredentials.clientId mustBe (newClientId)
      newCredentials.clientSecret mustBe (newClientSecret)

      verify(cacheMock, times(2)).get[OAuth2Credentials](CACHE_KEY)
      verify(cacheMock, times(2)).set(
        org.mockito.Matchers.eq(CACHE_KEY),
        anyObject[OAuth2Credentials],
        org.mockito.Matchers.eq(Duration(OAuth2Constants.credentialsCacheExpiryTime, SECONDS)))
      verifyNoMoreInteractions(cacheMock)
    }
    
    "allow cache invalidation" in {
      val cacheMock = mock[CacheApi]
      when(cacheMock.get[OAuth2Credentials](CACHE_KEY)).thenReturn(None)
      val provider = new OAuth2CredentialsProvider(app, cacheMock)
      
      provider.invalidateCache()

      verify(cacheMock, times(1)).remove(CACHE_KEY)
      verifyNoMoreInteractions(cacheMock)
    }
    
  }
}
