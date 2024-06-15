package gluonw.txs

import edge.boxes.{CustomBoxData, FundsToAddressBox}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox, OracleBuybackBox}
import gluonw.common.{
  GluonWAlgorithm,
  GluonWBase,
  GluonWBoxOutputAssetAmount,
  GluonWCalculator,
  GluonWConstants,
  GluonWFeesCalculator,
  GluonWTokens,
  TGluonWConstants
}
import org.ergoplatform.appkit.{
  Address,
  ContextVar,
  InputBox,
  UnsignedTransaction
}

import scala.util.Random

/**
  * Neutrons to Protons
  */
class BetaDecayMinusSpec extends GluonWBase {
  client.setClient()
  // 1. BetaDecayMinusTx Success
  // a. Looping through multiple BetaDecayMinusTx and
  // getting the right value
  // b. Chained Tx through multiple BetaDecayMinusTx
  "BetaDecayMinusTx" should {
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

    "loop through multiple betaDecayMinusTx correctly" in {
      client.getClient.execute { implicit ctx =>
        // 1. Create a fission box
        // 2. Create a seq of erg to redeem
        val maxNeutrons: Long = 1_000L
        val maxNeutronsInPrecision: Long =
          maxNeutrons * GluonWBoxConstants.PRECISION
        val changeAddress: Address = trueAddress
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 20).foreach { _ =>
          implicit val gluonWFeesCalculator: GluonWFeesCalculator =
            GluonWFeesCalculator()(gluonWBox, gluonWConstants)

          val random: Double = new Random().nextDouble()
          val neutronsToTransmute: Long =
            (maxNeutronsInPrecision * random).toLong

          // Create payment box to pay for transaction
          val paymentBox: InputBox =
            createPaymentBox(
              value = gluonWConstants.neutronsToNanoErg(
                neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
                neutronsAmount = neutronsToTransmute,
                fissionedErg = gluonWBox.ergFissioned,
                goldPriceNanoErgPerGram = oracleBox.getPricePerGram
              ) / 10,
              neutronsValue = neutronsToTransmute
            )

          implicit val currentHeight: Long = ctx.getHeight
          val oracleBuybackBox: InputBox = OracleBuybackBox
            .testBox()
            .getAsInputBox(ctx.newTxBuilder())
            .withContextVars(ContextVar.of(0.toByte, 0))
          val oracleBuybackBoxTopUpRoute: InputBox =
            OracleBuybackBox.setTopUp(oracleBuybackBox)
          val betaDecayMinusTx: BetaDecayMinusTx = BetaDecayMinusTx(
            inputBoxes = Seq(
              gluonWBox.getAsInputBox(),
              paymentBox,
              oracleBuybackBoxTopUpRoute
            ),
            neutronsToTransmute = neutronsToTransmute,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          betaDecayMinusTx.signTx

          val outBoxes: Seq[InputBox] =
            betaDecayMinusTx.getOutBoxesAsInputBoxes()
          val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
          val outPaymentBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.head)

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            gluonWCalculator.betaDecayMinus(
              rErg = gluonWBox.ergFissioned,
              volumeMinus = outGluonWBox.volumeMinusRegister.value.toList,
              volumePlus = outGluonWBox.volumePlusRegister.value.toList,
              neutronsToDecay = neutronsToTransmute
            )(
              oracleBox.getPricePerGram
            )

          val outServiceFeeBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.tail.head)

          val outOracleFeeBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.tail.tail.head)

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
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == outputAssetAmount.protonsAmount
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.neutronId))
          )
        }
      }
    }

    "chain through multiple betaDecayMinusTx" in {
      client.getClient.execute { implicit ctx =>
        val maxNeutrons: Long = 10_000L
        val maxNeutronsInPrecision: Long =
          maxNeutrons * GluonWBoxConstants.PRECISION
        val changeAddress: Address = trueAddress
        var inGluonWBox: GluonWBox = gluonWBox
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 10).foreach { _ =>
          implicit val gluonWFeesCalculator: GluonWFeesCalculator =
            GluonWFeesCalculator()(inGluonWBox, gluonWConstants)

          val testGluonWCalculator: GluonWCalculator = GluonWCalculator(
            sProtons = inGluonWBox.protonsCirculatingSupply,
            sNeutrons = inGluonWBox.neutronsCirculatingSupply,
            rErg = inGluonWBox.ergFissioned,
            gluonWConstants = gluonWConstants
          )

          val random: Double = new Random().nextDouble()
          val neutronsToTransmute: Long =
            (maxNeutronsInPrecision * random).toLong

          // Payment box to pay for the transaction
          val paymentBox: InputBox =
            createPaymentBox(
              value = gluonWConstants.neutronsToNanoErg(
                neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
                neutronsAmount = neutronsToTransmute,
                fissionedErg = gluonWBox.ergFissioned,
                goldPriceNanoErgPerGram = oracleBox.getPricePerGram
              ) / 10,
              neutronsValue = neutronsToTransmute
            )

          implicit val currentHeight: Long = ctx.getHeight
          val oracleBuybackBox: InputBox =
            OracleBuybackBox.testBox().getAsInputBox(ctx.newTxBuilder())
          val oracleBuybackBoxTopUpRoute: InputBox =
            OracleBuybackBox.setTopUp(oracleBuybackBox)
          val betaDecayMinusTx: BetaDecayMinusTx = BetaDecayMinusTx(
            inputBoxes = Seq(
              inGluonWBox.getAsInputBox(),
              paymentBox,
              oracleBuybackBoxTopUpRoute
            ),
            neutronsToTransmute = neutronsToTransmute,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          betaDecayMinusTx.signTx

          val outBoxes: Seq[InputBox] =
            betaDecayMinusTx.getOutBoxesAsInputBoxes()
          val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
          val outPaymentBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.head)

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            testGluonWCalculator.betaDecayMinus(
              rErg = inGluonWBox.ergFissioned,
              volumeMinus = outGluonWBox.volumeMinusRegister.value.toList,
              volumePlus = outGluonWBox.volumePlusRegister.value.toList,
              neutronsToDecay = neutronsToTransmute
            )(
              oracleBox.getPricePerGram
            )

          val outServiceFeeBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.tail.head)

          val outOracleFeeBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.tail.tail.head)

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
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == outputAssetAmount.protonsAmount
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.neutronId))
          )

          inGluonWBox = outGluonWBox
        }
      }
    }
  }

  // 2. BetaDecayMinusTx Failures
  "BetaDecayMinusTx Failures (User Perspective):" should {
    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm =
      GluonWAlgorithm(gluonWConstants)

    val gluonWBox: GluonWBox = genesisGluonWBox()

    val oracleBox: OracleBox = createTestOracleBox
    client.getClient.execute { implicit ctx =>
      val maxNeutrons: Long = 1_000L
      val maxNeutronsInPrecision: Long =
        maxNeutrons * GluonWBoxConstants.PRECISION
      val changeAddress: Address = trueAddress
      var inGluonWBox: GluonWBox = gluonWBox
      implicit val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)
      val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()
      val testGluonWCalculator: GluonWCalculator = GluonWCalculator(
        sProtons = inGluonWBox.protonsCirculatingSupply,
        sNeutrons = inGluonWBox.neutronsCirculatingSupply,
        rErg = inGluonWBox.ergFissioned,
        gluonWConstants = gluonWConstants
      )

      val random: Double = new Random().nextDouble()
      val neutronsToTransmute: Long = (maxNeutronsInPrecision * random).toLong

      // Payment box to pay for the transaction
      val paymentBox: InputBox =
        createPaymentBox(
          value = gluonWConstants.neutronsToNanoErg(
            neutronsInCirculation = gluonWBox.neutronsCirculatingSupply,
            neutronsAmount = neutronsToTransmute,
            fissionedErg = gluonWBox.ergFissioned,
            goldPriceNanoErgPerGram = oracleBox.getPricePerGram
          ) / 10,
          neutronsValue = neutronsToTransmute + 1000
        )

      implicit val currentHeight: Long = ctx.getHeight
      val oracleBuybackBox: InputBox =
        OracleBuybackBox.testBox().getAsInputBox(ctx.newTxBuilder())
      val oracleBuybackBoxTopUpRoute: InputBox =
        OracleBuybackBox.setTopUp(oracleBuybackBox)
      val betaDecayMinusTx: BetaDecayMinusTx = BetaDecayMinusTx(
        inputBoxes = Seq(
          inGluonWBox.getAsInputBox(),
          paymentBox,
          oracleBuybackBoxTopUpRoute
        ),
        neutronsToTransmute = neutronsToTransmute,
        changeAddress = changeAddress,
        dataInputs = Seq(oracleBoxInputBox)
      )

      val outBoxes: Seq[InputBox] = betaDecayMinusTx.getOutBoxesAsInputBoxes()
      val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
      val outPaymentBox: FundsToAddressBox =
        FundsToAddressBox.from(outBoxes.tail.head)

      // a. Get more protons for the neutrons given
      "Get more Protons" in {
        val protonsToChange: Long = 1000

        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.protonId,
                -protonsToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.protonId,
                protonsToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          betaDecayMinusTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // b. Get less protons for the neutrons given
      "Get less Protons" in {
        val protonsToChange: Long = 1000

        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.protonId,
                protonsToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.protonId,
                -protonsToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          betaDecayMinusTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }
    }
  }
}
