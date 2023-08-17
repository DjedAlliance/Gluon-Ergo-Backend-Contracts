package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.{Address, NetworkType}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

object MainNetOracleConfig extends ConfigHelper with TOracleConfig {

  val address: Address =
    Address.create(readKey(s"oracle.MAINNET.address").replaceAll("/$", ""))

  val nft: ErgoToken = ErgoToken(
    ErgoId.create(
      readKey(s"oracle.MAINNET.nft").replaceAll("/$", "")
    ),
    1
  )

  val paymentNft: ErgoToken = ErgoToken(
    ErgoId.create(
      readKey(s"oracle.MAINNET.payment.nft").replaceAll("/$", "")
    ),
    1
  )

  val paymentAddress: Address = Address.create(
    readKey(s"oracle.MAINNET.payment.address").replaceAll("/$", "")
  )
}

object TestNetOracleConfig extends ConfigHelper with TOracleConfig {

  val address: Address =
    Address.create(readKey(s"oracle.TESTNET.address").replaceAll("/$", ""))

  val nft: ErgoToken = ErgoToken(
    ErgoId.create(
      readKey(s"oracle.TESTNET.nft").replaceAll("/$", "")
    ),
    1
  )

  val paymentNft: ErgoToken = ErgoToken(
    ErgoId.create(
      readKey(s"oracle.TESTNET.payment.nft").replaceAll("/$", "")
    ),
    1
  )

  val paymentAddress: Address = Address.create(
    readKey(s"oracle.TESTNET.payment.address").replaceAll("/$", "")
  )
}

trait TOracleConfig {
  val address: Address
  val nft: ErgoToken
  val paymentNft: ErgoToken
  val paymentAddress: Address
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
