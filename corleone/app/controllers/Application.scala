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

import javax.inject.{Singleton, _}

import models.Error.ShortError
import models.{NotHandledError, MessageConstantViolatedConstraintError, NotFoundError, LanguageCodes}
import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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
                    case Right(err) => handleFailure(Right(err),tags)
                  }
                }
              }
              case Some(Left(key)) => {
                translationManager.getIfExistWithKey(key).map { message =>
                  message match {
                    case Left(messages) => Ok(views.html.main(tags)(views.html.translationSearchView(messages))(null))
                    case Right(err) => handleFailure(Right(err),tags)
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
        case Right(err) =>Ok(views.html.main(Seq())(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(null))
        case Left(tags) => Ok(views.html.main(tags)(views.html.TranslationCreationForm(translationManager.getAllLanguages()))(null));
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
            case Right(errors) =>handleFailure(Left(errors),tags)
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
                  case Right(error) => handleFailure(Right(error),tags)

                }

              }
              case Right(errors) => Future {
                handleFailure(Left(errors),tags)
              }
            }
        }
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
          Logger.error("Validate")
          Helper.validatCreateRequest(map) match {

            case Left(message) => {
              Logger.error("Im before")
              translationManager.createMessageConstant(message).map {
                messages => {

                  messages match {

                    case Left(message) => Ok(views.html.main(tags)(null)(views.html.Succes("Message Created")));
                    case Right(error) => handleFailure(Right(error),tags)
                  }

                }
              }
            }
            case Right(errors) => Future {
              handleFailure(Left(errors),tags)
            }
          }
      }
    }
  }
def handleFailure(error:Either[Seq[String],ShortError],tags:Seq[String]):Result={
error match {
  case Right(shortError) => shortError match {
    case err : MessageConstantViolatedConstraintError => Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("There is already a message with the specific key"))))
    case err: NotFoundError =>Ok(views.html.main(tags)(null)(null))
    case err : NotHandledError =>Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("I do not know what happens. Could you please report the error?"))))
    case _ =>Ok(views.html.main(tags)(null)(views.html.Error(Seq[String]("The data source system is down wait a moment and try again.\n Could you please report the error in case it happens frequently? "))))

  }
  case Left(errors) =>  Ok(views.html.main(tags)(null)(views.html.Error(errors)))
}
}
}


