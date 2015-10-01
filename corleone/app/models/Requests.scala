package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, JsPath, Writes}

import scala.reflect.internal.pickling.Translations

/**
 * Created by ychahbi on 9/21/15.
 */
object Requests {
  case class ExportRequest(tag: String, delimiter: String,csvEncoding: String)
  case class ImportRequest(language: LanguageCodes.LanguageCode, translations: Map[String,String])

}
