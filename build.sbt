ThisBuild / scalaVersion := "3.3.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"

lazy val root = (project in file("."))
  .settings(
    name := "scala-template",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wunused:all"
    ),
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.4" % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
