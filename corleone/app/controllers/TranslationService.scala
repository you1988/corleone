package controllers
import dao.TranslationKeyDAO
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
import javax.inject._
import play.api.data.validation.ValidationError
@Singleton
class TranslationService @Inject() (translationManager: TranslationManage) extends Controller {
  def getTranslaions(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]) = Action {
    request =>
      {
      Helper.validateLanguageCodes(languageCodes) match {
        case Some(error) => Status(400)(Json.toJson(Error.Error(request.uri, 400, error.title, error.detail, request.uri, error._links)))
        case None =>
          val msgConstants = translationManager.getTranslationMessage(languageCodes, tags, limit, after, before);
          val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
          val messageConstantHash = Codecs.sha1((msgConstants).toString());
          request.headers.get(IF_NONE_MATCH).collect {
            case value if (messageConstantHash.equals(value)) => NotModified
          } getOrElse Ok(Json.toJson(Response.SearchResponse(msgConstants, hATEOAS))).withHeaders(
            CACHE_CONTROL -> "max-age=36",
            ETAG -> messageConstantHash)
        }
      }
  }

  def createTranslaions() = Action(parse.json) {
    request =>
      request.body.validate[Seq[MessageConstant.MessageConstant]] match {
        case e: JsError => Status(400)(Json.toJson(Error.Error(request.uri, 400, "Request malformed", "Errors: " + JsError.toJson(e).toString(), "tes")))
        case s: JsSuccess[Seq[MessageConstant.MessageConstant]] => {
          val messageConstants = s.get
          if (messageConstants exists (e => !translationManager.getIfExistWithKey(e.key).isEmpty)) {
            val key = messageConstants.find { x => !translationManager.getIfExistWithKey(x.key).isEmpty }.get.key;
            Status(409)(Json.toJson(Error.Error(request.uri, 409, "Resource already exist", "Resources with this key " + key + "already exist", request.uri)))
          } else {
            val error = translationManager.createMessageConstants(messageConstants);
            error match {
              case None => {
                val hATEOAS = Helper.getHATEOAS(request.method, messageConstants, request.host);
                Ok(Json.toJson(hATEOAS))
              }
              case Some(er) => Status(422)(Json.toJson(Error.Error(request.uri, 422, er.title, er.detail, "tes", er._links)))
            }
          }
        }
      }
  }

  def getTranslation(key: String, languageCodes: Option[Seq[String]]) = Action {
    request =>
      Helper.validateLanguageCodes(languageCodes) match {
        case Some(error) => Status(400)(Json.toJson(Error.Error(request.uri, 400, error.title, error.detail, "tes", error._links)))
        case None =>
          translationManager.getIfExist(key, languageCodes match {
            case Some(languageCodes) => languageCodes
            case None                => Seq[String]()
          }) match {
            case None => Status(404)(Json.toJson(Error.Error(request.uri, 404, "Resource not Found", "No message constant found for " + key, request.uri)))
            case Some(msg) => {
              val msgConstants = Seq[MessageConstant.MessageConstant](msg);
              val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
              val hash = Codecs.sha1(msg.toString());
              request.headers.get(IF_NONE_MATCH).collect {
                case value if (hash.equals(value)) => NotModified
              } getOrElse Ok(Json.toJson(Response.MessageConstantResponse(msg, hATEOAS))).withHeaders(
                CACHE_CONTROL -> "max-age=36",
                ETAG -> hash)
            }
          }
      }
  }

  def putTranslation(key: String) = Action(parse.json) {
    request =>
      request.body.validate[MessageConstant.MessageConstant] match {
        case e: JsError => Status(400)(Json.toJson(Error.Error(request.uri, 400, "Request malformed", "Errors: " + JsError.toJson(e).toString(), request.uri)))
        case s: JsSuccess[MessageConstant.MessageConstant] => {
          val messageConstant = s.get
          val result = translationManager.getIfExistWithKey(key);
          result match {
            case None =>
              val error = translationManager.createMessageConstant(messageConstant);
              error match {
                case Some(er) => Status(422)(Json.toJson(Error.Error(request.uri, 422, er.title, er.detail, "test", er._links)))
                case None => {
                  val hATEOAS = Helper.getHATEOAS(request.method, Seq[MessageConstant.MessageConstant](messageConstant), request.host);
                  val messageConstantCreatedHash = Codecs.sha1((messageConstant).toString());
                  Status(204)(Json.toJson(hATEOAS)).withHeaders(
                    CACHE_CONTROL -> "max-age=36",
                    ETAG -> messageConstantCreatedHash)
                }
              }
            case Some(msg) => {
              val msgConstants = Seq[MessageConstant.MessageConstant](msg);
              val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
              val currentMsgStateHash = Codecs.sha1(msg.toString());
              request.headers.get(IF_NONE_MATCH).collect {
                case value if (currentMsgStateHash.equals(value)) => {
                  val error = translationManager.updateMessageConstant(messageConstant);
                  error match {
                    case Some(er) => Status(422)(Json.toJson(Error.Error(request.uri, 422, er.title, er.detail, "test", er._links)))
                    case None => {
                      val hATEOAS = Helper.getHATEOAS(request.method, Seq[MessageConstant.MessageConstant](messageConstant), request.host);
                      val messageConstantUpdatedHash = Codecs.sha1(messageConstant.toString());

                      Status(204)(Json.toJson(hATEOAS)).withHeaders(
                        CACHE_CONTROL -> "max-age=36",
                        ETAG -> messageConstantUpdatedHash)
                    }
                  }
                }
              } getOrElse Status(412)(Json.toJson(Response.MessageConstantResponse(msg, hATEOAS))).withHeaders(
                CACHE_CONTROL -> "max-age=36",
                ETAG -> currentMsgStateHash)
            }
          }
        }
      }
  }

  def patchTranslation(key: String) = Action(parse.json) {
    request =>
      request.body.validate[MessageConstant.MessageConstant] match {
        case e: JsError => Status(400)(Json.toJson(Error.Error(request.uri, 400, "Request malformed", "Errors: " + JsError.toJson(e).toString(), "tes")))
        case s: JsSuccess[MessageConstant.MessageConstant] => {
          val messageConstant = s.get
          val result = translationManager.getIfExistWithKey(key);
          result match {
            case None => Status(404)(Json.toJson(Error.Error(request.uri, 404, "Message Constant not found", "MessageConstant whith the following key " + key + " not found.", request.uri)))
            case Some(message) => {
              val msgConstants = Seq[MessageConstant.MessageConstant](message);
              val hATEOAS = Helper.getHATEOAS(request.method, msgConstants, request.host);
              var hash = Codecs.sha1(message.toString());
              request.headers.get(IF_NONE_MATCH).collect {
                case value if (hash.equals(value)) => {
                  val error = translationManager.updateMessageConstant(messageConstant);
                  error match {
                    case Some(er) => Status(422)(Json.toJson(Error.Error(request.uri, 422, er.title, er.detail, request.uri, er._links)))
                    case None => {
                      var messageConstantUpdatedHash = ""
                      translationManager.getIfExistWithKey(key) match {
                        case None                 => messageConstantUpdatedHash = ""
                        case Some(messageUpdated) => messageConstantUpdatedHash = Codecs.sha1(messageUpdated.toString());
                      }
                      val hATEOAS = Helper.getHATEOAS(request.method, Seq[MessageConstant.MessageConstant](messageConstant), request.host);
                      Ok(Json.toJson(hATEOAS)).withHeaders(
                        CACHE_CONTROL -> "max-age=36",
                        ETAG -> messageConstantUpdatedHash)
                    }
                  }
                }
              } getOrElse Status(412)(Json.toJson(Response.MessageConstantResponse(message, hATEOAS))).withHeaders(
                CACHE_CONTROL -> "max-age=36",
                ETAG -> hash)
            }
          }
        }
      }
  }
 
  def deleteTranslation(key: String) = Action {
    request =>
      val result = translationManager.getIfExistWithKey(key);
      result match {
        case None => Status(404)(Json.toJson(Error.Error(request.uri, 404, "Message Constant not found", "MessageConstant whith the following key " + key + " not found.", request.uri)))
        case Some(message) => {
          val hATEOAS = Helper.getHATEOAS(request.method, Seq[MessageConstant.MessageConstant](message), request.host);
          translationManager.deleteMessageConstant(key)
          Status(204)(Json.toJson(hATEOAS))
        }
      }
    }
  }