import sbt.Keys.scalaVersion
import sbt.{CrossVersion, Def}

object Utils {

  def forScalaVersions[A](f: ((Long, Long)) => A) = Def.setting {
    f(CrossVersion.partialVersion(scalaVersion.value).getOrElse(sys.error("Improperly formatted Scala version.")))
  }
}
