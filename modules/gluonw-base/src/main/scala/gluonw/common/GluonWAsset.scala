package gluonw.common

import commons.configs.{GluonWTokenConfig, TGluonWTokens}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

object GluonWAsset extends Enumeration {
  type Asset = Value
  val ERG, NEUTRON, PROTON, NFT = Value
}

object GluonWTokens extends TGluonWTokens {

  override val gluonWBoxNFTId: ErgoId =
    GluonWTokenConfig.getTokens().gluonWBoxNFTId
  override val neutronId: ErgoId = GluonWTokenConfig.getTokens().neutronId

  override val protonId: ErgoId =
    GluonWTokenConfig.getTokens().protonId

  def getId(asset: String): ErgoId =
    asset match {
      case "NEUTRON" =>
        GluonWTokens.neutronId
      case "PROTON" =>
        GluonWTokens.protonId
      case "NFT" =>
        GluonWTokens.gluonWBoxNFTId
    }

  def get(asset: String, amount: Long): ErgoToken =
    asset match {
      case "NEUTRON" =>
        new ErgoToken(GluonWTokens.neutronId, amount)
      case "PROTON" =>
        new ErgoToken(GluonWTokens.protonId, amount)
      case "NFT" =>
        new ErgoToken(GluonWTokens.gluonWBoxNFTId, amount)
    }
}
