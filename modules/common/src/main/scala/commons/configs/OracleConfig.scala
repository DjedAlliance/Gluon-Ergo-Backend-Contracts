package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

object MainNetOracleConfig extends ConfigHelper with TOracleConfig {

  val address: String =
    readKey(s"oracle.MAINNET.address").replaceAll("/$", "")

  val nft: ErgoToken = new ErgoToken(
    ErgoId.create(
      readKey(s"oracle.MAINNET.nft").replaceAll("/$", "")
    ),
    1
  )
}

object TestNetOracleConfig extends ConfigHelper with TOracleConfig {

  val address: String =
    readKey(s"oracle.TESTNET.address").replaceAll("/$", "")

  val nft: ErgoToken = new ErgoToken(
    ErgoId.create(
      readKey(s"oracle.TESTNET.nft").replaceAll("/$", "")
    ),
    1
  )
}

trait TOracleConfig {
  val address: String
  val nft: ErgoToken
}

object OracleConfig {

  def get(
    isMainNet: Boolean = (networkType == NetworkType.MAINNET)
  ): TOracleConfig =
    if (isMainNet) {
      MainNetOracleConfig
    } else {
      TestNetOracleConfig
    }
}
