package controllers
import models.MessageConstant
import models.Error
import models.Link
import play.api.libs.json._
import play.api.http._
import play.api.http.HeaderNames._
import scala.Predef._
import play.api._
import play.api.libs._
import play.api.mvc._
import services.TranslationManage
import models.Translation
import services.TranslationManageImpl
import models.Response
import services.Helper
import scala.concurrent.Future;
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject._
import play.api.data.validation.ValidationError
@Singleton
class TranslationService @Inject() (translationManager: TranslationManage) extends Controller {
  def getTranslaions(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]) = Action.async {
    request =>
      {
        Helper.validateLanguageCodes(languageCodes) match {
          case Some(error) => Future { Status(400)(Json.toJson(Error.Error(request.uri, 400, error.title, error.detail, request.uri, error._links))) }
          case None =>
            translationManager.getTranslationMessage(languageCodes, tags, limit, after, before).map { responses =>
              responses match {
                case Some(response) =>
                  val msgConstants = response.messageConstants
                  val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
                  val messageConstantHash = Codecs.sha1((msgConstants).toString());
                  request.headers.get(IF_NONE_MATCH).collect {
                    case value if (messageConstantHash.equals(value)) => NotModified
                  } getOrElse {
                    Ok(Json.toJson(Response.SearchResponse(msgConstants, hATEOAS))).withHeaders(
                      CACHE_CONTROL -> "max-age=3600",
                      ETAG -> messageConstantHash,
                      "X-Remainder-Count" -> (if (response.count < limit.getOrElse[Integer](1000)) 0 else response.count - limit.getOrElse[Integer](1000)).toString())
                  }
                case None => BadRequest
              }
            }
        }
      }
  }

  def createTranslaions() = Action.async(parse.json) {
    request =>
      request.body.validate[Seq[MessageConstant.MessageConstant]] match {
        case e: JsError => Future { Status(400)(Json.toJson(Error.Error(request.uri, 400, "Request malformed", "Errors: " + JsError.toJson(e).toString(), "tes"))) }
        case s: JsSuccess[Seq[MessageConstant.MessageConstant]] => {
          val messageConstants = s.get

          translationManager.createMessageConstants(messageConstants).map { error =>
            error match {
              case None => {
                val hATEOAS = Helper.getHATEOAS(request.method, messageConstants, request.host);
                Ok(Json.toJson(hATEOAS))
              }
              case Some(er) => if (er.title.equals("Resource already exist")) Status(409)(Json.toJson(Error.Error(request.uri, 409, "Resource already exist", er.detail, request.uri))) else Status(422)(Json.toJson(Error.Error(request.uri, 422, er.title, er.detail, "tes", er._links)))
            }
          }
        }
      }
  }

  def getTranslation(key: String, languageCodes: Option[Seq[String]]) = Action.async {
    request =>
      Helper.validateLanguageCodes(languageCodes) match {
        case Some(error) => Future { Status(400)(Json.toJson(Error.Error(request.uri, 400, error.title, error.detail, "tes", error._links))) }
        case None =>
          translationManager.getIfExist(key, languageCodes).map {
            case None => Status(404)(Json.toJson(Error.Error(request.uri, 404, "Resource not Found", "No message constant found for " + key, request.uri)))
            case Some(msg) => {
              val msgConstants = Seq[MessageConstant.MessageConstant](msg);
              val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
              val hash = Codecs.sha1(msg.toString());
              request.headers.get(IF_NONE_MATCH).collect {
                case value if (hash.equals(value)) => NotModified
              } getOrElse Ok(Json.toJson(Response.MessageConstantResponse(msg, hATEOAS))).withHeaders(
                CACHE_CONTROL -> "max-age=3600",
                ETAG -> hash)
            }
          }
      }
  }

  def putTranslation(key: String) = Action.async(parse.json) {
    request =>
          request.body.validate[MessageConstant.MessageConstant] match {
            case e: JsError => Future{Status(400)(Json.toJson(Error.Error(request.uri, 400, "Request malformed", "Errors: " + JsError.toJson(e).toString(), request.uri)))}
            case s: JsSuccess[MessageConstant.MessageConstant] => {
              val messageConstant = s.get
             translationManager.getIfExistWithKey(key).flatMap {
                result => 
                  result match {
                case None => executeAction(key, messageConstant, request, translationManager.createMessageConstant, handlePutSucces, handleUpdateUnSucces)
                case Some(msg) => {
                  val msgConstants = Seq[MessageConstant.MessageConstant](msg);
                  val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
                  val currentMsgStateHash = Codecs.sha1(msg.toString());
                  request.headers.get(IF_NONE_MATCH).collect {
                    case value if (currentMsgStateHash.equals(value)) => executeAction(key, messageConstant, request, translationManager.updateMessageConstant, handlePutSucces, handleUpdateUnSucces)
                  } getOrElse[Future[Result]] Future{ Status(412)(Json.toJson(Response.MessageConstantResponse(msg, hATEOAS))).withHeaders(
                    CACHE_CONTROL -> "max-age=36",
                    ETAG -> currentMsgStateHash)
                }
                }
                  }
              }
            }
              
          }
  }

  def patchTranslation(key: String) = Action.async(parse.json) {
    request =>
      request.body.validate[MessageConstant.MessageConstant] match {
        case e: JsError => Future { Status(400)(Json.toJson(Error.Error(request.uri, 400, "Request malformed", "Errors: " + JsError.toJson(e).toString(), "tes"))) }
        case s: JsSuccess[MessageConstant.MessageConstant] => {
          val messageConstant = s.get
          translationManager.getIfExistWithKey(key).flatMap { result =>
            result match {
              case None => Future{Status(404)(Json.toJson(Error.Error(request.uri, 404, "Message Constant not found", "MessageConstant whith the following key " + key + " not found.", request.uri))) }
              case Some(message) => {
                val msgConstants = Seq[MessageConstant.MessageConstant](message);
                val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
                var hash = Codecs.sha1(message.toString());
                request.headers.get(IF_NONE_MATCH).collect {
                  case value if (hash.equals(value)) => executeAction(key, messageConstant, request, translationManager.updateMessageConstant, handleUpdateSucces, handleUpdateUnSucces).map(result=>result)
                } getOrElse[Future[Result]] Future{Status(412)(Json.toJson(Response.MessageConstantResponse(message, hATEOAS))).withHeaders(
                  CACHE_CONTROL -> "max-age=36",
                  ETAG -> hash)
                }
              }
            }
          }
        }
      }
  }

  def deleteTranslation(key: String) = Action {
    request => BadRequest
    //      val result = translationManager.getIfExistWithKey(key);
    //      result match {
    //        case None => Status(404)(Json.toJson(Error.Error(request.uri, 404, "Message Constant not found", "MessageConstant whith the following key " + key + " not found.", request.uri)))
    //        case Some(message) => {
    //          val hATEOAS = Helper.getHATEOAS(request.method, Seq[MessageConstant.MessageConstant](message), request.host);
    //          translationManager.deleteMessageConstant(key)
    //          Status(204)(Json.toJson(hATEOAS))
    //        }
    //      }
  }

  def handlePutSucces(key: String, method: String, host: String): Future[Result] = {
    handleSucces(key, method, host, 204)
  }
  def handleSucces(key: String, method: String, host: String, resultStatus: Integer): Future[Result] = {
    translationManager.getIfExistWithKey(key).flatMap { result =>
      {
        val messageConstantUpdatedHash = result match {
          case None                 => ("", Seq[MessageConstant.MessageConstant]())
          case Some(messageUpdated) => (Codecs.sha1(messageUpdated.toString()), Seq[MessageConstant.MessageConstant](messageUpdated));
        }
        val hATEOAS = Helper.getHATEOAS(method, messageConstantUpdatedHash._2, host);
        Future {
          Status(resultStatus)(Json.toJson(hATEOAS)).withHeaders(
            CACHE_CONTROL -> "max-age=3600",
            ETAG -> messageConstantUpdatedHash._1)
        }
      }
    }
  }
  def handleUpdateSucces(key: String, method: String, host: String): Future[Result] = {
    handleSucces(key, method, host, 200)
  }
  def handleUpdateUnSucces(request: Request[JsValue], er: Error.ShortError): Future[Result] = {
    Future{Status(422)(Json.toJson(Error.Error(request.uri, 422, er.title, er.detail, request.uri, er._links)))}
  }
  def executeAction(key: String, msg: MessageConstant.MessageConstant, request: Request[JsValue], action: (MessageConstant.MessageConstant) => Future[Option[Error.ShortError]], handlerSucces: (String, String, String) => Future[Result], handlerError: (Request[JsValue], Error.ShortError) => Future[Result]): Future[Result] = {
    action(msg).flatMap { error =>
      error match {
        case None      => handlerSucces(key, request.method, request.host)
        case Some(err) => handlerError(request, err)
      }
    }
  }

}