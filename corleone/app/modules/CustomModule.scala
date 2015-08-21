package modules
import play.api.inject._
import services.TranslationManage
import services.TranslationManageImpl
import services.FakeTranslationManager
import services.FakeTranslationManagerController
class CustomModule extends Module {
  def bindings(environment: play.api.Environment,
               configuration: play.api.Configuration) = Seq(
    configuration.getString("env.mode", Some(Set("mock"))) match {
      case _ => {
        bind[TranslationManage].to[TranslationManageImpl]
        }
//      case Some("mock") => bind[TranslationManage].to[FakeTranslationManager]
//      case _            => bind[TranslationManage].to[TranslationManageImpl]
    })
}