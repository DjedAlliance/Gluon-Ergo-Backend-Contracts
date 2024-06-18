package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.{Address, NetworkType}
import org.ergoplatform.sdk.JavaHelpers
import org.ergoplatform.sdk.JavaHelpers.SigmaDsl
import sigmastate.eval.CostingSigmaDslBuilder.proveDlog

object ServiceConfig extends ConfigHelper {

  lazy val mainNetServiceOwner: Address =
    Address.create(readKey("service.MAINNET.owner"))

  lazy val mainNetServiceFeeAddress: Address =
    Address.create(readKey("service.MAINNET.feeAddress"))

  lazy val testNetServiceOwner: Address =
    Address.create(readKey("service.TESTNET.owner"))

  lazy val testNetServiceFeeAddress: Address =
    Address.create(readKey("service.TESTNET.feeAddress"))
}

object MultiSigConfig extends ConfigHelper {
  lazy val testnetBound: Int = readKey("service.TESTNET.multisig.bound").toInt
  lazy val mainnetBound: Int = readKey("service.MAINNET.multisig.bound").toInt

  lazy val testnetAddress1: Address =
    Address.create(readKey("service.TESTNET.multisig.address1"))

  lazy val testnetAddress2: Address =
    Address.create(readKey("service.TESTNET.multisig.address2"))

  lazy val testnetAddress3: Address =
    Address.create(readKey("service.TESTNET.multisig.address3"))

  lazy val mainnetAddress1: Address =
    Address.create(readKey("service.MAINNET.multisig.address1"))

  lazy val mainnetAddress2: Address =
    Address.create(readKey("service.MAINNET.multisig.address2"))

  lazy val mainnetAddress3: Address =
    Address.create(readKey("service.MAINNET.multisig.address3"))

  lazy val testnetMultisig: MultiSig = MultiSig(
    testnetBound,
    Array(testnetAddress1, testnetAddress2, testnetAddress3)
  )

  lazy val mainnetMultisig: MultiSig = MultiSig(
    mainnetBound,
    Array(mainnetAddress1, mainnetAddress2, mainnetAddress3)
  )
}

case class MultiSig(bound: Int, addresses: Array[Address])

object MultiSig {

  def getServiceMultiSig(
    isMainNet: Boolean = (networkType == NetworkType.MAINNET)
  ): MultiSig =
    if (isMainNet) {
      MultiSigConfig.mainnetMultisig
    } else {
      MultiSigConfig.testnetMultisig
    }

  def getServiceMultiSigAddress: Address = {
    val multiSig = getServiceMultiSig()
    val sigmaProps = multiSig.addresses.map { address =>
      proveDlog(address.getPublicKeyGE)
    }
    val collSigmaProps =
      JavaHelpers.SigmaDsl.Colls.fromArray(sigmaProps)

    val sigmaProp = SigmaDsl.atLeast(multiSig.bound, collSigmaProps)

    Address.fromPropositionBytes(
      NetworkType.TESTNET,
      sigmaProp.propBytes.toArray
    )
  }
}

object GetServiceConfig {

  def getServiceOwner(
    isMainNet: Boolean = (networkType == NetworkType.MAINNET)
  ): Address =
    if (isMainNet) {
      ServiceConfig.mainNetServiceOwner
    } else {
      ServiceConfig.testNetServiceOwner
    }

  def getServiceFeeAddress(
    isMainNet: Boolean = (networkType == NetworkType.MAINNET)
  ): Address =
    if (isMainNet) {
      ServiceConfig.mainNetServiceFeeAddress
    } else {
      ServiceConfig.testNetServiceFeeAddress
    }
}
