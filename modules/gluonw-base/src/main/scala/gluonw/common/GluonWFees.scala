package gluonw.common

import commons.configs.{GetServiceConfig, OracleConfig}
import edge.boxes.FundsToAddressBox
import gluonw.boxes.{GluonWBox, OracleBox}
import org.ergoplatform.appkit.{Address, Parameters}

case class GluonWFeesCalculator(
  devAddress: Address = GetServiceConfig.getServiceOwner(),
  uiAddress: Address = null,
  oraclePaymentAddress: Address = OracleConfig.get().paymentAddress
)(gluonWBox: GluonWBox, gluonWConstants: TGluonWConstants) {
  val devFeesNumerator: Long = 500L
  val uiFeesNumerator: Long = 400L
  val oracleFeesNumerator: Long = 100L
  val denominator: Long = 100_000L

  def getGluonWFees(devFee: Long, uiFee: Long, oracleFee: Long): GluonWFees =
    GluonWFees(
      devFee = (devFee, devAddress),
      uiFee = (uiFee, uiAddress),
      oracleFee = (oracleFee, oraclePaymentAddress)
    )

  def getDecayedDevFee(devFee: Long): Long = {
    val maxFee: Long = gluonWBox.MaxFeeThreshold
    val maxFeeMinusRepaid: BigInt = BigInt(maxFee - gluonWBox.DevFeeRepaid)
    val devFeeMultiplyMaxFeeMinusRepaid: BigInt =
      BigInt(devFee) * maxFeeMinusRepaid
    val decayed: BigInt = devFeeMultiplyMaxFeeMinusRepaid / maxFee

    decayed.toLong
  }

  def getFissionOrFusionFees(ergAmount: Long): GluonWFees =
    getGluonWFees(
      devFee = getDecayedDevFee(ergAmount * devFeesNumerator / denominator),
      uiFee = ergAmount * uiFeesNumerator / denominator,
      oracleFee = 0L
    )

  def getBetaDecayPlusFees(
    protonsAmount: Long,
    oracleBox: OracleBox
  ): GluonWFees = {
    val protonsNanoErgPrice: BigInt =
      BigInt(
        gluonWConstants.protonsToNanoErg(
          neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
          protonsInCirculation = gluonWBox.protonsCirculatingSupply,
          protonsAmount = protonsAmount,
          fissionedErg = gluonWBox.ergFissioned,
          goldPriceNanoErgPerGram = oracleBox.getPricePerGram
        )
      )
    val devFee: Long =
      (protonsNanoErgPrice * devFeesNumerator / denominator).toLong
    val uiFee: Long =
      (protonsNanoErgPrice * uiFeesNumerator / denominator).toLong
    val oracleFee: Long =
      (protonsNanoErgPrice * oracleFeesNumerator / denominator).toLong

    getGluonWFees(
      devFee = getDecayedDevFee(devFee),
      uiFee = uiFee,
      oracleFee = oracleFee
    )
  }

  def getBetaDecayMinusFees(
    neutronsAmount: Long,
    oracleBox: OracleBox
  ): GluonWFees = {
    val neutronsNanoErgPrice: BigInt =
      BigInt(
        gluonWConstants
          .neutronsToNanoErg(
            neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
            neutronsAmount = neutronsAmount,
            fissionedErg = gluonWBox.ergFissioned,
            goldPriceNanoErgPerGram = oracleBox.getPricePerGram
          )
      )
    val devFee: Long =
      (neutronsNanoErgPrice * devFeesNumerator / denominator).toLong
    val uiFee: Long =
      (neutronsNanoErgPrice * uiFeesNumerator / denominator).toLong
    val oracleFee: Long =
      (neutronsNanoErgPrice * oracleFeesNumerator / denominator).toLong

    getGluonWFees(
      devFee = getDecayedDevFee(devFee),
      uiFee = uiFee,
      oracleFee = oracleFee
    )
  }

  /**
    * Due to oracle buyback contract, we have to put the oracle fee first.
    * https://www.ergoforum.org/t/buying-back-tokens-from-liqudity-pool/4275
    * @param gluonWFees
    * @return
    */
  def getFeesOutBox(gluonWFees: GluonWFees): Seq[FundsToAddressBox] = {
    val outBox: Seq[Option[FundsToAddressBox]] = Seq(
      if (gluonWFees.oracleFee._1 > 0)
        Option(
          FundsToAddressBox(
            value = gluonWFees.oracleFee._1 + Parameters.MinFee,
            address = gluonWFees.oracleFee._2
          )
        )
      else None,
      Option(
        FundsToAddressBox(
          value = gluonWFees.devFee._1 + Parameters.MinFee,
          address = gluonWFees.devFee._2
        )
      ),
      if (gluonWFees.uiFee._2 != null)
        Option(
          FundsToAddressBox(
            value = gluonWFees.uiFee._1 + Parameters.MinFee,
            address = gluonWFees.uiFee._2
          )
        )
      else None
    )

    outBox.flatten
  }
}

case class GluonWFees(
  devFee: (Long, Address),
  uiFee: (Long, Address),
  oracleFee: (Long, Address)
) {
  def getTotalFeeAmount: Long = devFee._1 + uiFee._1 + oracleFee._1
}
