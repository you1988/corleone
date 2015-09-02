/*
 * Copyright [2015] Zalando SE
 *
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

import javax.inject._

import models.Error.ShortError
import models._
import play.api.libs._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import services.{Constants, Helper, TranslationManage}

import scala.Predef._
import scala.concurrent.Future
/**
 * Corleone rest api implementation.
 */
@Singleton
class TranslationService @Inject()(translationManager: TranslationManage) extends Controller {
  val map = Map("PUT" -> 204, "PATCH" -> 200, "DELETE" -> 204, "GET" -> 200, "POST" -> 201)

  /**
   * This function searchs for all the message:
   * case : tags is specified and languageCodes is specified:  all translations associated with the specified language codes and tagged with one of tags.
   * case : tags is not specified and languageCodes is specified:  all translations associated with the specified language codes.
   * case : tags is  specified and languageCodes is not specified:  all translations tagged with one of tags.
   * case : tags is  not specified and languageCodes is not specified:  all message constants.
   * The result will sorted by key name filtred y before and after case specified(it means : all message constants that have key name between after and befor).
   * If filtred with limit if specified(it means: return just a part of message constants)  if not specified it will return 1000 message constant.
   * @param languageCodes To be used to filter desired translations.
   * @param tags To be used to filter desired message constants.
   * @param limit The maximum amount of message constants by default 1000
   * @param before
   * @param after
   * @return 200 http response case search query succeeded , the result is not empty and are different from the version the user has.
   *         304 http response case case search query succeeded , the result is not empty and the result is same as the version the user has
   *         503 http response case the data base is down (time out error)
   *         500 http response case an error unexpected happens
   *         400 http response case the request params are mal formed
   *
   */
  def getTranslaions(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]) = Action.async {
    request => {
      Helper.validateLanguageCodes(languageCodes) match {
        // case language code is not supported return 400 http response.
        case Some(error) => Future {
          handleFailure(request, Right(error))
        }
        case None =>
          translationManager.getTranslationMessage(languageCodes, tags, limit, after, before).map { responses =>
            responses match {
              // case succeeded search query or not found error return 304 if the result query not updated and  return 200 http response in the other case.
              case Right(err: NotFoundError) => handleGetSucces(request, Seq(), Some(limit.getOrElse[Integer](1000)))
              case Left(messages) => handleGetSucces(request, messages, Some(limit.getOrElse[Integer](1000)))
              // case error two possible error database is down or an unexpected error
              //in this case send 503 or 500 http response
              case Right(err) => handleFailure(request, Right(err))
            }
          }
      }
    }
  }

  /**
   * This function is for :
   * creating  message constant content of request body
   * @return 201 http response case the creating operation is done .
   *         409 http response case the message constant is already exist
   *         503 http response case the data base is down (time out error)
   *         500 http response case an error unexpected happens
   *         422 http response case the request payload is not valid
   *
   */
  def createTranslaions() = Action.async(parse.json) {
    request =>
      request.body.validate[Seq[MessageConstant.MessageConstant]] match {
        // case payload is not valide return 422 http response.
        case jsError: JsError => Future {
          handleFailure(request, Left(jsError))
        }
        case s: JsSuccess[Seq[MessageConstant.MessageConstant]] => {
          val messageConstants = s.get

          translationManager.createMessageConstants(messageConstants).map { error =>
            error match {
              // case message Constant created succeeded return 201 http response.
              case None => handleSuccess(request, messageConstants)
              //possible errors: database is down or an unexpected error or already a message constant exist with the specified key.
              //in this case send 503 or 500 or 409 http response
              case Some(er) => handleFailure(request, Right(er))
            }
          }
        }
      }
  }

  /**
   * This function searchs for all the translations  associated to language codes if no lanaguage codes specified all the translations.
   * @param languageCodes To be used to filter desired translations.
   * @return 200 http response case search query succeeded , the result is not empty and are different from the version the user has.
   *         304 http response case search query succeeded , the result is not empty and the result is same as the version the user has
   *         404 http response case no record found for specified message constant key
   *         503 http response case the data base is down (time out error)
   *         500 http response case an error unexpected happens
   *         400 http response case the request params are mal formed
   *
   */
  def getTranslation(key: String, languageCodes: Option[Seq[String]]) = Action.async {
    request =>
      Helper.validateLanguageCodes(languageCodes) match {
        // case language code is not supported return 400 http response.
        case Some(error) => Future {
          handleFailure(request, Right(error))
        }
        case None =>
          translationManager.getIfExist(key, languageCodes).map {
            case Left(msg) =>
              // case succeeded search query or not found error return 304 if the user has an up to date version  and 200 http response in the other case.
              handleGetSucces(request, msg)
            // case error : possible errors database is down or an unexpected error or not found message constant
            //in this case send 503 or 500 or 404 http response
            case Right(err) => handleFailure(request, Right(err))

          }
      }
  }

  /**
   * This function is for :
   * creating  message constant with key case there is no one already with the same key
   * updaing  message constant in the other case.
   * @param key Of message constant to create or update
   * @return 200 http response case the updating operation is done .
   *         412 http response case the user version of message constant is out of date
   *         503 http response case the data base is down (time out error)
   *         500 http response case an error unexpected happens
   *         422 http response case the request payload is not valid
   *         404 http response case there is no message constant with the specified key.
   */
  def patchTranslation(key: String) = Action.async(parse.json) {
    request =>
      request.body.validate[MessageConstantDelta.MessageConstantDelta] match {
        // case payload is not valide return 422 http response.
        case e: JsError => Future {
          handleFailure(request, Left(e))
        }
        case s: JsSuccess[MessageConstantDelta.MessageConstantDelta] => {
          val messageConstant = s.get
          translationManager.getIfExistWithKey(key).flatMap { result =>
            result match {
              // case error : possible errors database is down or an unexpected error or not found message constant
              //in this case send 503 or 500 or 404 http response
              case Right(err) => Future {
                handleFailure(request, Right(err))
              }
              case Left(message) =>
                // case there is a message constant with this key
                //check if message constant is up to date.
                handleOutOfDateCase(request, message.head) match {
                  case None =>{

                    translationManager.updateMessageConstant(transformMessageConstantDelta(message.head,messageConstant)).map {
                      result => result match {
                        // update operation done return 204 http response
                        case Left(updatedMessage) =>
                          handleSuccess(request, Seq(updatedMessage))
                        case Right(err) =>
                          // possible error database down or an unexpected error
                          //return 500 or 503 response
                          handleFailure(request, Right(err))
                      }
                    }
                    }
                  // case message constant is out of date return 412 response
                  case Some(err) => err
                }
            }
          }
        }
      }
  }


  /**
   * This function is for :
   * creating  message constant with key case there is no one already with the same key
   * updaing  message constant in the other case.
   * @param key Of message constant to create or update
   * @return 204 http response case the updating or creating operation is done .
   *         412 http response case the user version of message constant is out of date
   *         503 http response case the data base is down (time out error)
   *         500 http response case an error unexpected happens
   *         422 http response case the request payload is not valid
   *
   */
  def putTranslation(key: String) = Action.async(parse.json) {
    request =>
      request.body.validate[MessageConstant.MessageConstant] match {
        case e: JsError => Future {
          // case payload is not valide return 422 http response.
          handleFailure(request, Left(e))
        }
        case s: JsSuccess[MessageConstant.MessageConstant] => {
          val messageConstant = s.get
          translationManager.getIfExistWithKey(key).flatMap {
            result =>
              result match {
                //if there is no message constant with key.
                case Right(err: NotFoundError) =>
                  translationManager.createMessageConstant(messageConstant).map { error =>
                    error match {
                      //If the creation is ok resturn 204 http response.
                      case Left(createdMessage) =>
                        handleSuccess(request, Seq(createdMessage))
                      // case error two possible error database is down or an unexpected error
                      //in this case send 503 or 500 http response
                      case Right(err) =>
                        handleFailure(request, Right(err))
                    }
                  }
                case Left(msg) => {
                  // case there is a message constant with this key
                  //check if message constant is up to date.
                  handleOutOfDateCase(request, msg.headOption.get) match {
                    case None =>
                      //case message constant is up to date
                      translationManager.updateMessageConstant(messageConstant).map {
                        result => result match {
                          // update operation done return 204 http response
                          case Left(updatedMessage) =>
                            handleSuccess(request, Seq(updatedMessage))
                          case Right(err) =>
                            // possible error database down or an unexpected error
                            //return 500 or 503 response
                            handleFailure(request, Right(err))
                        }
                      }
                    // case message constant is out of date return 412 response
                    case Some(err) => err
                  }
                }
                //possible error database is down or unexpected error
                //return 503 or 500 response
                case Right(err) => Future {
                  handleFailure(request, Right(err))
                }
              }
          }
        }

      }
  }
  /**
   * This function is for delete  message constant associated to key
   * @param key Of the message to be deleted.
   * @return 204 http response case the delete operation is done .
   *         503 http response case the data base is down (time out error)
   *         500 http response case an error unexpected happens
   *         404 http response case there is no message constant associated to key
   *
   */
  def deleteTranslation(key: String) = Action.async {
    request =>
      translationManager.getIfExistWithKey(key).flatMap {
        case Right(err) =>
          // possible error database down or an unexpected error or not found message constant
          //return 500 or 503 or 404 response
          Future{handleFailure(request, Right(err))}

        case Left(message) =>
          translationManager.deleteMessageConstant(key).map { result=> result match {
            // delete operation done return 204 http response
            case None => handleSuccess (request, message)
            case Some (err) =>
            // possible error database down or an unexpected error
            //return 500 or 503 response
          handleFailure (request, Right (err) )
          }
          }

      }
  }
  private def handleFailure(request: Request[Any], error: Either[JsError, ShortError]): Result = {
    error match {
      case Left(jsError) => Status(422)(Json.toJson(Error.Error(request.uri, 422, Constants.REQUEST_NOT_VALID_ERROR_TITLE, JsError.toJson(jsError).toString(), request.uri))).withHeaders(
        CONTENT_TYPE -> "application/problem+json"
      )
      case Right(shortError) => shortError match {
        case shortError: MessageConstantViolatedConstraintError => Status(409)(Json.toJson(Error.Error(request.uri, 409, Constants.REQUEST_MESSAGE_CONSTANT_ALREADY_EXIST_ERROR_TITLE, shortError.detail, request.uri))).withHeaders(
          CONTENT_TYPE -> "application/problem+json"
        )
        case shortError: TimeOutError => Status(503)(Json.toJson(Error.Error(request.uri, 503, Constants.REQUEST_SERVICE_UNAVAILABLE_ERROR_TITLE, shortError.detail, request.uri))).withHeaders(
          CONTENT_TYPE -> "application/problem+json"
        )
        case shortError: NotHandledError => Status(500)(Json.toJson(Error.Error(request.uri, 503, Constants.REQUEST_UNEXPECTED_EXCEPTION_ERROR_TITLE, shortError.detail, request.uri))).withHeaders(
          CONTENT_TYPE -> "application/problem+json"
        )
        case shortError: OutOfDateError => Status(412).withHeaders(
          CONTENT_TYPE -> "application/problem+json"
        )
        case shortError: NotFoundError => Status(404)(Json.toJson(Error.Error(request.uri, 404, shortError.title, shortError.detail, request.uri))).withHeaders(
          CONTENT_TYPE -> "application/problem+json"
        )
        case shortError: MalFormedError => Status(400)(Json.toJson(Error.Error(request.uri, 400, shortError.title, shortError.detail, request.uri))).withHeaders(
          CONTENT_TYPE -> "application/problem+json"
        )
      }
    }
  }

  private def handleSuccess(request: Request[Any], messageConstant: Seq[MessageConstant.MessageConstant], limit: Option[Integer] = None): Result = {
    val hATEOAS = Helper.getHATEOAS(request.method, messageConstant, request.host)
    val messageConstantHash = Codecs.sha1(messageConstant.toString())
    request.method match {
      case update if update == "PUT" || update == "PATCH" =>

        Status(map.get(update).get)(Json.toJson(hATEOAS)).withHeaders(
          CACHE_CONTROL -> "max-age=3600",
          ETAG -> messageConstantHash,
          CONTENT_TYPE -> "application/x.zalando.logistics.translations+json"
        )
      case "GET" => {
        var result = if (!limit.isEmpty) Status(map.get("GET").get)(Json.toJson(Response.SearchResponse(messageConstant, hATEOAS))) else Status(map.get("GET").get)(Json.toJson(Response.MessageConstantResponse(messageConstant.headOption.get, hATEOAS)))

        var headers: Seq[(String, String)] = Seq(CACHE_CONTROL -> "max-age=3600", ETAG -> messageConstantHash,CONTENT_TYPE -> "application/x.zalando.logistics.translations+json")

        if (!limit.isEmpty) headers = headers :+ "X-Remainder-Count" -> (if (messageConstant.size < limit.get) 0 else messageConstant.size - limit.get).toString()

        result.withHeaders(headers: _*)
      }
      case "POST" => Status(map.get("POST").get)(Json.toJson(hATEOAS)).withHeaders(CONTENT_TYPE -> "application/x.zalando.logistics.translations+json")
      case "DELETE" => Status(map.get("DELETE").get)(Json.toJson(hATEOAS)).withHeaders(CONTENT_TYPE -> "application/x.zalando.logistics.translations+json")


    }


  }

  private def handleOutOfDateCase(request: Request[Any], upTodateMessage: MessageConstant.MessageConstant): Option[Future[Result]] = {
    val messageConstantUpdatedHash = Codecs.sha1(Seq(upTodateMessage).toString())
    val hATEOAS = Helper.getHATEOAS(request.method, Seq(upTodateMessage), request.host)
    request.headers.get(IF_NONE_MATCH) match {
      case Some(oldStateMessageConstantHash) if oldStateMessageConstantHash == messageConstantUpdatedHash => None
      case _ => Some(Future {
        Status(412)(Json.toJson(Response.MessageConstantResponse(upTodateMessage, hATEOAS))).withHeaders(
          CACHE_CONTROL -> "max-age=36",
          ETAG -> messageConstantUpdatedHash,
        CONTENT_TYPE -> "application/problem+json"
        )
      })
    }
  }

  private def handleGetSucces(request: Request[Any], messages: Seq[MessageConstant.MessageConstant], limit: Option[Integer] = None): Result = {
    val hATEOAS = Helper.getHATEOAS(request.method, messages, request.host);
    val hash = Codecs.sha1(messages.toString());
    request.headers.get(IF_NONE_MATCH).collect {
      case value if (hash.equals(value)) => NotModified.withHeaders(CONTENT_TYPE -> "application/x.zalando.logistics.translations+json")
    } getOrElse handleSuccess(request, messages, limit)

  }
private def transformMessageConstantDelta(message:MessageConstant.MessageConstant,messageConstantDelta:MessageConstantDelta.MessageConstantDelta):MessageConstant.MessageConstant={
var result = message
  if(!(messageConstantDelta.translations==null ||messageConstantDelta.translations.isEmpty)) result= message.copy(translations = messageConstantDelta.translations)
  if(!(messageConstantDelta.tags==null || messageConstantDelta.tags.isEmpty)) result= message.copy(tags = messageConstantDelta.tags)
  result
  }

}