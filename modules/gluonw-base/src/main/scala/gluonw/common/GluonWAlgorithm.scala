package gluonw.common

import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import io.circe.Json
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

/**
  * AssetPrice
  * Gives the price of the asset
  */
case class AssetPrice(name: String, price: Long, id: ErgoId) {

  def toJson: Json =
    Json.fromFields(
      List(
        ("assetName", Json.fromString(name)),
        ("id", Json.fromString(id.toString)),
        ("price", Json.fromLong(price))
      )
    )
}

trait TGluonWConstants {
  def qStar: Long

  def fusionRatio(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): Long

  def phiFission: Long

  def phiFusion: Long

  def varPhiBeta: Long

  val precision: Long
}

case class GluonWConstants(precision: Long = GluonWBoxConstants.PRECISION)
    extends TGluonWConstants {
  override def qStar: Long = (0.0066 * precision).toLong

  override def fusionRatio(
    neutronsInCirculation: Long,
    phiFission: Long,
    fissionedErg: Long
  ): Long = {
    val rightHandMinVal: Long =
      neutronsInCirculation * (phiFission / fissionedErg)
    Math.min(qStar, rightHandMinVal)
  }

  override def phiFission: Long = (0.01 * precision).toLong

  override def phiFusion: Long = (0.01 * precision).toLong

  override def varPhiBeta: Long = (0.02 * precision).toLong
}

/**
  * Algorithm and rates
  *
  * Note regarding Prices:
  * When getting rate, we will get rates for both Protons and Neutrons
  * most of the time. Unless it's a one to one tx.
  * For fusion, we will take the amount of Erg to be redeemed, and
  * return the amount of Protons and Neutrons needed for fusion. This
  * will be further improve to aid user experience.
  */
trait TGluonWAlgorithm {

  def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox

  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox

  def betaDecayPlus(inputGluonWBox: GluonWBox, neutronsAmount: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox

  def betaDecayMinus(inputGluonWBox: GluonWBox, protonsAmount: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox

  def fissionPrice(
    inputGluonWBox: GluonWBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def fusionPrice(
    inputGluonWBox: GluonWBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice])

  def betaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    neutronsAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def betaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    protonsAmount: Long
  ): (GluonWBox, Seq[AssetPrice])
}

/**
  * @param sProtons Protons Circulating Supply
  * @param sNeutrons Neutrons Circulating Supply
  * @param rErg Erg fissioned amount
  */
case class GluonWCalculator(
  sProtons: Long,
  sNeutrons: Long,
  rErg: Long,
  gluonWConstants: TGluonWConstants
) {

  def fission(ergToFission: Long): GluonWBoxOutputAssetAmount = {
    val outNeutronsAmount: Long =
      ergToFission * sNeutrons * (gluonWConstants.precision - gluonWConstants.phiFission) / (rErg)
    val outProtonsAmount: Long =
      ergToFission * sProtons * (gluonWConstants.precision - gluonWConstants.phiFission) / (rErg)

    GluonWBoxOutputAssetAmount(
      ergAmount = -ergToFission,
      neutronsAmount = outNeutronsAmount,
      protonsAmount = outProtonsAmount
    )
  }

  def fusion(ergFusioned: Long): GluonWBoxOutputAssetAmount = {
    val inNeutronsAmount: Long =
      ergFusioned * sNeutrons * gluonWConstants.precision / (rErg * (gluonWConstants.precision - gluonWConstants.phiFusion))
    val inProtonsAmount: Long =
      ergFusioned * sProtons * gluonWConstants.precision / (rErg * (gluonWConstants.precision - gluonWConstants.phiFusion))

    GluonWBoxOutputAssetAmount(
      ergAmount = ergFusioned,
      neutronsAmount = -inNeutronsAmount,
      protonsAmount = -inProtonsAmount
    )
  }

  def betaDecayPlus(
    protonsToDecay: Long
  )(goldPrice: Long): GluonWBoxOutputAssetAmount = {
    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, goldPrice, rErg)

    val oneMinusPhiBeta: Long =
      gluonWConstants.precision - gluonWConstants.varPhiBeta
    val oneMinusFusionRatio: Long = gluonWConstants.precision - fusionRatio
    val minusesMultiplied: Long =
      oneMinusPhiBeta * oneMinusFusionRatio / gluonWConstants.precision
    val outNeutronsAmount: BigInt =
      (((protonsToDecay * minusesMultiplied) / fusionRatio) * sNeutrons) / sProtons

    GluonWBoxOutputAssetAmount(
      ergAmount = 0,
      neutronsAmount = outNeutronsAmount.toLong,
      protonsAmount = -protonsToDecay
    )
  }

  def betaDecayMinus(
    neutronsToDecay: Long
  )(goldPrice: Long): GluonWBoxOutputAssetAmount = {
    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, goldPrice, rErg)

    val outProtonsAmount: Long =
      neutronsToDecay * (gluonWConstants.precision - gluonWConstants.varPhiBeta) *
        fusionRatio / (gluonWConstants.precision - fusionRatio) * (sProtons / sNeutrons)

    GluonWBoxOutputAssetAmount(
      ergAmount = 0,
      neutronsAmount = -neutronsToDecay,
      protonsAmount = outProtonsAmount
    )
  }
}

/**
  * @param ergAmount Amount of Ergs
  * @param neutronsAmount Amount of Neutrons
  * @param protonsAmount Amount of Protons
  */
case class GluonWBoxOutputAssetAmount(
  ergAmount: Long,
  neutronsAmount: Long,
  protonsAmount: Long
)

case class GluonWAlgorithm(gluonWConstants: TGluonWConstants)
    extends TGluonWAlgorithm {

  def outputGluonWBox(
    inputGluonWBox: GluonWBox,
    gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount
  ): GluonWBox = {
    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.neutronId =>
          new ErgoToken(
            token.getId,
            token.getValue + gluonWBoxOutputAssetAmount.neutronsAmount
          )
        case GluonWTokens.protonId =>
          new ErgoToken(
            token.getId,
            token.getValue + gluonWBoxOutputAssetAmount.protonsAmount
          )
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value + gluonWBoxOutputAssetAmount.ergAmount,
      tokens = tokens
    )
  }

  override def fission(
    inputGluonWBox: GluonWBox,
    ergAmount: Long
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value

    val ergToChange: Long = ergAmount

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).fission(ergToChange)

    outputGluonWBox(inputGluonWBox, gluonWBoxOutputAssetAmount)
  }

  override def fusion(
    inputGluonWBox: GluonWBox,
    ergRedeemed: Long
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value

    val ergToChange: Long = ergRedeemed

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).fusion(ergToChange)

    outputGluonWBox(inputGluonWBox, gluonWBoxOutputAssetAmount)
  }

  override def betaDecayPlus(
    inputGluonWBox: GluonWBox,
    protonsAmount: Long
  )(implicit oracleBox: OracleBox): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).betaDecayPlus(protonsAmount)(oracleBox.getPrice)

    outputGluonWBox(inputGluonWBox, gluonWBoxOutputAssetAmount)
  }

  override def betaDecayMinus(
    inputGluonWBox: GluonWBox,
    neutronsAmount: Long
  )(implicit oracleBox: OracleBox): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).betaDecayMinus(neutronsAmount)(oracleBox.getPrice)

    outputGluonWBox(inputGluonWBox, gluonWBoxOutputAssetAmount)
  }

  override def fissionPrice(
    inputGluonWBox: GluonWBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, ergAmount)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.NEUTRON.toString,
          inputGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue,
          GluonWTokens.neutronId
        ),
        AssetPrice(
          name = GluonWAsset.PROTON.toString,
          inputGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue,
          GluonWTokens.protonId
        )
      )
    )
  }

  /**
    *
    * @param inputGluonWBox GluonWBox to undergo tx
    * @param ergRedeemed erg to be redeemed. This is a positive amount
    * @return
    */
  override def fusionPrice(
    inputGluonWBox: GluonWBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fusion(inputGluonWBox, ergRedeemed)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.NEUTRON.toString,
          outGluonWBox.Neutrons.getValue - inputGluonWBox.Neutrons.getValue,
          GluonWTokens.neutronId
        ),
        AssetPrice(
          name = GluonWAsset.PROTON.toString,
          outGluonWBox.Protons.getValue - inputGluonWBox.Protons.getValue,
          GluonWTokens.protonId
        )
      )
    )
  }

  override def betaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    protonsAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      betaDecayPlus(inputGluonWBox, protonsAmount)(oracleBox)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.PROTON.toString,
          inputGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue,
          GluonWTokens.protonId
        )
      )
    )
  }

  override def betaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    neutronsAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      betaDecayMinus(inputGluonWBox, neutronsAmount)(oracleBox)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.NEUTRON.toString,
          inputGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue,
          GluonWTokens.neutronId
        )
      )
    )
  }
}
