package gluonw.common

import commons.math.MathUtils
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.sdk.ErgoToken

class GluonWCalculatorSpec extends GluonWBase {
  val gluonWConstants: TGluonWConstants = GluonWConstants()

  val gluonWBox: GluonWBox = genesisGluonWBox(200, 1, 1)
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

      val neutronsAtPrecision: Float =
        outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat
      val protonsAtPrecision: Float =
        outputAssetAmount.protonsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      assert(MathUtils.~=(neutronsAtPrecision, 0.0495, 3))
      assert(MathUtils.~=(protonsAtPrecision, 0.0495, 3))
    }
  }

  "GluonWCalculator: Fusion" should {
    "request for right amount of Proton and Neutron for fusion" in {
      val ergToFusion: Long = 10 * Parameters.OneErg
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.fusion(ergToFusion)

      val neutronsAtPrecision: Float =
        outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat
      val protonsAtPrecision: Float =
        outputAssetAmount.protonsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      assert(
        MathUtils.~=(neutronsAtPrecision, -0.050251257, 6),
        s"Neutrons at Precision: ${neutronsAtPrecision}"
      )
      assert(
        MathUtils.~=(protonsAtPrecision, -0.050251257, 5),
        s"Protons at precision: ${protonsAtPrecision}"
      )
    }
  }

  "GluonWCalculator: BetaDecayPlus" should {
    "request for right amount of Neutron for BetaDecayPlus -> Volume 0" in {
      val protonToDecay: Long = (0.05 * GluonWBoxConstants.PRECISION).toLong
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.betaDecayPlus(
          rErg = 200 * Parameters.OneErg,
          volumePlus = List.fill(7)(0L),
          volumeMinus = List.fill(7)(0L),
          protonsToDecay = protonToDecay
        )(
          oracleBox.getPricePerGram
        )

      val neutronsAtPrecision: Float =
        outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      assert(
        MathUtils.~=(neutronsAtPrecision, 0.13943309, 4),
        s"neutronsAtPrecision: ${neutronsAtPrecision}, RealResult: 0.137347"
      )
    }

    "request for right amount of Neutron for BetaDecayPlus -> Volume 140" in {
      val protonToDecay: Long = (0.05 * GluonWBoxConstants.PRECISION).toLong
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.betaDecayPlus(
          rErg = 200 * Parameters.OneErg,
          volumePlus = List.fill(7)(20L * GluonWBoxConstants.PRECISION),
          volumeMinus = List.fill(7)(10L * GluonWBoxConstants.PRECISION),
          protonsToDecay = protonToDecay
        )(
          oracleBox.getPricePerGram
        )

      val neutronsAtPrecision: Float =
        outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      assert(
        MathUtils.~=(neutronsAtPrecision, 0.11490968, 4),
        s"neutronsAtPrecision: ${neutronsAtPrecision}, RealResult: 0.11490968"
      )
    }
  }

  "GluonWCalculator: BetaDecayMinus" should {
    "request for right amount of Protons for BetaDecayMinus -> volume 0" in {
      val neutronToDecay: Long = (0.05 * GluonWBoxConstants.PRECISION).toLong
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.betaDecayMinus(
          rErg = 200 * Parameters.OneErg,
          volumePlus = List.fill(7)(0L),
          volumeMinus = List.fill(7)(0L),
          neutronsToDecay = neutronToDecay
        )(
          oracleBox.getPricePerGram
        )

      val protonsAtPrecision: Float =
        outputAssetAmount.protonsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      assert(
        MathUtils.~=(protonsAtPrecision, 0.017661696, 4),
        s"neutronsAtPrecision: ${protonsAtPrecision}, RealResult: 0.17661696"
      )
    }

    "request for right amount of Protons for BetaDecayMinus -> volume 140" in {
      val neutronToDecay: Long = (0.05 * GluonWBoxConstants.PRECISION).toLong
      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.betaDecayMinus(
          rErg = 200 * Parameters.OneErg,
          volumeMinus = List.fill(7)(20L * GluonWBoxConstants.PRECISION),
          volumePlus = List.fill(7)(10L * GluonWBoxConstants.PRECISION),
          neutronsToDecay = neutronToDecay
        )(
          oracleBox.getPricePerGram
        )

      val protonsAtPrecision: Float =
        outputAssetAmount.protonsAmount.toFloat / GluonWBoxConstants.PRECISION.toFloat

      assert(
        MathUtils.~=(protonsAtPrecision, 0.01462888, 4),
        s"neutronsAtPrecision: ${protonsAtPrecision}, RealResult: 0.0173494"
      )
    }
  }

  "GluonWCalculator: Fission Simulation" should {
    object Operations {
      val FISSION = "FISSION"
      val FUSION = "FUSION"
      val BETA_DECAY_PLUS = "BETA_DECAY_PLUS"
      val BETA_DECAY_MINUS = "BETA_DECAY_MINUS"
    }

    var simulationGluonWBox: GluonWBox = gluonWBox

    val amountAndOperationAndResults
      : Seq[(Long, (String, (GluonWBoxOutputAssetAmount)))] =
      Seq(
        (
          (0.05 * GluonWBoxConstants.PRECISION).toLong,
          (
            Operations.BETA_DECAY_PLUS,
            GluonWBoxOutputAssetAmount(
              ergAmount = 0,
              neutronsAmount =
                (0.1373472 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = -(0.05 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (0.131283 * GluonWBoxConstants.PRECISION).toLong,
          (
            Operations.BETA_DECAY_MINUS,
            GluonWBoxOutputAssetAmount(
              ergAmount = 0,
              neutronsAmount =
                -(0.131283 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = (0.0458515 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (10 * Parameters.OneErg).toLong,
          (
            Operations.FISSION,
            GluonWBoxOutputAssetAmount(
              ergAmount = -10 * Parameters.OneErg,
              neutronsAmount =
                (0.0498001 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = (0.04929465 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (100 * Parameters.OneErg).toLong,
          (
            Operations.FISSION,
            GluonWBoxOutputAssetAmount(
              ergAmount = -100 * Parameters.OneErg,
              neutronsAmount =
                (0.49776464 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = (0.49271177 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (6752 * Parameters.OneErg).toLong,
          (
            Operations.FISSION,
            GluonWBoxOutputAssetAmount(
              ergAmount = -6752 * Parameters.OneErg,
              neutronsAmount = (33.50065 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = (33.16058 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (10 * Parameters.OneErg).toLong,
          (
            Operations.BETA_DECAY_PLUS,
            GluonWBoxOutputAssetAmount(
              ergAmount = 0,
              neutronsAmount = (28.02254 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = -(10 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (2000 * Parameters.OneErg).toLong,
          (
            Operations.FUSION,
            GluonWBoxOutputAssetAmount(
              ergAmount = 2000 * Parameters.OneErg,
              neutronsAmount =
                -(18.044008 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount = -(7.0653980 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        ),
        (
          (4387 * Parameters.OneErg).toLong,
          (
            Operations.FISSION,
            GluonWBoxOutputAssetAmount(
              ergAmount = -4387 * Parameters.OneErg,
              neutronsAmount =
                (38.637085 * GluonWBoxConstants.PRECISION).toLong,
              protonsAmount =
                (15.12892108 * GluonWBoxConstants.PRECISION).toLong
            )
          )
        )
      )

    for ((amountAndOperationAndResult, count) <- amountAndOperationAndResults.zipWithIndex) {
      val results: GluonWBoxOutputAssetAmount =
        amountAndOperationAndResult._2._2
      val operationType: String = amountAndOperationAndResult._2._1

//      s"Simulation: $operationType at position $count" in {
//        val simGluonWCalculator: GluonWCalculator = GluonWCalculator(
//          sProtons = simulationGluonWBox.protonsCirculatingSupply,
//          sNeutrons = simulationGluonWBox.neutronsCirculatingSupply,
//          rErg = simulationGluonWBox.ergFissioned,
//          gluonWConstants = gluonWConstants
//        )
//
//        val outputAssetAmount: GluonWBoxOutputAssetAmount =
//          operationType match {
//            case Operations.FISSION =>
//              simGluonWCalculator.fission(amountAndOperationAndResult._1)
//            case Operations.FUSION =>
//              simGluonWCalculator.fusion(amountAndOperationAndResult._1)
//            case Operations.BETA_DECAY_PLUS =>
//              simGluonWCalculator.betaDecayPlus(amountAndOperationAndResult._1)(
//                oracleBox.getPricePerGrams
//              )
//            case Operations.BETA_DECAY_MINUS =>
//              simGluonWCalculator.betaDecayMinus(
//                amountAndOperationAndResult._1
//              )(
//                oracleBox.getPricePerGrams
//              )
//          }
//
//        val tokens: Seq[ErgoToken] = simulationGluonWBox.tokens.map { token =>
//          token.getId match {
//            case GluonWTokens.neutronId =>
//              ErgoToken(
//                token.getId,
//                token.getValue - outputAssetAmount.neutronsAmount
//              )
//            case GluonWTokens.protonId =>
//              ErgoToken(
//                token.getId,
//                token.getValue - outputAssetAmount.protonsAmount
//              )
//            case _ => token
//          }
//        }
//
//        simulationGluonWBox = simulationGluonWBox.copy(
//          value = simulationGluonWBox.value - outputAssetAmount.ergAmount,
//          tokens = tokens
//        )
//        assert(
//          MathUtils.~=(
//            outputAssetAmount.ergAmount.toFloat / GluonWBoxConstants.PRECISION,
//            results.ergAmount.toFloat / GluonWBoxConstants.PRECISION,
//            3
//          ),
//          s"\noutputErgAmount: ${outputAssetAmount.ergAmount.toFloat / GluonWBoxConstants.PRECISION}, expected: ${results.ergAmount.toFloat / GluonWBoxConstants.PRECISION}"
//        )
//        assert(
//          MathUtils
//            .~=(
//              outputAssetAmount.protonsAmount.toFloat / GluonWBoxConstants.PRECISION,
//              results.protonsAmount.toFloat / GluonWBoxConstants.PRECISION,
//              3
//            ),
//          s"\noutputProtonsAmount: ${outputAssetAmount.protonsAmount.toFloat / GluonWBoxConstants.PRECISION}, expected: ${results.protonsAmount.toFloat / GluonWBoxConstants.PRECISION}"
//        )
//        assert(
//          MathUtils.~=(
//            outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION,
//            results.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION,
//            3
//          ),
//          s"\noutputNeutronsAmount: ${outputAssetAmount.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION}, expected: ${results.neutronsAmount.toFloat / GluonWBoxConstants.PRECISION}"
//        )
//      }
    }
  }
}
