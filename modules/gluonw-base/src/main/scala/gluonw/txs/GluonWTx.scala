package gluonw.txs

import edge.boxes.{BoxWrapper, FundsToAddressBox}
import edge.commons.{ErgCommons, ErgoBoxHelper}
import gluonw.boxes.{GluonWBox, OracleBox}
import gluonw.common.TGluonWAlgorithm
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import edge.txs.TTx
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

abstract class GluonWTx(algorithm: TGluonWAlgorithm) extends TTx {

  def outputGluonTokens(
    userTokens: Seq[ErgoToken],
    neutronsCost: (ErgoId, Long),
    protonsCost: (ErgoId, Long)
  ): Seq[ErgoToken] = {
    // Method: Calculate Gluon Tokens
    def calculateGluonTokens(
      token: ErgoToken,
      cost: Long
    ): Option[ErgoToken] = {
      val totalTokenAmount: Long = token.getValue - cost
      if (totalTokenAmount > 0) {
        Option(ErgoToken(token.getId, totalTokenAmount))
      } else None
    }

    var outUserTokens: Seq[ErgoToken] = userTokens

    def evaluateTokens(
      userTokens: Seq[ErgoToken],
      tokenCost: (ErgoId, Long)
    ): Seq[ErgoToken] = {
      val isTokenExist: Boolean =
        userTokens.exists(_.getId.equals(tokenCost._1))

      if (isTokenExist) {
        val result = userTokens.flatMap { token =>
          if (token.getId.equals(tokenCost._1)) {
            calculateGluonTokens(token, tokenCost._2)
          } else {
            Option(token)
          }
        }

        return result
      } else if (tokenCost._2 < 0) {
        val result = userTokens :+ ErgoToken(tokenCost._1, -tokenCost._2)
        return result
      }

      userTokens
    }

    outUserTokens = evaluateTokens(outUserTokens, neutronsCost)
    outUserTokens = evaluateTokens(outUserTokens, protonsCost)

    outUserTokens
  }

}

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

    val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
    val protonsId: ErgoId = inGluonWBox.Protons.getId

    val isNeutronsExist: Boolean =
      userBox.tokens.exists(_.getId.equals(neutronsId))
    val isProtonsExist: Boolean =
      userBox.tokens.exists(_.getId.equals(protonsId))

    var outUserTokens: Seq[ErgoToken] = userBox.tokens
    if (isNeutronsExist) {
      outUserTokens = outUserTokens.map { token =>
        if (token.getId.equals(neutronsId)) {
          ErgoToken(neutronsId, token.getValue + neutronsGained)
        } else {
          token
        }
      }
    } else {
      outUserTokens = outUserTokens :+ ErgoToken(neutronsId, neutronsGained)
    }

    if (isProtonsExist) {
      outUserTokens = outUserTokens.map { token =>
        if (token.getId.equals(protonsId)) {
          ErgoToken(protonsId, token.getValue + protonsGained)
        } else {
          token
        }
      }
    } else {
      outUserTokens = outUserTokens :+ ErgoToken(protonsId, protonsGained)
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

    val outUserTokens: Seq[ErgoToken] = userBox.tokens.flatMap { token =>
      def calculateGluonTokens(
        token: ErgoToken,
        cost: Long
      ): Option[ErgoToken] = {
        val totalTokenAmount: Long = token.getValue - cost
        if (totalTokenAmount > 0) {
          Option(ErgoToken(token.getId, totalTokenAmount))
        } else None
      }

      val neutronsId: ErgoId = inGluonWBox.Neutrons.getId
      val protonsId: ErgoId = inGluonWBox.Protons.getId
      if (token.getId.equals(neutronsId)) {
        calculateGluonTokens(token, neutronsCost)
      } else if (token.getId.equals(protonsId)) {
        calculateGluonTokens(token, protonsCost)
      } else {
        Option(token)
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
    val protonsCost: Long =
      outGluonWBox.Protons.getValue - inGluonWBox.Protons.getValue
    val ergsCost: Long = outGluonWBox.value - inGluonWBox.value

    val finalCalculatedGluonTokens: Seq[ErgoToken] = outputGluonTokens(
      userBox.tokens,
      (inGluonWBox.Neutrons.getId, neutronsCost),
      (inGluonWBox.Protons.getId, protonsCost)
    )

    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value - ergsCost - ErgCommons.MinMinerFee,
      tokens = finalCalculatedGluonTokens
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

    val neutronsCost: Long =
      outGluonWBox.Neutrons.getValue - inGluonWBox.Neutrons.getValue
    val protonsCost: Long =
      outGluonWBox.Protons.getValue - inGluonWBox.Protons.getValue
    val ergsCost: Long = outGluonWBox.value - inGluonWBox.value

    val finalCalculatedGluonTokens: Seq[ErgoToken] = outputGluonTokens(
      userBox.tokens,
      (inGluonWBox.Neutrons.getId, neutronsCost),
      (inGluonWBox.Protons.getId, protonsCost)
    )

    val outUserBox: FundsToAddressBox = userBox.copy(
      value = userBox.value - ergsCost - ErgCommons.MinMinerFee,
      tokens = finalCalculatedGluonTokens
    )

    Seq(outGluonWBox, outUserBox)
  }
}
