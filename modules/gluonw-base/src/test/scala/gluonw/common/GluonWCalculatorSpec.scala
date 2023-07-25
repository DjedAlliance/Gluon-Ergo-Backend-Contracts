package gluonw.common

import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import org.ergoplatform.appkit.Parameters

class GluonWCalculatorSpec extends GluonWBase {
  val gluonWConstants: TGluonWConstants = GluonWConstants()

  val gluonWBox: GluonWBox = genesisGluonWBox
  val oracleBox: OracleBox = createTestOracleBox

  val gluonWCalculator: GluonWCalculator = GluonWCalculator(
    sProtons = gluonWBox.protonsCirculatingSupply,
    sNeutrons = gluonWBox.neutronsCirculatingSupply,
    rErg = gluonWBox.ergFissioned,
    gluonWConstants = gluonWConstants
  )

  // Fission Algorithm
  "GluonWCalculator: Fission" should {
    "give the right Proton and Neutron amount" in {
      val ergToFission: Long = 10 * Parameters.OneErg
      val outputAssetAmount: GluonWBoxOutputAssetAmount = gluonWCalculator
        .fission(ergToFission)
      val outputAssetAmountAfterDecay: GluonWBoxOutputAssetAmount =
        gluonWCalculator
          .betaDecayPlus(outputAssetAmount.protonsAmount)(oracleBox.getPrice)

      val totalNeutrons: Long =
        outputAssetAmount.neutronsAmount + outputAssetAmountAfterDecay.neutronsAmount

      val neutronsAtPrecision: Float =
        totalNeutrons.toFloat / GluonWBoxConstants.PRECISION.toFloat
      val neutronsBeforeDecayAtPrecision: Float =
        outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      val check: Long = 1L
    }
  }

  "GluonWCalculator: Fusion" should {
    "request for right amount of Proton and Neutron for fusion" in {
      val ergToFusion: Long = 10 * Parameters.OneErg
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.fusion(ergToFusion)
    }
  }

  "GluonWCalculator: BetaDecayPlus" should {
    "request for right amount of Neutron for BetaDecayPlus" in {
      val protonToDecay: Long = 10 * GluonWBoxConstants.PRECISION
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.betaDecayPlus(protonToDecay)(oracleBox.getPrice)
    }
  }

  "GluonWCalculator: BetaDecayMinus" should {
    "request for right amount of Protons for BetaDecayMinus" in {
      val neutronToDecay: Long = 10 * GluonWBoxConstants.PRECISION
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.betaDecayMinus(neutronToDecay)(oracleBox.getPrice)
    }
  }
}
