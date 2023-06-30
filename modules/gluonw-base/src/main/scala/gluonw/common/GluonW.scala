package gluonw.common

import commons.node.Client
import edge.pay
import edge.pay.ErgoPayResponse
import io.circe.Json
import org.ergoplatform.appkit.Address
import txs.Tx

import javax.inject.Inject

trait AssetRate {
  val name: String
  val rate: Long

  def toJson: Json =
    Json.fromFields(
      List(
        ("assetName", Json.fromString(name)),
        ("rate", Json.fromLong(rate))
      )
    )
}

trait TGluonW {

  /**
    * Fission
    * Gets the Fission Tx
    * @param ergAmount Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def fission(ergAmount: Long, walletAddress: Address): Seq[Tx]

  /**
    * Fission Rate
    * Gets the rate for the Fission Tx
    * @param ergAmount Amount of Erg to be transacted
    * @return
    */
  def fissionRate(ergAmount: Long): AssetRate

  /**
    * Transmute SigGold to SigGoldRsv
    * Beta Decay Plus Tx
    * @param goldAmount Amount of SigGold to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def transmuteSigGoldToSigGoldRsv(
    goldAmount: Long,
    walletAddress: Address
  ): Seq[Tx]

  /**
    * Transmute SigGold to SigGoldRsv Rate
    * Beta Decay Plus Tx
    * @param goldAmount Amount of SigGold to be transacted
    * @return
    */
  def transmuteSigGoldToSigGoldRsvRate(goldAmount: Long): AssetRate

  /**
    * Transmute SigGoldRsv to SigGoldRsv
    * Beta Decay Plus Tx
    * @param rsvAmount Amount of SigGold to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def transmuteSigGoldRsvToSigGold(
    rsvAmount: Long,
    walletAddress: Address
  ): Seq[Tx]

  /**
    * Transmute SigGoldRsv to SigGold Rate
    * Beta Decay Plus Tx
    * @param rsvAmount Amount of SigGoldRsv to be transacted
    * @return
    */
  def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): AssetRate

  /**
    * Redeem SigGold to Erg
    * @param goldAmount Amount of SigGold to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def redeemSigGold(goldAmount: Long, walletAddress: Address): Seq[Tx]

  /**
    * Redeem SigGold to Erg rate
    * @param goldAmount Amount of SigGold to be redeemed
    * @return
    */
  def redeemSigGoldRate(goldAmount: Long): AssetRate

  /**
    * Redeem SigGoldRsv to Erg rate
    * @param rsvAmount Amount of SigGoldRsv to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def redeemSigGoldRsv(rsvAmount: Long, walletAddress: Address): Seq[Tx]

  /**
    * Redeem SigGoldRsv to Erg rate
    * @param rsvAmount Amount of SigGoldRsv to be redeemed
    * @return
    */
  def redeemSigGoldRsvRate(rsvAmount: Long): AssetRate

  /**
    * Mint SigGold with Erg
    * @param ergAmount Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def mintSigGold(ergAmount: Long, walletAddress: Address): Seq[Tx]

  /**
    * Mint SigGold with Erg rate
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  def mintSigGoldRate(ergAmount: Long): AssetRate

  /**
    * Mint SigGoldRsv with Erg
    * @param ergAmount Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def mintSigGoldRsv(ergAmount: Long, walletAddress: Address): Seq[Tx]

  /**
    * Mint SigGoldRsv with Erg rate
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  def mintSigGoldRsvRate(ergAmount: Long): AssetRate
}

trait TxConverter {

  def convert(
    txs: Seq[Tx],
    address: Address,
    message: String = ""
  ): Seq[ErgoPayResponse] =
    txs.zipWithIndex.map((indexedTx) =>
      ErgoPayResponse.getResponse(
        reducedTx = indexedTx._1.reduceTx,
        message = s"${indexedTx._2} ${message}",
        recipient = address
      )
    )
}

class GluonW @Inject() (
  client: Client,
  gluonWBoxExplorer: GluonWBoxExplorer
) extends TGluonW {

  /**
    * Fission
    * Gets the Fission Tx
    *
    * @param ergAmount     Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def fission(ergAmount: Long, walletAddress: Address): Seq[Tx] = ???

  /**
    * Fission Rate
    * Gets the rate for the Fission Tx
    *
    * @param ergAmount Amount of Erg to be transacted
    * @return
    */
  override def fissionRate(ergAmount: Long): AssetRate = ???

  /**
    * Transmute SigGold to SigGoldRsv
    * Beta Decay Plus Tx
    *
    * @param goldAmount    Amount of SigGold to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def transmuteSigGoldToSigGoldRsv(
    goldAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = ???

  /**
    * Transmute SigGold to SigGoldRsv Rate
    * Beta Decay Plus Tx
    *
    * @param goldAmount Amount of SigGold to be transacted
    * @return
    */
  override def transmuteSigGoldToSigGoldRsvRate(goldAmount: Long): AssetRate =
    ???

  /**
    * Transmute SigGoldRsv to SigGoldRsv
    * Beta Decay Plus Tx
    *
    * @param rsvAmount     Amount of SigGold to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def transmuteSigGoldRsvToSigGold(
    rsvAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = ???

  /**
    * Transmute SigGoldRsv to SigGold Rate
    * Beta Decay Plus Tx
    *
    * @param rsvAmount Amount of SigGoldRsv to be transacted
    * @return
    */
  override def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): AssetRate =
    ???

  /**
    * Redeem SigGold to Erg
    *
    * @param goldAmount    Amount of SigGold to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def redeemSigGold(
    goldAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = ???

  /**
    * Redeem SigGold to Erg rate
    *
    * @param goldAmount Amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGoldRate(goldAmount: Long): AssetRate = ???

  /**
    * Redeem SigGoldRsv to Erg rate
    *
    * @param rsvAmount     Amount of SigGoldRsv to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def redeemSigGoldRsv(
    rsvAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = ???

  /**
    * Redeem SigGoldRsv to Erg rate
    *
    * @param rsvAmount Amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsvRate(rsvAmount: Long): AssetRate = ???

  /**
    * Mint SigGold with Erg
    *
    * @param ergAmount     Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def mintSigGold(ergAmount: Long, walletAddress: Address): Seq[Tx] =
    ???

  /**
    * Mint SigGold with Erg rate
    *
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintSigGoldRate(ergAmount: Long): AssetRate = ???

  /**
    * Mint SigGoldRsv with Erg
    *
    * @param ergAmount     Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def mintSigGoldRsv(
    ergAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = ???

  /**
    * Mint SigGoldRsv with Erg rate
    *
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintSigGoldRsvRate(ergAmount: Long): AssetRate = ???
}
