package models

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.Constants

object MessageConstant {
  /**
   * Validator to ensure that field is not empty.
   */
  def NotEmptySeqValidator(field: String): Reads[Seq[String]] =
    Reads.filter(ValidationError(Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_EMPTY_FIELD_ERROR_MESSAGE.stripMargin.format(field)))(fieldValue => {
      fieldValue != null && !fieldValue.isEmpty
    })
  /**
   * Validator to ensure that field is not empty.
   */
  def NotEmptyStringValidator(field: String): Reads[String] =
    Reads.StringReads.filter(ValidationError(
      Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_EMPTY_FIELD_ERROR_MESSAGE.stripMargin.format(field)
    ))(fieldValue => {
      fieldValue != null && !fieldValue.isEmpty
    })
  /**
   * Validator to ensure that field match this regex [a-z_A-Z0-9]*.
   */
  def StringValuesFormatValidator(field: String): Reads[String] =
    Reads.StringReads.filter(ValidationError(
      Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_FIELD_FORMAT_UNSUPORTED_ERROR_MESSAGE.stripMargin.format(field)
    ))(fieldValue => {
      field.matches( """[a-z_A-Z0-9]*""")
    })

  /**
   * Validator to ensure that values of the field match this regex [a-z_A-Z0-9]*.
   */
  def SeqValuesFormatValidator(field: String): Reads[Seq[String]] =
    Reads.filter(ValidationError(
      Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_FIELD_FORMAT_UNSUPORTED_ERROR_MESSAGE.stripMargin.format(field)
    ))(fieldValues => {
      fieldValues.exists(str => !str.matches( """[a-z_A-Z0-9]*"""))
    })
  /**
   * Validator to ensure that message constant do not contain duplicate tag.
   */
  val tagsValidator: Reads[Seq[String]] =
    Reads.filter(ValidationError(
      Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_TAGS_ERROR_MESSAGE
    ))(tags => {
      tags.distinct.size == tags.size
    })
  /**
   * Validator to ensure that message constant do not contain multiple translation message for same language.
   */
  val translationValidator: Reads[Seq[Translation.Translation]] =
    Reads.filter(ValidationError(Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_TRANSLATIONS_ERROR_MESSAGE))(translation => {
      translation.groupBy(_.languageCode).keySet.size == translation.size
    })

  /**
   * Validator to ensure that field is not empty.
   */
  val NotEmptyTransaltionsValidator: Reads[Seq[Translation.Translation]] =
    Reads.filter(ValidationError(Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_EMPTY_FIELD_ERROR_MESSAGE.stripMargin.format("translations")))(fieldValue => {
      fieldValue != null && !fieldValue.isEmpty
    })

  implicit val messageConstantWrites: Writes[MessageConstant] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "version").write[String] and
      (JsPath \ "tags").write[Seq[String]] and
      (JsPath \ "translations").write[Seq[Translation.Translation]])(unlift(MessageConstant.unapply))
  implicit val messageConstantReads: Reads[MessageConstant] = (
    (JsPath \ "key").read[String](NotEmptyStringValidator("key").keepAnd(StringValuesFormatValidator("key"))) and
      (JsPath \ "version").read[String] and
      (JsPath \ "tags").read[Seq[String]](NotEmptySeqValidator("tags").keepAnd(SeqValuesFormatValidator("tags")).keepAnd(tagsValidator)) and
      (JsPath \ "translations").read[Seq[Translation.Translation]](NotEmptyTransaltionsValidator.keepAnd(translationValidator)))(MessageConstant.apply _)


  case class MessageConstant(key: String, version: String, tags: Seq[String], translations: Seq[Translation.Translation])

}

object Translation {

  val languageCodeValidator: Reads[String] =
    Reads.StringReads.filter(ValidationError("Invalid Language Code!"))(str => {
      str.matches( """[a-z]{2}-[A-Z]{2}|[A-Z]{2}""")
    })
  val suportedLanguageCodeValidator: Reads[String] =
    Reads.StringReads.filter(ValidationError("Invalid Language Code!"))(languageCode => {
      LanguageCodes.values.map(language => language.toString).contains(languageCode)
    })
  implicit val translationWrites: Writes[Translation] = (
    (JsPath \ "languageCode").write[String] and
      (JsPath \ "message").write[String])(unlift(Translation.unapply))
  implicit val translationReads: Reads[Translation] = (
    (JsPath \ "languageCode").read[String](languageCodeValidator.keepAnd(suportedLanguageCodeValidator)) and
      (JsPath \ "message").read[String](MessageConstant.NotEmptyStringValidator("message")))(Translation.apply _)

  case class Translation(languageCode: String, message: String)


} 