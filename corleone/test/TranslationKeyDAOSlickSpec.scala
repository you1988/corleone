import java.sql.Timestamp
import java.time.LocalDateTime
import helpers.PostgresDriverExtended.api._
import models.TranslationKey
import play.api.Application
import play.api.test.{FakeApplication, PlaySpecification, WithApplication}
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TranslationKeyDAOSlickSpec extends PlaySpecification with Logging  with DbSpec{
  
  "TranslationKeyDAO" should {
    "insert a Test Record" in new WithApplication(app){ 
      val tkName = "tk_1"

      Await.result(tkdao.insert(TranslationKey(None,tkName,true,Timestamp.valueOf(LocalDateTime.now()))).map( r  => r should be equalTo(1)),30 seconds)
      tkdao.findActiveByName(tkName).map( 
        r  => r.name should equals(tkName)) 
    }
  }
}
