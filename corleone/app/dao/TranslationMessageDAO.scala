import helpers.PostgresDriverExtended.api._
import models.{TranslationKeyTable, TranslationMessageTable, TranslationKey, TranslationMessage}

import scala.concurrent.Future

class TranslationMessageDAO{
  
  private val translationMessageTable = TableQuery[TranslationMessageTable]
  private val translationKeyTable = TableQuery[TranslationKeyTable]
  private def db: Database = Database.forConfig("slick.dbs.default")
  
  private def filterValueActive(value: String): Query[TranslationMessageTable, TranslationMessage, Seq] = translationMessageTable.filter(x => x.value === value && x.isActive === true)

  def findValueActive(value: String): Future[TranslationMessage] = try db.run(filterValueActive(value).result.head) finally db.close()
  
  def hasValue(value: String): Query[TranslationMessageTable, TranslationMessage, Seq] = translationMessageTable.filter(x => x.value === value)

  def isActive(isActive: Boolean): Query[TranslationMessageTable, TranslationMessage, Seq] = translationMessageTable.filter(x => x.isActive === isActive)
  
  def insert(tm: TranslationMessage): Future[Int] = try db.run(translationMessageTable += tm) finally db.close()

  def deleteAll = try db.run(translationMessageTable.filter(x => x.id === x.id).delete) finally db.close()
  
  def getTranslationKeyFromValueActive(value: String) : Future[TranslationKey] = try db.run((for {
    tm <- translationMessageTable if tm.value === value && tm.isActive
    tk <- translationKeyTable if tm.translationKeyId === tk.id
  } yield (tk)).result.head) finally db.close()
  
  
}