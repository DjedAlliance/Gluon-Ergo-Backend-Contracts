package commons.node

import commons.configs.{MainNetNodeConfig, NodeConfig, TestNetNodeConfig}
import edge.node.{
  BaseClient,
  MainNetNodeExplorerInfo,
  NodeInfo,
  TestNetNodeExplorerInfo
}
import org.ergoplatform.appkit.NetworkType

import javax.inject.Singleton

@Singleton
case class MainNodeInfo(networkType: NetworkType = NodeConfig.networkType)
    extends NodeInfo(
      mainNetNodeExplorerInfo = MainNetNodeExplorerInfo(
        mainnetNodeUrl = MainNetNodeConfig.nodeUrl,
        mainnetExplorerUrl = MainNetNodeConfig.explorerUrl
      ),
      testNetNodeExplorerInfo = TestNetNodeExplorerInfo(
        testnetNodeUrl = TestNetNodeConfig.nodeUrl,
        testnetExplorerUrl = TestNetNodeConfig.explorerUrl
      ),
      networkType = networkType
    )

@Singleton
class Client(networkType: NetworkType = NodeConfig.networkType)
    extends BaseClient(
      nodeInfo = MainNodeInfo(networkType)
    ) {}
