package models


import java.sql.Timestamp

import helpers.PostgresDriverExtended.api._
import models.LanguageCodes.LanguageCode
import models.Operations.Operation
import slick.lifted.Tag


class TestTable(tag: Tag) extends Table[Test](tag, Some("ts_data"),"test"){
  def id = column[Long]("t_id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("t_name")
  def * = (id.?, name) <> (Test.tupled, Test.unapply _)
}


case class TranslationKeyTable(tag: Tag) extends Table[TranslationKey](tag, Some("ts_data"),"translation_key"){
  def id = column[Long]("tk_id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("tk_name")
  def isActive = column[Boolean]("tk_is_active")
  def created = column[Timestamp]("tk_created")
  def * = (id.?, name, isActive, created) <> (TranslationKey.tupled, TranslationKey.unapply _)
}


case class TranslationMessageTable(tag: Tag) extends Table[TranslationMessage](tag, Some("ts_data"),"translation_message"){
  
  def id = column[Long]("tm_id", O.PrimaryKey, O.AutoInc)
  def languageCode = column[LanguageCode]("tm_language_code")
  def translationKeyId = column[Long]("tm_translation_key_id")
  def value = column[String]("tm_value")
  def isActive = column[Boolean]("tm_is_active")
  def lastModified = column[Timestamp]("tm_last_modified")
  def created = column[Timestamp]("tm_created")
  def * = (id.?, languageCode, translationKeyId, value, isActive, lastModified, created) <> (TranslationMessage.tupled, TranslationMessage.unapply _)

  def translationKey = foreignKey("tm_translation_key_fk", translationKeyId, TableQuery[TranslationKeyTable])(_.id)
}


case class VersionTable(tag: Tag) extends Table[Version](tag, Some("ts_data"),"version") {
  def id = column[Long]("v_id", O.PrimaryKey, O.AutoInc)
  def name= column[String]("v_name")
  def translationKeyId = column[Long]("v_translation_key_id")
  def translationMessageId = column[Long]("v_translation_message_id")
  def performedOperation = column[Operation]("v_performed_operation")
  def lastApply = column[Timestamp]("v_last_apply")
  def created = column[Timestamp]("v_created")
  def * = (id.?, name, translationKeyId, translationMessageId, performedOperation, lastApply, created) <> (Version.tupled, Version.unapply _)

  def translationKey = foreignKey("v_translation_key_fk", translationKeyId, TableQuery[TranslationKeyTable])(_.id)

  def translationMessage = foreignKey("v_translation_message_fk", translationMessageId, TableQuery[TranslationMessageTable])(_.id)

}

case class TagTable(tag: Tag) extends Table[TagHolder](tag, Some("ts_data"),"tag"){
  def id = column[Long]("t_id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("t_name")
  def created = column[Timestamp]("t_created")
  def * = (id.?, name, created) <> (TagHolder.tupled, TagHolder.unapply _)
}


case class TranslationTaggingTable(tag: Tag) extends Table[TranslationTagging](tag, Some("ts_data"),"translation_tagging"){
  def id = column[Long]("tt_id", O.PrimaryKey, O.AutoInc)
  def translationKeyId = column[Long]("tt_translation_key_id")
  def tagId = column[Long]("tt_tag_id")
  def isActive = column[Boolean]("tt_is_active")
  def lastModified = column[Timestamp]("tt_last_modified")
  def created = column[Timestamp]("tt_created")
  def * = (id.?, translationKeyId, tagId, isActive, lastModified, created) <> (TranslationTagging.tupled, TranslationTagging.unapply _)

  def translationKey = foreignKey("tt_translation_key_fk", translationKeyId, TableQuery[TranslationKeyTable])(_.id)
  def tagHolder = foreignKey("tt_tag_fk", tagId, TableQuery[TagTable])(_.id)

}
