package domain
import play.api.libs.json.{ JsPath, Reads, Writes }
import play.api.libs.functional.syntax._
object TestDomain {
  case class MessageConstant(key: String, tags: Seq[String], translations: Map[String, String], version: String)
  implicit def messageConstantReads: Reads[MessageConstant] = (
    (JsPath \ "key").read[String] and
    (JsPath \ "tags").read[Seq[String]] and
    (JsPath \ "translations").read[Map[String, String]] and
    (JsPath \ "version").read[String])(MessageConstant.apply _)
  implicit val messageConstantWrites: Writes[MessageConstant] = (
    (JsPath \ "key").write[String] and
    (JsPath \ "tags").write[Seq[String]] and
    (JsPath \ "translations").write[Map[String, String]] and
    (JsPath \ "version").write[String])(unlift(MessageConstant.unapply))
}
