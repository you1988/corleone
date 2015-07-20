import java.sql.Timestamp
import java.time.LocalDateTime

import models.{LanguageCodes, TranslationMessage, TranslationKey}
import play.api.Application
import play.api.test.{FakeApplication, PlaySpecification, WithApplication}
import slick.util.Logging

import scala.concurrent.ExecutionContext.Implicits.global

class TranslationMessageDAOSlickSpec extends PlaySpecification with Logging{
   val app = FakeApplication()
   val app2tmdao = Application.instanceCache[TranslationMessageDAO]
   val tmdao: TranslationMessageDAO = app2tmdao(app)
   val app2tkdao = Application.instanceCache[TranslationKeyDAO]
   val tkdao: TranslationKeyDAO = app2tkdao(app)

  trait before {
    tkdao.deleteAll
    tmdao.deleteAll
  }
  
   "TranslationMessageDAO" should {
     "insert a Test Record" in new WithApplication(app) with before{
       private val tkName: String = "tkName"
       private val message: String = "hello"
       private val now: Timestamp = Timestamp.valueOf(LocalDateTime.now())
       
       tkdao.delete(tkName)
       tkdao.insert(TranslationKey(None, tkName, true, now)).map( r  => println(r.toString))
       tkdao.findActiveByName(tkName).map(
         tk => tmdao.insert(TranslationMessage(None,LanguageCodes.EN_GB,tk.id.get,message,true,now,now)).map( 
           tm  => println(tm.toString)))

       tmdao.insert(TranslationMessage(None,LanguageCodes.DE_DE,1l,message,true,now,now)).onFailure{case e :Throwable => println(e)}
       
       tmdao.findValueActive(message).onSuccess{
         case r  =>  r.value should equals(message)}
       tmdao.getTranslationKeyFromValueActive(message).onSuccess{
         case r  => r.name should equals(tkName)}
     }
   }
 }

  