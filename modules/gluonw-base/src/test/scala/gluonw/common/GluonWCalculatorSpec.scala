package gluonw.common

import gluonw.boxes.{GluonWBox, GluonWBoxConstants}
import org.ergoplatform.appkit.Parameters

class GluonWCalculatorSpec extends GluonWBase {
  val gluonWConstants: TGluonWConstants = GluonWConstants()

  val gluonWBox: GluonWBox = GluonWBox.create(
    protonAmount =
      GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY - (0.2 * GluonWBoxConstants.PRECISION).toLong,
    neutronAmount =
      GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY - (0.8 * GluonWBoxConstants.PRECISION).toLong,
    ergAmount = Parameters.OneErg
  )

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

      val check: Long = 1L
    }
  }
}
