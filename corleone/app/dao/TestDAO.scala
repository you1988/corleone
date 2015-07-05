package dao

import slick.ast.ColumnOption.AutoInc

import scala.concurrent.Future

import javax.inject.Inject
import models.Test
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

class TestDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] with SchemaConfig {
  import driver.api._

  private val Tests = TableQuery[TestsTable]

  def all(): Future[Seq[Test]] = db.run(Tests.result)
  
  def delete(name: String): Future[Int] = db.run(Tests.filter(_.name === name).delete)

  def insert(test: Test): Future[Unit] = db.run(Tests += test).map { _ => () }

  private class TestsTable(tag: Tag) extends Table[Test](tag, schema, "test") {
    
    def id = column[Int]("t_id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("t_name")

    
    def * = name <> (Test,Test.unapply)
     
  }
  
  
  
  
}