package services

import models.Error.ShortError
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http._
import play.api.http.HeaderNames._
import models._

import com.google.inject.ImplementedBy
import scala.concurrent.Future
trait TranslationManage  {
  def getTranslationMessage(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]]
  def getIfExist(key: String, languages: Option[Seq[String]]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]]
  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Either[MessageConstant.MessageConstant, ShortError]]
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Either[MessageConstant.MessageConstant, ShortError]]
  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Either[Seq[MessageConstant.MessageConstant],Error.ShortError]]
  def deleteMessageConstant(key: String):Future[Option[ShortError]]
  def getIfExistWithKey(key: String): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]]
  def getIfExistWithTag(key: String): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]]
  def getTranslationMessages(keys:Seq[String],language:LanguageCodes.LanguageCode,transaltions:Map[String,String]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]]
  def updateMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]]
    def getAllTags(): Future[Either[Seq[String], ShortError]]
  def getAllLanguages(): Seq[String]
  
}