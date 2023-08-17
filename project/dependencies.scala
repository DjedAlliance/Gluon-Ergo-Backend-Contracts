import play.sbt.PlayImport.{guice, jdbc}
import sbt._
import versions._

object dependencies {

  val Ergo: List[ModuleID] = List(
    "org.scorexfoundation" %% "scrypto"     % ScryptoVersion,
    "org.ergoplatform"     %% "ergo-appkit" % ErgoAppKitVersion,
    "org.scorexfoundation" %% "sigma-state" % SigmaStateVersion
  )

  val Edge: List[ModuleID] = List(
    "io.github.ergo-lend"  %% "edge"                 % EdgeVersion,
    "ergoscript-simulator" %% "ergoscript-simulator" % SimVersion
  )

  val Circe: List[ModuleID] = List(
    "com.dripower" %% "play-circe" % PlayCirceVersion
  )

  val PlayApi: List[ModuleID] = List(
    "org.playframework.anorm" %% "anorm" % AnormVersion
  )

  val HttpDep: List[ModuleID] = List(
    "org.scalaj" %% "scalaj-http" % ScalaJHttpVersion
  )

  val Testing: List[ModuleID] = List(
    "org.scalatestplus.play" %% "scalatestplus-play" % ScalaTestPlusPlayVersion % Test
  )

  // Java
  val DependencyInjection: List[ModuleID] = List(
    guice
  )

  // Commons
  val Enumeratum: List[ModuleID] = List(
    "com.beachape" %% "enumeratum" % EnumeratumVersion
  )
}
