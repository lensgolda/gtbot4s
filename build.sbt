import Dependencies._

val scala3Version = "3.3.6"

lazy val root = project
  .in(file("."))
  .settings(

    name := "gtbot4s",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= (zioDeps ++ loggingDeps),

    Compile / run / javaOptions ++= Seq(
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED"
    ),

    Test / fork := true,
    run / fork  := true,

  )
