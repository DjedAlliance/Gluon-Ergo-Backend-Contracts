package gluonw.common

import commons.node.Client
import edge.pay.ErgoPayResponse
import io.circe.Json
import org.ergoplatform.appkit.{Address, BlockchainContext}
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
  implicit val algorithm: TGluonWAlgorithm = GluonWAlgorithm

  /**
    * Fission
    * Gets the Fission Tx
    *
    * @param ergAmount     Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def fission(ergAmount: Long, walletAddress: Address): Seq[Tx] = {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Create FissionTx
    ???
  }

  /**
    * Fission Rate
    * Gets the rate for the Fission Tx
    *
    * @param ergAmount Amount of Erg to be transacted
    * @return
    */
  override def fissionRate(ergAmount: Long): AssetRate = {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Use Algorithm to calculate rate
    ???
  }

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
  ): Seq[Tx] = {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Create BetaDecayPlusTx
    ???
  }

  /**
    * Transmute SigGold to SigGoldRsv Rate
    * Beta Decay Plus Tx
    *
    * @param goldAmount Amount of SigGold to be transacted
    * @return
    */
  override def transmuteSigGoldToSigGoldRsvRate(goldAmount: Long): AssetRate = {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Use Algorithm to calculate BetaDecayPlus rate
    ???
  }


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
  ): Seq[Tx] = {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Create BetaDecayMinusTx
    ???
  }

  /**
    * Transmute SigGoldRsv to SigGold Rate
    * Beta Decay Plus Tx
    *
    * @param rsvAmount Amount of SigGoldRsv to be transacted
    * @return
    */
  override def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): AssetRate =
  {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Use Algorithm to calculate BetaDecayMinus rate
    ???
  }

  /**
    * Redeem SigGold to Erg
    *
    * Redeeming Erg with purely sigGold requires decaying some of the sigGold
    * to sigGoldRsv and then consecutively carrying out a fusion tx
    * BetaDecay- -> Fusion
    * @param goldAmount    Amount of SigGold to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def redeemSigGold(
    goldAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Calculate amount required for fusion
    // 5. Create BetaDecayMinusTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create FusionTx
    ???
  }

  /**
    * Redeem SigGold to Erg rate
    *
    * @param goldAmount Amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGoldRate(goldAmount: Long): AssetRate = {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount required for BetaDecayMinusTx and fusion
    ???
  }

  /**
    * Redeem SigGoldRsv to Erg
    *
    * Redeeming Erg with purely sigGoldRsv requires decaying some of the sigGoldRsv
    * to sigGold and then consecutively carrying out a fusion tx
    * BetaDecay+ -> Fusion
    * @param rsvAmount     Amount of SigGoldRsv to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def redeemSigGoldRsv(
    rsvAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Calculate amount required for BetaDecay+ and fusion
    // 5. Create BetaDecayPlusTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create FusionTx
    ???
  }

  /**
    * Redeem SigGoldRsv to Erg rate
    *
    * @param rsvAmount Amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsvRate(rsvAmount: Long): AssetRate = {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount required for BetaDecayPlusTx and fusion
    ???
  }

  /**
    * Mint SigGold with Erg
    *
    * Minting pure SigGold with Erg requires a fission tx to retrieve both
    * SigGold and SigGoldRsv, and then converting the SigGoldRsv into SigGold
    * Fission -> BetaDecay-
    * @param ergAmount     Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def mintSigGold(ergAmount: Long, walletAddress: Address): Seq[Tx] =
  {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 5. Create FissionTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create BetaDecay-Tx with SigGoldRsv retrieved
    ???
  }

  /**
    * Mint SigGold with Erg rate
    *
    * Fission -> BetaDecay-
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintSigGoldRate(ergAmount: Long): AssetRate = {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount of SigGold received
    // with fissionTx and BetaDecay-Tx
    ???
  }

  /**
    * Mint SigGoldRsv with Erg
    *
    * Fission -> BetaDecay+
    * @param ergAmount     Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def mintSigGoldRsv(
    ergAmount: Long,
    walletAddress: Address
  ): Seq[Tx] = {
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 5. Create FissionTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create BetaDecay+Tx with SigGoldRsv retrieved
    ???
  }

  /**
    * Mint SigGoldRsv with Erg rate
    *
    * Minting pure SigGoldRsv with Erg requires a fission tx to retrieve both
    * SigGoldRsv and SigGold, and then converting the SigGold into SigGoldRsv
    * Fission -> BetaDecay+
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintSigGoldRsvRate(ergAmount: Long): AssetRate = {
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount of SigGold received
    // with fissionTx and BetaDecay+Tx
    ???
  }
}
