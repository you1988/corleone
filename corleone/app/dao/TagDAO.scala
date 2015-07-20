package dao

import helpers.PostgresDriverExtended.api._
import models._

import scala.concurrent.Future

class TagDAO{

  private val tagTable = TableQuery[TagTable]
  private def db: Database = Database.forConfig("slick.dbs.default")
  
  def filterById(id: Long): Query[TagTable, TagHolder, Seq] =
    tagTable.filter(_.id === id)
  
  def findById(id: Long): Future[TagHolder] = try db.run(filterById(id).result.head) finally db.close()
  
}