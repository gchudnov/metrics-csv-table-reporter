import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.9.0-SNAPSHOT"
ThisBuild / organization     := "com.github"
ThisBuild / organizationName := "gchudnov"

lazy val root = (project in file("."))
  .settings(
    name := "metrics-csv-table-reporter",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      metricsCore
      )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
