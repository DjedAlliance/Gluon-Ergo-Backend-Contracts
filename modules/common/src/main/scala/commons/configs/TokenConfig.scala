package commons.configs

import commons.configs.Configs.readKey
import commons.configs.NodeConfig.networkType
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.sdk.ErgoId

object GluonWTokenConfig {

  def getTokens(
    isMainNet: Boolean = networkType == NetworkType.MAINNET
  ): GluonWConfigTokens = {
    val networkTypeString: String = if (isMainNet) "MAINNET" else "TESTNET"

    GluonWConfigTokens(
      gluonWBoxNFTId =
        ErgoId.create(readKey(s"tokens.${networkTypeString}.gluonWNft")),
      neutronId =
        ErgoId.create(readKey(s"tokens.${networkTypeString}.neutron")),
      protonId = ErgoId.create(readKey(s"tokens.${networkTypeString}.proton"))
    )
  }
}

case class GluonWConfigTokens(
  override val gluonWBoxNFTId: ErgoId,
  override val neutronId: ErgoId,
  override val protonId: ErgoId
) extends TGluonWTokens

trait TGluonWTokens {
  val neutronId: ErgoId
  val protonId: ErgoId
  val gluonWBoxNFTId: ErgoId
}
