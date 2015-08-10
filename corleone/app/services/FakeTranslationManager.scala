package services
import models.MessageConstant
import models.Link
import models.Translation
import models.Error
import models.Response
class FakeTranslationManager extends TranslationManage {
  def getTranslationMessage(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Seq[MessageConstant.MessageConstant] = {
    throw new NotImplementedError()
  }
  def getIfExist(key: String, languages: Seq[String]): Option[MessageConstant.MessageConstant] = {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "10", Seq[String]("pack", "sort"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantOldVersion = MessageConstant.MessageConstant("outbound_pack_message_old_version", "10", Seq[String]("pack", "sort"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer")))
    key match {
      case "outbound_pack_message"             => Some(msgConstant)
      case "outbound_pack_message_old_version" => Some(msgConstantOldVersion)
      case _                                   => None
    }
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
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Option[Error.ShortError] = {
    throw new NotImplementedError()
  }
  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Option[Error.ShortError] = {
    throw new NotImplementedError()
  }
  def deleteMessageConstant(key: String) = {
    throw new NotImplementedError()
  }

}