package services
import models.MessageConstant
import models.Link
import models.Translation
import models.Error
import models.Response
import helpers.PostgresDriverExtended.api._
import models.Tables
import play.api._
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{ Failure, Success }
import models.Tables.TranslationKeyTable
import models.{ TranslationKey, TranslationMessage, LanguageCodes, Tables }
import helpers.PostgresDriverExtended.api._
import scala.concurrent.Future
import java.sql.Timestamp
import java.time.LocalDateTime
import models.TranslationMessage
import javax.inject._
import models.{ TranslationKey, TranslationMessage, Operations, TranslationTagging, TagHolder, Version, LanguageCodes, Tables }
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

@Singleton
class TranslationManageImpl extends TranslationManage {
  private val db = Database.forConfig("mydb")
  def getTranslationMessage(languageCodesOp: Option[Seq[String]], tagsOp: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Future[Option[Response.MsgConstntsResponse]] = {
    val languages = languageCodesOp.getOrElse[Seq[String]](LanguageCodes.values.toSeq.map { x =>
      x.toString()
    }).map(languageCode =>
      LanguageCodes.withName(languageCode.toUpperCase()))
    val searchquery = languageCodesOp match {
      case None => (for {
        translationkey <- Tables.translationKey join Tables.translationMessage on (_.id === _.translationKeyId) join Tables.translationTagging on (_._1.id === _.translationKeyId) join Tables.tag on (_._2.tagId === _.id) if (translationkey._1._1._2.languageCode inSet languages)
      } yield {
        (translationkey._1._1._1.name, (translationkey._1._1._2.languageCode, translationkey._1._1._2.value), translationkey._2.name)

      })
      case Some(tags) => (for {
        translationkey <- Tables.translationKey join
          Tables.translationMessage on
          (_.id === _.translationKeyId) join
          Tables.translationTagging on
          (_._1.id === _.translationKeyId) join
          Tables.tag on
          (_._2.tagId === _.id) if (translationkey._2.name inSet tags) &&
          (translationkey._1._1._2.languageCode inSet languages)
      } yield {

        (translationkey._1._1._1.name, (translationkey._1._1._2.languageCode, translationkey._1._1._2.value), translationkey._2.name)

      })

    }

    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String)]] = db.run(searchquery.result)
    transaction.map(res => {
      val keys = res.map(row => row._1).distinct
      val messages: Seq[MessageConstant.MessageConstant] = keys.map(key => {
        val trans = res.filter(x => x._1 == key).map(row => row._2).distinct.map(s => Translation.Translation(s._1.toString(), s._2))
        val tags = res.filter(x => x._1 == key).map(row => row._3).distinct
        MessageConstant.MessageConstant(key, "tes", tags, trans)
      })
      if (messages == null || messages.isEmpty)
        None
      else Some(Response.MsgConstntsResponse(messages, messages.size))

    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        None
      }
    }
  }
  def getIfExist(key: String, languageCodes: Option[Seq[String]]): Future[Option[MessageConstant.MessageConstant]] = {
    val searchquery = (for {
      translationkey <- Tables.translationKey join Tables.translationMessage on
        (_.id === _.translationKeyId) join
        Tables.translationTagging on
        (_._1.id === _.translationKeyId) join
        Tables.tag on
        (_._2.tagId === _.id) if (translationkey._1._1._1.name === key) &&
        (translationkey._1._1._2.languageCode inSet
          languageCodes.getOrElse[Seq[String]](LanguageCodes.values.toSeq.map { x =>
            x.toString()
          }).map(languageCode =>
            LanguageCodes.withName(languageCode.toUpperCase())))
    } yield {

      (translationkey._1._1._1.name,
        (translationkey._1._1._2.languageCode, translationkey._1._1._2.value),
        translationkey._2.name)

    })

    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String)]] = db.run(searchquery.result)
    transaction.map(res => {
      val keys = res.map(row => row._1).distinct
      val messages: Seq[MessageConstant.MessageConstant] = keys.map(key => {
        val trans = res.filter(x => x._1 == key).map(row => row._2).distinct.map(s => Translation.Translation(s._1.toString(), s._2))
        val tags = res.filter(x => x._1 == key).map(row => row._3).distinct
        MessageConstant.MessageConstant(key, "tes", tags, trans)
      })
      if (messages == null || messages.isEmpty)
        None
      else Some(messages.head)

    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        None
      }
    }
  }
  def getIfExistWithKey(key: String): Future[Option[MessageConstant.MessageConstant]] = {
    val innerJoin = (for {
      translationkey <- Tables.translationKey if translationkey.name === key
      translationMessage <- Tables.translationMessage if translationkey.id === translationMessage.translationKeyId
      translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId
      tag <- Tables.tag if tag.id === translationTagging.tagId
    } yield {

      (translationkey.name, (translationMessage.languageCode, translationMessage.value), tag.name)

    })
    val t = innerJoin.result

    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String)]] = db.run(t)
    transaction.map(res => {
      val keys = res.map(row => row._1).distinct
      val messages: Seq[MessageConstant.MessageConstant] = keys.map(key => {
        val trans = res.filter(x => x._1 == key).map(row => row._2).distinct.map(s => Translation.Translation(s._1.toString(), s._2))
        val tags = res.filter(x => x._1 == key).map(row => row._3).distinct
        MessageConstant.MessageConstant(key, "tes", tags, trans)
      })
      if (messages == null || messages.isEmpty)
        None
      else Some(messages.head)

    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        None
      }
    }
  }
  //  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
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
  //    } yield ()).transactionally
  //    val t = innerJoin
  //
  //    val transaction: Future[Unit] = db.run(t)
  //    transaction.map {
  //      ex => None
  //    }.recover {
  //      case ex: Exception => {
  //        Logger.error(ex.getMessage)
  //        Some(Error.ShortError("test333", "test3333"))
  //      }
  //    }
  //  }
  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
       val translationky = TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now()))
    val innerJoin = (for {
      translationkey2 <- Tables.translationKey.filter(translationKey=>(translationKey.name === messageConstant.key && translationKey.isActive)).result.headOption
      _ <- Tables.translationMessage.filter(translationMessage=>(translationMessage.translationKeyId === translationkey2.get.id.get && translationMessage.isActive)).map(_.isActive).update(false)
      _ <- Tables.translationTagging.filter(translationTagging=>(translationTagging.translationKeyId === translationkey2.get.id.get&&translationTagging.isActive)).map(_.isActive).update(false)
      _ <- Tables.translationKey.filter(translationKey=>(translationKey.name === messageConstant.key && translationKey.isActive)).map(_.isActive).update(false)
      msg <- (Tables.translationKey returning Tables.translationKey.map(_.id) into ((translationky, id) => translationky.copy(id = Some(id)))) += translationky
      translation <- Tables.translationMessage ++= (messageConstant.translations map { translation => TranslationMessage(None, LanguageCodes.withName(translation.languageCode), msg.id.get, translation.message, true, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())) })
      tagsExist <- Tables.tag.filter(_.name inSet messageConstant.tags).result
      tagsCreated <- Tables.tag ++= (messageConstant.tags.filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
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
      _ <- Tables.version += Version(None, messageConstant.key, msg.id.get, Operations.MODIFIED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()))

    } yield ()).transactionally
    val t = innerJoin

    val transaction: Future[Unit] = db.run(t)
    transaction.map {
      ex => None
    }.recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Some(Error.ShortError("test333", "test3333"))
      }
    }
  }
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Option[Error.ShortError]] = {
    var transaltionMessages: Seq[TranslationMessage] = Seq[TranslationMessage]()
    var tags: Seq[TagHolder] = Seq[TagHolder]()
    messageConstant.translations.foreach {
      tran => Logger.error(tran.toString())
    }
    val translationKey = TableQuery[TranslationKeyTable]
    val translationky = TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now()))
    val tagTable = TableQuery[Tables.TagTable]
    val translationTaggingTable = TableQuery[Tables.TranslationTaggingTable]

    val translationMessage = Tables.translationMessage

    val transaction = (for {
      msg <- (translationKey returning translationKey.map(_.id) into ((translationky, id) => translationky.copy(id = Some(id)))) += translationky
      translation <- translationMessage ++= (messageConstant.translations map { translation => TranslationMessage(None, LanguageCodes.withName(translation.languageCode), msg.id.get, translation.message, true, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())) })
      tagsExist <- tagTable.filter(_.name inSet messageConstant.tags).result
      tagsCreated <- tagTable ++= (messageConstant.tags.filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
        TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
      })
      tags <- tagTable.filter(_.name inSet messageConstant.tags).result
      _ <- translationTaggingTable ++= tags.map { tahHolder =>
        TranslationTagging(None,
          tahHolder.id.get,
          msg.id.get,
          true,
          Timestamp.valueOf(LocalDateTime.now()),
          Timestamp.valueOf(LocalDateTime.now()))
      }
      _ <- Tables.version += Version(None, msg.name, msg.id.get, Operations.CREATED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()))

    } yield {

    }).transactionally
    val f: Future[Unit] = db.run(transaction)
    f.map {
      ex => None
    }.recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Some(Error.ShortError("test333", "test3333"))
      }
    }

  }

  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]] = {
    var transaltionMessages: Seq[TranslationMessage] = Seq[TranslationMessage]()
    var tags: Seq[TagHolder] = Seq[TagHolder]()

    val translationKeys: Seq[TranslationKey] = messageConstants.map(messageConstant => TranslationKey(None, messageConstant.key, true, Timestamp.valueOf(LocalDateTime.now())))

    val tagTable = TableQuery[Tables.TagTable]
    val translationTaggingTable = TableQuery[Tables.TranslationTaggingTable]

    val translationMessage = Tables.translationMessage

    val transaction = (for {
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
      tagsExist <- tagTable.filter(_.name inSet messageConstants.flatMap(messageConstant => messageConstant.tags)).result
      tagsCreated <- tagTable ++= (messageConstants.flatMap(messageConstant => messageConstant.tags).filter(tag => !tagsExist.exists(x => tag == x.name)) map { tag =>
        TagHolder(None, tag, Timestamp.valueOf(LocalDateTime.now()))
      })
      tags <- tagTable.filter(_.name inSet messageConstants.flatMap(messageConstant => messageConstant.tags)).result
      tagMap = tags.map(tag => tag.name -> tag.id.get).toMap
      _ <- translationTaggingTable ++= messageConstants.flatMap(messageConstant => messageConstant.tags.map { tag =>
        TranslationTagging(None,
          tagMap.get(tag).get,
          msgkeyMap.get(messageConstant.key).get,
          true,
          Timestamp.valueOf(LocalDateTime.now()),
          Timestamp.valueOf(LocalDateTime.now()))
      })
      _ <- Tables.version ++= msgKey.map(el => Version(None, el._1, el._2, Operations.CREATED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())))

    } yield {

    }).transactionally
    val f: Future[Unit] = db.run(transaction)
    f.map {
      ex => None
    }.recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Some(Error.ShortError("test333", "test3333"))
      }
    }

  }
  def deleteMessageConstant(key: String) = {
    val innerJoin = (for {
      //      translationKeyId <- for { translationkey <- Tables.translationKey if translationkey.name === messageConstant.key } yield translationkey.id
      translationkey2 <- Tables.translationKey.filter(translationKey=>(translationKey.name === key && translationKey.isActive)).result.headOption
      _ <- Tables.translationMessage.filter(translationMessage=>(translationMessage.translationKeyId === translationkey2.get.id.get && translationMessage.isActive)).map(_.isActive).update(false)
      _ <- Tables.translationTagging.filter(translationTagging=>(translationTagging.translationKeyId === translationkey2.get.id.get && translationTagging.isActive)).map(_.isActive).update(false)
      _ <- Tables.translationKey.filter(translationKey=>(translationKey.name === key && translationKey.isActive)).map(_.isActive).update(false)
      _ <- Tables.version += Version(None, key, translationkey2.get.id.get, Operations.DELETED, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()))
    } yield ()).transactionally
    val t = innerJoin

    val transaction: Future[Unit] = db.run(t)
    transaction.map {
      ex => None
    }.recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Some(Error.ShortError("test333", "test3333"))
      }
    }
  }
  def getAllTags(): Future[Option[Seq[String]]] = {
    val innerJoin = (for {
      tag <- Tables.tag
    } yield {

      tag.name

    })
    val t = innerJoin.result

    val transaction: Future[Seq[String]] = db.run(t)
    transaction.map(res => {

      if (res == null || res.isEmpty)
        Some(Seq())
      else Some(res)

    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        None
      }
    }
  }
  def getIfExistWithTag(tag: String): Future[Option[Seq[MessageConstant.MessageConstant]]] = {
    val innerJoin = (for {
      (translationTaggings, tags) <- Tables.translationTagging join Tables.tag on (_.tagId === _.id) if tags.name === tag &&translationTaggings.isActive
      translationkey <- Tables.translationKey if translationkey.id === translationTaggings.translationKeyId && translationkey.isActive
      translationMessage <- Tables.translationMessage if translationkey.id === translationMessage.translationKeyId && translationMessage.isActive
      translationTagging <- Tables.translationTagging if translationkey.id === translationTagging.translationKeyId && translationTagging.isActive
      tag <- Tables.tag if tag.id === translationTagging.tagId
    } yield {

      (translationkey.name, (translationMessage.languageCode, translationMessage.value), tag.name)

    })
    val t = innerJoin.result

    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String)]] = db.run(t)
    transaction.map(res => {
      val keys = res.map(row => row._1).distinct
      val messages: Seq[MessageConstant.MessageConstant] = keys.map(key => {
        val trans = res.filter(x => x._1 == key).map(row => row._2).distinct.map(s => Translation.Translation(s._1.toString(), s._2))
        val tags = res.filter(x => x._1 == key).map(row => row._3).distinct
        MessageConstant.MessageConstant(key, "tes", tags, trans)
      })
      if (messages == null || messages.isEmpty)
        Some(Seq())
      else Some(messages)

    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        None
      }
    }
  }
  def getAllLanguages(): Seq[String] = {
    LanguageCodes.values.toSeq.map { x =>
      x.toString()
    }
  }

}