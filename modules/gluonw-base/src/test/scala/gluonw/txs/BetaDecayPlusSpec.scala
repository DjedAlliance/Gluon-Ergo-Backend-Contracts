package gluonw.txs

import edge.boxes.{CustomBoxData, FundsToAddressBox}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import gluonw.common.{
  GluonWAlgorithm,
  GluonWBase,
  GluonWBoxOutputAssetAmount,
  GluonWCalculator,
  GluonWConstants,
  GluonWTokens,
  TGluonWConstants
}
import org.ergoplatform.appkit.{Address, InputBox, UnsignedTransaction}

import scala.util.Random

/**
  * Protons to Neutrons
  */
class BetaDecayPlusSpec extends GluonWBase {
  client.setClient()
  // 1. BetaDecayPlusTx Success
  // a. Looping through multiple BetaDecayPlusTx and
  // getting the right value
  // b. Chained Tx through multiple BetaDecayPlusTx
  "BetaDecayPlusTx" should {
    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm =
      GluonWAlgorithm(gluonWConstants)

    val gluonWBox: GluonWBox = genesisGluonWBox(20_000_000L, 100_000L, 100_000L)

    val gluonWCalculator: GluonWCalculator = GluonWCalculator(
      sProtons = gluonWBox.protonsCirculatingSupply,
      sNeutrons = gluonWBox.neutronsCirculatingSupply,
      rErg = gluonWBox.ergFissioned,
      gluonWConstants = gluonWConstants
    )

    val oracleBox: OracleBox = createTestOracleBox

    "loop through multiple betaDecayPlusTx correctly" in {
      client.getClient.execute { implicit ctx =>
        // 1. Create a fission box
        // 2. Create a seq of erg to redeem
        val maxProtons: Long = 1_000L
        val maxProtonsInPrecision: Long =
          maxProtons * GluonWBoxConstants.PRECISION
        val changeAddress: Address = trueAddress
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 20).foreach { _ =>
          val random: Double = new Random().nextDouble()
          val protonsToDecay: Long = (maxProtonsInPrecision * random).toLong

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            gluonWCalculator.betaDecayPlus(protonsToDecay)(oracleBox.getPrice)

          // @todo kii create ProxyBoxes
          val paymentBox: InputBox =
            createPaymentBox(protonsValue = protonsToDecay)

          val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
            inputBoxes = Seq(gluonWBox.getAsInputBox(), paymentBox),
            protonsToTransmute = protonsToDecay,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          betaDecayPlusTx.signTx

          val outBoxes: Seq[InputBox] =
            betaDecayPlusTx.getOutBoxesAsInputBoxes()
          val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
          val outPaymentBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.head)

          assert(
            outGluonWBox.value == gluonWBox.value + outputAssetAmount.ergAmount
          )
          assert(
            outGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value == gluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value - outputAssetAmount.neutronsAmount
          )
          assert(
            outGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == gluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value - outputAssetAmount.protonsAmount
          )

          // Check payment Box
          assert(
            outPaymentBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value == outputAssetAmount.neutronsAmount
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.protonId))
          )
        }
      }
    }

    "chain through multiple betaDecayPlusTx" in {
      client.getClient.execute { implicit ctx =>
        val maxProtons: Long = 10_000L
        val maxProtonsInPrecision: Long =
          maxProtons * GluonWBoxConstants.PRECISION
        val changeAddress: Address = trueAddress
        var inGluonWBox: GluonWBox = gluonWBox
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 10).foreach { _ =>
          val testGluonWCalculator: GluonWCalculator = GluonWCalculator(
            sProtons = inGluonWBox.protonsCirculatingSupply,
            sNeutrons = inGluonWBox.neutronsCirculatingSupply,
            rErg = inGluonWBox.ergFissioned,
            gluonWConstants = gluonWConstants
          )

          val random: Double = new Random().nextDouble()
          val protonsToTransmute: Long = (maxProtonsInPrecision * random).toLong

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            testGluonWCalculator.betaDecayPlus(protonsToTransmute)(
              oracleBox.getPrice
            )

          // Payment box to pay for the transaction
          val paymentBox: InputBox =
            createPaymentBox(protonsValue = protonsToTransmute)

          val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
            inputBoxes = Seq(inGluonWBox.getAsInputBox(), paymentBox),
            protonsToTransmute = protonsToTransmute,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          betaDecayPlusTx.signTx

          val outBoxes: Seq[InputBox] =
            betaDecayPlusTx.getOutBoxesAsInputBoxes()
          val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
          val outPaymentBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.head)

          assert(
            outGluonWBox.value == inGluonWBox.value + outputAssetAmount.ergAmount
          )
          assert(
            outGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value == inGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value - outputAssetAmount.neutronsAmount
          )
          assert(
            outGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == inGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value - outputAssetAmount.protonsAmount
          )

          // Check payment Box
          assert(
            outPaymentBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value == outputAssetAmount.neutronsAmount
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.protonId))
          )

          inGluonWBox = outGluonWBox
        }
      }
    }
  }

  // 2. BetaDecayPlusTx Failures
  "BetaDecayPlusTx Failures (User Perspective):" should {
    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm =
      GluonWAlgorithm(gluonWConstants)

    val gluonWBox: GluonWBox = genesisGluonWBox()

    val oracleBox: OracleBox = createTestOracleBox

    client.getClient.execute { implicit ctx =>
      val maxProtons: Long = 10_000L
      val maxProtonsInPrecision: Long =
        maxProtons * GluonWBoxConstants.PRECISION
      val changeAddress: Address = trueAddress
      var inGluonWBox: GluonWBox = gluonWBox
      val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

      val random: Double = new Random().nextDouble()
      val protonsToTransmute: Long = (maxProtonsInPrecision * random).toLong

      // Payment box to pay for the transaction
      val paymentBox: InputBox =
        createPaymentBox(protonsValue = protonsToTransmute)

      val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
        inputBoxes = Seq(inGluonWBox.getAsInputBox(), paymentBox),
        protonsToTransmute = protonsToTransmute,
        changeAddress = changeAddress,
        dataInputs = Seq(oracleBoxInputBox)
      )

      val outBoxes: Seq[InputBox] = betaDecayPlusTx.getOutBoxesAsInputBoxes()
      val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
      val outPaymentBox: FundsToAddressBox =
        FundsToAddressBox.from(outBoxes.tail.head)

      // a. Get more neutrons for the protons given
      "Get more Neutrons" in {
        val neutronsToChange: Long = 1000

        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.neutronId,
                -neutronsToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.neutronId,
                neutronsToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          betaDecayPlusTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // b. Get less neutrons for the protons given
      "Get less Neutrons" in {
        val neutronsToChange: Long = 1000

        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.neutronId,
                neutronsToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.neutronId,
                -neutronsToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          betaDecayPlusTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }
    }
  }
}
