package gluonw.txs

import edge.boxes.FundsToAddressBox
import edge.commons.ErgCommons
import edge.registers.{CollBytePairRegister, LongPairRegister}
import edge.txs.Tx
import gluonw.boxes.{GluonWBox, OracleBox}
import gluonw.common.{GluonWBase, GluonWTokens}
import org.ergoplatform.appkit.{Address, Parameters}
import org.ergoplatform.sdk.ErgoToken

class HackSpec extends GluonWBase {
  client.setClient()

  "Hack fails when" should {
    client.getClient.execute { implicit ctx =>
      val gluonWBox: GluonWBox = genesisGluonWBox()
      val changeAddress: Address = trueAddress
      val oracleBox: OracleBox = createTestOracleBox

      def hackThisTx(
        outGluonWBox: GluonWBox,
        hackerBox: FundsToAddressBox
      )(gluonWBox: GluonWBox, oracleBox: OracleBox): Unit = {
        val hackTx: Tx = Tx(
          inputBoxes = Seq(gluonWBox.getAsInputBox()),
          changeAddress = changeAddress,
          dataInputs = Seq(oracleBox.getAsInputBox()),
          outBoxes = Seq(outGluonWBox, hackerBox)
        )

        assertThrows[Throwable] {
          hackTx.signTx
        }
      }

      // 1. Trying to get Erg out from GluonWBox
      "get Ergs out of GluonWBox" in {
        val ergToHack: Long = Parameters.OneErg
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - ergToHack
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ergToHack - ErgCommons.MinMinerFee,
          address = trueAddress
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 2. Trying to get Protons out from GluonWBox
      "get Protons out of GluonWBox" in {
        val tokensToHack: Long = 1000L
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - (ErgCommons.MinMinerFee * 2),
          tokens = getManipulatedToken(
            gluonWBox.tokens,
            GluonWTokens.protonId,
            -tokensToHack
          )
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ErgCommons.MinMinerFee,
          address = trueAddress,
          tokens = Seq(ErgoToken(GluonWTokens.protonId, tokensToHack))
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 3. Trying to get Neutrons out from GluonWBox
      "get Neutrons out of GluonWBox" in {
        val tokensToHack: Long = 1000L
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - (ErgCommons.MinMinerFee * 2),
          tokens = getManipulatedToken(
            gluonWBox.tokens,
            GluonWTokens.neutronId,
            -tokensToHack
          )
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ErgCommons.MinMinerFee,
          address = trueAddress,
          tokens = Seq(ErgoToken(GluonWTokens.neutronId, tokensToHack))
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 4. Trying to get Erg and Protons out from GluonWBox
      "get Ergs and Protons out of GluonWBox" in {
        val ergsToHack: Long = Parameters.OneErg
        val tokensToHack: Long = 1000L
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - ergsToHack,
          tokens = getManipulatedToken(
            gluonWBox.tokens,
            GluonWTokens.protonId,
            -tokensToHack
          )
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ergsToHack - ErgCommons.MinMinerFee,
          address = trueAddress,
          tokens = Seq(ErgoToken(GluonWTokens.protonId, tokensToHack))
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 5. Trying to get Erg and Neutrons out from GluonWBox
      "get Erg and Neutrons out of GluonWBox" in {
        val ergsToHack: Long = Parameters.OneErg
        val tokensToHack: Long = 1000L
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - ergsToHack,
          tokens = getManipulatedToken(
            gluonWBox.tokens,
            GluonWTokens.neutronId,
            -tokensToHack
          )
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ergsToHack - ErgCommons.MinMinerFee,
          address = trueAddress,
          tokens = Seq(ErgoToken(GluonWTokens.neutronId, tokensToHack))
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 6. Trying to get Protons and Neutrons out from GluonWBox
      "get Protons and Neutrons out of GluonWBox" in {
        val ergsToHack: Long = Parameters.OneErg
        val tokensToHack: Long = 1000L
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - (ErgCommons.MinMinerFee * 2),
          tokens = getManipulatedToken(
            getManipulatedToken(
              gluonWBox.tokens,
              GluonWTokens.neutronId,
              -tokensToHack
            ),
            GluonWTokens.protonId,
            -tokensToHack
          )
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ErgCommons.MinMinerFee,
          address = trueAddress,
          tokens = Seq(
            ErgoToken(GluonWTokens.neutronId, tokensToHack),
            ErgoToken(GluonWTokens.protonId, tokensToHack)
          )
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 7. Trying to get Ergs, Protons and Neutrons out from GluonWBox
      "get Ergs, Protons and Neutrons out of GluonWBox" in {
        val ergsToHack: Long = Parameters.OneErg
        val tokensToHack: Long = 1000L
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - ergsToHack,
          tokens = getManipulatedToken(
            getManipulatedToken(
              gluonWBox.tokens,
              GluonWTokens.neutronId,
              -tokensToHack
            ),
            GluonWTokens.protonId,
            -tokensToHack
          )
        )
        val hackerBox: FundsToAddressBox = FundsToAddressBox(
          value = ergsToHack - ErgCommons.MinMinerFee,
          address = trueAddress,
          tokens = Seq(
            ErgoToken(GluonWTokens.neutronId, tokensToHack),
            ErgoToken(GluonWTokens.protonId, tokensToHack)
          )
        )

        hackThisTx(outGluonBox, hackerBox)(gluonWBox, oracleBox)
      }

      // 8. Change value of Registers
      "change value of registers, when tokens and ergs stay same" in {
        val outGluonBox: GluonWBox = gluonWBox.copy(
          value = gluonWBox.value - ErgCommons.MinMinerFee,
          totalSupplyRegister = new LongPairRegister(100, 85),
          tokenIdRegister = new CollBytePairRegister(
            GluonWTokens.gluonWBoxNFTId.getBytes,
            GluonWTokens.gluonWBoxNFTId.getBytes
          )
        )

        hackThisTx(outGluonBox, null)(gluonWBox, oracleBox)
      }
    }
  }
}
