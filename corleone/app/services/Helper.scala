package services


import models._
import play.Logger

import scala.util.matching.Regex

/**
 * @author ychahbi
 */
object Helper {
  def getHATEOAS(method: String, resources: Seq[MessageConstant.MessageConstant], host: String): Seq[Link.Link] = {
    var links = Seq[Link.Link]();
    resources.foreach { message =>
      method match {
        case "GET" =>
          links = links :+ Link.Link("delet", host + "/translations/" + message.key) :+ Link.Link("update", host + "/translations/" + message.key) :+ Link.Link("patch", host + "/translations/" + message.key) :+ Link.Link("get", host + "/translations/" + message.key);
        case "POST" =>
          links = links :+ Link.Link("delet", host + "/translations/" + message.key) :+ Link.Link("update", host + "/translations/" + message.key) :+ Link.Link("patch", host + "/translations/" + message.key) :+ Link.Link("get", host + "/translations/" + message.key);
        case "UPDATE" =>
          links = links :+ Link.Link("delet", host + "/translations/" + message.key) :+ Link.Link("update", host + "/translations/" + message.key) :+ Link.Link("patch", host + "/translations/" + message.key) :+ Link.Link("get", host + "/translations/" + message.key);
        case "PUT" =>
          links = links :+ Link.Link("delet", host + "/translations/" + message.key) :+ Link.Link("update", host + "/translations/" + message.key) :+ Link.Link("patch", host + "/translations/" + message.key) :+ Link.Link("get", host + "/translations/" + message.key);

        case "PATCH" =>
          links = links :+ Link.Link("delet", host + "/translations/" + message.key) :+ Link.Link("update", host + "/translations/" + message.key) :+ Link.Link("patch", host + "/translations/" + message.key) :+ Link.Link("get", host + "/translations/" + message.key);
        case "DELETE" =>
      }
    }
    return links;

  }

  def validateLanguageCodes(languageCodes: Option[Seq[String]]): Option[Error.ShortError] = {
    languageCodes match {
      case None => None
      case Some(lCodes) => {
        lCodes.filter(language => {
          !LanguageCodes.values.seq.map(languageCode => languageCode.toString)
            .contains(language)
        }) match {
          case List() => None
          case e => Some(new MalFormedError(Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_LANGUAGE_CODE_ERROR_MESSAGE + e.toString()))
        }

      }
    }
  }

  def validatCreateRequest(params: Map[String, Seq[String]]):Either[MessageConstant.MessageConstant,Seq[String]] = {
    Logger.error("test " + params.toString())
    var seq = Seq[String]()
    val key = params.get("key");
    key match {
      case None => seq = seq :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("tags")
      case Some(keyValue) =>      if (!key.get(0).matches( """[a-z_A-Z0-9]*""")) {
        seq = seq :+Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_FIELD_FORMAT_UNSUPORTED_ERROR_MESSAGE.stripMargin.format("key")
      }
    }
    val translationsLanguages = params.get("translationslanguage").get.seq
    val translationsmessage = params.get("translationsmessage").get.seq
    val tags = params.get("tags").get.seq

    if (tags.isEmpty) {
      seq = seq :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("tags")
    } else if (tags.distinct.size != tags.size) {
      seq = seq :+ Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_TAGS_ERROR_MESSAGE
    }else if (tags.exists(tag=> !tag.matches( """[a-z_A-Z0-9]*"""))){
      seq = seq :+Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_FIELD_FORMAT_UNSUPORTED_ERROR_MESSAGE.stripMargin.format("tags")
    }


    if (translationsLanguages.isEmpty) {
      seq = seq :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("translations")

    }else if (translationsLanguages.exists(language=>
      !LanguageCodes.values.seq.map(languageCode => languageCode.toString)
        .contains(language))) {
      seq = seq :+Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_UNSUPORTED_LANGUAGE_CODE_ERROR_MESSAGE

    }

    else if (translationsLanguages.distinct.size != translationsLanguages.size) {
           seq = seq :+ Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_TRANSLATIONS_ERROR_MESSAGE
    }
    else {
      if (translationsmessage.isEmpty) {
        seq = seq :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("translations")

      } else if (translationsLanguages.size != translationsmessage.size) {


        seq = seq :+ Constants.REQUEST_TRANSLATIONS_INVALID_TRANSLITIONS_LANGUAGES_DIFFERENT_ERROR_MESSAGE
      }
    }

    if (seq.length == 0) {

      val translaions = translationsLanguages.zipWithIndex map { x => Translation.Translation(x._1, translationsmessage(x._2)) }

      Left(MessageConstant.MessageConstant(key.get(0), "test", tags, translaions))
    } else Right(seq)
  }

  /**
   * This function validate the search request param.
   * Valid params are Map("searchValue"->Seq(...),"searchType"->Seq("tag"))
   * Map("searchValue"->Seq(...),"searchType"->Seq("key"))
   *
   * @param params The search request params.
   * @return None case the search request is not valide.
   *         Left(keyValue) case the search params contains
   *           "searchType"->"key" with keyValue is the value of "searchValue"
   *         Right(tagValue) case the search params contains
   *            "searchType"->"tag" with tagValue is the value of "searchValue"
   */
  def getAndValidatSearchRequest(params: Map[String, Seq[String]]): Option[Either[String, String]] = {
    (params.get("searchValue"), params.get("searchType")) match {
      case (Some(serachValue), Some(searchType)) =>
        (searchType.exists(key => key.equals("key")), searchType.exists(tag => tag.equals("tag"))) match {
          case (false, true) => Some(Right(serachValue.headOption.getOrElse("")))
          case (true, false) => Some(Left(serachValue.headOption.getOrElse("")))
          case(_,_)=>None
        }
        case(_,_)=> None
    }
  }

  def getMessageError(pattern: Regex, message: String, exceptionMessage: String): String = {

    val allMatches = pattern.findAllMatchIn(exceptionMessage)
    if (allMatches.isEmpty) exceptionMessage
    else message.stripMargin.format(allMatches.next().group(1))

  }


}