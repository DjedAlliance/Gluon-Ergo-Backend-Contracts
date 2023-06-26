import sbt.Keys.{moduleName, name}
import sbt.{file, Project}

object utils {

  def mkModule(id: String, description: String): Project =
    Project(id, file(s"modules/$id"))
      .settings(
        moduleName := id,
        name := description
      )

  lazy val commonScalacOptions = List(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-feature",
    "-unchecked",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ypartial-unification"
  )
}
