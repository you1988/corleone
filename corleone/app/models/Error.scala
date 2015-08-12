package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
/**
 * @author ychahbi
 */
object Link {
  case class Link(rel: String, href: String)
  implicit val linkWrites: Writes[Link] = (
    (JsPath \ "rel").write[String] and
    (JsPath \ "href").write[String])(unlift(Link.unapply))
  implicit val linkReads: Reads[Link] = (
    (JsPath \ "rel").read[String] and
    (JsPath \ "href").read[String])(Link.apply _)

}
object Error {
  case class Error(url: String, status: Int, title: String, detail: String, instance: String, _links: Seq[Link.Link] = Seq[Link.Link]())
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
  case class ShortError(title: String, detail: String,_links: Seq[Link.Link] = Seq[Link.Link]())

}
