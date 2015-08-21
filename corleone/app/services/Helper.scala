package services
import models.MessageConstant
import models.Link
import models.Error
import play.api.mvc._
import scala.collection.mutable
import models.Translation
/**
 * @author ychahbi
 */
import scala.concurrent.Future
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
        lCodes.filter(str => { !str.matches("""[a-z]{2}_[A-Z]{2}|[A-Z]{2}""") }) match {
          case List() => None
          case e      => Some(Error.ShortError("languageCodes not valid", "This languages codes are not valid : " + e.toString()))
        }

      }
    }
  }
  def validatCreateReques(params: Map[String, Seq[String]]): (Option[Seq[String]], Option[MessageConstant.MessageConstant]) = {
    var seq = Seq[String]()
    val key = params.get("key");
    key match {
      case None => seq = seq :+ "Key should not be emty"
      case _    => None
    }
    val translationsLanguages = params.get("translationslanguage").get.seq
    val translationsmessage = params.get("translationsmessage").get.seq
    val tags = params.get("tags").get.seq

    if (tags.isEmpty) {
      seq = seq :+ "translationsLanguages should not be empty"

    } else if (tags.distinct.size != tags.size) { seq = seq :+ "Duplicate translationsLanguages" }

    if (translationsLanguages.isEmpty) {
      seq = seq :+ "translationsLanguages should not be empty"

    } else if (translationsLanguages.distinct.size != translationsLanguages.size) { seq = seq :+ "Duplicate translationsLanguages" }
    else {
      if (translationsmessage.isEmpty) {
        seq = seq :+ "translationsmessage should not be empty"

      } else if (translationsLanguages.size != translationsmessage.size) { seq = seq :+ "translationsLanguages and translationsLanguages" }
    }

    if (seq.length == 0) {

      val translaions = translationsLanguages.zipWithIndex map { x => Translation.Translation(x._1, translationsmessage(x._2)) }

      (None, Some(MessageConstant.MessageConstant(key.get(0), "test", tags, translaions)))
    } else (Some(seq), None)
  }
  def validatSearchRequest(params: Map[String, Seq[String]]): (Option[String], Option[String]) = {
    var seq = Seq[String]()
   
    var key:Option[String] = None
    var tag:Option[String] = None
    if(params.get("searchValue")!=None&&params.get("searchType")!=None){
    val searchValue = params.get("searchValue").get.seq;
    val searchType = params.get("searchType").get.seq
      if (!searchType.isEmpty && !searchValue.isEmpty && searchType(0).equals("key")) key = Some(searchValue(0))
    if (!searchType.isEmpty && !searchValue.isEmpty && searchType(0).equals("tag")) tag = Some(searchValue(0))
    }
    (key, tag)
  }
  
  //        key <- req.body.asFormUrlEncoded.get("key")
  //      translationsLanguage <- req.body.asFormUrlEncoded.get("translations_language")
  //      if(translationsLanguage.distinct.size==translationsLanguage.size)
  //      translationMessage <- req.body.asFormUrlEncoded.get("translations_message")
  //      if(translationMessage.length()==translationsLanguage.length())
  //      
  //      
  //      tags <- req.body.asFormUrlEncoded.get("tag")

}