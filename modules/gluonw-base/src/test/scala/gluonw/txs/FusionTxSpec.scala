package gluonw.txs

import edge.boxes.{CustomBoxData, FundsToAddressBox}
import edge.commons.ErgCommons
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
import org.ergoplatform.appkit.{
  Address,
  InputBox,
  Parameters,
  UnsignedTransaction
}

import scala.util.Random

class FusionTxSpec extends GluonWBase {
  client.setClient()
  // 1. FusionTx Success
  // a. Looping through multiple FusionTx and
  // getting the right amount back
  // b. Chained Tx through multiple FusionTx
  "FusionTx" should {
    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm =
      GluonWAlgorithm(gluonWConstants)

    // @todo kii change this using Calculator
    val gluonWBox: GluonWBox = genesisGluonWBox

    val gluonWCalculator: GluonWCalculator = GluonWCalculator(
      sProtons = gluonWBox.protonsCirculatingSupply,
      sNeutrons = gluonWBox.neutronsCirculatingSupply,
      rErg = gluonWBox.ergFissioned,
      gluonWConstants = gluonWConstants
    )

    val oracleBox: OracleBox = createTestOracleBox

    "loop through multiple fusionTx correctly" in {
      client.getClient.execute { implicit ctx =>
        // 1. Create a fission box
        // 2. Create a seq of erg to redeem
        val maxErgs: Long = 100_000L
        val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
        val changeAddress: Address = trueAddress
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 100).foreach { _ =>
          val random: Double = new Random().nextDouble()
          val ergsToFusion: Long = (maxErgsInNanoErgs * random).toLong

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            gluonWCalculator.fusion(ergsToFusion)

          // Payment box to pay for the transaction
          val paymentBox: InputBox = createPaymentBox(
            value = ErgCommons.MinMinerFee,
            neutronsValue = outputAssetAmount.neutronsAmount,
            protonsValue = outputAssetAmount.protonsAmount
          )

          val fusionTx: FusionTx = FusionTx(
            inputBoxes = Seq(gluonWBox.getAsInputBox(), paymentBox),
            ergToRetrieve = ergsToFusion,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          fusionTx.signTx

          val outBoxes: Seq[InputBox] = fusionTx.getOutBoxesAsInputBoxes()
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
              .value == gluonWBox.value + outputAssetAmount.neutronsAmount
          )
          assert(
            outGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == gluonWBox.value + outputAssetAmount.protonsAmount
          )

          // Check payment Box
          assert(
            outPaymentBox.value == ergsToFusion
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.protonId))
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.neutronId))
          )
        }
      }
    }

    "chain through multiple fusion tx" in {
      client.getClient.execute { implicit ctx =>
        val maxErgs: Long = 10_000L
        val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
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
          val ergsToFusion: Long = (maxErgsInNanoErgs * random).toLong

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            testGluonWCalculator.fission(ergsToFusion)

          // Payment box to pay for the transaction
          val paymentBox: InputBox = createPaymentBox(
            value = ErgCommons.MinMinerFee,
            neutronsValue = outputAssetAmount.neutronsAmount,
            protonsValue = outputAssetAmount.protonsAmount
          )

          val fusionTx: FusionTx = FusionTx(
            inputBoxes = Seq(inGluonWBox.getAsInputBox()),
            ergToRetrieve = ergsToFusion,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          val outBoxes: Seq[InputBox] = fusionTx.getOutBoxesAsInputBoxes()
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
              .value == inGluonWBox.value + outputAssetAmount.neutronsAmount
          )
          assert(
            outGluonWBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == inGluonWBox.value + outputAssetAmount.protonsAmount
          )

          // Check payment Box
          assert(
            outPaymentBox.value == ergsToFusion
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.protonId))
          )
          assert(
            !outPaymentBox.tokens.exists(_.getId.equals(GluonWTokens.neutronId))
          )

          inGluonWBox = outGluonWBox
        }
      }
    }
  }

  // 2. FusionTx Failures
  "FusionTx Failures (User Perspective):" should {
    client.getClient.execute { implicit ctx =>
      val gluonWConstants: TGluonWConstants = GluonWConstants()
      implicit val gluonWAlgorithm: GluonWAlgorithm =
        GluonWAlgorithm(gluonWConstants)

      // @todo kii change this using Calculator
      val gluonWBox: GluonWBox = genesisGluonWBox

      val gluonWCalculator: GluonWCalculator = GluonWCalculator(
        sProtons = gluonWBox.protonsCirculatingSupply,
        sNeutrons = gluonWBox.neutronsCirculatingSupply,
        rErg = gluonWBox.ergFissioned,
        gluonWConstants = gluonWConstants
      )

      val oracleBox: OracleBox = createTestOracleBox

      // 1. Create a fission box
      // 2. Create a seq of erg to redeem
      val maxErgs: Long = 1_000L
      val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
      val changeAddress: Address = trueAddress
      val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()
      val ergsToFusion: Long = maxErgsInNanoErgs

      val outputAssetAmount: GluonWBoxOutputAssetAmount =
        gluonWCalculator.fusion(ergsToFusion)

      // Payment box to pay for the transaction
      val paymentBox: InputBox = createPaymentBox(
        value = ErgCommons.MinMinerFee,
        neutronsValue = outputAssetAmount.neutronsAmount,
        protonsValue = outputAssetAmount.protonsAmount
      )

      val fusionTx: FusionTx = FusionTx(
        inputBoxes = Seq(gluonWBox.getAsInputBox(), paymentBox),
        ergToRetrieve = ergsToFusion,
        changeAddress = changeAddress,
        dataInputs = Seq(oracleBoxInputBox)
      )

      val outBoxes: Seq[InputBox] = fusionTx.getOutBoxesAsInputBoxes()
      val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
      val outPaymentBox: FundsToAddressBox =
        FundsToAddressBox.from(outBoxes.tail.head)

      // a. Trying to give more Neutrons
      "Giving more Neutrons than expected" in {
        val amountToChange: Long = 1000
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.neutronId,
                amountToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.neutronId,
                -amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // b. Trying to give more Protons
      "Giving more Protons than expected" in {
        val amountToChange: Long = 1000
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.protonId,
                amountToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.protonId,
                -amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // c. Trying to give more Neutrons and Protons
      "Giving more Protons and Neutrons than expected" in {
        val amountToChange: Long = 1000
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                getManipulatedToken(
                  outGluonWBox.tokens,
                  GluonWTokens.protonId,
                  amountToChange
                ),
                GluonWTokens.neutronId,
                amountToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                getManipulatedToken(
                  outPaymentBox.tokens,
                  GluonWTokens.protonId,
                  -amountToChange
                ),
                GluonWTokens.neutronId,
                -amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // d. Trying to give less Neutrons
      "Give lesser Neutrons" in {
        val amountToChange: Long = 1000
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.neutronId,
                -amountToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.neutronId,
                amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // e. Trying to give less Protons
      "Give lesser Protons" in {
        val amountToChange: Long = 1000
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outGluonWBox.tokens,
                GluonWTokens.protonId,
                -amountToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                outPaymentBox.tokens,
                GluonWTokens.protonId,
                amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // f. Trying to give less Neutrons and Protons
      "Give lesser Neutrons and Protons" in {
        val amountToChange: Long = 1000
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                getManipulatedToken(
                  outGluonWBox.tokens,
                  GluonWTokens.protonId,
                  -amountToChange
                ),
                GluonWTokens.neutronId,
                -amountToChange
              )
            )
          ),
          CustomBoxData(customTokens =
            Option(
              getManipulatedToken(
                getManipulatedToken(
                  outPaymentBox.tokens,
                  GluonWTokens.protonId,
                  amountToChange
                ),
                GluonWTokens.neutronId,
                amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // g. Trying to get more Ergs from the right amount
      "Get more Ergs than expected amount" in {
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customValue =
            Option(outGluonWBox.value - Parameters.OneErg)
          ),
          CustomBoxData(customValue =
            Option(outPaymentBox.value + Parameters.OneErg)
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // h. Trying to get less Ergs from the right amount
      "Get lesser Ergs than expected amount" in {
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customValue =
            Option(outGluonWBox.value + Parameters.OneErg)
          ),
          CustomBoxData(customValue =
            Option(outPaymentBox.value - Parameters.OneErg)
          )
        )

        val unsignedTx: UnsignedTransaction =
          fusionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }
    }
  }
}
