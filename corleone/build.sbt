name := """corleone"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  filters,
  "org.webjars"  % "bootstrap"    % "3.3.5",
  "org.webjars"  % "jquery"       % "2.1.4",
  "org.webjars"  % "jquery-ui"    % "1.11.4",
  "org.webjars" %% "webjars-play" % "2.4.0-1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator



// generate scm-source.json
val process = Process("scripts/generate_scm_source.sh")
val exitCode = process !

// add scm-source.json 
mappings in Universal ++=
  (baseDirectory.value / "."  * "scm-source.json" get) map (x => x -> ("scm-source.json"))


// Docker related configuration
// see http://www.scala-sbt.org/sbt-native-packager/formats/docker.html
maintainer := "Team WHIP <team-whip@zalando.de>"

// use the Zalando base image
// see https://registry.hub.docker.com/u/zalando/openjdk/tags/manage/
dockerBaseImage := "zalando/openjdk:8u40-b09-4"

// exposing the play ports
dockerExposedPorts := Seq(9000, 9443)

// define Zalando docker registry
dockerRepository := Some("https://pierone.stups.zalan.do/whip")


import com.typesafe.sbt.packager.docker._

dockerCommands := dockerCommands.value.filterNot {
  // ExecCmd is a case class, and args is a varargs variable, so you need to bind it with @
  // we need to remove them here because they are not placed at the end of the Dockerfile otherwise
  case ExecCmd("ENTRYPOINT", args @ _*) => true
  case ExecCmd("CMD", args @ _*) => true

  // dont filter the rest
  case cmd  => false
}

// directory must be accessible for non-root users
dockerCommands += Cmd("RUN", "chmod -R a+rwx /opt/docker")

// scm-source.json has to be placed in the root dircectory of a Docker image
dockerCommands += Cmd("ADD", "/opt/docker/scm-source.json / ")

// application execution
dockerCommands += Cmd("ENTRYPOINT", "bin/corleone")
dockerCommands += Cmd("CMD", "")
