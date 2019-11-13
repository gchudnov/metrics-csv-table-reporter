import ReleaseTransformations._
import Dependencies._
import Utils._

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"
lazy val supportedScalaVersions = List(scala212, scala213)

credentials ++= Seq(
  Credentials(Path.userHome / ".sbt" / ".credentials-github"),
  Credentials(Path.userHome / ".sbt" / ".credentials-sonatype")
)

lazy val commonSettings = Seq(
  organization := "com.github.gchudnov",
  homepage := Some(url("https://github.com/gchudnov/metrics-csv-table-reporter")),
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/gchudnov/metrics-csv-table-reporter"),
      "scm:git@github.com:gchudnov/metrics-csv-table-reporter.git"
    )
  ),
  developers := List(
    Developer(id = "gchudnov", name = "Grigorii Chudnov", email = "g.chudnov@gmail.com", url = url("https://github.com/gchudnov"))
  ),
  crossScalaVersions := supportedScalaVersions,
  scalaVersion := scala213,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
  ),
  scalacOptions ++= lintFlags.value
)

lazy val sonatypeSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some("Sonatype Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
  releaseCrossBuild := true,
  releaseIgnoreUntrackedFiles := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommandAndRemaining("sonatypeReleaseAll"),
    setNextVersion,
    commitNextVersion,
    pushChanges,
  )
)

lazy val githubSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some("GitHubPackages" at "https://maven.pkg.github.com/gchudnov/metrics-csv-table-reporter"),
  releaseCrossBuild := true,
  releaseIgnoreUntrackedFiles := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val metrics = (project in file("."))
  .settings(commonSettings)
  .settings(sonatypeSettings)
  .settings(
    name := "metrics-csv-table-reporter",
    libraryDependencies ++= crossDependencies.value
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

lazy val crossDependencies = {
  val common = Seq(
    scalaTest % Test,
    metricsCore
  )

  def withCommon(values: ModuleID*) =
    common ++ values

  forScalaVersions {
    case (2, 12) =>
      withCommon(
        scalaCollectionCompat,
        utilBackports
      )

    case (2, 13) =>
      withCommon()

    case _ =>
      withCommon()
  }
}
