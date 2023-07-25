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

    val gluonWBox: GluonWBox = genesisGluonWBox

    val gluonWCalculator: GluonWCalculator = GluonWCalculator(
      sProtons = gluonWBox.protonsCirculatingSupply,
      sNeutrons = gluonWBox.neutronsCirculatingSupply,
      rErg = gluonWBox.ergFissioned,
      gluonWConstants = gluonWConstants
    )

    val oracleBox: OracleBox = createTestOracleBox

    "loop through multiple fissionTx correctly" in {
      client.getClient.execute { implicit ctx =>
        // 1. Create a fission box
        // 2. Create a seq of erg to redeem
        val maxErgs: Long = 100_000L
        val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
        val changeAddress: Address = trueAddress
        val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()

        (1 to 100).foreach { _ =>
          val random: Double = new Random().nextDouble()
          val ergsToFission: Long = (maxErgsInNanoErgs * random).toLong

          val outputAssetAmount: GluonWBoxOutputAssetAmount =
            gluonWCalculator.fission(ergsToFission)

          // Payment box to pay for the transaction
          val paymentBox: InputBox = createPaymentBox(
            value = ergsToFission + (ErgCommons.MinMinerFee * 2)
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
            outPaymentBox.value == FundsToAddressBox
              .from(paymentBox)
              .value - ergsToFission - ErgCommons.MinMinerFee
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
            value = ergsToFission + (ErgCommons.MinMinerFee * 2)
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
            outPaymentBox.value == FundsToAddressBox
              .from(paymentBox)
              .value - ergsToFission - ErgCommons.MinMinerFee
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

    val gluonWBox: GluonWBox = genesisGluonWBox

    val gluonWCalculator: GluonWCalculator = GluonWCalculator(
      sProtons = gluonWBox.protonsCirculatingSupply,
      sNeutrons = gluonWBox.neutronsCirculatingSupply,
      rErg = gluonWBox.ergFissioned,
      gluonWConstants = gluonWConstants
    )

    val oracleBox: OracleBox = createTestOracleBox

    client.getClient.execute { implicit ctx =>
      val maxErgs: Long = 10_000L
      val maxErgsInNanoErgs: Long = maxErgs * Parameters.OneErg
      val changeAddress: Address = trueAddress
      var inGluonWBox: GluonWBox = gluonWBox
      val oracleBoxInputBox: InputBox = oracleBox.getAsInputBox()
      val testGluonWCalculator: GluonWCalculator = GluonWCalculator(
        sProtons = inGluonWBox.protonsCirculatingSupply,
        sNeutrons = inGluonWBox.neutronsCirculatingSupply,
        rErg = inGluonWBox.ergFissioned,
        gluonWConstants = gluonWConstants
      )

      val random: Double = new Random().nextDouble()
      val ergsToFission: Long = (maxErgsInNanoErgs * random).toLong

      // Payment box to pay for the transaction
      val paymentBox: InputBox = createPaymentBox(
        value = ergsToFission + (ErgCommons.MinMinerFee * 2)
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
        val amountToChange: Long = Parameters.OneErg
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
