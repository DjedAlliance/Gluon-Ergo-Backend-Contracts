package gluonw.txs

import boxes.BoxWrapper
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import txs.Tx

case class FusionTx() extends Tx {
  override val changeAddress: Address = ???
  override implicit val ctx: BlockchainContext = ???
  override val inputBoxes: Seq[InputBox] = ???

  override def defineOutBoxWrappers: Seq[BoxWrapper] = ???
}

case class FissionTx() extends Tx {
  override val changeAddress: Address = ???
  override implicit val ctx: BlockchainContext = ???
  override val inputBoxes: Seq[InputBox] = ???

  override def defineOutBoxWrappers: Seq[BoxWrapper] = ???
}

/**
  * Transmute Gold to Rsv
  */
case class BetaDecayPlusTx() extends Tx {
  override val changeAddress: Address = ???
  override implicit val ctx: BlockchainContext = ???
  override val inputBoxes: Seq[InputBox] = ???

  override def defineOutBoxWrappers: Seq[BoxWrapper] = ???
}

/**
  * Transmute Rsv to Gold
  */
case class BetaDecayMinusTx() extends Tx {
  override val changeAddress: Address = ???
  override implicit val ctx: BlockchainContext = ???
  override val inputBoxes: Seq[InputBox] = ???

  override def defineOutBoxWrappers: Seq[BoxWrapper] = ???
}
