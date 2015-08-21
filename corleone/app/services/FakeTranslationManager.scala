package services
import models.MessageConstant
import models.Link
import models.Translation
import models.Error
import models.Response
import scala.concurrent.Future
class FakeTranslationManager  {
  def getTranslationMessage(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Future[Option[Response.MsgConstntsResponse]] = {
    throw new NotImplementedError()
  }
  def getIfExist(key: String, languages: Option[Seq[String]]): Future[Option[MessageConstant.MessageConstant]] = {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "10", Seq[String]("pack", "sort"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantOldVersion = MessageConstant.MessageConstant("outbound_pack_message_old_version", "10", Seq[String]("pack", "sort"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer")))
    key match {
      case "outbound_pack_message"             => Some(msgConstant)
      case "outbound_pack_message_old_version" => Some(msgConstantOldVersion)
      case _                                   => None
    }
      throw new NotImplementedError()
  }
  def getIfExistWithKey(key: String): Option[MessageConstant.MessageConstant] = {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "10", Seq[String]("pack", "sort"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantOldVersion = MessageConstant.MessageConstant("outbound_pack_message_old_version", "10", Seq[String]("pack", "sort"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer")))
    key match {
      case "outbound_pack_message"             => Some(msgConstant)
      case "outbound_pack_message_old_version" => Some(msgConstantOldVersion)
      case _                                   => None
    }
  }
  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Option[Error.ShortError] = {
    throw new NotImplementedError()
  }
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant):Future[Option[Error.ShortError]] = {
    throw new NotImplementedError()
  }
  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]] = {
    throw new NotImplementedError()
  }
  def deleteMessageConstant(key: String) = {
    throw new NotImplementedError()
  }
  def getAllTags(): Seq[String]={
 return Seq[String]("PACK","SORT","PICK","STOW")
  }
  def getAllLanguages(): Seq[String]={
   return Seq[String]("de","en","it","fr")
  }
  def getIfExistWithTag(key: String): Option[Seq[MessageConstant.MessageConstant]]={
      throw new NotImplementedError()    
   }
  def getIfExistWithKey2(key: String): Future[Option[Seq[MessageConstant.MessageConstant]]] = {
    throw new NotImplementedError()
  }
  def update2(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
    throw new NotImplementedError()
  }
  def getIfExistWithTag2(tag: String): Future[Option[Seq[MessageConstant.MessageConstant]]] = {
    throw new NotImplementedError()
  }

  def insert(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
    throw new NotImplementedError()
  }

}