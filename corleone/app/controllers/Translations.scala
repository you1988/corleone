package controllers
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.Singleton
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.twirl.api.Html;
import models.MessageConstant.MessageConstant
class Translations extends Controller {
//  def createTranslation = Action { req =>
//    
// 
//    Logger.debug(req.body.toString());
//    val message = new MessageConstant("test",req.body.asFormUrlEncoded.get("PACK_1"),Map(("en","test"),("tag","tag")),"mag");
// 
//    
//    Ok(views.html.index(message))
//  }
  
//   def searchWithKey(constantKey:String) = Action{ 
//     val message = new MessageConstant(constantKey,Seq("tag1","tag2"),Map(("en","test"),("tag","tag")),constantKey);
//       Ok(Json.toJson(message))
//  }
//   def searchWithTag(tag:String) = Action{ 
//     val message = new MessageConstant(tag,Seq("tag1","tag2"),Map(("en","test"),("tag","tag")),tag);
//       Ok(Json.toJson(message))
//  }
// 
//     def searchWithKey(constantKey:String) = Action{ 
//     val message = new MessageConstant(constantKey,Seq("tag1","tag2"),Map(("en","test"),("tag","tag")),constantKey);
//       Ok(Json.toJson(message))
//  }
   def search = Action{ 
     req =>
       val messages = Seq();

    Ok(views.html.main(Seq("tag1", "tag2"))(views.html.translationSearchView(messages))(null));

  }
   def updateForm = Action{ 
     req =>
       val message = null;
    Ok(views.html.main(Seq("tag1", "tag2"))(views.html.UpdateTranslationForm(message,Seq("en", "fr")))(null));

  }
   
    
}