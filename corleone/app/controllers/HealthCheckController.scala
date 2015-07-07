package controllers


import play.api.mvc._

class HealthCheckController extends Controller {
  
  def heartbeat = Action(Ok)
}
