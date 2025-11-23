import Dependencies.*
import au.com.onegeek.sbtdotenv.SbtDotenv.autoImport.*

val isCI = sys.env.get("CI").contains("true")

ThisBuild / organization := "com.github.lensgolda"
// ThisBuild / run / javaOptions ++= envVars.value.map { case (k, v) =>
//     s"-D$k=$v"
// }.toSeq

val scala3Version = "3.3.6"

lazy val root = project
    .in(file("."))
    .settings(
      name := "gtbot4s",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := scala3Version,
      resolvers ++= Seq(
        "Sonatype OSS releases" at "https://oss.sonatype.org/content/repositories/releases"
      ),
      libraryDependencies ++= zioDeps
          ++ loggingDeps
          ++ googleapisDeps
          ++ Seq("org.scala-lang.modules" %% "scala-xml" % "2.4.0"),
      libraryDependencies += scalafixDependencies,
      Compile / run / javaOptions ++= Seq(
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED"
      ),

      // if local run, take env vars from .env file with sbtdotenv plugin
      if (!isCI) {
          Def.settings(
            run / javaOptions ++= envVars.value.map { case (k, v) =>
                s"-D$k=$v"
            }.toSeq
          )
      } else {
          Def.settings()
      }

      // run / envVars := envVars.value
    )
