package commons.configs

import commons.configs.Configs.{readKey}
import org.ergoplatform.appkit.Address

object ServiceConfig {
  lazy val serviceOwner: Address = Address.create(readKey("service.owner"))

  lazy val serviceFeeAddress: Address =
    Address.create(readKey("service.feeAddress"))
  lazy val serviceFee: Long = readKey("service.fee").toLong

  lazy val profitSharingPercentage: Long = readKey(
    "service.profitSharingPercentage"
  ).toLong
}
