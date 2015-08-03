import helpers.PostgresDriverExtended.api._
import models.Tables
import org.specs2.specification.BeforeAll
import play.api.Application
import play.api.test.{FakeApplication, PlaySpecification}
import slick.jdbc.meta.MTable
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}


trait DbSpec extends PlaySpecification with BeforeAll {
  val app = FakeApplication()
  val app2tmdao = Application.instanceCache[TranslationMessageDAO]
  val tmdao : TranslationMessageDAO = app2tmdao(app)
  val app2tkdao = Application.instanceCache[TranslationKeyDAO]
  val tkdao : TranslationKeyDAO = app2tkdao(app)
  
  override def beforeAll  {
    Await.result(tmdao.deleteAll, 30 seconds)
    Await.result(tkdao.deleteAll, 30 seconds)
  }
}
