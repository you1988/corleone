package dao

import play.api.Play
import play.api.Play.current
/**
 * @author mblascoespar
 */
trait SchemaConfig {
  
  def schema :Option[String] =  {
    Play.configuration.getString("db.default.schemas")
  }



}
