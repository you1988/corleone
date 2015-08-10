import fakeservices.FakeTranslationManager
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._
import play.api.libs._
import play.api.http._
import play.api.mvc._
import org.specs2.mock._
import controllers.TranslationService
import models.MessageConstant
import models.Link
import models.Translation
import services.TranslationManage
import models.Error
import play.api.libs.json._
import controllers.TranslationService
@RunWith(classOf[JUnitRunner])
class TranslationRoutersSpec extends Specification with Mockito with Controller {

  "GET action: Route test for succefull fetch" in {
    running(FakeApplication(additionalConfiguration = Map[String, String](("env.mode" -> "mock")))) {
      val result = route(FakeRequest(GET, "/translations/outbound_pack_message?languageCodes=en,er-BG")).get
      status(result) must equalTo(200)
    }
  }

  "GET action: Route test for bad params" in {
    running(FakeApplication(additionalConfiguration = Map[String, String](("env.mode" -> "mock")))) {
      val result = route(FakeRequest(GET, "/translations/outbound_pack_message?languageCodes=en,er-bg")).get
      status(result) must equalTo(400)
    }
  }
   
  
  "GET action: Route test for bad params" in {
    running(FakeApplication(additionalConfiguration = Map[String, String](("env.mode" -> "mock")))) {
      val result = route(FakeRequest(GET, "/translations/outbound_pack_message?languageCodes=enc,er")).get
      status(result) must equalTo(400)
    }
  }

}