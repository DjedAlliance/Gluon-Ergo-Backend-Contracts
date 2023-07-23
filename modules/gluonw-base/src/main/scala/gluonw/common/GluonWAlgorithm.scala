package gluonw.common

import edge.commons.ErgCommons
import gluonw.boxes.{GluonWBox, OracleBox}
import io.circe.Json
import org.ergoplatform.appkit.{ErgoId, ErgoToken}

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

case class GluonWConstants() extends TGluonWConstants {
  override def qStar: Long = (0.0066 * precision).toLong

  override def fusionRatio(
    neutronsInCirculation: Long,
    phiFission: Long,
    fissionedErg: Long
  ): Long = {
    val rightHandMinVal: Long =
      neutronsInCirculation * phiFission / fissionedErg
    Math.min(qStar, rightHandMinVal)
  }

  override def phiFission: Long = (0.01 * precision).toLong

  override def phiFusion: Long = (0.01 * precision).toLong

  override def varPhiBeta: Long = (0.02 * precision).toLong

  /**
    * Gold rate is 1kg per 1nanoErg therefore has to be
    * divided by 1,000,000,000
    */
  override val precision: Long = 1 * 1000 * 1000 * 1000
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

  def fission(inputGluonWBox: GluonWBox, ergAmount: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox

  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox

  def betaDecayPlus(inputGluonWBox: GluonWBox, neutronsAmount: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox

  def betaDecayMinus(inputGluonWBox: GluonWBox, protonsAmount: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox

  def fissionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def fusionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
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

case class GluonWAlgorithm(gluonWConstants: TGluonWConstants)
    extends TGluonWAlgorithm {

  override def fission(inputGluonWBox: GluonWBox, ergAmount: Long)(
    implicit oracleBox: OracleBox
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value

    val ergToChange: Long = ergAmount

    val outNeutronsAmount: Long =
      ergToChange * (sNeutrons / rErg) * (1 - gluonWConstants.phiFission)
    val outProtonsAmount: Long =
      ergToChange * (sProtons / rErg) * (1 - gluonWConstants.phiFission)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.neutronId =>
          new ErgoToken(token.getId, token.getValue - outNeutronsAmount)
        case GluonWTokens.protonId =>
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
    implicit oracleBox: OracleBox
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value
    val phiFusion: Long = gluonWConstants.phiFusion

    val ergToChange: Long = ergRedeemed

    val inNeutronsAmount: Long =
      ergToChange * sNeutrons / rErg / (1 - phiFusion)
    val inProtonsAmount: Long =
      ergToChange * sProtons / rErg / (1 - phiFusion)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.neutronId =>
          new ErgoToken(token.getId, token.getValue + inNeutronsAmount)
        case GluonWTokens.protonId =>
          new ErgoToken(token.getId, token.getValue + inProtonsAmount)
        case _ => token
      }
    }

    // In fusion, the erg is removed
    inputGluonWBox.copy(
      value = inputGluonWBox.value - ergToChange,
      tokens = tokens
    )
  }

  override def betaDecayPlus(
    inputGluonWBox: GluonWBox,
    protonsAmount: Long
  )(implicit oracleBox: OracleBox): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value
    val varPhiBeta: Long = gluonWConstants.varPhiBeta

    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, oracleBox.getPrice, rErg)

    val outNeutronsAmount: Long = protonsAmount * (1 - varPhiBeta) *
      (1 - fusionRatio) / fusionRatio * (sNeutrons / sProtons)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.neutronId =>
          new ErgoToken(token.getId, token.getValue - outNeutronsAmount)
        case GluonWTokens.protonId =>
          new ErgoToken(token.getId, token.getValue + protonsAmount)
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
    neutronsAmount: Long
  )(implicit oracleBox: OracleBox): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value
    val varPhiBeta: Long = gluonWConstants.varPhiBeta

    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, oracleBox.getPrice, rErg)

    val outProtonsAmount: Long = neutronsAmount * (1 - varPhiBeta) *
      fusionRatio / (1 - fusionRatio) * (sProtons / sNeutrons)

    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.neutronId =>
          new ErgoToken(token.getId, token.getValue + neutronsAmount)
        case GluonWTokens.protonId =>
          new ErgoToken(token.getId, token.getValue - outProtonsAmount)
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value,
      tokens = tokens
    )
  }

  override def fissionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, ergAmount)(oracleBox)

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
    * @param oracleBox Oracle box that provides the price of the assets
    * @param ergRedeemed erg to be redeemed. This is a positive amount
    * @return
    */
  override def fusionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, ergRedeemed)(oracleBox)

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
    neutronsAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, neutronsAmount)(oracleBox)

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
    protonsAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, protonsAmount)(oracleBox)

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
