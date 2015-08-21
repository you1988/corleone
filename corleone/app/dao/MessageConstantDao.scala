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
//import models._
////{ TranslationKey, TranslationMessage, Operations, TranslationTagging, TagHolder, Version, LanguageCodes, Tables }
//import java.sql.Timestamp
//import java.time.LocalDateTime
import models.MessageConstant
import helpers.PostgresDriverExtended.api._
import models.Tables.TranslationKeyTable
import play.api._
import scala.concurrent.ExecutionContext.Implicits._
import slick.dbio.{NoStream,Effect}
import models.{ TranslationKey, TranslationMessage, LanguageCodes, Tables }
import scala.concurrent.Future
import java.sql.Timestamp
import java.time.LocalDateTime
import models.TranslationMessage
import javax.inject._
import models.{ TranslationKey, TranslationMessage, Operations, TranslationTagging, TagHolder, Version, LanguageCodes, Tables }
import scala.concurrent.Future
import java.sql.Timestamp
import java.time.LocalDateTime
import models.TranslationMessage
import models.Translation
import models.TagHolder
import akka.dispatch.OnFailure
import com.zaxxer.hikari.{ HikariDataSource, HikariConfig }
import javax.inject._

/**
 * Responsible of building the different query needed for the translation service.o
 */
 object MessageConstantQueryBuilder {
  /**
   * Build the query to insert the message Constant.
   */
  
  def insertMessageConstantQuery(messageConstant:MessageConstant.MessageConstant)={
     val translationKey = TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now()))
  
    val messageInsertionQuery =(for {
      msg <- (Tables.translationKey returning Tables.translationKey.map(_.id) into ((translationKey, id) => translationKey.copy(id = Some(id)))) += translationKey
      translation <- Tables.translationMessage ++= (messageConstant.translations map { translation => TranslationMessage(None, LanguageCodes.withName(translation.languageCode), msg.id.get, translation.message, true, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())) })
      tagsAlreadyExist <- Tables.tag.filter(_.name inSet messageConstant.tags).result
      tagsCreated <- Tables.tag ++= (messageConstant.tags.filter(tag => !tagsAlreadyExist.exists(x => tag == x.name)) map { tag =>
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
      _ <- Tables.version += Version(None, msg.name, msg.id.get, Operations.CREATED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()))

    } yield {

    })
    messageInsertionQuery.andThen( messageInsertionQuery)
  }
}