package services

import models.Error.ShortError

import models._

import helpers.PostgresDriverExtended.api._
import scala.concurrent.Future

class FakeTranslationManager() {
  private val translationKeyTable = Tables.translationKey
  private val db=Database.forConfig("mydb")
  private val translationMessageTable = Tables.translationMessage
  var col: Seq[MessageConstant.MessageConstant] = Seq[MessageConstant.MessageConstant]()
  def getTranslationMessage(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    throw new NotImplementedError()
  }
  def getIfExist(key: String, languages: Option[Seq[String]]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    throw new NotImplementedError()
  }
  def getIfExistWithKey(key: String): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    throw new NotImplementedError()
  }
  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Either[MessageConstant.MessageConstant, ShortError]] = {
    throw new NotImplementedError()
  }
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Either[MessageConstant.MessageConstant, ShortError]]= {
    throw new NotImplementedError()
  }
  def getIfExistWithTag(key: String): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    throw new NotImplementedError()
  }

  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]] = {
    throw new NotImplementedError()
  }
  def deleteMessageConstant(key: String) = {
    throw new NotImplementedError()
  }
  def getAllTags(): Future[Either[Seq[String], ShortError]]= {
    throw new NotImplementedError()
  }
  def getAllLanguages(): Seq[String] = {
    return Seq[String]("de", "en", "it", "fr")
  }
  def getTranslationMessages(keys:Seq[String],language:LanguageCodes.LanguageCode,transaltions:Map[String,String]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    throw new NotImplementedError()
  }
  def updateMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]]= {
    throw new NotImplementedError()
  }
  }