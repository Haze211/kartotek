ThisBuild / scalaVersion := "3.3.8"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.kartotek"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all"
)

Global / autoStartServer := false

val Http4sVersion     = "0.23.36"
val CirceVersion      = "0.14.10"
val Mongo4CatsVersion = "0.7.17"
val LaminarVersion    = "0.14.2"
val MunitVersion      = "1.0.4"

lazy val root = (project in file("."))
  .aggregate(backend, frontend)
  .settings(
    name := "kartotek",
    publish / skip := true,
    // The root project holds no sources of its own — everything lives in the
    // backend/ and frontend/ modules.
    Compile / unmanagedSourceDirectories := Nil,
    Test / unmanagedSourceDirectories    := Nil
  )

lazy val backend = (project in file("backend"))
  .settings(
    name := "kartotek-backend",
    libraryDependencies ++= Seq(
      "org.http4s"                 %% "http4s-dsl"          % Http4sVersion,
      "org.http4s"                 %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"                 %% "http4s-circe"        % Http4sVersion,
      "io.circe"                   %% "circe-core"          % CirceVersion,
      "io.circe"                   %% "circe-generic"       % CirceVersion,
      "io.circe"                   %% "circe-parser"        % CirceVersion,
      "io.github.kirill5k"         %% "mongo4cats-core"     % Mongo4CatsVersion,
      "io.github.kirill5k"         %% "mongo4cats-circe"    % Mongo4CatsVersion,
      "com.typesafe.scala-logging" %% "scala-logging"       % "3.9.5",
      "ch.qos.logback"              % "logback-classic"     % "1.5.6",
      "org.scalameta"              %% "munit"               % MunitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "kartotek-frontend",
    libraryDependencies += "com.raquo" %%% "laminar" % LaminarVersion,
    scalaJSUseMainModuleInitializer := true
  )

