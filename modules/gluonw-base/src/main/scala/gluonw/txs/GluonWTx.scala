package gluonw.txs

import boxes.{BoxWrapper, FundsToAddressBox}
import gluonw.boxes.GluonWBox
import gluonw.common.{GluonWAlgorithm, TGluonWAlgorithm}
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
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
    val gluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      FundsToAddressBox.from(inputBoxes.tail.head)

    val outGluonWBox: GluonWBox =
      algorithm.fission(gluonWBox, ergToExchange)
    val outUserBox: FundsToAddressBox = ???

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
    val gluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      FundsToAddressBox.from(inputBoxes.tail.head)

    val outGluonWBox: GluonWBox =
      GluonWAlgorithm.fusion(gluonWBox, ergToRetrieve)
    val outUserBox: FundsToAddressBox = ???

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
    val gluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      FundsToAddressBox.from(inputBoxes.tail.head)

    val outGluonWBox: GluonWBox =
      GluonWAlgorithm.betaDecayPlus(gluonWBox, goldToTransmute)
    val outUserBox: FundsToAddressBox = ???

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
    val gluonWBox: GluonWBox = GluonWBox.from(inputBoxes.head)
    val userBox: FundsToAddressBox =
      FundsToAddressBox.from(inputBoxes.tail.head)

    val outGluonWBox: GluonWBox =
      GluonWAlgorithm.betaDecayMinus(gluonWBox, goldToTransmute)
    val outUserBox: FundsToAddressBox = ???

    Seq(outGluonWBox, outUserBox)
  }
}
