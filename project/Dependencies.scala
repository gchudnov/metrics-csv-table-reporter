import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  lazy val metricsCore = "io.dropwizard.metrics" % "metrics-core" % "4.1.1"
  lazy val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2"
  lazy val utilBackports = "com.github.bigwheel" %% "util-backports" % "2.0"
}
