import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.specs2.mutable.Specification
import dao.TestDAO
import models.Test
import play.api.Application
import play.api.test.{FakeApplication, WithApplication, WithApplicationLoader}

class TestDAOSlickSpec extends Specification with Logging{
  
  def getTestConfig() = Map("db.default.schemas" -> "ts_test")
  
  "TestDAO" should {
    "work as expected" in new WithApplication(FakeApplication(additionalConfiguration = getTestConfig())){
      
      val app2dao = Application.instanceCache[TestDAO]
      val dao: TestDAO = app2dao(app)
      val testEntities = Set(
        Test("black"),
        Test("orange"),
        Test("grey"))

      Future.sequence(testEntities.map(dao.insert)).map{
        storedTests =>
          val storedNames = dao.all().map{ s =>
            s must equalTo(testEntities)
          }
      }
      private val sequence: Future[Int] =  dao.delete(testEntities.head.name)
      val storedNames = dao.all().map{ s =>
        s must equalTo(testEntities - testEntities.head)
      }

      
    }
  }

}
