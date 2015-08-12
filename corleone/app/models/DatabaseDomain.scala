package models

import java.sql.Timestamp

import models.LanguageCodes.LanguageCode
import models.Operations.Operation

case class Test(id: Option[Long], name: String)

case class TranslationKey(id: Option[Long],
                          name: String,
                          isActive: Boolean,
                          created:Timestamp)

object LanguageCodes extends Enumeration {
  type LanguageCode = Value
  val EN_GB, EN_US, DE_DE = Value
}

case class TranslationMessage(id: Option[Long],
                              languageCode: LanguageCode,
                              translationKeyId: Long,
                              value: String,
                              isActive: Boolean,
                              lastModified:Timestamp,
                              created:Timestamp)

object Operations extends Enumeration {
  type Operation = Value
  val CREED, MODIFIED, DELETED = Value
}

case class Version(id: Option[Long],
                   name: String,
                   translationKeyId: Long,
                   translationMessageId: Long,
                   performedOperation: Operation,
                   lastApply:Timestamp,
                   created:Timestamp)

case class TagHolder(id: Option[Long],
                          name: String,
                          created:Timestamp)

case class TranslationTagging(id: Option[Long],
                              tagId: Long,
                              translationKeyId: Long,
                              isActive: Boolean,
                              lastModified:Timestamp,
                              created:Timestamp)