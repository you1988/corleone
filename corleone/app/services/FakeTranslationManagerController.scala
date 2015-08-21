package services
import play.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import models.MessageConstant
import models.Link
import models.Translation
import models.Error
import models.Response
import helpers.PostgresDriverExtended.api._
import models.Tables
import play.api.Application
import slick.jdbc.meta.MTable
import slick.dbio
import dao.TagDAO
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import dao.TranslationKeyDAO
import java.util.concurrent.TimeoutException
import scala.util.{ Failure, Success }
import models.Tables.TranslationKeyTable
import dao.TranslationMessageDAO
import models.{ TranslationKey, TranslationMessage, TranslationTagging, TagHolder, LanguageCodes, Tables }
import helpers.PostgresDriverExtended.api._
import scala.concurrent.Future
import java.sql.Timestamp
import java.time.LocalDateTime
import models.TranslationMessage
import models.Translation
import models.TagHolder
import akka.dispatch.OnFailure
import com.zaxxer.hikari.{ HikariDataSource, HikariConfig }
import javax.inject._
class FakeTranslationManagerController()  {
  private val translationKeyTable = Tables.translationKey
  private val db=Database.forConfig("mydb")
  private val translationMessageTable = Tables.translationMessage
  var col: Seq[MessageConstant.MessageConstant] = Seq[MessageConstant.MessageConstant]()
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
    col = col map { x => if (x.key.equals(messageConstant.key)) messageConstant else x }
    None
  }
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]]= {
    col = col :+ messageConstant
    None
    throw new NotImplementedError()
  }
  def getIfExistWithTag(key: String): Option[Seq[MessageConstant.MessageConstant]] = {
    Some(col)
  }

  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]] = {
    throw new NotImplementedError()
  }
  def deleteMessageConstant(key: String) = {
    throw new NotImplementedError()
  }
  def getAllTags(): Seq[String] = {
    return Seq[String]("PACK", "SORT", "PICK", "STOW")
  }
  def getAllLanguages(): Seq[String] = {
    return Seq[String]("de", "en", "it", "fr")
  }

//  def getIfExistWithKey2(key: String): Future[Option[Seq[MessageConstant.MessageConstant]]] = {
//    val innerJoin = (for {
//      translationkey <- Tables.translationKey if translationkey.name === key
//      translationMessage <- Tables.translationMessage if translationkey.id === translationMessage.translationKeyId
//      translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId
//      tag <- Tables.tag if tag.id === translationTagging.tagId
//    } yield {
//
//      (translationkey.name, (translationMessage.languageCode, translationMessage.value), tag.name)
//
//    })
//    val t = innerJoin.result
//
//      val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String)]] = db.run(t)
//      transaction.map(res => {
//        val keys = res.map(row => row._1).distinct
//        val messages: Seq[MessageConstant.MessageConstant] = keys.map(key => {
//          val trans = res.filter(x => x._1 == key).map(row => row._2).distinct.map(s => Translation.Translation(s._1.toString(), s._2))
//          val tags = res.filter(x => x._1 == key).map(row => row._3).distinct
//          MessageConstant.MessageConstant(key, "tes", tags, trans)
//        })
//        if (messages == null || messages.isEmpty)
//          None
//        else Some(messages)
//
//      }).recover {
//        case ex: Exception => {
//          Logger.error(ex.getMessage)
//          None
//        }
//      }
//  }
//
//  def update2(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
//    val innerJoin = (for {
//      //      translationKeyId <- for { translationkey <- Tables.translationKey if translationkey.name === messageConstant.key } yield translationkey.id
//      translationkey2 <- Tables.translationKey.filter(_.name === messageConstant.key).result.headOption
//      _ <- Tables.translationMessage.filter(_.translationKeyId === translationkey2.get.id.get).delete
//      _ <- Tables.translationTagging.filter(_.translationKeyId === translationkey2.get.id.get).delete
//      translation <- (Tables.translationMessage returning Tables.translationMessage.map(_.id)) ++= (messageConstant.translations map { translation => TranslationMessage(None, LanguageCodes.withName(translation.languageCode), translationkey2.get.id.get, translation.message, true, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())) })
//      tagsExist <- Tables.tag.filter(_.name inSet messageConstant.tags).result
//      tagsCreated <- (Tables.tag returning Tables.tag.map(_.id)) ++= (messageConstant.tags.filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
//        TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
//      })
//      tags <- Tables.tag.filter(_.name inSet messageConstant.tags).result
//      _ <- Tables.translationTagging ++= tags.map { tahHolder =>
//        TranslationTagging(None,
//          tahHolder.id.get,
//          translationkey2.get.id.get,
//          true,
//          Timestamp.valueOf(LocalDateTime.now()),
//          Timestamp.valueOf(LocalDateTime.now()))
//      }
//
//      //      translationMessage <-Tables.translationMessage.filter(translationkey.id === _.translationKeyId).result
//      //      translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId
//      //
//      //      tag <- Tables.tag if tag.id === translationTagging.tagId
//    } yield ()).transactionally
//    val t = innerJoin
//
//      val transaction: Future[Unit] = db.run(t)
//      transaction.map {
//        ex => None
//      }.recover {
//        case ex: Exception => {
//          Logger.error(ex.getMessage)
//          Some(Error.ShortError("test333", "test3333"))
//        }
//      }
//  }
//
//  def getIfExistWithTag2(tag: String): Future[Option[Seq[MessageConstant.MessageConstant]]] = {
//    val innerJoin = (for {
//      (translationTaggings, tags) <- Tables.translationTagging join Tables.tag on (_.tagId === _.id) if tags.name === tag
//      translationkey <- Tables.translationKey if translationkey.id === translationTaggings.translationKeyId
//      translationMessage <- Tables.translationMessage if translationkey.id === translationMessage.translationKeyId
//      translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId
//      tag <- Tables.tag if tag.id === translationTagging.tagId
//    } yield {
//
//      (translationkey.name, (translationMessage.languageCode, translationMessage.value), tag.name)
//
//    })
//    val t = innerJoin.result
//
//      val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String)]] = db.run(t)
//      transaction.map(res => {
//        val keys = res.map(row => row._1).distinct
//        val messages: Seq[MessageConstant.MessageConstant] = keys.map(key => {
//          val trans = res.filter(x => x._1 == key).map(row => row._2).distinct.map(s => Translation.Translation(s._1.toString(), s._2))
//          val tags = res.filter(x => x._1 == key).map(row => row._3).distinct
//          MessageConstant.MessageConstant(key, "tes", tags, trans)
//        })
//        if (messages == null || messages.isEmpty)
//          None
//        else Some(messages)
//
//      }).recover {
//        case ex: Exception => {
//          Logger.error(ex.getMessage)
//          None
//        }
//      }
//
//  }
//
//  def insert(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
//    var transaltionMessages: Seq[TranslationMessage] = Seq[TranslationMessage]()
//    var tags: Seq[TagHolder] = Seq[TagHolder]()
//    messageConstant.translations.foreach {
//      tran => Logger.error(tran.toString())
//    }
//    val translationKey = TableQuery[TranslationKeyTable]
//    val translationky = TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now()))
//    val tagTable = TableQuery[Tables.TagTable]
//    val translationTaggingTable = TableQuery[Tables.TranslationTaggingTable]
//
//    val translationMessage = Tables.translationMessage
//
//    val transaction = (for {
//      msg <- (translationKey returning translationKey.map(_.id) into ((translationky, id) => translationky.copy(id = Some(id)))) += translationky
//      translation <- translationMessage ++= (messageConstant.translations map { translation => TranslationMessage(None, LanguageCodes.withName(translation.languageCode), msg.id.get, translation.message, true, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())) })
//      tagsExist <- tagTable.filter(_.name inSet messageConstant.tags).result
//      tagsCreated <- tagTable ++= (messageConstant.tags.filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
//        TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
//      })
//      tags <- tagTable.filter(_.name inSet messageConstant.tags).result
//      _ <- translationTaggingTable ++= tags.map { tahHolder =>
//        TranslationTagging(None,
//          tahHolder.id.get,
//          msg.id.get,
//          true,
//          Timestamp.valueOf(LocalDateTime.now()),
//          Timestamp.valueOf(LocalDateTime.now()))
//      }
//
//    } yield {
//
//    }).transactionally
//      val f: Future[Unit] = db.run(transaction)
//      f.map {
//        ex => None
//      }.recover {
//        case ex: Exception => {
//          Logger.error(ex.getMessage)
//          Some(Error.ShortError("test333", "test3333"))
//        }
//      }
//
//  }
}