package gluonw.txs

import edge.boxes.{CustomBoxData, FundsToAddressBox}
import edge.commons.ErgCommons
import gluonw.boxes.{GluonWBox, OracleBox}
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
  InputBox,
  Parameters,
  UnsignedTransaction
}

import scala.util.Random

class FissionTxSpec extends GluonWBase {
  client.setClient()
  // 1. FissionTx Success
  // a. Looping through multiple FissionTx and
  // getting the right value
  // b. Chained Tx through multiple FissionTx
  "FissionTx" should {
    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm =
      GluonWAlgorithm(gluonWConstants)

    val oracleBox: OracleBox = createTestOracleBox
    val gluonWBox: GluonWBox = genesisGluonWBox()
    "loop through multiple fissionTx correctly" in {
      client.getClient.execute { implicit ctx =>
        val gluonWCalculator: GluonWCalculator = GluonWCalculator(
          sProtons = gluonWBox.protonsCirculatingSupply,
          sNeutrons = gluonWBox.neutronsCirculatingSupply,
          rErg = gluonWBox.ergFissioned,
          gluonWConstants = gluonWConstants
        )

        implicit val gluonWFeesCalculator: GluonWFeesCalculator =
          GluonWFeesCalculator()(gluonWBox, gluonWConstants)

        // 1. Create a fission box
        // 2. Create a seq of erg to redeem
        val maxErgs: Long = 1_000L
        val minErgs: Long = 5L
        val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
        val changeAddress: Address = trueAddress
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 20).foreach { _ =>
          val random: Double = new Random().nextDouble()
          val ergsToFission: Long =
            (maxErgsInNanoErgs * random).toLong + minErgs

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            gluonWCalculator.fission(ergsToFission)

          // Payment box to pay for the transaction
          val paymentBox: InputBox = createPaymentBox(
            value =
              ergsToFission + (ErgCommons.MinMinerFee * 2) + (ergsToFission / 10)
          )

          val fissionTx: FissionTx = FissionTx(
            inputBoxes = Seq(gluonWBox.getAsInputBox(), paymentBox),
            ergToExchange = ergsToFission,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          fissionTx.signTx

          val outBoxes: Seq[InputBox] = fissionTx.getOutBoxesAsInputBoxes()
          val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
          val outPaymentBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.head)

          assert(
            outGluonWBox.value == gluonWBox.value - outputAssetAmount.ergAmount
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
            outPaymentBox.value == FundsToAddressBox
              .from(paymentBox)
              .value - ergsToFission - ErgCommons.MinMinerFee - getFissionOrFusionFees(
              ergsToFission
            )(gluonWFeesCalculator)
          )
          assert(
            outPaymentBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value == outputAssetAmount.neutronsAmount
          )
          assert(
            outPaymentBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == outputAssetAmount.protonsAmount
          )
        }
      }
    }

    "chain through multiple fission tx" in {
      client.getClient.execute { implicit ctx =>
        val maxErgs: Long = 10_000L
        val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
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
          val ergsToFission: Long = (maxErgsInNanoErgs * random).toLong

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            testGluonWCalculator.fission(ergsToFission)

          // Payment box to pay for the transaction
          val paymentBox: InputBox = createPaymentBox(
            value =
              ergsToFission + (ErgCommons.MinMinerFee * 2) + (ergsToFission / 10)
          )

          val fissionTx: FissionTx = FissionTx(
            inputBoxes = Seq(inGluonWBox.getAsInputBox(), paymentBox),
            ergToExchange = ergsToFission,
            changeAddress = changeAddress,
            dataInputs = Seq(oracleBoxInputBox)
          )

          fissionTx.signTx

          val outBoxes: Seq[InputBox] = fissionTx.getOutBoxesAsInputBoxes()
          val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
          val outPaymentBox: FundsToAddressBox =
            FundsToAddressBox.from(outBoxes.tail.head)

          assert(
            outGluonWBox.value == inGluonWBox.value - outputAssetAmount.ergAmount
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
            outPaymentBox.value == FundsToAddressBox
              .from(paymentBox)
              .value - ergsToFission - ErgCommons.MinMinerFee - getFissionOrFusionFees(
              ergsToFission
            )(gluonWFeesCalculator)
          )
          assert(
            outPaymentBox.tokens
              .filter(_.getId.equals(GluonWTokens.neutronId))
              .head
              .value == outputAssetAmount.neutronsAmount
          )
          assert(
            outPaymentBox.tokens
              .filter(_.getId.equals(GluonWTokens.protonId))
              .head
              .value == outputAssetAmount.protonsAmount
          )

          inGluonWBox = outGluonWBox
        }
      }
    }
  }

  // 2. FissionTx Failures
  "FissionTx Failures (User Perspective):" should {
    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm =
      GluonWAlgorithm(gluonWConstants)

    val gluonWBox: GluonWBox = genesisGluonWBox()
    implicit val gluonWFeesCalculator: GluonWFeesCalculator =
      GluonWFeesCalculator()(gluonWBox, gluonWConstants)

    val oracleBox: OracleBox = createTestOracleBox

    client.getClient.execute { implicit ctx =>
      val maxErgs: Long = 10_000L
      val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
      val minErgs: Long = 5L * Parameters.OneErg
      val changeAddress: Address = trueAddress
      var inGluonWBox: GluonWBox = gluonWBox
      val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

      val random: Double = new Random().nextDouble()
      val ergsToFission: Long = (maxErgsInNanoErgs * random).toLong + minErgs

      // Payment box to pay for the transaction
      // Give more ergs for cases.
      val paymentBox: InputBox = createPaymentBox(
        value =
          ergsToFission + (ErgCommons.MinMinerFee * 2) + (ergsToFission / 10)
      )

      val fissionTx: FissionTx = FissionTx(
        inputBoxes = Seq(inGluonWBox.getAsInputBox(), paymentBox),
        ergToExchange = ergsToFission,
        changeAddress = changeAddress,
        dataInputs = Seq(oracleBoxInputBox)
      )

      val outBoxes: Seq[InputBox] = fissionTx.getOutBoxesAsInputBoxes()
      val outGluonWBox: GluonWBox = GluonWBox.from(outBoxes.head)
      val outPaymentBox: FundsToAddressBox =
        FundsToAddressBox.from(outBoxes.tail.head)

      // a. Trying to get more Neutrons
      "Get more Neutrons" in {
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
                +amountToChange
              )
            )
          )
        )

        val unsignedTx: UnsignedTransaction =
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // b. Trying to get more Protons
      "Get more Protons" in {
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
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // c. Trying to get more Neutrons and Protons
      "Get more Neutrons and Protons" in {
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
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // d. Trying to get less Neutrons
      "Get lesser Neutrons" in {
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
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // e. Trying to get less Protons
      "Get lesser Protons" in {
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
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // f. Trying to get less Neutrons and Protons
      "Get lesser Neutrons and Protons" in {
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
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // g. Trying to give more Ergs from the right amount
      "Give more Ergs than expected amount" in {
        val amountToChange: Long = Parameters.OneErg
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customValue =
            Option(outGluonWBox.value - amountToChange)
          ),
          CustomBoxData(customValue =
            Option(outPaymentBox.value + amountToChange)
          )
        )

        val unsignedTx: UnsignedTransaction =
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }

      // h. Trying to give less Ergs from the right amount
      "Give less Ergs than expected amount" in {
        val amountToChange: Long = Parameters.MinFee * 3
        val customBoxData: Seq[CustomBoxData] = Seq(
          CustomBoxData(customValue =
            Option(outGluonWBox.value + amountToChange)
          ),
          CustomBoxData(customValue =
            Option(outPaymentBox.value - amountToChange)
          )
        )

        val unsignedTx: UnsignedTransaction =
          fissionTx.buildCustomTx(customBoxData)

        assertThrows[Throwable] {
          dummyProver.sign(unsignedTx)
        }
      }
    }
  }
}
