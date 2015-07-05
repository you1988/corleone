package models

import org.joda.time.LocalDateTime



case class Test(name: String)


case class TranslationKey(id: Option[Int],
                          name: String,
                          isActive: Boolean,
                          created:LocalDateTime)

class Locale extends Enumeration {
  type Locale = Value
  val EN_GB, EN_US, DE_DE = Value
}

case class TranslationMessage(id: Option[Int],
                              name: String,
                              translationKey: TranslationKey,
                              isActive: Boolean,
                              lastModified:LocalDateTime,
                              created:LocalDateTime)

class Operation extends Enumeration {
  type Operation = Value
  val MODIFIED, DELETE = Value
}

case class Version(id: Option[Int],
                   name: String,
                   translationKey: TranslationKey,
                   translationMessage: TranslationMessage,
                   performedOperation: Operation,
                   lastApply:LocalDateTime,
                   created:LocalDateTime)

case class Tag(id: Option[Int],
                          name: String,
                          created:LocalDateTime)

case class TranslationTagging(id: Option[Int],
                              name: String,
                              created:LocalDateTime)