import java.sql.Timestamp
import java.time.LocalDateTime

import models.TranslationKey
import play.api.Application
import play.api.test.{FakeApplication, PlaySpecification, WithApplication}
import slick.util.Logging

import scala.concurrent.ExecutionContext.Implicits.global

class TranslationKeyDAOSlickSpec extends PlaySpecification with Logging{
  val app = FakeApplication()
  val app2dao = Application.instanceCache[TranslationKeyDAO]
  val dao: TranslationKeyDAO = app2dao(app)
  
  trait before {
    dao.deleteAll
  }
  
  "TranslationKeyDAO" should {
    "insert a Test Record" in new WithApplication(app) with before{
      val tkName = "tk_1"
      
      dao.delete(tkName)
      dao.insert(TranslationKey(None,tkName,true,Timestamp.valueOf(LocalDateTime.now()))).map( r  => r should be equalTo(1))
      dao.findActiveByName(tkName).map( 
        r  => r.name should equals(tkName))
    }
  }
}

  