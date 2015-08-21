package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
object MessageConstant {

  case class MessageConstant(key: String, version: String, tags: Seq[String], translations: Seq[Translation.Translation])

  implicit val messageConstantWrites: Writes[MessageConstant] = (
    (JsPath \ "key").write[String] and
    (JsPath \ "version").write[String] and
    (JsPath \ "tags").write[Seq[String]] and
    (JsPath \ "translations").write[Seq[Translation.Translation]])(unlift(MessageConstant.unapply))
  implicit val messageConstantReads: Reads[MessageConstant] = (
    (JsPath \ "key").read[String] and
    (JsPath \ "version").read[String] and
    (JsPath \ "tags").read[Seq[String]] and
    (JsPath \ "translations").read[Seq[Translation.Translation]])(MessageConstant.apply _)
}
object Translation {
  case class Translation(languageCode: String, message: String)
 val languageCodeValidator: Reads[String] = 
    Reads.StringReads.filter(ValidationError("Invalid Language Code!"))(str => {
        str.matches("""[a-z]{2}-[A-Z]{2}|[A-Z]{2}""")
    })
  implicit val translationWrites: Writes[Translation] = (
    (JsPath \ "languageCode").write[String] and
    (JsPath \ "message").write[String])(unlift(Translation.unapply))
  implicit val translationReads: Reads[Translation] = (
    (JsPath \ "languageCode").read[String](languageCodeValidator) and
    (JsPath \ "message").read[String])(Translation.apply _  )

} 