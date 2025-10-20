import sbt._

object Dependencies {
    // Versions
    lazy val zioVersion = "2.1.21"
    lazy val zioHttpVersion = "3.5.1"
    lazy val zioJsonVersion = "0.7.44"
    lazy val zioConfigVersion = "4.0.4"
    lazy val logbackClassicVersion = "1.5.19"
    lazy val zioLoggingVersion = "2.5.1"
    lazy val zioSchemaVersion = "1.7.5"

    // Libraries

    val zioDeps = Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-test" % zioVersion,
      "dev.zio" %% "zio-test-sbt" % zioVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion,
      "dev.zio" %% "zio-schema" % zioSchemaVersion,
      "dev.zio" %% "zio-schema-json" % zioSchemaVersion,
      "dev.zio" %% "zio-schema-derivation" % zioSchemaVersion
    )

    val loggingDeps = Seq(
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
      "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",
      "org.slf4j" % "slf4j-api" % "2.0.17",

      // For JSON layout
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.2"
    )

    val scalafixDependencies =
        "com.github.liancheng" % "organize-imports_2.13" % "0.6.0" excludeAll (
          ExclusionRule(organization = "org.scala-lang.modules")
        ),

}
