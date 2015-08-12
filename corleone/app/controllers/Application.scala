package controllers

import play.api.mvc._


class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Corleone is ready"))
  }

}
