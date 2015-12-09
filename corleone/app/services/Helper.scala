package services

import java.io.FileInputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.{Properties, Calendar}

import com.google.common.base.Charsets
import models._
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Result
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


  def validatExportRequest(params: Map[String, Seq[String]]):Either[Requests.ExportRequest,Seq[String]] = {
    var seq = Seq[String]()
    val tag = params.get("tag");
    tag match {
      case None => seq = seq :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("tag")
      case Some(keyValue) =>      if (!tag.get(0).matches( """[a-z_A-Z0-9]*""")) {
        seq = seq :+Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_FIELD_FORMAT_UNSUPORTED_ERROR_MESSAGE.stripMargin.format("tag")
      }
    }
    val csvType = params.get("csv_type");
    Logger.error(csvType.get.toString())
    var csvTypeValue:String=""
    csvType match {
      case None => seq = seq :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("csv type")
      case Some(keyValue) =>csvTypeValue=csvType.get(0)
    }


    if (seq.length == 0) {


      Left(Requests.ExportRequest(tag.get(0),";",csvTypeValue))
    } else Right(seq)
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


  def getAndValidateCreateBasedOnPropsRequest(file:Option[FilePart[TemporaryFile]],languageParam:Option[Seq[String]],tag:Option[Seq[String]]): Either[Seq[MessageConstant.MessageConstant], Seq[String]] = {
    var errors: Seq[String] = Seq()
    var translations: Seq[MessageConstant.MessageConstant] = Seq()
    var tags: Seq[String] = Seq()
    var csvTypeValue:String=""
    Logger.error("tes " + languageParam + " tag " + tag)
    val prop = new Properties()
    tag match {
      case None => {
        errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("csv type")
        return Right(errors)
      }
      case Some(tagsValue) =>tags=tagsValue
    }


    languageParam match {
      case None => {
        errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("language")
        Right(errors)
      }
      case Some(langs) => {

        if(langs.isEmpty) {
          errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("language")
          Right(errors)

        }else {
          val lang:String=langs(0)
          if (!LanguageCodes.values.seq.map(languageCode => languageCode.toString).contains(lang)) {
            errors = errors :+ Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_UNSUPORTED_LANGUAGE_CODE_ERROR_MESSAGE
            Right(errors)
          } else {
            val language: LanguageCodes.LanguageCode = LanguageCodes.withName(lang)
            file match {
              case None => {
                errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("file")
                Right(errors)
              }
              case Some(file) => {
                if (file.filename.endsWith("properties")) {
                    try {
                      prop.load(new FileInputStream(file.ref.file))
                      val map = prop.entrySet().iterator()
                      while(map.hasNext){
                      val next = map.next()

                        translations= translations :+MessageConstant.MessageConstant(next.getKey.toString, "test", tags, Seq(Translation.Translation(language.toString,next.getValue.toString)))
                      }



                    } catch { case e: Exception => {
                      errors = errors :+ e.getMessage
                      Logger.error(e.getMessage)
                    }
                      return  Right(errors)
                    }
                  Logger.info("tes " + translations)
                    Left(translations)

                } else {
                  errors = errors :+ Constants.REQUEST_FILE_TYPE_IS_NOT_SUPPORTED.stripMargin.format("file")
                  Right(errors)
                }
              }
            }
          }
        }
      }
    }
  }




  def getAndValidatImportRequest(file:Option[FilePart[TemporaryFile]],languageParam:Option[Seq[String]],csvType:Option[Seq[String]]): Either[Requests.ImportRequest, Seq[String]] = {
    var errors: Seq[String] = Seq()
    var csvTypeValue:String=""
    csvType match {
      case None => {
        errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("csv type")
      return Right(errors)
      }
      case Some(keyValue) =>csvTypeValue=csvType.get(0)
    }


    languageParam match {
      case None => {
        errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("language")
        Right(errors)
      }
      case Some(langs) => {

        if(langs.isEmpty) {
          errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("language")
          Right(errors)

        }else {
          val lang:String=langs(0)
          if (!LanguageCodes.values.seq.map(languageCode => languageCode.toString).contains(lang)) {
            errors = errors :+ Constants.REQUEST_MESSAGE_CONSTANT_MAL_FORMED_UNSUPORTED_LANGUAGE_CODE_ERROR_MESSAGE
            Right(errors)
          } else {
            val language: LanguageCodes.LanguageCode = LanguageCodes.withName(lang)
            file match {
              case None => {
                errors = errors :+ Constants.REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE.stripMargin.format("file")
                Right(errors)
              }
              case Some(file) => {
                if (file.filename.endsWith("csv")) {
                  if (!scala.io.Source.fromFile(file.ref.file,csvTypeValue).getLines.isEmpty) {
                    val header: Seq[String] = scala.io.Source.fromFile(file.ref.file,csvTypeValue).getLines.toSeq(0).split(Constants.REGEX_SPLIT_CSV_FILE).toSeq
                    if (header.contains("key") && header.contains(lang)) {
                      val indexLanguage = header.indexWhere(p => p.equals(lang))
                      val indexKey = header.indexWhere(p => p.equals("key"))
                      var data: Seq[Seq[String]] = scala.io.Source.fromFile(file.ref.file,csvTypeValue).getLines.map(line => {
                        line.split(Constants.REGEX_SPLIT_CSV_FILE).toSeq
                      }
                      ).toSeq
                      if (!data.exists(p =>{
                        Logger.error("line " + p.toString())
                        p.length < indexLanguage || p.length < indexKey

                      })) {
                        val translations: Map[String, String] = data.map(line => {
                          (line(indexKey).replaceAll("\"", ""), line(indexLanguage).replaceAll("\"", ""))}).toMap
                        Left(Requests.ImportRequest(language, translations.filter(p => !(p._1.equals("key")&&p._2.equals(lang)))))
                      } else {
                        Logger.error("Error The csv file contains at least one line with more columns " + data.find(p=>  p.length < indexLanguage || p.length < indexKey).toString)
                        errors = errors :+ Constants.REQUEST_CSV_FILE_CONTENT_IS_NOT_VALID
                        Right(errors)
                      }
                    } else {
                      errors = errors :+ Constants.REQUEST_CSV_FILE_CONTENT_IS_NOT_CORRECT.stripMargin.format(lang)
                      Right(errors)
                    }
                  } else {
                    errors = errors :+ Constants.REQUEST_FILE_CONTENT_IS_EMPTY
                    Right(errors)
                  }
                } else {
                  errors = errors :+ Constants.REQUEST_FILE_TYPE_IS_NOT_SUPPORTED.stripMargin.format("file")
                  Right(errors)
                }
              }
            }
          }
        }
      }
    }
  }


  def toCsv(messages: Seq[MessageConstant.MessageConstant], request: Requests.ExportRequest): (String,String) = {
    val rowDelim: String = "\r\n"
    var header: String = "key,tags"
    LanguageCodes.values.toSeq.foreach(l=>header=header +"," + l.toString )

    var data: String = header
    messages.foreach(message => {
      var line: String = rowDelim + message.key + ","
      message.tags.foreach(tag => line += tag + " ");
      LanguageCodes.values.toSeq.foreach(l=>line=line +"," +  message.translations.find(trans => trans.languageCode.equals(l.toString)).getOrElse(Translation.Translation(l.toString, "")).message )
      data += line
    })
    Logger.error("tetststtsts " + request.csvEncoding)
    val fileName: String = request.tag + "_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(Calendar.getInstance().getTime()) + ".csv"
    val contentDisposition: String = "attachment; filename=" + fileName
    (new String(Charset.forName(request.csvEncoding).encode(data).array(),Charset.forName(request.csvEncoding)),contentDisposition)
  }



}