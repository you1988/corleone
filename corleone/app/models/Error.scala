package models

import models.Error.ShortError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.Constants

/**
 * @author ychahbi
 */
object Link {

  implicit val linkWrites: Writes[Link] = (
    (JsPath \ "rel").write[String] and
      (JsPath \ "href").write[String])(unlift(Link.unapply))
  implicit val linkReads: Reads[Link] = (
    (JsPath \ "rel").read[String] and
      (JsPath \ "href").read[String])(Link.apply _)

  case class Link(rel: String, href: String)

}

/**
 * Error to handle the case of a non found message constant.
 * @param detail Full description of the error. Contains the identifier of the not found message.
 */
class NotFoundError(detail: String) extends ShortError(Constants.REQUEST_NOT_FOUND_ERROR_TITLE, detail)

/**
 * Error thrown when there is no database response.
 */
class TimeOutError(detail:String) extends ShortError(Constants.REQUEST_SERVICE_UNAVAILABLE_ERROR_TITLE,  Constants.REQUEST_SERVER_DOWN_ERROR_MESSAGE + detail)

/**
 * Error thrown when the user try to create a message constant violating  the database Constraint.
 * @param detail A description of the violated constraint.
 */
class MessageConstantViolatedConstraintError(detail: String) extends ShortError(Constants.REQUEST_MESSAGE_CONSTANT_ALREADY_EXIST_ERROR_TITLE, detail)

/**
 * Error thrown when there is an expected exception.
 * @param detail Stack of the expected exception.
 */
class NotHandledError(detail: String) extends ShortError(Constants.REQUEST_UNEXPECTED_EXCEPTION_ERROR_TITLE, Constants.REQUEST_UNEXPECTED_EXCEPTION_ERROR_MESSAGE + detail)
/**
 * Error thrown when there is an expected exception.
 * @param detail Stack of the expected exception.
 */
class OutOfDateError(detail: String) extends ShortError(Constants.REQUEST_MESSAGE_CONSTANT_OUT_OF_DATE_ERROR_TITLE, Constants.REQUEST_OUT_OF_DATE_ERROR_MESSAGE + detail)
/**
 * Error thrown when the request is mal formed.
 * @param detail error message.
 */
class MalFormedError(detail: String) extends ShortError(Constants.REQUEST_MAL_FORMED_ERROR_TITLE, detail)


object Error {

  implicit val linkWrites: Writes[Error] = (
    (JsPath \ "url").write[String] and
      (JsPath \ "status").write[Int] and
      (JsPath \ "title").write[String] and
      (JsPath \ "detail").write[String] and
      (JsPath \ "instance").write[String] and
      (JsPath \ "_links").write[Seq[Link.Link]])(unlift(Error.unapply))
  implicit val linkReads: Reads[Error] = (
    (JsPath \ "url").read[String] and
      (JsPath \ "status").read[Int] and
      (JsPath \ "title").read[String] and
      (JsPath \ "detail").read[String] and
      (JsPath \ "instance").read[String] and
      (JsPath \ "_links").read[Seq[Link.Link]])(Error.apply _)

  case class Error(url: String, status: Int, title: String, detail: String, instance: String, _links: Seq[Link.Link] = Seq[Link.Link]())


  /**
   * An intern error to handle different exception.
   * @param title Small description of the error.
   * @param detail Full description of the error.
   * @param _links Different links that can help the user to understand the error.
   */
  case class ShortError(title: String, detail: String, _links: Seq[Link.Link] = Seq[Link.Link]())


}
