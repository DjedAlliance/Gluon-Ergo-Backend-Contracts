package gluonw.txs

import edge.boxes.{BoxWrapper, FundsToAddressBox}
import edge.commons.{ErgCommons, ErgoBoxHelper}
import gluonw.boxes.{GluonWBox, OracleBox}
import gluonw.common.TGluonWAlgorithm
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import edge.txs.TTx
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

abstract class GluonWTx(algorithm: TGluonWAlgorithm) extends TTx

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

    implicit val neutronOracleBox: OracleBox =
      OracleBox.from(dataInputs.head)

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

    implicit val neutronOracleBox: OracleBox =
      OracleBox.from(dataInputs.head)
    val outGluonWBox: GluonWBox =
      algorithm.fusion(inGluonWBox, ergToRetrieve)

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
  * Transmute Protons to Neutrons
  */
case class BetaDecayPlusTx(
  inputBoxes: Seq[InputBox],
  protonsToTransmute: Long,
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext, implicit val algorithm: TGluonWAlgorithm)
    extends GluonWTx(algorithm) {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {
    val inGluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      ErgoBoxHelper.consolidateBoxes(inputBoxes.tail).head

    implicit val neutronOracleBox: OracleBox =
      OracleBox.from(dataInputs.head)
    val outGluonWBox: GluonWBox =
      algorithm.betaDecayPlus(inGluonWBox, protonsToTransmute)

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
  * Transmute Neutrons to Protons
  */
case class BetaDecayMinusTx(
  inputBoxes: Seq[InputBox],
  neutronsToTransmute: Long,
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext, implicit val algorithm: TGluonWAlgorithm)
    extends GluonWTx(algorithm) {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {
    val inGluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      ErgoBoxHelper.consolidateBoxes(inputBoxes.tail).head

    implicit val neutronOracleBox: OracleBox =
      OracleBox.from(dataInputs.head)
    val outGluonWBox: GluonWBox =
      algorithm.betaDecayMinus(inGluonWBox, neutronsToTransmute)

    val neutronsGained: Long =
      outGluonWBox.Neutrons.getValue - inGluonWBox.Neutrons.getValue
    val protonsCost: Long =
      outGluonWBox.Protons.getValue - inGluonWBox.Protons.getValue
    val ergsCost: Long = outGluonWBox.value - inGluonWBox.value

    val outUserTokens: Seq[ErgoToken] = userBox.tokens.flatMap { token =>
      val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
      val protonsId: ErgoId = inGluonWBox.Protons.getId
      if (token.getId.equals(neutronsId)) {
        val totalNeutrons: Long = token.getValue + neutronsGained
        if (totalNeutrons != 0)
          Option(ErgoToken(neutronsId, totalNeutrons))
        else None
      } else if (token.getId.equals(protonsId)) {
        val totalProtons: Long = token.getValue + protonsCost
        if (totalProtons != 0)
          Option(ErgoToken(protonsId, token.getValue + protonsCost))
        else None
      } else {
        Option(token)
      }
    }

    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value - ergsCost - ErgCommons.MinMinerFee,
      tokens = outUserTokens
    )

    Seq(outGluonWBox, outUserBox)
  }
}
