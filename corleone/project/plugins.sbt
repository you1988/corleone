// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")

//Flyway plugin
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "3.2.1")

resolvers += "Flyway" at "http://flywaydb.org/repo"

// web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")


addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

