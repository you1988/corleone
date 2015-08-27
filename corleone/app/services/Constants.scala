package services

import scala.util.matching.Regex

object Constants {


  //DATABASE ERROR
  val PSQL_UNIQUE_VIOLATION_CODE_ERROR = "23505"
  val PSQL_ERROR_CONSTRAINT_MAPPING: Map[String, Map[String, Tuple2[Regex, String]]] = Map(
    PSQL_UNIQUE_VIOLATION_CODE_ERROR ->
      Map(
        "translation_key_tk_name_tk_is_active_idx" ->
          new Tuple2( """[(]([a-z_A-Z0-9]*)[,][ ][t][)]""".r, Constants.MESSAGE_CONSTANT_DUPLICATE_TAG_ERROR_MESSAGE)
        ,
        "translation_message_tm_translation_key_id_tm_language_code_tm_is_active_idx" ->
          new Tuple2( """[(][0-9]*[,][ ]([a-z_A-Z0-9]*)[,][ ][t][)]""".r, Constants.MESSAGE_CONSTANT_DUPLICATE_TRANSLATION_MESSAGE_ERROR_MESSAGE)
        ,
        "translation_tagging_tt_tag_id_tt_translation_key_id_tt_is_active_idx" ->
          new Tuple2( """[(]([0-9]*)[,][ ][0-9]*[,][ ][t][)]""".r, Constants.MESSAGE_CONSTANT_DUPLICATE_MESSAGE_KEY_ERROR_MESSAGE)

      )
  )
  //User Errors Message
  val MESSAGE_CONSTANT_DUPLICATE_MESSAGE_KEY_ERROR_MESSAGE = """Message constant with the following key: %s already exist."""
  val MESSAGE_CONSTANT_DUPLICATE_TRANSLATION_MESSAGE_ERROR_MESSAGE = """Message constant contains multiple translation for the following language: %s ."""
  val MESSAGE_CONSTANT_DUPLICATE_TAG_ERROR_MESSAGE = """Message constant contains duplicate tag ( tag : %s )"""
  val MESSAGE_CONSTANT_NOT_FOUND_WHITH_KEY = """There is no message constant with following key: %s """
  val MESSAGE_CONSTANT_NOT_FOUND_WHITH_TAG = """There is no message constant with following tag: %s """
  val MESSAGE_CONSTANT_NOT_FOUND_WHITH_NOT_DEFINED_IDENTIFIER = """There is no message constant with following identifiers: %s """
//Error Titles
  val REQUEST_MAL_FORMED_ERROR_TITLE="REQUEST MAL FORMED"
  val REQUEST_NOT_VALID_ERROR_TITLE="REQUEST IS NOT VALID"
  val REQUEST_NOT_FOUND_ERROR_TITLE="MESSAGE CONSTANT NOT FOUND"
  val REQUEST_MESSAGE_CONSTANT_ALREADY_EXIST_ERROR_TITLE="MESSAGE CONSTANT ALREADY EXIST"
  val REQUEST_SERVICE_UNAVAILABLE_ERROR_TITLE="CORLEONE IS UNAVAILABLE !!!!!!!"
  val REQUEST_UNEXPECTED_EXCEPTION_ERROR_TITLE="UNEXPECTED ERROR !!!!!!"
  val REQUEST_MESSAGE_CONSTANT_OUT_OF_DATE_ERROR_TITLE="MESSAGE CONSTANT IS OUT OF DATE"
  val REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_TAGS_ERROR_MESSAGE="Tags is invalid either duplicate or empty tags."
  val REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_TRANSLATIONS_ERROR_MESSAGE="Translations is invalid either duplicate translation for same language or none translations."
  val REQUEST_MESSAGE_CONSTANT_MAL_FORMED_UNSUPORTED_LANGUAGE_CODE_ERROR_MESSAGE="Language Code is not not supported."
  val REQUEST_MESSAGE_CONSTANT_MAL_FORMED_INVALID_LANGUAGE_CODE_ERROR_MESSAGE="Language is not not valide."
  val REQUEST_MESSAGE_CONSTANT_MAL_FORMED_EMPTY_FIELD_ERROR_MESSAGE="The following field %s should not be empty."
  val REQUEST_MESSAGE_CONSTANT_MAL_FORMED_FIELD_FORMAT_UNSUPORTED_ERROR_MESSAGE="The following field: %s should respect the following regex[a-z_A-Z0-9]*."
  //ERROR MESSAGE
  val REQUEST_SERVER_DOWN_ERROR_MESSAGE="The server is down for the moment."
  val REQUEST_UNEXPECTED_EXCEPTION_ERROR_MESSAGE="An error unexpected happened please contact the team."
  val REQUEST_OUT_OF_DATE_ERROR_MESSAGE="The version of constant message is out of date."

  //Error Frontend
  val REQUEST_FIELD_INVALID_IS_EMPTY_ERROR_MESSAGE="Message constant %s should not be empty."
  val REQUEST_FIELD_INVALID_VIOLATED_FORMAT_ERROR_MESSAGE="Message constant %s should respect the following regex [a-zA-z_0-9]*."
  val REQUEST_TRANSLATIONS_INVALID_TRANSLITIONS_LANGUAGES_DIFFERENT_ERROR_MESSAGE="The number of messages and language codes is not the same."
}