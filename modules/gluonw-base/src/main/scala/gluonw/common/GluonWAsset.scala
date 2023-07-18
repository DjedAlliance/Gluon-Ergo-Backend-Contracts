package gluonw.common

import commons.configs.{GluonWTokenConfig, TGluonWTokens}
import org.ergoplatform.appkit.{ErgoId, ErgoToken}

object GluonWAsset extends Enumeration {
  type Asset = Value
  val ERG, SIGGOLD, SIGGOLDRSV, NFT = Value
}

object GluonWTokens extends TGluonWTokens {

  override val gluonWBoxNFTId: ErgoId =
    GluonWTokenConfig.getTokens().gluonWBoxNFTId
  override val sigGoldId: ErgoId = GluonWTokenConfig.getTokens().sigGoldId

  override val sigGoldRsvId: ErgoId =
    GluonWTokenConfig.getTokens().sigGoldRsvId

  def getId(asset: String): ErgoId =
    asset match {
      case "SIGGOLD" =>
        GluonWTokens.sigGoldId
      case "SIGGOLDRSV" =>
        GluonWTokens.sigGoldRsvId
      case "NFT" =>
        GluonWTokens.gluonWBoxNFTId
    }

  def get(asset: String, amount: Long): ErgoToken =
    asset match {
      case "SIGGOLD" =>
        new ErgoToken(GluonWTokens.sigGoldId, amount)
      case "SIGGOLDRSV" =>
        new ErgoToken(GluonWTokens.sigGoldRsvId, amount)
      case "NFT" =>
        new ErgoToken(GluonWTokens.gluonWBoxNFTId, amount)
    }
}
