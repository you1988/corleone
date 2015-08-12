package controllers
import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future;
import services.TranslationManageImpl;
import com.google.inject._
import com.google.inject.name._
import modules.CustomModule
import models.Error
import play.api.libs.json._

object Global extends play.api.GlobalSettings  {
  
  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest(Json.toJson(Error.Error(request.uri, 400, "Bad query parameters", error, request.uri))))
  }

}
