package commons.configs

import commons.configs.Configs.readKey
import org.ergoplatform.appkit.ErgoId

object GluonWTokenConfig {
  lazy val sigGold: String = readKey("tokens.sigGold")
  lazy val sigGoldRsv: String = readKey("tokens.sigGoldRsv")
}

trait TGluonWTokens {
  val sigGoldId: ErgoId
  val sigGoldRsvId: ErgoId
}
