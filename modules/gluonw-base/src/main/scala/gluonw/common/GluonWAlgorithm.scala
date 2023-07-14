package gluonw.common

import commons.ErgCommons
import gluonw.boxes.{GluonWBox, GoldOracleBox}
import io.circe.Json
import org.ergoplatform.appkit.{ErgoId, ErgoToken}

/**
  * AssetPrice
  * Gives the price of the asset
  */
trait TAssetPrice {
  val name: String
  val id: ErgoId
  val price: Long

  def toJson: Json =
    Json.fromFields(
      List(
        ("assetName", Json.fromString(name)),
        ("id", Json.fromString(id.toString)),
        ("price", Json.fromLong(price))
      )
    )
}

case class AssetPrice(name: String, price: Long, id: ErgoId) extends TAssetPrice

trait TGluonWConstants {
  def qStar: Long

  def rightHandMin(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): Long

  def fusionRatio(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): Long

  def phiT: Long

  def phiFusion: Long

  def varPhiBeta: Long

  val precision: Long
}

case class GluonWConstants() extends TGluonWConstants {
  override def qStar: Long = (0.0066 * precision).toLong

  def rightHandMin(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): Long =
    neutronsInCirculation * pt / fissionedErg

  override def fusionRatio(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): Long = {
    val rightHandMin = rightHandMin(neutronsInCirculation, pt, fissionedErg)
    Math.min(qStar, rightHandMin)
  }

  override def phiT: Long = (0.01 * precision).toLong

  override def phiFusion: Long = (0.01 * precision).toLong

  override def varPhiBeta: Long = (0.02 * precision).toLong

  override val precision: Long = ErgCommons.MinBoxFee
}

/**
  * Algorithm and rates
  *
  * Note regarding Prices:
  * When getting rate, we will get rate for both
  */
trait TGluonWAlgorithm {

  def fission(inputGluonWBox: GluonWBox, ergAmount: Long)(
    implicit goldOracleBox: GoldOracleBox
  ): GluonWBox

  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long)(
    implicit goldOracleBox: GoldOracleBox
  ): GluonWBox

  def betaDecayPlus(inputGluonWBox: GluonWBox, goldAmount: Long)(
    implicit goldOracleBox: GoldOracleBox
  ): GluonWBox

  def betaDecayMinus(inputGluonWBox: GluonWBox, rsvAmount: Long)(
    implicit goldOracleBox: GoldOracleBox
  ): GluonWBox

  def calculateFissionPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def calculateFusionPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice])

  def calculateBetaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    goldAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def calculateBetaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    rsvAmount: Long
  ): (GluonWBox, Seq[AssetPrice])
}

case class GluonWAlgorithm(gluonWConstants: TGluonWConstants)
    extends TGluonWAlgorithm {

  override def fission(inputGluonWBox: GluonWBox, ergAmount: Long)(
    implicit goldOracleBox: GoldOracleBox
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val phiT: Long = gluonWConstants.phiT

    val ergToChange: Long = ergAmount - ErgCommons.MinMinerFee

    val outNeutronsAmount: Long = ergToChange * (rErg / sNeutrons) / (1 - phiT)
    val outProtonsAmount: Long = ergToChange * (rErg / sProtons) / (1 - phiT)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.sigGoldId =>
          new ErgoToken(token.getId, token.getValue - outNeutronsAmount)
        case GluonWTokens.sigGoldRsvId =>
          new ErgoToken(token.getId, token.getValue - outProtonsAmount)
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value + ergToChange,
      tokens = tokens
    )
  }

  override def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long)(
    implicit goldOracleBox: GoldOracleBox
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val phiFusion: Long = gluonWConstants.phiFusion

    val ergToChange: Long = ergRedeemed - ErgCommons.MinMinerFee

    val inNeutronsAmount: Long =
      ergToChange * (1 - phiFusion) / (rErg / sNeutrons)
    val inProtonsAmount: Long =
      ergToChange * (1 - phiFusion) / (rErg / sProtons)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.sigGoldId =>
          new ErgoToken(token.getId, token.getValue + inNeutronsAmount)
        case GluonWTokens.sigGoldRsvId =>
          new ErgoToken(token.getId, token.getValue + inProtonsAmount)
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value + ergToChange,
      tokens = tokens
    )
  }

  override def betaDecayPlus(
    inputGluonWBox: GluonWBox,
    goldAmount: Long
  )(implicit goldOracleBox: GoldOracleBox): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val varPhiBeta: Long = gluonWConstants.varPhiBeta

    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, goldOracleBox.getGoldPrice, rErg)

    val outProtonsAmount: Long = goldAmount * (1 - varPhiBeta) *
      (1 - fusionRatio) / fusionRatio * (sNeutrons / sProtons)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.sigGoldId =>
          new ErgoToken(token.getId, token.getValue - goldAmount)
        case GluonWTokens.sigGoldRsvId =>
          new ErgoToken(token.getId, token.getValue + outProtonsAmount)
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value,
      tokens = tokens
    )
  }

  override def betaDecayMinus(
    inputGluonWBox: GluonWBox,
    rsvAmount: Long
  )(implicit goldOracleBox: GoldOracleBox): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val varPhiBeta: Long = gluonWConstants.varPhiBeta

    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, goldOracleBox.getGoldPrice, rErg)

    val outNeutronsAmount: Long = rsvAmount * (1 - varPhiBeta) *
      fusionRatio / (1 - fusionRatio) * (sProtons / sNeutrons)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.sigGoldId =>
          new ErgoToken(token.getId, token.getValue - outNeutronsAmount)
        case GluonWTokens.sigGoldRsvId =>
          new ErgoToken(token.getId, token.getValue - rsvAmount)
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value,
      tokens = tokens
    )
  }

  override def calculateFissionPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = ???

  override def calculateFusionPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice]) = ???

  override def calculateBetaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    goldAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = ???

  override def calculateBetaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    rsvAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = ???
}
