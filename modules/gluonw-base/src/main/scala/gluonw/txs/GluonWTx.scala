package gluonw.txs

import boxes.{BoxWrapper, FundsToAddressBox}
import commons.{ErgCommons, ErgoBoxHelper}
import gluonw.boxes.GluonWBox
import gluonw.common.{GluonWAlgorithm, TGluonWAlgorithm}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoId,
  ErgoToken,
  InputBox
}
import txs.Tx

abstract class GluonWTx(algorithm: TGluonWAlgorithm) extends Tx

case class FissionTx(
  inputBoxes: Seq[InputBox],
  ergToExchange: Long,
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext, implicit val algorithm: TGluonWAlgorithm)
    extends GluonWTx(algorithm) {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {
    val inGluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)

    // Consolidate user boxes to get total value
    val userBox: FundsToAddressBox =
      ErgoBoxHelper.consolidateBoxes(inputBoxes.tail).head

    val outGluonWBox: GluonWBox =
      algorithm.fission(inGluonWBox, ergToExchange)

    // @todo kii : Add protocol fee for DAO
    val neutronsGained: Long =
      inGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue
    val protonsGained: Long =
      inGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue
    val ergsCost: Long = outGluonWBox.value - inGluonWBox.value

    val outUserTokens: Seq[ErgoToken] = userBox.tokens.map { token =>
      val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
      val protonsId: ErgoId = inGluonWBox.Protons.getId
      if (token.getId.equals(neutronsId)) {
        new ErgoToken(neutronsId, token.getValue + neutronsGained)
      } else if (token.getId.equals(protonsId)) {
        new ErgoToken(protonsId, token.getValue + protonsGained)
      } else {
        token
      }
    }

    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value - ergsCost - ErgCommons.MinMinerFee,
      tokens = outUserTokens
    )

    Seq(outGluonWBox, outUserBox)
  }
}

case class FusionTx(
  inputBoxes: Seq[InputBox],
  ergToRetrieve: Long,
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext, implicit val algorithm: TGluonWAlgorithm)
    extends GluonWTx(algorithm) {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {
    val inGluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      FundsToAddressBox.from(inputBoxes.tail.head)

    val outGluonWBox: GluonWBox =
      GluonWAlgorithm.fusion(inGluonWBox, ergToRetrieve)

    val neutronsCost: Long =
      outGluonWBox.Neutrons.getValue - inGluonWBox.Neutrons.getValue
    val protonsCost: Long =
      outGluonWBox.Protons.getValue - inGluonWBox.Protons.getValue
    val ergsGained: Long = inGluonWBox.value - outGluonWBox.value

    val outUserTokens: Seq[ErgoToken] = userBox.tokens.map { token =>
      val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
      val protonsId: ErgoId = inGluonWBox.Protons.getId
      if (token.getId.equals(neutronsId)) {
        new ErgoToken(neutronsId, token.getValue - neutronsCost)
      } else if (token.getId.equals(protonsId)) {
        new ErgoToken(protonsId, token.getValue - protonsCost)
      } else {
        token
      }
    }

    // Ergs gained should minus protocol fee
    val ergsToUser: Long = ergsGained
    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value + ergsToUser - ErgCommons.MinMinerFee,
      tokens = outUserTokens
    )

    Seq(outGluonWBox, outUserBox)
  }
}

/**
  * Transmute Gold to Rsv
  */
case class BetaDecayPlusTx(
  inputBoxes: Seq[InputBox],
  goldToTransmute: Long,
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext, implicit val algorithm: TGluonWAlgorithm)
    extends GluonWTx(algorithm) {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {
    val inGluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      ErgoBoxHelper.consolidateBoxes(inputBoxes.tail).head

    val outGluonWBox: GluonWBox =
      GluonWAlgorithm.betaDecayPlus(inGluonWBox, goldToTransmute)

    val neutronsCost: Long =
      outGluonWBox.Neutrons.getValue - inGluonWBox.Neutrons.getValue
    val protonsGained: Long =
      inGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue
    val ergsCost: Long = outGluonWBox.value - inGluonWBox.value

    val outUserTokens: Seq[ErgoToken] = userBox.tokens.map { token =>
      val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
      val protonsId: ErgoId = inGluonWBox.Protons.getId
      if (token.getId.equals(neutronsId)) {
        new ErgoToken(neutronsId, token.getValue - neutronsCost)
      } else if (token.getId.equals(protonsId)) {
        new ErgoToken(protonsId, token.getValue + protonsGained)
      } else {
        token
      }
    }

    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value - ergsCost - ErgCommons.MinMinerFee,
      tokens = outUserTokens
    )

    Seq(outGluonWBox, outUserBox)
  }
}

/**
  * Transmute Rsv to Gold
  */
case class BetaDecayMinusTx(
  inputBoxes: Seq[InputBox],
  goldToTransmute: Long,
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext, implicit val algorithm: TGluonWAlgorithm)
    extends GluonWTx(algorithm) {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {
    val inGluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      ErgoBoxHelper.consolidateBoxes(inputBoxes.tail).head

    val outGluonWBox: GluonWBox =
      GluonWAlgorithm.betaDecayMinus(inGluonWBox, goldToTransmute)

    val neutronsGained: Long =
      inGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue
    val protonsCost: Long =
      outGluonWBox.Protons.getValue - inGluonWBox.Protons.getValue
    val ergsCost: Long = outGluonWBox.value - inGluonWBox.value

    val outUserTokens: Seq[ErgoToken] = userBox.tokens.map { token =>
      val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
      val protonsId: ErgoId = inGluonWBox.Protons.getId
      if (token.getId.equals(neutronsId)) {
        new ErgoToken(neutronsId, token.getValue + neutronsGained)
      } else if (token.getId.equals(protonsId)) {
        new ErgoToken(protonsId, token.getValue - protonsCost)
      } else {
        token
      }
    }

    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value - ergsCost - ErgCommons.MinMinerFee,
      tokens = outUserTokens
    )

    Seq(outGluonWBox, outUserBox)
  }
}
