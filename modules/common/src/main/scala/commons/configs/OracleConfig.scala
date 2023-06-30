package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.{Address, NetworkType}

object OracleConfig extends ConfigHelper {

  lazy val mainNetOracleAddress: String =
    readKey(s"oracle.MAINNET.address").replaceAll("/$", "")

  lazy val testNetOracleAddress: String =
    readKey(s"oracle.TESTNET.address").replaceAll("/$", "")
}

object GetOracleConfig {

  def get(
    isMainNet: Boolean = (networkType == NetworkType.MAINNET)
  ): Address =
    if (isMainNet) {
      Address.create(OracleConfig.mainNetOracleAddress)
    } else {
      Address.create(OracleConfig.testNetOracleAddress)
    }
}
