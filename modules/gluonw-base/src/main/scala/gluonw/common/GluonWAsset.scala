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

  def get(asset: String, amount: Long): ErgoToken =
    asset match {
      case GluonWAsset.SIGGOLD.toString =>
        new ErgoToken(GluonWTokens.sigGoldId, amount)
      case GluonWAsset.SIGGOLDRSV.toString =>
        new ErgoToken(GluonWTokens.sigGoldRsvId, amount)
      // @todo kii: Should throw Token does not exists
      case _ => throw Throwable
    }
}
