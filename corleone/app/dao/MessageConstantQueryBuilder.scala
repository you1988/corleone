/*
 * Copyright [2015] Zalando SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dao

import java.sql.Timestamp
import java.time.LocalDateTime

import helpers.PostgresDriverExtended.api._
import models.{LanguageCodes, MessageConstant, Operations, Tables, TagHolder, TranslationKey, TranslationMessage, TranslationTagging, Version}
import slick.dbio
import slick.dbio.Effect.{Read, Write}
import slick.dbio.NoStream

import scala.concurrent.ExecutionContext.Implicits._

/**
 * Responsible of building the different query needed for the translation service.o
 */
object MessageConstantQueryBuilder {
  /**
   * Query to get messages constant.
   * @return query for getting all message constants active.
   */
  def buildSelectAllMessageConstantQuery():Query[(Rep[String], (Rep[LanguageCodes.Value], Rep[String]), Rep[String], Rep[String]), (String, (LanguageCodes.Value, String), String,String), Seq]={
    (for {
      translationkey <- Tables.translationKey if translationkey.isActive
      translationMessage <- Tables.translationMessage if translationkey.id === translationMessage.translationKeyId && translationMessage.isActive
      translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId && translationTagging.isActive
      tag <- Tables.tag if tag.id === translationTagging.tagId
      version<- Tables.version if version.translationKeyId===translationkey.id


    } yield {

        (translationkey.name, (translationMessage.languageCode, translationMessage.value), tag.name,version.name)

      })
  }

  /**
   * Query to insert message constant.
   * @param messageConstant To be inserted.
   * @return Action for inserting the message constant.
   */

  def buildInsertMessageConstantAction(messageConstant: MessageConstant.MessageConstant): dbio.DBIOAction[Unit, NoStream, Write with Write with Read with Write with Read with Write with Write with Write with Write with Read with Write with Read with Write with Write] = {
    val translationKey = TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now()))

    (for {
      msg <- (Tables.translationKey
        returning Tables.translationKey.map(_.id)
        into ((translationKey, id) => translationKey.copy(id = Some(id)))) += translationKey
      translation <- Tables.translationMessage ++= (messageConstant.translations map { translation =>
        TranslationMessage(None,
          LanguageCodes.withName(translation.languageCode),
          msg.id.get,
          translation.message,
          true,
          Timestamp.valueOf(LocalDateTime.now()),
          Timestamp.valueOf(LocalDateTime.now()))
      })

      tagsAlreadyExist <- Tables.tag.filter(_.name inSet messageConstant.tags).result

      tagsCreated <- Tables.tag ++= (messageConstant.tags.filter(tag =>
        !tagsAlreadyExist.exists(x => tag == x.name)) map { tag =>
        TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
      })

      tags <- Tables.tag.filter(_.name inSet messageConstant.tags).result
      _ <- Tables.translationTagging ++= tags.map { tahHolder =>
        TranslationTagging(None,
          tahHolder.id.get,
          msg.id.get,
          true,
          Timestamp.valueOf(LocalDateTime.now()),
          Timestamp.valueOf(LocalDateTime.now()))
      }

    } yield {

      })

  }

  /**
   * Query to get all messages constants with all their translations and tags that are taged by tag.
   * @return Query to fetch all message constants with tag.
   */
def buildSelectAllMessagesConstantsWithTagQuery(tag:String): Query[(Rep[String], (Rep[LanguageCodes.Value], Rep[String]), Rep[String], Rep[String]), (String, (LanguageCodes.Value, String), String,String), Seq] ={
  (for {
    (translationTaggings, tags) <- Tables.translationTagging join Tables.tag on (_.tagId === _.id) if tags.name === tag && translationTaggings.isActive
    translationkey <- Tables.translationKey if translationkey.id === translationTaggings.translationKeyId && translationkey.isActive
    translationMessage <- Tables.translationMessage if translationkey.id === translationMessage.translationKeyId && translationMessage.isActive
    translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId && translationTagging.isActive
    tag <- Tables.tag if tag.id === translationTagging.tagId
    version <- Tables.version if version.translationKeyId === translationkey.id
  } yield {

      (translationkey.name, (translationMessage.languageCode, translationMessage.value), tag.name, version.name)

    })
}
  /**
   * Query to delete the message Constant.
   * @param key of message contsnta to delete.
   * @return An sql delete of a row
   */
  def buildDeleteMessageConstantAction(key: String): dbio.DBIOAction[Unit, NoStream, Read with Write with Write with Write] =
    (for {
      translationkey <- Tables.translationKey.filter(translationKey =>
        (translationKey.name === key &&
          translationKey.isActive))
        .result
        .headOption
      _ <- Tables.translationMessage.filter(translationMessage =>
        (translationMessage.translationKeyId === translationkey.get.id.get &&
          translationMessage.isActive))
        .map(_.isActive)
        .update(false)
      _ <- Tables.translationTagging.filter(translationTagging =>
        (translationTagging.translationKeyId === translationkey.get.id.get &&
          translationTagging.isActive))
        .map(_.isActive)
        .update(false)
      _ <- Tables.translationKey.filter(translationKey =>
        (translationKey.name === key &&
          translationKey.isActive))
        .map(_.isActive)
        .update(false)
    } yield ())
  /**
   * Query to delete the message Constant.
   * @param keys of message contsnta to delete.
   * @return An sql delete of a row
   */
  def buildDeleteMessagesConstantAction(keys: Seq[String]): dbio.DBIOAction[Unit, NoStream, Read with Write with Write with Write] =
    (for {
      translationkey <- Tables.translationKey.filter(translationKey =>
        (translationKey.isActive && (translationKey.name inSet keys)
          ))
        .result
        .headOption
      _ <- Tables.translationMessage.filter(translationMessage =>
        (translationMessage.translationKeyId === translationkey.get.id.get &&
          translationMessage.isActive))
        .map(_.isActive)
        .update(false)
      _ <- Tables.translationTagging.filter(translationTagging =>
        (translationTagging.translationKeyId === translationkey.get.id.get &&
          translationTagging.isActive))
        .map(_.isActive)
        .update(false)
      _ <- Tables.translationKey.filter(translationKey =>
        (translationKey.isActive && (translationKey.name inSet keys)))
        .map(_.isActive)
        .update(false)
    } yield ())

  /**
   * Slick action to create multiple message constants in one transaction.
   * @param messageConstants To be created
   * @return Action to create message constants
   */
def buildCreateMessageContants(messageConstants: Seq[MessageConstant.MessageConstant]):dbio.DBIOAction[Unit, NoStream, Write with Write with Read with Write with Read with Write with Write]={
  val translationKeys: Seq[TranslationKey] = messageConstants.map(messageConstant => TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now())))
  (for {
    msgKey <- (Tables.translationKey returning Tables.translationKey.map(_.id) into ((translationKey, id) => (translationKey.name, id))) ++= translationKeys
    msgkeyMap = msgKey.toMap
    translation <- Tables.translationMessage ++= (messageConstants.flatMap(messageConstant =>
      messageConstant.translations map { translation =>
        TranslationMessage(None,
          LanguageCodes.withName(translation.languageCode),
          msgkeyMap.get(messageConstant.key).get,
          translation.message,
          true,
          Timestamp.valueOf(LocalDateTime.now()),
          Timestamp.valueOf(LocalDateTime.now()))
      }))
    tagsExist <- Tables.tag.filter(_.name inSet messageConstants.flatMap(messageConstant => messageConstant.tags)).result
    tagsCreated <- Tables.tag ++= (messageConstants.flatMap(messageConstant => messageConstant.tags).filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
      TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
    })
    tags <- Tables.tag.filter(_.name inSet messageConstants.flatMap(messageConstant => messageConstant.tags)).result
    tagMap = tags.map(tag => tag.name -> tag.id.get).toMap
    _ <- Tables.translationTagging ++= messageConstants.flatMap(messageConstant => messageConstant.tags.map { tag =>
      TranslationTagging(None,
        tagMap.get(tag).get,
        msgkeyMap.get(messageConstant.key).get,
        true,
        Timestamp.valueOf(LocalDateTime.now()),
        Timestamp.valueOf(LocalDateTime.now()))
    })
    _<- Tables.version  ++= msgKey.map(el => Version(None, el._1, el._2, Operations.CREATED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())))

  } yield {
    })
}
  /**
   * Slick action to create multiple message constants in one transaction.
   * @param messageConstants To be created
   * @return Action to create message constants
   */
  def buildUpdateMessageContants(messageConstants: Seq[MessageConstant.MessageConstant]):dbio.DBIOAction[Unit, NoStream, Write with Write with Read with Write with Read with Write with Write]={
    val translationKeys: Seq[TranslationKey] = messageConstants.map(messageConstant => TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now())))
    (for {
      msgKey <- (Tables.translationKey returning Tables.translationKey.map(_.id) into ((translationKey, id) => (translationKey.name, id))) ++= translationKeys
      msgkeyMap = msgKey.toMap
      translation <- Tables.translationMessage ++= (messageConstants.flatMap(messageConstant =>
        messageConstant.translations map { translation =>
          TranslationMessage(None,
            LanguageCodes.withName(translation.languageCode),
            msgkeyMap.get(messageConstant.key).get,
            translation.message,
            true,
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(LocalDateTime.now()))
        }))
      tagsExist <- Tables.tag.filter(_.name inSet messageConstants.flatMap(messageConstant => messageConstant.tags)).result
      tagsCreated <- Tables.tag ++= (messageConstants.flatMap(messageConstant => messageConstant.tags).filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
        TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
      })
      tags <- Tables.tag.filter(_.name inSet messageConstants.flatMap(messageConstant => messageConstant.tags)).result
      tagMap = tags.map(tag => tag.name -> tag.id.get).toMap
      _ <- Tables.translationTagging ++= messageConstants.flatMap(messageConstant => messageConstant.tags.map { tag =>
        TranslationTagging(None,
          tagMap.get(tag).get,
          msgkeyMap.get(messageConstant.key).get,
          true,
          Timestamp.valueOf(LocalDateTime.now()),
          Timestamp.valueOf(LocalDateTime.now()))
      })
      _<- Tables.version  ++= msgKey.map(el => Version(None, el._1, el._2, Operations.MODIFIED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())))

    } yield {
      })
  }
  /**
   * Build an insert query for versioning the creation, the update or the delete  of message constant.
   * @param key of the created Message constant.
   * @return An sql insert of a row in ts_data.verion table.
   */

  def buildInsertVersioning(key: String, operation: Operations.Operation): dbio.DBIOAction[String, NoStream, Write with Read] =
    (for {
      translationkey <- Tables.translationKey.filter(translationKey => (translationKey.name === key && translationKey.isActive)).result.headOption
      version <- Tables.version returning Tables.version.map(_.name) += Version(None, translationkey.get.name, translationkey.get.id.get, operation, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()))
    } yield {
        translationkey.get.name
      })



}


