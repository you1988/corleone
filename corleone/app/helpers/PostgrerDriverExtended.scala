package helpers

import com.github.tminglei.slickpg._
import slick.driver.PostgresDriver
import models.{Operations, LanguageCodes}

trait WithPostgresDriver {
  val driver: PostgresDriverExtended
}

trait PostgresDriverExtended extends PostgresDriver with PgEnumSupport{
  override val api = new API with EnumImplicits {}  
  trait EnumImplicits {
    implicit val languageCodeTypeMapper = createEnumJdbcType("language_code", LanguageCodes)
    implicit val languageCodeListTypeMapper = createEnumListJdbcType("language_code", LanguageCodes)
    implicit val languageCodeColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(LanguageCodes)
    implicit val languageCodeOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(LanguageCodes)

    implicit val operationTypeMapper = createEnumJdbcType("operation", Operations)
    implicit val operationListTypeMapper = createEnumListJdbcType("operation", Operations)
    implicit val operationColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(Operations)
    implicit val operationOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(Operations)

  }
}

object PostgresDriverExtended extends PostgresDriverExtended
