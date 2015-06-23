package controllers

import play.api._
import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Logger.debug("start page was opened")
    Ok(views.html.index("Corleone is ready ;-)"))
  }

}
