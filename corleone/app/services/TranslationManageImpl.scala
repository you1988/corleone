package services
import models.MessageConstant
import models.Link
import models.Translation
import models.Error
import models.Response


class TranslationManageImpl extends TranslationManage {
  def getTranslationMessage(languageCodes: Option[Seq[String]], tags: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Seq[MessageConstant.MessageConstant] = {
    throw new NotImplementedError()
  }
  def getIfExist(key: String, languages: Seq[String]): Option[MessageConstant.MessageConstant] = {
    throw new NotImplementedError()
  }
  def getIfExistWithKey(key: String): Option[MessageConstant.MessageConstant] = {
    throw new NotImplementedError()
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