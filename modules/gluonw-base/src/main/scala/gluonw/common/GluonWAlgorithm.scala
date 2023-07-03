package gluonw.common

import gluonw.boxes.{GluonWBox, GoldOracleBox}
import io.circe.Json

trait TAssetRate {
  val name: String
  val rate: Long

  def toJson: Json =
    Json.fromFields(
      List(
        ("assetName", Json.fromString(name)),
        ("rate", Json.fromLong(rate))
      )
    )
}

case class AssetRate(name: String, rate: Long) extends TAssetRate

object AssetRate {

  def consolidate(assetRates: Seq[AssetRate]): Seq[AssetRate] = {
    val assetRatesByName: Map[String, Seq[AssetRate]] =
      assetRates.groupBy(_.name)
    val consolidatedAssets: Seq[AssetRate] = assetRatesByName.map {
      case (name, rates) =>
        // Consolidate Value
        val assetValue: Long = rates.foldLeft(0L) {
          (acc: Long, rate: AssetRate) => acc + rate.rate
        }

        AssetRate(
          name = name,
          rate = assetValue
        )
    }.toSeq

    consolidatedAssets
  }
}

/**
  * Algorithm and rates
  *
  * Note regarding Rates:
  * When getting rate, we will get rate for both
  */
trait TGluonWAlgorithm {

  def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox
  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox
  def betaDecayPlus(inputGluonWBox: GluonWBox, goldAmount: Long): GluonWBox
  def betaDecayMinus(inputGluonWBox: GluonWBox, rsvAmount: Long): GluonWBox

  def calculateFissionRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetRate])

  def calculateFusionRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetRate])

  def calculateBetaDecayPlusRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    goldAmount: Long
  ): (GluonWBox, Seq[AssetRate])

  def calculateBetaDecayMinusRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    rsvAmount: Long
  ): (GluonWBox, Seq[AssetRate])
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

  override def calculateFissionRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetRate]) = ???

  override def calculateFusionRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetRate]) = ???

  override def calculateBetaDecayPlusRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    goldAmount: Long
  ): (GluonWBox, Seq[AssetRate]) = ???

  override def calculateBetaDecayMinusRate(
    inputGluonWBox: GluonWBox,
    goldOracleBox: GoldOracleBox,
    rsvAmount: Long
  ): (GluonWBox, Seq[AssetRate]) = ???
}
