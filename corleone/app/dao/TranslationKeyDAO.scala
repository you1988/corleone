package dao
import models.Tables.TranslationKeyTable
import models.{TranslationKey, Tables}
import helpers.PostgresDriverExtended.api._
import scala.concurrent.Future

class TranslationKeyDAO{
  
  private val translationKeyTable = Tables.translationKey
  private def db: Database = Database.forConfig("mydb")

  private def filterActiveName(name: String): Query[TranslationKeyTable, TranslationKey, Seq] =
    translationKeyTable.filter(x => x.name === name && x.isActive)

  def findActiveByName(name: String): Future[TranslationKey] = try db.run(filterActiveName(name).result.head) finally db.close()
  
  def insert(tk: TranslationKey): Future[Int] = try db.run(translationKeyTable += tk) finally db.close()

  def delete(name: String): Future[Int] = try db.run(filterActiveName(name).delete) finally db.close()

  def deleteAll = try db.run(translationKeyTable.filter(x => x.id === x.id).delete) finally db.close()
}