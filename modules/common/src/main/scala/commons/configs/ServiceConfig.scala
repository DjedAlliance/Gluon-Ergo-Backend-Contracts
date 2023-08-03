package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.{Address, NetworkType}

object ServiceConfig extends ConfigHelper {

  lazy val mainNetServiceOwner: Address =
    Address.create(readKey("service.MAINNET.owner"))

  lazy val mainNetServiceFeeAddress: Address =
    Address.create(readKey("service.MAINNET.feeAddress"))

  lazy val testNetServiceOwner: Address =
    Address.create(readKey("service.TESTNET.owner"))

  lazy val testNetServiceFeeAddress: Address =
    Address.create(readKey("service.TESTNET.feeAddress"))

  lazy val serviceFee: Long = readKey("service.fee").toLong

  lazy val profitSharingPercentage: Long = readKey(
    "service.profitSharingPercentage"
  ).toLong
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
