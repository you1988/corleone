package services
import models.MessageConstant
import models.Link
import models.Error
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
        lCodes.filter(str => { !str.matches("""[a-z]{2}-[A-Z]{2}|[a-z]{2}""") }) match {
          case List() => None
          case e      => Some(Error.ShortError("languageCodes not valid", "This languages codes are not valid : " +  e.toString()))
        }

      }
    }
  }
}