package commons.configs

import commons.configs.Configs.readKey
import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.{ErgoId, NetworkType}

object GluonWTokenConfig {

  def getTokens(
    isMainNet: Boolean = networkType == NetworkType.MAINNET
  ): GluonWConfigTokens = {
    val networkTypeString: String = if (isMainNet) "MAINNET" else "TESTNET"

    GluonWConfigTokens(
      gluonWBoxNFTId =
        ErgoId.create(readKey(s"tokens.${networkTypeString}.gluonWNft")),
      sigGoldId =
        ErgoId.create(readKey(s"tokens.${networkTypeString}.sigGold")),
      sigGoldRsvId =
        ErgoId.create(readKey(s"tokens.${networkTypeString}.sigGoldRsv"))
    )
  }
}

case class GluonWConfigTokens(
  override val gluonWBoxNFTId: ErgoId,
  override val sigGoldId: ErgoId,
  override val sigGoldRsvId: ErgoId
) extends TGluonWTokens

trait TGluonWTokens {
  val sigGoldId: ErgoId
  val sigGoldRsvId: ErgoId
  val gluonWBoxNFTId: ErgoId
}
