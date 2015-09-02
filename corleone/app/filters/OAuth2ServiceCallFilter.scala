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

import javax.inject.Inject

import play.api.http._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.{Logger, Play}
import security.{OAuth2Constants, OAuth2Helper}

import scala.concurrent.Future


/**
 * The OAuth2Filter filters all service requests for valid OAuth2 credentials, if the the filter is enabled.
 * Note that the filter validates the access token against the token info endpoint for each (enabled) request.
 */
class OAuth2ServiceCallFilter @Inject()(oauth2: OAuth2Helper) extends Filter {
  val headerTokenRegex = """([Bb][Ee][Aa][Rr][Ee][rR])\s(\S+)""".r
  val customServicePaths = oauth2.extractConfiguredValueList("oauth2.service.paths")

  val SERVICE_PATH = List("/api")
  val AUTHORIZATION_HEADER = "Authorization"
  val NO_AUTH_HEADER: String = "NO_AUTH_HEADER"
  val NO_TOKEN: String = "NO_TOKEN"


  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {

    if (OAuth2Constants.isOAuth2Enabled) {
      if (Logger.isDebugEnabled) Logger.debug("Entered OAuth2ServiceCallFilter for path " + requestHeader.path)

      // filter focuses on OAUTH2 validation for services only
      val pathMatcher = (entry: String) => requestHeader.path.startsWith(entry)
      if (SERVICE_PATH.find(pathMatcher).isEmpty && customServicePaths.find(pathMatcher).isEmpty)
        nextFilter.apply(requestHeader)
      else {
        if (Logger.isDebugEnabled) Logger.debug("OAuth2ServiceCallFilter is executed for path " + requestHeader.path)

        val authHeaderOption = requestHeader.headers.get(AUTHORIZATION_HEADER)
        val authHeader = authHeaderOption.getOrElse(NO_AUTH_HEADER)

        if (authHeader == NO_AUTH_HEADER) {
          Logger.info("No Authorization header was supplied -> respond with BAD_REQUEST (400)")
          Future[Result](Results.BadRequest("no authorization header provided in request"))
        }
        else {
          val accessToken = authHeader match {
            case headerTokenRegex(bearer, token) => token
            case _ => NO_TOKEN
          }

          if (accessToken == NO_TOKEN) {
            Logger.info("Authorization header does not contain any access token -> respond with BAD_REQUEST (400)")
            Future[Result](Results.BadRequest("Authorization header does not contain any access token"))
          }
          else {
            val tokenInfoResponseFuture = oauth2.requestTokenInfo(accessToken)

            tokenInfoResponseFuture flatMap { tokenInfoResponse =>
              processTokenInfo(nextFilter, requestHeader, tokenInfoResponse)
            }
          }
        }
      }
    }
    else {
      nextFilter.apply(requestHeader)
    }
  }


  private def processTokenInfo(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader, tokenInfoResponse: WSResponse): Future[Result] = {
    if (isAccessGranted(tokenInfoResponse)) {
      Logger.info("supplied access token IS valid and access is granted")

      // apply filter following in filter chain
      nextFilter(requestHeader)
      //.map { result => result}
       // result.withSession(requestHeader.session)
         // .withCookies(requestHeader.cookies.toList: _*)
        //.withHeaders(requestHeader.headers.headers: _*)
      //}
    }
    else {
      Logger.info("access is not granted -> respond with Forbidden (403)")
      Future[Result](Results.Forbidden("access is not granted"))
    }
  }


  private def isAccessGranted(response: WSResponse): Boolean = {
    // token is still valid. now. let's check if user has sufficient scopes 
    if (response.status == Status.OK) {
      // at the moment, we can only check if the user is an employee. In the future, it will be possible to assign
      // more scopes to an user
      val realmOption = (response.json \ "realm").asOpt[String]
      realmOption match {
        case Some(realm) => {

          Logger.debug("realm is " + realm);
          realm == "services"
        }
        case _ => false
      }
    }
    else {
      Logger.warn("request to token info endpoint was not successful -> access is not granted")
      false
    }
  }
}

