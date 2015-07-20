import slick.util.Logging
import scala.concurrent.ExecutionContext.Implicits.global
import models.Test
import play.api.Application
import play.api.test.{PlaySpecification, FakeApplication, WithApplication}

class TestDAOSlickSpec extends PlaySpecification with Logging{
  val app = FakeApplication()
  val app2dao = Application.instanceCache[TestDAO]
  val dao: TestDAO = app2dao(app)
  
  
  "TestDAO" should {
    "insert a Test Record" in new WithApplication(app){
      dao.insert(Test(None,"black")).map( r  => r should be equalTo(1))
      dao.findByName("black").map( 
        r  => println(r))
    }
  }
}

  