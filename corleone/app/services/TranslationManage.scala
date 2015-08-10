package services
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http._
import play.api.http.HeaderNames._
import models.MessageConstant
import models.Link
import models.Translation
import models.Error
import models.Response

import com.google.inject.ImplementedBy

trait TranslationManage {
  def getTranslationMessage(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Response.MsgConstntsResponse
  def getIfExist(key: String, languages: Seq[String]): Option[MessageConstant.MessageConstant]
  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Option[Error.ShortError]
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Option[Error.ShortError]
  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Option[Error.ShortError]
  def deleteMessageConstant(key: String)
  def getIfExistWithKey(key: String): Option[MessageConstant.MessageConstant]
}