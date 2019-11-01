import Dependencies._
import Utils._

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"

scalaVersion := scala213

lazy val commonSettings = Seq(
  organization := "com.github.gchudnov",
  homepage := Some(url("https://github.com/gchudnov/metrics-csv-table-reporter")),
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  developers := List(
    Developer("gchudnov", "Grigorii Chudnov", "g.chudnov@gmail.com", url("https://github.com/gchudnov"))
  ),
  crossScalaVersions := Seq(scala212, scala213),
  scalaVersion := scala213,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalacOptions ++= lintFlags.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val metrics = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "metrics-csv-table-reporter",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      metricsCore
    )
  )

lazy val lintFlags = {
  lazy val allVersionLintFlags = List("-encoding", "UTF-8", "-feature", "-unchecked", "-Ywarn-dead-code", "-Ywarn-numeric-widen")

  def withCommon(flags: String*) =
    allVersionLintFlags ++ flags

  forScalaVersions {
    case (2, 12) =>
      withCommon(
        "-deprecation", // Either#right is deprecated on Scala 2.13
        "-Xlint:_,-unused",
        "-Xfatal-warnings",
        "-Yno-adapted-args",
        "-Ywarn-unused:_,-implicits"
      ) // Some implicits are intentionally used just as evidences, triggering warnings

    case (2, 13) =>
      withCommon("-Ywarn-unused:_,-implicits")

    case _ =>
      withCommon()
  }
}
