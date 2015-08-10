package binders
import play.api._
import  play.api.mvc._
object Binders {
  
//   implicit def OptionBindable[T : play.api.mvc.PathBindable] = new play.api.mvc.PathBindable[Option[T]] {
//     
//    def bind(key: String, value: String): Either[String, Option[T]] =
//      implicitly[play.api.mvc.PathBindable[T]].
//        bind(key, value).
//        fold(
//          left => Left(left),
//          right => Right(Some(right))
//        )
//
//    def unbind(key: String, value: Option[T]): String = value map (_.toString) getOrElse ""
//  }
//
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
//    /**
//   * QueryString binder for List
//   */
//  implicit def bindableList[T: QueryStringBindable] = new QueryStringBindable[List[T]] {
//    def bind(key: String, params: Map[String, Seq[String]]) = Some(Right(bindList[T](key, params)))
//    def unbind(key: String, values: List[T]) = unbindList(key, values)
//  }
//
//  private def bindList[T: QueryStringBindable](key: String, params: Map[String, Seq[String]]): List[T] = {
//    Logger.error(params.toString())
//    for {
//      values <- params.get(key).toList
//      rawValue <- values
//      bound <- implicitly[QueryStringBindable[T]].bind(key, Map(key -> Seq(rawValue)))
//      value <- bound.right.toOption
//    } yield value
//  }
//
//  private def unbindList[T: QueryStringBindable](key: String, values: Iterable[T]): String = {
//    (for (value <- values) yield {
//      implicitly[QueryStringBindable[T]].unbind(key, value)
//    }).mkString("&")
//  }



}