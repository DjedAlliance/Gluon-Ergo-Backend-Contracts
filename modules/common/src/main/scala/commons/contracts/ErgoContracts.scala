package commons.contracts

import enumeratum._

import scala.collection.immutable
import scala.io.Source

sealed trait ErgoContract extends EnumEntry {
  // Top Folder
  val domain: String = ""
  // Sub Folder
  val domainType: String = ""
  val contractType: ContractType = ContractTypes.None
  val fileExtension: String = ".es"
  val dirName: String = "ErgoContracts"
  val version: Long = 0

  lazy val fileName: String = if (version <= 1) {
    this.toString + fileExtension
  } else {
    this.toString + s"_v$version" + fileExtension
  }
  lazy val contractScript: String = get()

  def getPath: String =
    List(dirName, domain, domainType, contractType.plural, fileName)
      .filter(_.nonEmpty)
      .mkString("/")

  def get(): String = {
    val getViaPath: () => String = () => {
      val fullPath: String = getPath
      try {
        val contractSource =
          Source.fromResource(fullPath)

        val contractString = contractSource.mkString
        contractSource.close()

        contractString
      } catch {
        case _: NullPointerException =>
          throw new NullPointerException(s"$fullPath not found")
      }
    }

    val contractString: String = getViaPath()

    contractString
  }
}

object ContractScripts extends Enum[ErgoContract] {
  val values: immutable.IndexedSeq[ErgoContract] = findValues

  /**
    * Finds the Exle Contract and returns it as a string
    */
  case object GluonWBoxGuardScript extends GluonWBoxGuardScript

  // ===== Test Assets ===== //
  case object DummyErgoScript extends TestAssetsContract
}

//<editor-fold desc="Contract Domains">
/**
  * // ===== Contract Domains Instantiation =====
  */
// GluonW Box Guard Script
sealed trait GluonWContract extends ErgoContract {
  override val domain: String = "GluonW"
  override val domainType: String = "Ergs"
}
//</editor-fold>

//<editor-fold desc="Detailed Contract Types">
/**
  * // ===== Detailed Level Contracts =====
  */
sealed trait GluonWBoxGuardScript extends GluonWContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}

// Test Assets
sealed trait TestAssetsContract extends ErgoContract {
  override val domain: String = "TestAssets"
}
//</editor-fold>

//<editor-fold desc="Contract Type Enum">
/**
  * Describes the different contract types as Enums
  */
sealed trait ContractType extends EnumEntry { val plural: String }

object ContractTypes extends Enum[ContractType] {
  val values: immutable.IndexedSeq[ContractType] = findValues

  case object ProxyContract extends ContractType {
    override val plural = "ProxyContracts"
  }

  case object BoxGuardScript extends ContractType {
    override val plural = "BoxGuardScripts"
  }
  case object None extends ContractType { override val plural = "" }
}
//</editor-fold>
