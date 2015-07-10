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

import play.api.Play

object OAuth2Constants {
  private lazy val configuration = Play.current.configuration

  lazy val accessTokenUrl = configuration.getString("oauth2.access.token.url").get
  lazy val tokenInfoUrl = configuration.getString("oauth2.token.info.url").get
  lazy val callbackUrl = configuration.getString("oauth2.callback.url").get
  lazy val authorizationUrl = configuration.getString("oauth2.authorization.url").get
  lazy val requestTimeout =configuration.getLong("oauth2.request.timeout").get
  lazy val expiryTimeLimitForTokenRefreshInSeconds = configuration.getInt("oauth2.token.refresh.expiry.limit").get
  lazy val isOAuth2Enabled = configuration.getBoolean("oauth2.enabled").get
  lazy val credentialsFilePath = configuration.getString("oauth2.credentials.filePath").get
  lazy val credentialsCacheExpiryTime = configuration.getLong("oauth2.credentials.cache.expiry.time").get

  val SESSION_KEY_ACCESS_TOKEN = "oauth2_access_token"
  val SESSION_KEY_REFRESH_TOKEN = "oauth2_refresh_token"
  val SESSION_KEY_STATE = "oauth2_state"
  val SESSION_KEY_ORIGINAL_REQUEST_URL = "oauth2_original_request_url"
  val SESSION_KEY_RETRY_AFTER_AUTH_ERROR = "oauth2_retry_after_auth_error"
}
