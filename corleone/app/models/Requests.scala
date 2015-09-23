package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, JsPath, Writes}

/**
 * Created by ychahbi on 9/21/15.
 */
object Requests {
  case class ExportRequest(tag: String, delimiter: String)
  implicit val searchResponseWrites: Writes[ExportRequest] = (
    (JsPath \ "tag").write[String] and
      (JsPath \ "delimiter").write[String])(unlift(ExportRequest.unapply))
  implicit val searchResponseReads: Reads[ExportRequest] = (
    (JsPath \ "tag").read[String] and
      (JsPath \ "delimiter").read[String])(ExportRequest.apply _)

}
