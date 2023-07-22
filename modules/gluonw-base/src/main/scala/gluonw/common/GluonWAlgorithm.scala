package gluonw.common

import edge.commons.ErgCommons
import gluonw.boxes.{GluonWBox, OracleBox
}
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

  def betaDecayPlus(inputGluonWBox: GluonWBox, goldAmount: Long)(
    implicit oracleBox: OracleBox

  ): GluonWBox

  def betaDecayMinus(inputGluonWBox: GluonWBox, rsvAmount: Long)(
    implicit oracleBox: OracleBox

  ): GluonWBox

  def fissionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def fusionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice])

  def betaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    goldAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def betaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    rsvAmount: Long
  ): (GluonWBox, Seq[AssetPrice])
}

case class GluonWAlgorithm(gluonWConstants: TGluonWConstants)
    extends TGluonWAlgorithm {

  override def fission(inputGluonWBox: GluonWBox, ergAmount: Long)(
    implicit oracleBox: OracleBox

  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val phiT: Long = gluonWConstants.phiFission

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
    implicit oracleBox: OracleBox

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
  )(implicit oracleBox: OracleBox
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val varPhiBeta: Long = gluonWConstants.varPhiBeta

    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, oracleBox.getPrice, rErg)

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
  )(implicit oracleBox: OracleBox
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.getProtonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.getNeutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.value - ErgCommons.MinMinerFee
    val varPhiBeta: Long = gluonWConstants.varPhiBeta

    val fusionRatio: Long =
      gluonWConstants.fusionRatio(sNeutrons, oracleBox.getPrice, rErg)

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

  override def fissionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, ergAmount)(oracleBox)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.SIGGOLD.toString,
          inputGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue,
          GluonWTokens.sigGoldId
        ),
        AssetPrice(
          name = GluonWAsset.SIGGOLDRSV.toString,
          inputGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue,
          GluonWTokens.sigGoldRsvId
        )
      )
    )
  }

  override def fusionPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, ergRedeemed)(oracleBox)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.SIGGOLD.toString,
          outGluonWBox.Neutrons.getValue - inputGluonWBox.Neutrons.getValue,
          GluonWTokens.sigGoldId
        ),
        AssetPrice(
          name = GluonWAsset.SIGGOLDRSV.toString,
          outGluonWBox.Protons.getValue - inputGluonWBox.Protons.getValue,
          GluonWTokens.sigGoldRsvId
        )
      )
    )
  }

  override def betaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    goldAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, goldAmount)(oracleBox)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.SIGGOLDRSV.toString,
          inputGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue,
          GluonWTokens.sigGoldRsvId
        )
      )
    )
  }

  override def betaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox
    ,
    rsvAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, rsvAmount)(oracleBox)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.SIGGOLD.toString,
          inputGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue,
          GluonWTokens.sigGoldId
        )
      )
    )
  }
}
