package gluonw.common

import commons.configs.{GluonWTokenConfig, TGluonWTokens}
import org.ergoplatform.appkit.{ErgoId, ErgoToken}

object GluonWAsset extends Enumeration {
  type Asset = Value
  val ERG, SIGGOLD, SIGGOLDRSV = Value
}

object GluonWTokens extends TGluonWTokens {
  override val sigGoldId: ErgoId = ErgoId.create(GluonWTokenConfig.sigGold)

  override val sigGoldRsvId: ErgoId =
    ErgoId.create(GluonWTokenConfig.sigGoldRsv)

  def getId(asset: String): ErgoId =
    asset match {
      case "SIGGOLD" =>
        GluonWTokens.sigGoldId
      case "SIGGOLDRSV" =>
        GluonWTokens.sigGoldRsvId
    }

  def get(asset: String, amount: Long): ErgoToken =
    asset match {
      case "SIGGOLD" =>
        new ErgoToken(GluonWTokens.sigGoldId, amount)
      case "SIGGOLDRSV" =>
        new ErgoToken(GluonWTokens.sigGoldRsvId, amount)
    }
}
