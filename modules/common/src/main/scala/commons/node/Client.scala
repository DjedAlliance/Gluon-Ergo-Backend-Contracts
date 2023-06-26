package commons.node

import commons.configs.{MainNetNodeConfig, NodeConfig, TestNetNodeConfig}
import node.{
  BaseClient,
  MainNetNodeExplorerInfo,
  NodeInfo,
  TestNetNodeExplorerInfo
}

import javax.inject.Singleton

@Singleton
case class MainNodeInfo()
    extends NodeInfo(
      mainNetNodeExplorerInfo = MainNetNodeExplorerInfo(
        mainnetNodeUrl = MainNetNodeConfig.nodeUrl,
        mainnetExplorerUrl = MainNetNodeConfig.explorerUrl
      ),
      testNetNodeExplorerInfo = TestNetNodeExplorerInfo(
        testnetNodeUrl = TestNetNodeConfig.nodeUrl,
        testnetExplorerUrl = TestNetNodeConfig.explorerUrl
      ),
      networkType = NodeConfig.networkType
    )

@Singleton
class Client()
    extends BaseClient(
      nodeInfo = MainNodeInfo()
    ) {}
