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

import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.{Singleton, _}

import models.Error.ShortError
import models._
import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import services.{Helper, TranslationManage}

import scala.concurrent.Future

@Singleton
class Application @Inject()(translationManager: TranslationManage) extends Controller {

  def index = Action.async {
    translationManager.getAllTags().flatMap { tags =>
      tags match {
        case Right(err) => Future {
          Ok(views.html.main(Seq())(null)(null))
        }
        case Left(tags) =>
          Future {
            Ok(views.html.main(tags)(null)(null))
          }
      }
    }
  }

  def search = Action.async {
    req =>
      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      translationManager.getAllTags().flatMap { tags =>
        tags match {
          case Left(tags) =>
            Helper.getAndValidatSearchRequest(map) match {
              case Some(Right(tag)) => {
                translationManager.getIfExistWithTag(tag).map { message =>
                  message match {
                    case Left(messages) => Ok(views.html.main(tags)(views.html.translationSearchView(messages))(null))
                    case Right(err) => handleFailure(Right(err), tags)
                  }
                }
              }
              case Some(Left(key)) => {
                translationManager.getIfExistWithKey(key).map { message =>
                  message match {
                    case Left(messages) => Ok(views.html.main(tags)(views.html.translationSearchView(messages))(null))
                    case Right(err) => handleFailure(Right(err), tags)
                  }
                }
              }
              case None => Future {
                Ok(views.html.main(tags)(null)(null))
              }
            }
          case Right(err) => Future {
            Ok(views.html.main(Seq())(null)(null))

          }
        }
      }
  }

  def createForm = Action.async {
    translationManager.getAllTags().map { tags =>
      tags match {
        case Right(err) => Ok(views.html.main(Seq())(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(null))
        case Left(tags) => Ok(views.html.main(tags)(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(null));
      }
    }
  }

  def exportForm = Action.async {
    translationManager.getAllTags().map { tags =>
      tags match {
        case Right(err) => Ok(views.html.main(Seq())(views.html.TranslationExporterForm(Seq()))(null))
        case Left(tags) => Ok(views.html.main(tags)(views.html.TranslationExporterForm(tags))(null));
      }
    }
  }

  def importForm = Action.async {
    translationManager.getAllTags().map { tags =>
      tags match {
        case Right(err) => Ok(views.html.main(Seq())(views.html.TranslationImporterForm(translationManager.getAllLanguages()))(null))
        case Left(tags) => Ok(views.html.main(tags)(views.html.TranslationImporterForm(translationManager.getAllLanguages()))(null));
      }
    }
  }

  def updateForm = Action.async {
    req =>
      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      translationManager.getAllTags().map { tags =>
        tags match {
          case Right(err) => Ok(views.html.main(Seq())(null)(null));
          case Left(tags) => Helper.validatCreateRequest(map) match {
            case Left(message) => Ok(views.html.main(tags)(views.html.UpdateTranslationForm(message, translationManager.getAllLanguages()))(null))
            case Right(errors) => handleFailure(Left(errors), tags)
          }
        }
      }
  }

  def updateTranslation = Action.async {
    req =>
      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      translationManager.getAllTags().flatMap { tags =>
        tags match {
          case Right(err) => Future {
            Ok(views.html.main(Seq())(null)(null));
          }
          case Left(tags) =>

            Helper.validatCreateRequest(map) match {
              case Left(message) => translationManager.updateMessageConstant(message).map { result =>
                result match {
                  case Left(message) => Ok(views.html.main(tags)(null)(views.html.Succes("Message Updated")));
                  case Right(error) => handleFailure(Right(error), tags)

                }

              }
              case Right(errors) => Future {
                handleFailure(Left(errors), tags)
              }
            }
        }
      }
  }

  /*
    translationManager.deleteMessageConstant(message.key).flatMap{ errors=> errors match{
      case None => Ok(views.html.main(tags)(null)(views.html.Succes("Message Updated")));
      case Some(error) => handleFailure(Right(error),tags)
    }
    }
    */
  def deleteTranslation = Action.async {
    req =>
      val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
      translationManager.getAllTags().flatMap { tags =>
        tags match {
          case Right(err) => Future {
            Ok(views.html.main(Seq())(null)(null))
          }
          case Left(tags) => Helper.validatCreateRequest(map) match {
            case Left(message) => translationManager.deleteMessageConstant(message.key).map { errors =>
              errors match {
                case None => Ok(views.html.main(tags)(null)(views.html.Succes("Message Deleted")))
                case Some(error) => handleFailure(Right(error), tags)
              }
            }
            case Right(errors) => Future {
              handleFailure(Left(errors), tags)
            }
          }
        }
      }
  }

  def handleFailure(error: Either[Seq[String], ShortError], tags: Seq[String]): Result = {
    error match {
      case Right(shortError) => shortError match {
        case err: MessageConstantViolatedConstraintError => Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("There is already a message with the specific key"))))
        case err: NotFoundError => Ok(views.html.main(tags)(null)(null))
        case err: NotHandledError => Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("I do not know what happens. Could you please report the error?"))))
        case _ => Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("The data source system is down wait a moment and try again.\n Could you please report the error in case it happens frequently? "))))

      }
      case Left(errors) => Ok(views.html.main(tags)(null)(views.html.Error(errors)))
    }
  }

  def createTranslation = Action.async { req =>
    val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
    translationManager.getAllTags().flatMap { tags =>
      tags match {
        case Right(err) => Future {
          Ok(views.html.main(Seq())(null)(null));
        }
        case Left(tags) =>
          Helper.validatCreateRequest(map) match {
            case Left(message) => {
              translationManager.createMessageConstant(message).map {
                messages => {
                  messages match {
                    case Left(message) => Ok(views.html.main(tags)(null)(views.html.Succes("Message Created")));
                    case Right(error) => handleFailure(Right(error), tags)
                  }
                }
              }
            }
            case Right(errors) => Future {
              handleFailure(Left(errors), tags)
            }
          }
      }
    }
  }

  def exportTranslation = Action.async { req =>
    val map: Map[String, Seq[String]] = req.body.asFormUrlEncoded.getOrElse(Map())
    translationManager.getAllTags().flatMap { tags =>
      tags match {
        case Right(err) => Future {
          Ok(views.html.main(Seq())(null)(null));
        }
        case Left(tags) =>
          Helper.validatExportRequest(map) match {
            case Left(request) => {
              translationManager.getIfExistWithTag(request.tag).map {
                messages => {
                  messages match {
                    case Left(message) => {
                      val data = Helper.toCsv(message, request)
                      Ok.chunked(Enumerator(data._1.getBytes(request.csvEncoding)).andThen(Enumerator.eof)).withHeaders(CONTENT_TYPE -> "text/csv", "Content-Disposition" -> data._2);
                    }
                    case Right(error) => handleFailure(Right(error), tags)
                  }

                }
              }
            }
            case Right(errors) => Future {
              handleFailure(Left(errors), tags)
            }
          }
      }
    }
  }


  def importTranslation = Action.async(parse.multipartFormData) { req =>

    Helper.getAndValidatImportRequest(req.body.file("csv"), req.body.asFormUrlEncoded.get("language"), req.body.asFormUrlEncoded.get("csv_type")) match {
      case Right(errors) => Future {
        handleFailure(Left(errors), Seq())
      }
      case Left(request) => {
        translationManager.getTranslationMessages(request.translations.map(l => l._1).toSeq, request.language, request.translations).flatMap {
          messages => {
            messages match {
              case Right(errors) => {
                Future {
                  handleFailure(Right(errors), Seq())
                }
              }
              case Left(message) => {
                translationManager.updateMessageConstants(message).map {
                  error => error match {
                    case None => Ok(views.html.main(Seq())(null)((views.html.Succes("CSV file is imported."))));
                    case Some(err) => {
                      handleFailure(Right(err), Seq())
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  def createTranslationsBasedOnProps = Action.async(parse.multipartFormData) { req =>
    Logger.error("tes ghsagdjhdjghasdjkhashd")
    Helper.getAndValidateCreateBasedOnPropsRequest(req.body.file("props"), req.body.asFormUrlEncoded.get("language"), req.body.asFormUrlEncoded.get("tags-props")) match {
      case Right(errors) => Future {
        handleFailure(Left(errors), Seq())
      }
      case Left(translation) => {
        translationManager.createMessageConstants(translation).map {
          errors => {
          errors match{
            case None => Ok(views.html.main(Seq())(null)((views.html.Succes("props file is imported."))))
            case Some(errors)=> handleFailure(Right(errors), Seq())
          }

          }
        }
      }
    }
  }


}


