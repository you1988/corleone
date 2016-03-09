package binders
import play.api._
import  play.api.mvc._
object Binders {
  implicit def ListBindable = new QueryStringBindable[Seq[String]] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[String]]] ={
     params.get(key) match{
        case Some(value)=> {
          Some(Right(value.head.split(",").toList))
          }
        case None => None
      } 
  }
    def unbind(key: String, value: Seq[String]): String = value.toString()
  }
  implicit def OptionListBindable = new play.api.mvc.PathBindable[Option[Seq[String]]] {
    def bind(key: String, value: String): Either[String, Option[Seq[String]]] ={
      Right(Some(value.split(",")))
  }

    def unbind(key: String, value: Option[Seq[String]]): String = value.toString()
  }
}