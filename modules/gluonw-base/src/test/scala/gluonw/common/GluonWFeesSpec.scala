package gluonw.common

import edge.boxes.FundsToAddressBox
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import org.ergoplatform.appkit.{Address, Parameters}

class GluonWFeesSpec extends GluonWBase {
  client.setClient()

  def checkGluonWFees(
    expectedFee: (Long, Address),
    actualFee: Long,
    actualAddress: Address
  ): Unit = {
    assert(
      expectedFee._1 == actualFee,
      s"Expected: ${expectedFee._1}, Actual: ${actualFee}"
    )
    assert(
      expectedFee._2.equals(actualAddress),
      s"Expected: ${expectedFee._2}, Actual: ${actualAddress}"
    )
  }

  def checkGluonWFeesBox(
    expectedFeeBox: FundsToAddressBox,
    actualFee: Long,
    actualAddress: Address
  ): Unit = {
    assert(
      expectedFeeBox.value - Parameters.MinFee == actualFee,
      s"Expected: ${expectedFeeBox.value}, Actual: ${actualFee}"
    )
    assert(
      expectedFeeBox.address.equals(actualAddress),
      s"Expected: ${expectedFeeBox.address}, Actual: ${actualAddress}"
    )
  }

  "GluonWFees:" should {
    val gluonWBox: GluonWBox = genesisGluonWBox()
    val gluonWConstants: GluonWConstants = GluonWConstants()
    val oracleBox: OracleBox = createTestOracleBox

    // Random addresses are used
    val devAddress: Address = trueAddress
    val uiAddress: Address = trueAndFalseAddress
    val oraclePaymentAddress: Address = exleDevAddress

    val gluonWFeesCalculator: GluonWFeesCalculator =
      GluonWFeesCalculator(devAddress, uiAddress, oraclePaymentAddress)(
        gluonWBox,
        gluonWConstants
      )

    "neutronsToNanoErg returns correct values" in {
      val neutronsAmount: Long = (0.0495 * GluonWBoxConstants.PRECISION).toLong
      val expectedNeutronVolume: Long = 6534000L

      val calculatedNeutronVolume: Long = gluonWConstants
        .neutronsToNanoErg(
          neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
          neutronsAmount = neutronsAmount,
          fissionedErg = gluonWBox.ergFissioned,
          goldPriceNanoErgPerGram = oracleBox.getPrice / 1000
        )

      assert(
        expectedNeutronVolume == calculatedNeutronVolume,
        s"Expected: ${expectedNeutronVolume}, Actual: ${calculatedNeutronVolume}"
      )
    }

    "protonsToNanoErg returns correct values" in {
      val protonsAmount: Long = (0.0495 * GluonWBoxConstants.PRECISION).toLong
      val expectedProtonVolume: Long = 3366000L
      val fusionRatio: Long = gluonWConstants
        .fusionRatio(
          gluonWBox.neutronsCirculatingSupply,
          oracleBox.getPricePerGram,
          gluonWBox.ergFissioned
        )
        .toLong
      val expectedFusionRatio: Long = 660000000L

      val calculatedProtonVolume: Long =
        gluonWConstants.protonsToNanoErg(
          neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
          protonsInCirculation = gluonWBox.protonsCirculatingSupply,
          protonsAmount = protonsAmount,
          fissionedErg = gluonWBox.ergFissioned,
          goldPriceNanoErgPerGram = oracleBox.getPricePerGram
        )

      assert(
        fusionRatio == expectedFusionRatio,
        s"Expected: ${expectedFusionRatio}, Actual: ${fusionRatio}"
      )
      assert(
        expectedProtonVolume == calculatedProtonVolume,
        s"Expected: ${expectedProtonVolume}, Actual: ${calculatedProtonVolume}"
      )
    }

    "getFissionOrFusionFees returns correct GluonWFees" in {
      val ergAmount: Long = 10 * Parameters.OneErg
      val fissionOrFusionFees: GluonWFees =
        gluonWFeesCalculator.getFissionOrFusionFees(ergAmount)

      val devFee: Long =
        ergAmount * gluonWFeesCalculator.devFeesNumerator / gluonWFeesCalculator.denominator
      val uiFee: Long =
        ergAmount * gluonWFeesCalculator.uiFeesNumerator / gluonWFeesCalculator.denominator
      val oracleFee: Long = 0

      checkGluonWFees(fissionOrFusionFees.devFee, devFee, devAddress)
      checkGluonWFees(fissionOrFusionFees.uiFee, uiFee, uiAddress)
      checkGluonWFees(
        fissionOrFusionFees.oracleFee,
        oracleFee,
        oraclePaymentAddress
      )

      val feeOutboxes: Seq[FundsToAddressBox] =
        gluonWFeesCalculator.getFeesOutBox(fissionOrFusionFees)

      assert(feeOutboxes.size == 2)
      checkGluonWFeesBox(feeOutboxes.head, devFee, devAddress)
      checkGluonWFeesBox(feeOutboxes.tail.head, uiFee, uiAddress)
    }

    // Protons to Neutrons
    "getBetaDecayPlusFees returns correct GluonWFees" in {
      // 1. Protons amount to exchange
      // 2. GluonWFeesCalculator. getBetaDecayPlusFees
      val protonsAmount: Long = 1 * GluonWBoxConstants.PRECISION
      val betaDecayPlusFees: GluonWFees =
        gluonWFeesCalculator.getBetaDecayPlusFees(protonsAmount, oracleBox)

      val devFee: Long = gluonWConstants.protonsToNanoErg(
        neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
        protonsInCirculation = gluonWBox.protonsCirculatingSupply,
        protonsAmount = protonsAmount,
        fissionedErg = gluonWBox.ergFissioned,
        goldPriceNanoErgPerGram = oracleBox.getPricePerGram
      ) * gluonWFeesCalculator.devFeesNumerator / gluonWFeesCalculator.denominator
      val uiFee: Long = gluonWConstants.protonsToNanoErg(
        neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
        protonsInCirculation = gluonWBox.protonsCirculatingSupply,
        protonsAmount = protonsAmount,
        fissionedErg = gluonWBox.ergFissioned,
        goldPriceNanoErgPerGram = oracleBox.getPricePerGram
      ) * gluonWFeesCalculator.uiFeesNumerator / gluonWFeesCalculator.denominator
      val oracleFee: Long = gluonWConstants.protonsToNanoErg(
        neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
        protonsInCirculation = gluonWBox.protonsCirculatingSupply,
        protonsAmount = protonsAmount,
        fissionedErg = gluonWBox.ergFissioned,
        goldPriceNanoErgPerGram = oracleBox.getPricePerGram
      ) * gluonWFeesCalculator.oracleFeesNumerator / gluonWFeesCalculator.denominator

      checkGluonWFees(betaDecayPlusFees.devFee, devFee, devAddress)
      checkGluonWFees(betaDecayPlusFees.uiFee, uiFee, uiAddress)
      checkGluonWFees(
        betaDecayPlusFees.oracleFee,
        oracleFee,
        oraclePaymentAddress
      )

      val feeOutboxes: Seq[FundsToAddressBox] =
        gluonWFeesCalculator.getFeesOutBox(betaDecayPlusFees)

      assert(feeOutboxes.size == 3)

      checkGluonWFeesBox(feeOutboxes.tail.head, devFee, devAddress)
      checkGluonWFeesBox(feeOutboxes.tail.tail.head, uiFee, uiAddress)
      checkGluonWFeesBox(
        feeOutboxes.head,
        oracleFee,
        oraclePaymentAddress
      )
    }

    "getBetaDecayMinusFees returns correct GluonWFees" in {
      val neutronsAmount: Long = 863274814008L
      val betaDecayMinusFees: GluonWFees =
        gluonWFeesCalculator.getBetaDecayMinusFees(neutronsAmount, oracleBox)

      val devFee: Long = (BigInt(
        gluonWConstants.neutronsToNanoErg(
          neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
          neutronsAmount = neutronsAmount,
          fissionedErg = gluonWBox.ergFissioned,
          goldPriceNanoErgPerGram = oracleBox.getPricePerGram
        )
      ) * gluonWFeesCalculator.devFeesNumerator / gluonWFeesCalculator.denominator).toLong
      val uiFee: Long = (BigInt(
        gluonWConstants.neutronsToNanoErg(
          neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
          neutronsAmount = neutronsAmount,
          fissionedErg = gluonWBox.ergFissioned,
          goldPriceNanoErgPerGram = oracleBox.getPricePerGram
        )
      ) * gluonWFeesCalculator.uiFeesNumerator / gluonWFeesCalculator.denominator).toLong
      val oracleFee: Long = (BigInt(
        gluonWConstants.neutronsToNanoErg(
          neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
          neutronsAmount = neutronsAmount,
          fissionedErg = gluonWBox.ergFissioned,
          goldPriceNanoErgPerGram = oracleBox.getPricePerGram
        )
      ) * gluonWFeesCalculator.oracleFeesNumerator / gluonWFeesCalculator.denominator).toLong

      checkGluonWFees(betaDecayMinusFees.devFee, devFee, devAddress)
      checkGluonWFees(betaDecayMinusFees.uiFee, uiFee, uiAddress)
      checkGluonWFees(
        betaDecayMinusFees.oracleFee,
        oracleFee,
        oraclePaymentAddress
      )

      val feeOutboxes: Seq[FundsToAddressBox] =
        gluonWFeesCalculator.getFeesOutBox(betaDecayMinusFees)

      assert(feeOutboxes.size == 3)
      checkGluonWFeesBox(feeOutboxes.tail.head, devFee, devAddress)
      checkGluonWFeesBox(feeOutboxes.tail.tail.head, uiFee, uiAddress)
      checkGluonWFeesBox(
        feeOutboxes.head,
        oracleFee,
        oraclePaymentAddress
      )
    }
  }
}
