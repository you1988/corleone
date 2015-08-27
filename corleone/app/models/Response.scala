package models
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Response {
  case class SearchResponse(messageConstants: Seq[MessageConstant.MessageConstant], _links: Seq[Link.Link])
  implicit val searchResponseWrites: Writes[SearchResponse] = (
    (JsPath \ "messageConstants").write[Seq[MessageConstant.MessageConstant]] and
    (JsPath \ "_links").write[Seq[Link.Link]])(unlift(SearchResponse.unapply))
  implicit val searchResponseReads: Reads[SearchResponse] = (
    (JsPath \ "messageConstants").read[Seq[MessageConstant.MessageConstant]] and
    (JsPath \ "_links").read[Seq[Link.Link]])(SearchResponse.apply _)
  case class MessageConstantResponse(messageConstants: MessageConstant.MessageConstant, _links: Seq[Link.Link])
  implicit val messageConstantResponseWrites: Writes[MessageConstantResponse] = (
    (JsPath \ "messageConstant").write[MessageConstant.MessageConstant] and
    (JsPath \ "_links").write[Seq[Link.Link]])(unlift(MessageConstantResponse.unapply))
  implicit val messageConstantResponseReads: Reads[MessageConstantResponse] = (
    (JsPath \ "messageConstant").read[MessageConstant.MessageConstant] and
    (JsPath \ "_links").read[Seq[Link.Link]])(MessageConstantResponse.apply _)


}
