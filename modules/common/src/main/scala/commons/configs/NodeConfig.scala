package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.NetworkType

object NodeConfig extends ConfigHelper {

  lazy val networkType: NetworkType =
    if (readKey("node.networkType").toLowerCase.equals("mainnet"))
      NetworkType.MAINNET
    else
      NetworkType.TESTNET
}

object MainNetNodeConfig extends ConfigHelper {
  lazy val nodeUrl: String = readKey(s"node.MAINNET.url").replaceAll("/$", "")

  lazy val explorerUrl: String =
    readKey(s"explorer.MAINNET.url").replaceAll("/$", "")

  lazy val explorerFront: String =
    readKey(s"explorer.MAINNET.front").replaceAll("/$", "")
}

object TestNetNodeConfig extends ConfigHelper {
  lazy val nodeUrl: String = readKey(s"node.TESTNET.url").replaceAll("/$", "")

  lazy val explorerUrl: String =
    readKey(s"explorer.TESTNET.url").replaceAll("/$", "")

  lazy val explorerFront: String =
    readKey(s"explorer.TESTNET.front").replaceAll("/$", "")
}

case class NodeConfig(
  nodeUrl: String,
  explorerUrl: String,
  explorerFront: String
)

object GetNodeConfig {

  def get(
    isMainNet: Boolean = (networkType == NetworkType.MAINNET)
  ): NodeConfig =
    if (isMainNet) {
      NodeConfig(
        nodeUrl = MainNetNodeConfig.nodeUrl,
        explorerUrl = MainNetNodeConfig.explorerUrl,
        explorerFront = MainNetNodeConfig.explorerFront
      )
    } else {
      NodeConfig(
        nodeUrl = TestNetNodeConfig.nodeUrl,
        explorerUrl = TestNetNodeConfig.explorerUrl,
        explorerFront = TestNetNodeConfig.explorerFront
      )
    }
}
