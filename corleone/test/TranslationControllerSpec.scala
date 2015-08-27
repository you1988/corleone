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
import models._
import services.TranslationManage
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api._

import play.api.libs.json._
@RunWith(classOf[JUnitRunner])
object TranslationControllerSpec extends Specification with Mockito {

  "GET action: Information fetched successfully" in {
        val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("pack"), Seq[Translation.Translation](Translation.Translation("en-GB","pack"))))

   val fakeTranslationManager= smartMock[TranslationManage]
    val hash = Codecs.sha1((msgConstants).toString());

    fakeTranslationManager.getTranslationMessage(Some(Seq[String]("en-GB")), None, Some(1000), None, None) returns Future{Left(msgConstants)};
    val result = new TranslationService(fakeTranslationManager).getTranslaions(Some(Seq[String]("en-GB")), None, Some(1000), None, None)(FakeRequest())
    status(result) must equalTo(OK)
    contentType(result) must beSome("application/json")
    charset(result) must beSome("utf-8")
    contentAsString(result) must contain(Json.toJson(msgConstants).toString())
    headers(result).toString() must contain("X-Remainder-Count -> 0")
    headers(result).toString() must contain("ETag -> "+hash)
    headers(result).toString() must contain("Cache-Control -> max-age=3600")
  }
  "GET action: Translation messages not modified" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("pack"), Seq[Translation.Translation](Translation.Translation("en-GB","pack"))))
    val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getTranslationMessage(Some(Seq[String]("en-GB")), None, None, None, None) returns Future{Left(msgConstants)};
    val hash = Codecs.sha1((msgConstants).toString());
    val result = new TranslationService(fakeTranslationManager).getTranslaions(Some(Seq[String]("en-GB")), None, None, None, None)(FakeRequest().withHeaders(IF_NONE_MATCH -> hash))
    status(result) must equalTo(NOT_MODIFIED)
  }

  "GET action: Bad query parameters" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("pack"), Seq[Translation.Translation](Translation.Translation("en-GB","pack"))))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getTranslationMessage(Some(Seq[String]("en-br")), None, None, None, None) returns Future{Right(new NotFoundError("message not found"))};
    val hash = Codecs.sha1((msgConstants).toString());
    val result = new TranslationService(fakeTranslationManager).getTranslaions(Some(Seq[String]("en-br")), None, None, None, None)(FakeRequest(GET, "/translations"))
    status(result) must equalTo(BAD_REQUEST)

  }

  "Respond to the create translation message action" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("pack"), Seq[Translation.Translation](Translation.Translation("en-GB","pack"))))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.createMessageConstants(msgConstants) returns Future{None}
    val json = Json.toJson(msgConstants);
    Logger.error(json.toString())
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(fakeTranslationManager).createTranslaions()(req)
    status(result) must equalTo(201)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")

  }

  "Create translation message with no valid payload" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("pack"), Seq[Translation.Translation]()))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.createMessageConstants(msgConstants) returns Future{None}
    val json = Json.parse("""[{"tage" : 1,"outbound_pack_message":"outbound_pack_message"}]""");
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(fakeTranslationManager).createTranslaions()(req)
    status(result) must equalTo(422)

  }

  "Create translation message already exist" in {
    val msgConstants = Seq[MessageConstant.MessageConstant](MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB","pack"))))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.createMessageConstants(msgConstants) returns Future{Some(new MessageConstantViolatedConstraintError("teststst"))}
    val json = Json.toJson(msgConstants);
    val req = FakeRequest(method = "POST", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(fakeTranslationManager).createTranslaions()(req)
    status(result) must equalTo(409)
  }


 
  "Put action: update message" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "GOOD Packer")))
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Bad Packer"), Translation.Translation("en-US", "Cool Packer")))

   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.updateMessageConstant(msgConstantNew) returns Future{Left(msgConstantNew)}
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Left(Seq(msgConstantold))}
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((Seq(msgConstantold)).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(fakeTranslationManager).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(204)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")

  }

  "Put action: update message out of date" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("de-DE", "Bad Packer")))
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "version_2", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.updateMessageConstant(msgConstantNew) returns Future{Left(msgConstantNew)}
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Left(Seq(msgConstantServerVersion))}
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((Seq(msgConstantold)).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(fakeTranslationManager).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(412)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")
  }

  "Put action: no valid payload" in {
    val msgConstantNew = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test-not-valid"), Seq[Translation.Translation](Translation.Translation("de-DE", "Bad Packer")))
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "versidonsd", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.updateMessageConstant(msgConstantNew)   returns Future{Left(msgConstantNew)}
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Left(Seq(msgConstantold))}
    val json = Json.toJson(msgConstantNew);
    val hash = Codecs.sha1((Seq(msgConstantold)).toString());
    val req = FakeRequest(method = "PUT", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(fakeTranslationManager).putTranslation("outbound_pack_message")(req)
    status(result) must equalTo(422)
  }


  "PATCH action : Message constant does not exist" in {
    val msgConstantNew = MessageConstantDelta.MessageConstantDelta("", "", Seq[String]("test"), Seq[Translation.Translation]())
    val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Right(new NotFoundError("message not found"))}
    val json = Json.toJson(msgConstantNew);
    val req = FakeRequest(method = "PATCH", uri = "/translations/:outbound_pack_message", headers = FakeHeaders().add("Content-type" -> "application/json"), body = json)
    val result = new TranslationService(fakeTranslationManager).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(404)
  }

  "PATCH action: update message" in {
    val msgConstantNew = MessageConstantDelta.MessageConstantDelta("", "", Seq[String]("PACK"), Seq[Translation.Translation]())
    val msgConstantUpdated = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("PACK"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))

    val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.updateMessageConstant(msgConstantUpdated) returns Future{Left(msgConstantUpdated)}
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Left(Seq(msgConstantold))}
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((Seq(msgConstantold)).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(fakeTranslationManager).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(200)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")

  }

  "PATCH action: update message out of date" in {
    val msgConstantNew = MessageConstantDelta.MessageConstantDelta("", "", Seq[String]("PACK"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "version_2", Seq[String]("PACK"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Left(Seq(msgConstantServerVersion))}
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((Seq(msgConstantold)).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(fakeTranslationManager).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(412)
    contentAsString(result) must contain("[{\"rel\":\"delet\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"update\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"patch\",\"href\":\"/translations/outbound_pack_message\"},{\"rel\":\"get\",\"href\":\"/translations/outbound_pack_message\"}]")
  }


  "PATCH action: no valid payload" in {
    val msgConstantNew = MessageConstantDelta.MessageConstantDelta("", "", Seq[String]("PACK-NOT-VALIDE-VALIDE"), Seq[Translation.Translation]())
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
    val msgConstantServerVersion = MessageConstant.MessageConstant("outbound_pack_message", "version_2", Seq[String]("PACK"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
   val fakeTranslationManager= smartMock[TranslationManage]
    val json = Json.toJson(msgConstantNew)
    val hash = Codecs.sha1((Seq(msgConstantold)).toString());
    val req = FakeRequest(method = "PATCH", uri = "/translations", headers = FakeHeaders().add("Content-type" -> "application/json").add(IF_NONE_MATCH -> hash), body = json)

    val result = new TranslationService(fakeTranslationManager).patchTranslation("outbound_pack_message")(req)
    status(result) must equalTo(422)
  }

  "DELETE action: Message constant has been deleted successfully" in {
    val msgConstantold = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-US", "Bad Packer"), Translation.Translation("en-GB", "Cool Packer")))
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.deleteMessageConstant("outbound_pack_message") returns Future{None};
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Left(Seq(msgConstantold))}
    val req = FakeRequest(method = "DELETE", uri = "/translations",headers = FakeHeaders(),body=null)
    val result = new TranslationService(fakeTranslationManager).deleteTranslation("outbound_pack_message")(req)
    status(result) must equalTo(204)
  }
  "DELETE action: Message constant does not exist." in {
   val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.deleteMessageConstant("outbound_pack_message") returns Future{None};
    fakeTranslationManager.getIfExistWithKey("outbound_pack_message") returns Future{Right(new NotFoundError("message not found"))}
    val req = FakeRequest(method = "DELETE", uri = "/translations",headers = FakeHeaders(),body=null)
    val result = new TranslationService(fakeTranslationManager).deleteTranslation("outbound_pack_message")(req)
    status(result) must equalTo(404)
  }

  "GET action : Information fetched successfully" in {
    val msgConstantGetResult = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))

    val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getIfExist("outbound_pack_message", Some(Seq[String]("en-GB"))) returns Future{Left(Seq(msgConstantGetResult))}
    val req = FakeRequest(method = "GET", uri = "/translations",headers = FakeHeaders(),body=null)
    val result = new TranslationService(fakeTranslationManager).getTranslation("outbound_pack_message", Some(Seq[String]("en-GB")))(req)
    status(result) must equalTo(200)
  }
  "GET action: The resource has not been modified according" in {
    val msgConstantGetResult = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))

    val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getIfExist("outbound_pack_message", Some(Seq[String]("en-GB"))) returns Future{Left(Seq(msgConstantGetResult))}
    val hash = Codecs.sha1((Seq(msgConstantGetResult)).toString())
    val req = FakeRequest(method = "GET", uri = "/translations",headers = FakeHeaders(),body=null)

    val result = new TranslationService(fakeTranslationManager).getTranslation("outbound_pack_message",Some(Seq[String]("en-GB")))(req.withHeaders((IF_NONE_MATCH->hash)))
    status(result) must equalTo(304)
  }

  "GET action: Bad query parameters" in {
    val msgConstantGetResult = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))

    val fakeTranslationManager= smartMock[TranslationManage]
    val hash = Codecs.sha1((Seq(msgConstantGetResult)).toString())
    val req = FakeRequest(method = "GET", uri = "/translations",headers = FakeHeaders(),body=null)

    val result = new TranslationService(fakeTranslationManager).getTranslation("outbound_pack_message",Some(Seq("en-K")))(req.withHeaders((IF_NONE_MATCH->hash)))
    status(result) must equalTo(400)
  }
  "GET action: No record found for specified message constant key" in {
    val msgConstantGetResult = MessageConstant.MessageConstant("outbound_pack_message", "version_1", Seq[String]("test"), Seq[Translation.Translation](Translation.Translation("en-GB", "Cool Packer")))

    val fakeTranslationManager= smartMock[TranslationManage]
    fakeTranslationManager.getIfExist("outbound_pack_message", Some(Seq[String]("en-GB"))) returns Future{Right(new NotFoundError("message not found"))}
    val hash = Codecs.sha1((Seq(msgConstantGetResult)).toString())
    val req = FakeRequest(method = "GET", uri = "/translations",headers = FakeHeaders(),body=null)

    val result = new TranslationService(fakeTranslationManager).getTranslation("outbound_pack_message",Some(Seq[String]("en-GB")))(req.withHeaders((IF_NONE_MATCH->hash)))
    status(result) must equalTo(404)
  }

}
 
