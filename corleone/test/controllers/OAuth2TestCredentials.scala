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

import play.api.mvc.{Action, Results, Handler}
import play.api.test

import scala.reflect.io.File


trait OAuth2TestCredentials {
  
  val OAUTH2_CALLBACK_CODE = "anyCode"
  val OAUTH2_CALLBACK_STATE = "anyState"
  val OAUTH2_CALLBACK_ERROR = "anyError"
  val OAUTH2_CALLBACK_ERROR_DESCRIPTION = "very bad problem"
  val REDIRECT_URL = "https://localhost:9000"
  val ACCESS_TOKEN = "0989cd01-333d-4220-a699-539b452d019c"
  val REFRESH_TOKEN = "e8e099cf-2bc7-43c4-9e80-0c8ba66e4141"

  val credentialsFile = File.makeTemp().toAbsolute
  credentialsFile.writeAll("{\"client_id\":\"my_client_id\",\"client_secret\":\"my_client_secret\"}")

  def testPort:Int
  def callbackUrl =  s"http://localhost:$testPort/oauth_callback"
  def accessTokenEndpoint = s"http://localhost:$testPort/access_token"
  def authorizationEndpoint = s"http://localhost:$testPort/z/oauth2/authorize"
  def tokenInfoEndpoint =  s"http://localhost:$testPort/oauth2/tokeninfo"

  def routes: PartialFunction[Tuple2[String, String], Handler ] = {
    case ("POST", "/access_token") => Action { Results.Ok("{\"access_token\":\"" + ACCESS_TOKEN+ "\",\"refresh_token\":\"" + REFRESH_TOKEN+ "\",\"scope\":\"uid cn\",\"token_type\":\"Bearer\",\"expires_in\":3599}") }
  }


  def fakeApp: test.FakeApplication = test.FakeApplication(
    additionalConfiguration = Map (
      "oauth2.enabled"              -> false,
      "oauth2.callback.url"         -> callbackUrl,
      "oauth2.access.token.url"     -> accessTokenEndpoint,
      "oauth2.authorization.url"    -> authorizationEndpoint,
      "oauth2.token.info.url"       ->tokenInfoEndpoint,
      "oauth2.credentials.filePath" -> credentialsFile.toURI.getPath
    ),
    withRoutes = routes)
}
