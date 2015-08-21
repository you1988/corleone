package controllers
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
import scala.concurrent.Future
import javax.inject._
import play.api.data.validation.ValidationError
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.Singleton
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.twirl.api.Html
import models.MessageConstant.MessageConstant
import scala.util.{ Success, Failure }
@Singleton
class Application @Inject() (translationManager: TranslationManage) extends Controller {
  def index = Action.async {
    translationManager.getAllTags().flatMap { tags =>
      tags match {
        case None => Future { Status(500) }
        case Some(tags) =>

          Future { Ok(views.html.main(tags)(null)(null)) };
      }
    }
  }
  def search = Action.async {
    req =>

      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      Logger.error(map.toString())
      translationManager.getAllTags().flatMap { tags =>
        tags match {
          case None => Future { Status(500) }
          case Some(tags) =>

            Helper.validatSearchRequest(map) match {
              case (None, Some(tag)) => {
                translationManager.getIfExistWithTag(tag).map { message =>
                  message match {
                    case None           => Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("No element found"))))
                    case Some(messages) => Ok(views.html.main(tags)(views.html.translationSearchView(messages))(null))
                  }
                }
              }
              case (Some(key), None) => {
                translationManager.getIfExistWithKey(key).map { message =>
                  message match {
                    case None           => Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("No element found"))))
                    case Some(messages) => Ok(views.html.main(tags)(views.html.translationSearchView(Seq(messages)))(null))
                  }
                }
              }
              case (None, None) => Future { Ok(views.html.main(tags)(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(views.html.Error(Seq[String]("No element found")))) }
              case (_, _)       => Future { BadRequest }
            }
        }
      }
  }

  def createForm = Action.async {
    translationManager.getAllTags().map { tags =>
      tags match {
        case None       => Status(500)
        case Some(tags) => Ok(views.html.main(tags)(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(null));
      }
    }
  }
  def updateForm = Action.async {
    req =>
      Logger.error("tes" + req.body.asFormUrlEncoded.get.toString())
      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      Helper.validatCreateReques(map) match {

        case (None, Some(message)) => translationManager.getAllTags().map { tags =>
          tags match {
            case None       => Status(500)
            case Some(tags) => Ok(views.html.main(tags)(views.html.UpdateTranslationForm(message, Seq("EN_GB", "EN_US", "DE_DE")))(null))
          }
        }
        case (_, _) => Future { BadRequest }
      }
  }
  def updateTranslation = Action.async {
    req =>
      Logger.error(req.body.asFormUrlEncoded.get.toString())
      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      translationManager.getAllTags().flatMap { tags =>
        tags match {
          case None => Future { Status(500) }
          case Some(tags) =>

            Helper.validatCreateReques(map) match {
              case (None, Some(message)) => translationManager.updateMessageConstant(message).map { result =>
                result match {
                  case None        => Ok(views.html.main(tags)(null)(views.html.Succes("Message Updated")));
                  case Some(error) => Ok(views.html.main(tags)(views.html.UpdateTranslationForm(message, translationManager.getAllLanguages()))(views.html.Error(Seq[String](error.title))))

                }

              }
              case (Some(errors), Some(message)) => Future { Ok(views.html.main(tags)(views.html.UpdateTranslationForm(message, translationManager.getAllLanguages()))(views.html.Error(errors))) }
              case (_, _)                        => Future { BadRequest }
            }
        }
      }
  }

  def createTranslation = Action.async { req =>
    Logger.error(req.body.asFormUrlEncoded.get.toString())
    val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
    translationManager.getAllTags().flatMap { tags =>
      tags match {
        case None => Future { Status(500) }
        case Some(tags) =>

          Helper.validatCreateReques(map) match {

            case (None, Some(message)) => {
              translationManager.createMessageConstant(message).map {
                messages =>
                  {
                    messages match {
                      case None        => Ok(views.html.main(tags)(null)(views.html.Succes("Message Created")));
                      case Some(error) => Ok(views.html.main(tags)(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(views.html.Error(Seq[String](error.title))))
                    }

                  }
              }
            }
            case (Some(errors), None) => Future { Ok(views.html.main(tags)(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(views.html.Error(errors))) }
            case (_, _)               => Future { BadRequest }
          }
      }
    }
  }

}
