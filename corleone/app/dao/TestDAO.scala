
import models.{TestTable, Test}
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future


class TestDAO{
  private val testTable = TableQuery[TestTable]
  
  private def db: Database = Database.forConfig("slick.dbs.default")

  private def filterQuery(id: Long): Query[TestTable, Test, Seq] =
    testTable.filter(_.id === id)

  def findById(id: Long): Future[Test] = db.run(filterQuery(id).result.head)

  def findByName(name: String): Future[Test] = db.run(testTable.filter(x => x.name === name).result.head)
  
  def insert(test : Test): Future[Int] = db.run(testTable += test)
  
}