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

package services

import javax.inject._

import dao.MessageConstantQueryBuilder
import helpers.PostgresDriverExtended.api._
import models.Error.ShortError
import models._
import org.postgresql.util.PSQLException
import play.api._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.matching.Regex

/**
 * Service contains all logic needed from Corleone.
 */
@Singleton
class TranslationManageImpl extends TranslationManage {
  private val db = Database.forConfig("postgresDb")

  /**
   * This function return :
   * case languageCodesOp and tagsOp are not empty  : message constants that has at
   * least one transaltion in one of languageCodesOp and tag in tagsOp.
   * case languageCodesOp is not empty  : message constants that has at
   * least one transaltion in one of languageCodesOp.
   * case tagsOp is not empty  : message constants that has at
   * least one tag in one of tags.
   * If languageCodesOp and tagsOp are  empty: all message constants.
   * @return Message constants respect the previous conditions.
   *         NotFoundError if no message constant with key and has translation in one of  languageCodes.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */

  def getTranslationMessage(languageCodesOp: Option[Seq[String]], tagsOp: Option[Seq[String]], limit: Option[Integer], after: Option[String], before: Option[String]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    val languages = languageCodesOp.getOrElse[Seq[String]](LanguageCodes.values.toSeq.map { x =>
      x.toString()
    }).map(languageCode =>
      LanguageCodes.withName(languageCode))
    var searchquery = MessageConstantQueryBuilder.buildSelectAllMessageConstantQuery()
    searchquery = searchquery.filter(x => (tagsOp match {
      case None => x._2._1 inSet languages
      case Some(tags) => (x._3 inSet tags) && (x._2._1 inSet languages)
    })).sortBy(_._1.asc).take(limit.getOrElse[Integer](1000))
    if (!after.isEmpty) searchquery = searchquery.filter(_._1 >= after.get)
    if (!before.isEmpty) searchquery = searchquery.filter(_._1 < before.get)
    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]] = db.run(searchquery.result)
    handleSearchQueryResponse(transaction)
  }


  /**
   * This function return :
   * case languageCodesOp and tagsOp are not empty  : message constants that has at
   * least one transaltion in one of languageCodesOp and tag in tagsOp.
   * case languageCodesOp is not empty  : message constants that has at
   * least one transaltion in one of languageCodesOp.
   * case tagsOp is not empty  : message constants that has at
   * least one tag in one of tags.
   * If languageCodesOp and tagsOp are  empty: all message constants.
   * @return Message constants respect the previous conditions.
   *         NotFoundError if no message constant with key and has translation in one of  languageCodes.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */

  def getTranslationMessages(keys:Seq[String],language:LanguageCodes.LanguageCode,transaltions:Map[String,String]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    var searchquery = MessageConstantQueryBuilder.buildSelectAllMessageConstantQuery()
    searchquery = searchquery.filter(x => (x._1 inSet keys))
    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]] = db.run(searchquery.result)
    handleSearchQueryResponse(transaction,transaltions,language)
  }



  /**
   * This function return the message constant mapped to the key
   * @param key The key of the message constant
   * @return The message constant with key if found.
   *         NotFoundError if no message constant with the specified key.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def getIfExistWithKey(key: String): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    getIfExist(key, None)
  }

  /**
   * This function return the translations mapped to a key and specific languages.
   * If language codes is empty all the translations.
   * @param key The key of the message constant
   * @return The translations in languageCodes for message with key if languageCodes empty
   *         the whole message constant if found.
   *         NotFoundError if no message constant with key and has translation in one of  languageCodes.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def getIfExist(key: String, languageCodes: Option[Seq[String]]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    val languages: Seq[LanguageCodes.LanguageCode] = languageCodes.getOrElse(LanguageCodes.values.seq.map(l => l.toString())).map(l => LanguageCodes.withName(l)).toSeq
    var searchquery = MessageConstantQueryBuilder.buildSelectAllMessageConstantQuery().filter(_._2._1 inSet languages).filter(_._1 === key)
    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]] = db.run(searchquery.result)
    handleSearchQueryResponse(transaction)
  }

  /**
   * This function return the message constant tagged with tag
   * @param tag The tag of the message constant
   * @return The message constant tagged with tag if found.
   *         NotFoundError if no message constant with tagged with tag.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def getIfExistWithTag(tag: String): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    val searchQuery = MessageConstantQueryBuilder.buildSelectAllMessagesConstantsWithTagQuery(tag)
    val searchResult = searchQuery.result
    val transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]] = db.run(searchResult)
    handleSearchQueryResponse(transaction)
  }

  /**
   * This function updates a message constant , process to update a message constant is done in one transaction:
   *        -Disable the old version of the message constant based on the message constant message key by setting the active propertyof  translations and tags to false.
   *        -Create a new translation Message by inserting its different properties into the transaltion_tagging ,tansaltion_message and transalation_key tables
   *         and tag tables case that tag is new.
   *        -Create a version by inserting a row in version table for versioning the update version of message constant.
   * @param messageConstant The up to date version of the message constant
   * @return The updated version if the update transaction is success.
   *         MessageConstantViolatedConstraintError Case the message constant violate one of the data base constraint.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def updateMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Either[MessageConstant.MessageConstant, Error.ShortError]] = {
    val updateResult: Future[String] = db.run((MessageConstantQueryBuilder.buildDeleteMessageConstantAction(messageConstant.key).andThen(MessageConstantQueryBuilder.buildInsertMessageConstantAction(messageConstant)
    ).andThen(MessageConstantQueryBuilder.buildInsertVersioning(messageConstant.key, Operations.MODIFIED)))
      .transactionally)
    handleUpSetAction(messageConstant, updateResult)
  }
  /**
   * This function updates a message constant , process to update a message constant is done in one transaction:
   *        -Disable the old version of the message constant based on the message constant message key by setting the active propertyof  translations and tags to false.
   *        -Create a new translation Message by inserting its different properties into the transaltion_tagging ,tansaltion_message and transalation_key tables
   *         and tag tables case that tag is new.
   *        -Create a version by inserting a row in version table for versioning the update version of message constant.
   * @param messageConstants The up to date version of the message constant
   * @return The updated version if the update transaction is success.
   *         MessageConstantViolatedConstraintError Case the message constant violate one of the data base constraint.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def updateMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Option[Error.ShortError]]= {
    val updateResult: Future[Unit] = db.run((MessageConstantQueryBuilder.buildDeleteMessagesConstantAction(messageConstants.map(msg=>msg.key)).andThen(MessageConstantQueryBuilder.buildCreateMessageContants(messageConstants))
      .transactionally))
      updateResult.map {
      ex => None
    }.recover {
      case ex: Exception => Some(mapExceptionError(ex))
    }
  }

  /**
   * This function creates a message constant , process to insert a message constant is done in one transaction:
   *        -Create a new message constant by inserting its different properties into the transaltion_tagging ,tansaltion_message and transalation_key tables
   *         and tag tables case that tag is new.
   *        -Create a version by inserting a row in version table for versioning the message constant.
   * @param messageConstant  The message constant to be created
   * @return The updated version if the update transaction is success.
   *         Short error in the other case.
   */
  def createMessageConstant(messageConstant: MessageConstant.MessageConstant): Future[Either[MessageConstant.MessageConstant, Error.ShortError]] = {
    val insertResult: Future[String] = db.run((MessageConstantQueryBuilder.buildInsertMessageConstantAction(messageConstant)
      .andThen(MessageConstantQueryBuilder.buildInsertVersioning(messageConstant.key, Operations.CREATED)))
      .transactionally)
    handleUpSetAction(messageConstant, insertResult)

  }
  /**
   * This function insert a list of  message constants , process to insert a list of message constants is done in one transaction:
   *        -Create a new message constants by inserting their different properties into the transaltion_tagging ,tansaltion_message and transalation_key tables
   *         and tag tables case that tag is new.
   *        -Create versions by inserting a row in version table for versioning all created message constants.
   * @param messageConstants The message constants to be created
   * @return The updated version if the update transaction is success.
   *         MessageConstantViolatedConstraintError Case the message constant violate one of the data base constraint.
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def createMessageConstants(messageConstants: Seq[MessageConstant.MessageConstant]): Future[Either[Seq[MessageConstant.MessageConstant],Error.ShortError]] = {


    val searchQuery = MessageConstantQueryBuilder.buildSelectAllMessagesConstantsWithKeysQuery(messageConstants.map(message => message.key))
    val searchResult = searchQuery.result

    val result: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]] = db.run(searchResult)
    handleSearchQueryResponse(result).flatMap { res => res match {
      case Right(err)=> {
        err match {
          case err: NotFoundError => {

            val transaction = MessageConstantQueryBuilder.buildCreateMessageContants(messageConstants)
            val f: Future[Unit] = db.run(transaction.transactionally)
            f.map {
              ex => Left(Seq())
            }.recover {
              case ex: Exception => Right(mapExceptionError(ex))
            }
          }
          case err: ShortError=> Future{Right(err)}

        }
      }
      case Left(seq)=>{
        val transaction = MessageConstantQueryBuilder.buildCreateMessageContants(messageConstants.filter(p => !seq.exists(a=> a.key.equals(p.key))))
        val f: Future[Unit] = db.run(transaction.transactionally)
        f.map {
          ex => res
        }.recover {
          case ex: Exception => Right(mapExceptionError(ex))
        }
      }
    }
    }
  }


  /**
   * This function insert a list of  message constants , process to insert a list of message constants is done in one transaction:
   *        -Disable the old version of the message constant based on the message constant message key by setting the active property of  translations and tags to false.
   *         -Create version in version table with DELETED operation type for versioning the delete of the message constant.
   * @param key The key of the message constant to be deleted
   * @return None case the operation success
   *         TimeOutError if postgres data base return a time out exception.
   *         NotExpectedError if a problem happen in the database
   */
  def deleteMessageConstant(key: String):Future[Option[ShortError]] = {

    val transaction: Future[Unit] = db.run(
      MessageConstantQueryBuilder.buildInsertVersioning(key, Operations.DELETED)
        .andThen(MessageConstantQueryBuilder.buildDeleteMessageConstantAction(key)))
    transaction.map {
      ex => None
    }.recover {
      case ex: Exception => Some(mapExceptionError(ex))
    }
  }
  /**
   * This function list all tags in the data base
   * @return All tags found in the data base if select query successed.
   *         Short error in the other case.
   */
  def getAllTags(): Future[Either[Seq[String], ShortError]] = {

    val innerJoin = (for {
      tag <- Tables.tag
    } yield {

        tag.name

      })
    val t = innerJoin.result

    val transaction: Future[Seq[String]] = db.run(t)
    transaction.map(res => {
      if (res == null || res.isEmpty)
        Right(new NotFoundError(""))
      else Left(res)

    }).recover {
      case ex: Exception => Right(mapExceptionError(ex))
    }
  }


  def getAllLanguages(): Seq[String] = {
    LanguageCodes.values.toSeq.map {
      x =>
        x.toString()
    }
  }
  /**
   * This function handle search queries response.
   * @param transaction     The search query result.
   * @return                The list of message constants resulted from the query search if search query successed
   *                        Short error if search query failed
   */
  private def handleSearchQueryResponse(transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]]): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    transaction.map(res => {
      val messageConstants = transformTuppleToMessageConstants(res)
      if (messageConstants.isEmpty) Right(new NotFoundError(Constants.MESSAGE_CONSTANT_NOT_FOUND_WHITH_TAG.stripMargin.format("tag")))
      else Left(messageConstants)

    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Right(mapExceptionError(ex))
      }
    }
  }

  /**
   * This function handle search queries response.
   * @param transaction     The search query result.
   * @return                The list of message constants resulted from the query search if search query successed
   *                        Short error if search query failed
   */
  private def handleSearchQueryResponse(transaction: Future[Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]],transaltions:Map[String,String],language:LanguageCodes.LanguageCode): Future[Either[Seq[MessageConstant.MessageConstant], ShortError]] = {
    transaction.map(res => {
      val messageConstants = transformTuppleToMessageConstants(res)
      if (messageConstants.isEmpty) Right(new NotFoundError(Constants.MESSAGE_CONSTANT_NOT_FOUND_WHITH_TAG.stripMargin.format("tag")))
      else {

        Left(messageConstants.map(messageConstant => {
          var updatedTransaltions:Seq[Translation.Translation]=messageConstant.translations.map(translation=> {
            if (translation.languageCode.toString.equals(language.toString)) {
              translation.copy(message=transaltions.getOrElse(messageConstant.key,translation.message) )
            }
            else translation
          })
          if(!updatedTransaltions.exists(translation => translation.languageCode.toString.equals(language.toString) && !transaltions.getOrElse(messageConstant.key,"").isEmpty))
            updatedTransaltions= updatedTransaltions :+ Translation.Translation(language.toString,transaltions.get(messageConstant.key).get)
          messageConstant.copy(translations = updatedTransaltions)}))
      }
    }).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Right(mapExceptionError(ex))
      }
    }
  }

  /**
   * This function transforms a tupple of message constant properties to a list of message constant.
   * @param messageConstants Tuple contains different properties of message constants.
   * @return The list of message constant
   */
  private def transformTuppleToMessageConstants(messageConstants: Seq[(String, (LanguageCodes.LanguageCode, String), String, String)]): Seq[MessageConstant.MessageConstant] = {

    messageConstants.groupBy(x => x._1 -> x._4).map(messageConstant => MessageConstant.MessageConstant(
      messageConstant._1._1,
      messageConstant._1._2,
      messageConstant._2.groupBy(_._3).keySet.toSeq,
      messageConstant._2.groupBy(_._2).keySet.map(message => Translation.Translation(message._1.toString(), message._2)).toSeq
    )
    ).toSeq
  }
  /**
   * This function handle  exception by mapping it to  an intern error.
   * @param exception To be handled.
   * @return The mapped intern error to exception if a mapping found
   *         NotHandledError in the other case.
   */
 private def mapExceptionError(exception: Exception): ShortError = {
    exception match {
      case pslqException: PSQLException => {
        getDetailMessageForPSQLException(pslqException) match {
          case Right(error) => error
          case Left(messageError) => new MessageConstantViolatedConstraintError(messageError)
        }
      }
      case _ => new NotHandledError(exception.getMessage)
    }
  }

  /**
   * This function return the mapped error message to exception.
   * @param exception To be handled.
   * @return Customize error message if this type exception expected.
   *         NotHandledError if this type exception not expected.
   */
  private def getDetailMessageForPSQLException(exception: PSQLException): Either[String, NotHandledError] = {
    Constants.PSQL_ERROR_CONSTRAINT_MAPPING.getOrElse[Map[String, Tuple2[Regex, String]]](exception.getServerErrorMessage.getSQLState, Map()).get(exception.getServerErrorMessage().getConstraint())
    match {
      case Some(constraintHandled) => {
        Left(Helper.getMessageError(constraintHandled._1,
        constraintHandled._2,
        exception.getServerErrorMessage.getMessage))

      }
      case None => {

        Right(new NotHandledError(exception.getMessage))

      }
    }

  }
  /**
   * This function handle  insert or update of message constant.
   * @param messageConstant To be updated or inserted.
   * @param upsetFuture     The result of an insert or an update action.
   * @return                Current version of the message constant if  upsetaction success
   *                        Short error if upset action failure
   */
  def handleUpSetAction(messageConstant: MessageConstant.MessageConstant, upsetFuture: Future[String]): Future[Either[MessageConstant.MessageConstant, Error.ShortError]] = {
    upsetFuture.map {
      version => Left(messageConstant.copy(version = version))
    }.recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        Right(mapExceptionError(ex))
      }
    }
  }

}