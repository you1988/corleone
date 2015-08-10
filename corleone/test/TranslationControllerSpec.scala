import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._
import play.api.libs._
/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import org.specs2.mock._
import controllers.TranslationService
import models.MessageConstant
import models.Link
import models.Translation
import services.TranslationManage
import models.Error
import play.api.libs.json._
@RunWith(classOf[JUnitRunner])
object TranslationControllerSpec extends Specification with Mockito {

  "GET action: Information fetched successfully" in {
    val m = smartMock[TranslationManage]
    m.getTranslationMessage(Some(Seq[String]("en")), None, None, None, None) returns Seq.empty[MessageConstant.MessageConstant];
    val result = new TranslationService(m).getTranslaions(Some(Seq[String]("en")), None, None, None, None)(FakeRequest())
    status(result) must equalTo(OK)
    contentType(result) must beSome("application/json")
    charset(result) must beSome("utf-8")
    contentAsString(result) must contain("{\"messageConstants\":[],\"_links\":[]}")
  }
  "GET action: Translation messages not modified" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]()))
    val m = smartMock[TranslationManage]
    m.getTranslationMessage(Some(Seq[String]("en")), None, None, None, None) returns msgConstants;
    val hash = Codecs.sha1((msgConstants).toString());
    val result = new TranslationService(m).getTranslaions(Some(Seq[String]("en")), None, None, None, None)(FakeRequest().withHeaders(IF_NONE_MATCH -> hash))
    status(result) must equalTo(NOT_MODIFIED)
  }

  "GET action: Bad query parameters" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]()))
    val m = smartMock[TranslationManage]
    m.getTranslationMessage(Some(Seq[String]("en-br")), None, None, None, None) returns msgConstants;
    val hash = Codecs.sha1((msgConstants).toString());
    val result = new TranslationService(m).getTranslaions(Some(Seq[String]("en-br")), None, None, None, None)(FakeRequest(GET, "/translations"))
    status(result) must equalTo(BAD_REQUEST)

  }

  "Respond to the create translation message action" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer"))))
    val m = smartMock[TranslationManage]
    m.createMessageConstants(msgConstants) returns None
    m.getIfExistWithKey("outbound_pack_message") returns None
    val json = Json.toJson(msgConstants);
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(m).createTranslaions()(req)
    status(result) must equalTo(OK)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")

  }
  "Create translation message with a no valid request" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]()))
    val m = smartMock[TranslationManage]
    m.createMessageConstants(msgConstants) returns None
    m.getIfExistWithKey("outbound_pack_message") returns None
    val json = Json.parse("""[{"tage" : 1,"outbound_pack_message":"outbound_pack_message"}]""");
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(m).createTranslaions()(req)
    status(result) must equalTo(BAD_REQUEST)

  }

  "Create translation message already exist" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]()))
    val m = smartMock[TranslationManage]
    m.createMessageConstants(msgConstants) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]()))
    val json = Json.toJson(msgConstants);
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(m).createTranslaions()(req)
    status(result) must equalTo(409)
  }
  "Create translation message with no valid payload" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]()))
    val m = smartMock[TranslationManage]
    m.createMessageConstants(msgConstants) returns Some(Error.ShortError("Resource Not valid", "mulötiple translation for same message"))
    m.getIfExistWithKey("outbound_pack_message") returns None
    val json = Json.toJson(msgConstants);
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(m).createTranslaions()(req)
    status(result) must equalTo(422)
  }

  "Put action : new message constant" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.createMessageConstant(msgConstant) returns None
    m.getIfExistWithKey("outbound_pack_message") returns None
    val json = Json.toJson(msgConstant);
    val req = FakeRequest(method = "PUT", uri = "/translations:outbound_pack_message", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(m).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(204)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")
  }
  "Put action: update message" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))

    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantold)
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(204)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")

  }

  "Put action: update message out of date" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantServerVersion)
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(412)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")
  }
  "Put action: payload is malformed" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantServerVersion)
    val json = Json.parse("""[{"tage" : 1,"outbound_pack_message":"outbound_pack_message"}]""");
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(400)
  }
  "Put action: no valid payload" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en", "Cool Packer")))
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns Some(Error.ShortError("Message Constant not valid", "mutiple translation for same language"))
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantold)
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(422)
  }

  "PATCH action : Message constant does not exist" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstant) returns None
    m.getIfExistWithKey("outbound_pack_message") returns None
    val json = Json.toJson(msgConstant);
    val req = FakeRequest(method = "PATCH", uri = "/translations/:outbound_pack_message", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(m).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(404)
  }
  "PATCH action: update message" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))

    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantold)
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(200)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")

  }

  "PATCH action: update message out of date" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantServerVersion)
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(412)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")
  }
  "PATCH action: payload is malformed" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns None
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantServerVersion)
    val json = Json.parse("""[{"tage" : 1,"outbound_pack_message":"outbound_pack_message"}]""");
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(400)
  }
  "PATCH action: no valid payload" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en", "Cool Packer")))
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.updateMessageConstant(msgConstantNew) returns Some(Error.ShortError("Message Constant not valid", "mutiple translation for same language"))
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstantold)
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((msgConstantold).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(m).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(422)
  }

  "DELETE action: Message constant has been deleted successfully" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.deleteMessageConstant("outbound_pack_message");
    m.getIfExistWithKey("outbound_pack_message") returns Some(msgConstant)
    val result = new TranslationService(m).deleteTranslation("outbound_pack_message")(FakeRequest())
    status(result) must equalTo(204)
  }
  "DELETE action: Message constant does not exist." in {
    val m = smartMock[TranslationManage]
    m.deleteMessageConstant("outbound_pack_message");
    m.getIfExistWithKey("outbound_pack_message") returns None
    val result = new TranslationService(m).deleteTranslation("outbound_pack_message")(FakeRequest())
    status(result) must equalTo(404)
  }

  "GET action : Information fetched successfully" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val m = smartMock[TranslationManage]
    m.getIfExist("outbound_pack_message", Seq[String]("en")) returns Some(msgConstant)
    val result = new TranslationService(m).getTranslation("outbound_pack_message", Some(Seq[String]("en")))(FakeRequest())
    status(result) must equalTo(200)
  }
  "GET action: The resource has not been modified according" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))

    val m = smartMock[TranslationManage]
    m.getIfExist("outbound_pack_message", Seq[String]()) returns Some(msgConstant)
    val hash = Codecs.sha1((msgConstant).toString());
    val result = new TranslationService(m).getTranslation("outbound_pack_message",None)(FakeRequest().withHeaders((IF_NONE_MATCH->hash)))
    status(result) must equalTo(304)
  }

  "GET action: Bad query parameters" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))

    val m = smartMock[TranslationManage]
    m.getIfExist("outbound_pack_message", Seq[String]("en-K")) returns Some(msgConstant)
    val hash = Codecs.sha1((msgConstant).toString());
    val result = new TranslationService(m).getTranslation("outbound_pack_message",Some(Seq[String]("en-K")))(FakeRequest().withHeaders((IF_NONE_MATCH->hash)))
    status(result) must equalTo(400)
  }
  "GET action: No record found for specified message constant key" in {
    val msgConstant = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))

    val m = smartMock[TranslationManage]
    m.getIfExist("outbound_pack_message", Seq[String]("en-GB")) returns None
    val hash = Codecs.sha1((msgConstant).toString());
    val result = new TranslationService(m).getTranslation("outbound_pack_message",Some(Seq[String]("en-GB")))(FakeRequest().withHeaders((IF_NONE_MATCH->hash)))
    status(result) must equalTo(404)
  }

}
 
