package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.ErgoAddressEncoder

object Configs extends ConfigHelper {
  lazy val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)

  lazy val refundThreadInterval: Int = readKey("threadIntervals.refund").toInt
}
