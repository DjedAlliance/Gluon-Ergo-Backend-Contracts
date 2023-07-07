package gluonw.common

import gluonw.boxes.{GluonWBox, GoldOracleBox}
import io.circe.Json
import org.ergoplatform.appkit.ErgoId

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

/**
  * Algorithm and rates
  *
  * Note regarding Prices:
  * When getting rate, we will get rate for both
  */
trait TGluonWAlgorithm {

  def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox
  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox
  def betaDecayPlus(inputGluonWBox: GluonWBox, goldAmount: Long): GluonWBox
  def betaDecayMinus(inputGluonWBox: GluonWBox, rsvAmount: Long): GluonWBox

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

object GluonWAlgorithm extends TGluonWAlgorithm {

  override def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox =
    ???

  override def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox =
    ???

  override def betaDecayPlus(
    inputGluonWBox: GluonWBox,
    goldAmount: Long
  ): GluonWBox = ???

  override def betaDecayMinus(
    inputGluonWBox: GluonWBox,
    rsvAmount: Long
  ): GluonWBox = ???

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
