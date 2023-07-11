package gluonw.common

import commons.ErgCommons
import commons.node.Client
import edge.pay.ErgoPayResponse
import gluonw.boxes.{GluonWBox, GoldOracleBox}
import gluonw.txs.{BetaDecayPlusTx, FissionTx}
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import txs.Tx

import javax.inject.Inject
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
import scala.jdk.CollectionConverters.seqAsJavaListConverter

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
    * Fission Price
    * Gets the rate for the Fission Tx
    * @param ergAmount Amount of Erg to be transacted
    * @return
    */
  def fissionPrice(ergAmount: Long): Seq[AssetPrice]

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
    * Transmute SigGold to SigGoldRsv Price
    * Beta Decay Plus Tx
    * @param goldAmount Amount of SigGold to be transacted
    * @return
    */
  def transmuteSigGoldToSigGoldRsvPrice(goldAmount: Long): Seq[AssetPrice]

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
    * Transmute SigGoldRsv to SigGold Price
    * Beta Decay Plus Tx
    * @param rsvAmount Amount of SigGoldRsv to be transacted
    * @return
    */
  def transmuteSigGoldRsvToSigGoldPrice(rsvAmount: Long): Seq[AssetPrice]

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
  def redeemSigGoldPrice(goldAmount: Long): Seq[AssetPrice]

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
  def redeemSigGoldRsvPrice(rsvAmount: Long): Seq[AssetPrice]

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
  def mintSigGoldPrice(ergAmount: Long): Seq[AssetPrice]

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
  def mintSigGoldRsvPrice(ergAmount: Long): Seq[AssetPrice]
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
  val algorithm: TGluonWAlgorithm = GluonWAlgorithm

  def getPriceFromAlgorithm(
    assetAmount: Long,
    algorithmFunc: (GluonWBox, GoldOracleBox, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): Seq[AssetPrice] = {
    // 1. Get the Latest GluonWBox
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

    getPriceAndGluonWBoxFromAlgorithmWithGluonWBox(
      assetAmount,
      gluonWBox,
      algorithmFunc
    )._2
  }

  def getPriceAndGluonWBoxFromAlgorithm(
    assetAmount: Long,
    algorithmFunc: (GluonWBox, GoldOracleBox, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): (GluonWBox, Seq[AssetPrice]) = {
    // 1. Get the Latest GluonWBox
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

    getPriceAndGluonWBoxFromAlgorithmWithGluonWBox(
      assetAmount,
      gluonWBox,
      algorithmFunc
    )
  }

  def getPriceAndGluonWBoxFromAlgorithmWithGluonWBox(
    assetAmount: Long,
    gluonWBox: GluonWBox,
    algorithmFunc: (GluonWBox, GoldOracleBox, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): (GluonWBox, Seq[AssetPrice]) = {
    // 1. Get the Oracle Box
    val goldOracleBox: GoldOracleBox = gluonWBoxExplorer.getGoldOracleBox

    // 2. Use Algorithm to calculate rate
    algorithmFunc(gluonWBox, goldOracleBox, assetAmount)
  }

  /**
    * Fission
    * Gets the Fission Tx
    *
    * @param ergAmount     Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return Tx that will give the user the amount of SigGold and
    *         SigGoldRsv they deserve
    */
  override def fission(ergAmount: Long, walletAddress: Address): Seq[Tx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: java.util.List[InputBox] =
        client.getCoveringBoxesFor(walletAddress, ergAmount).getBoxes

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val goldOracleBox: GoldOracleBox = gluonWBoxExplorer.getGoldOracleBox

      // 4. Create FissionTx
      val fissionTx: FissionTx = FissionTx(
        ergToExchange = ergAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(goldOracleBox.box.get.input)
      )(ctx, algorithm)

      Seq(fissionTx)
    }

  /**
    * Fission Price
    * Gets the rate for the Fission Tx
    *
    * @param ergAmount Amount of Erg to be transacted
    * @return SigGold AssetPrice and SigGoldRsv AssetPrice
    */
  override def fissionPrice(ergAmount: Long): Seq[AssetPrice] =
    // Use Algorithm to calculate Fission rate
    getPriceFromAlgorithm(ergAmount, algorithm.calculateFissionPrice)

  /**
    * Transmute SigGold to SigGoldRsv
    * Beta Decay Plus Tx
    *
    * @param goldAmount    Amount of SigGold to be transacted
    * @param walletAddress Wallet Address of the user
    * @return Tx that will return the amount of SigGoldRsv the user deserves
    *         back to the user
    */
  override def transmuteSigGoldToSigGoldRsv(
    goldAmount: Long,
    walletAddress: Address
  ): Seq[Tx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: List[InputBox] =
        client.getCoveringBoxesFor(
          walletAddress,
          amount = ErgCommons.MinMinerFee,
          tokensToSpend = Seq(
            GluonWTokens.get(GluonWAsset.SIGGOLD.toString, goldAmount)
          ).asJava
        )

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val goldOracleBox: GoldOracleBox = gluonWBoxExplorer.getGoldOracleBox

      // 4. Create BetaDecayPlusTx
      val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
        goldToTransmute = goldAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(goldOracleBox.box.get.input)
      )(ctx, algorithm)

      Seq(betaDecayPlusTx)
    }

  /**
    * Transmute SigGold to SigGoldRsv Price
    * Beta Decay Plus Tx
    *
    * @param goldAmount Amount of SigGold to be transacted
    * @return AssetPrice of SigGoldRsv
    */
  override def transmuteSigGoldToSigGoldRsvPrice(
    goldAmount: Long
  ): Seq[AssetPrice] =
    // Use Algorithm to calculate BetaDecayPlus rate
    getPriceFromAlgorithm(goldAmount, algorithm.calculateBetaDecayPlusPrice)

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
  ): Seq[Tx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: List[InputBox] =
        client.getCoveringBoxesFor(
          walletAddress,
          amount = ErgCommons.MinMinerFee,
          tokensToSpend = Seq(
            GluonWTokens.get(GluonWAsset.SIGGOLDRSV.toString, rsvAmount)
          ).asJava
        )

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val goldOracleBox: GoldOracleBox = gluonWBoxExplorer.getGoldOracleBox

      // 4. Create BetaDecayMinusTx
      val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
        goldToTransmute = rsvAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(goldOracleBox.box.get.input)
      )(ctx, algorithm)

      Seq(betaDecayPlusTx)
    }

  /**
    * Transmute SigGoldRsv to SigGold Price
    * Beta Decay Minus Tx
    *
    * @param rsvAmount Amount of SigGoldRsv to be transacted
    * @return
    */
  override def transmuteSigGoldRsvToSigGoldPrice(
    rsvAmount: Long
  ): Seq[AssetPrice] =
    // Use Algorithm to calculate BetaDecayMinus rate
    getPriceFromAlgorithm(rsvAmount, algorithm.calculateBetaDecayMinusPrice)

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
  ): Seq[Tx] =
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Calculate amount required for fusion
    // 5. Create BetaDecayMinusTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create FusionTx
    ???

  /**
    * Redeem SigGold to Erg rate
    *
    * BetaDecay- -> Fusion
    * @param goldAmount Amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGoldPrice(goldAmount: Long): Seq[AssetPrice] = {
    // Calculate amount required for BetaDecayMinusTx and fusion
    // This has to be done in reverse, for example,
    // 1. Calculate how much of equilibrium to get the value
    val betaDecayMinusPriceAndGluonWBox: (GluonWBox, Seq[AssetPrice]) =
      getPriceAndGluonWBoxFromAlgorithm(
        goldAmount,
        algorithm.calculateBetaDecayMinusPrice
      )
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
  ): Seq[Tx] =
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Calculate amount required for BetaDecay+ and fusion
    // 5. Create BetaDecayPlusTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create FusionTx
    ???

  /**
    * Redeem SigGoldRsv to Erg rate
    *
    * @param rsvAmount Amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsvPrice(rsvAmount: Long): Seq[AssetPrice] =
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount required for BetaDecayPlusTx and fusion
    ???

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
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 5. Create FissionTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create BetaDecay-Tx with SigGoldRsv retrieved
    ???

  /**
    * Mint SigGold with Erg rate
    *
    * Fission -> BetaDecay-
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintSigGoldPrice(ergAmount: Long): Seq[AssetPrice] =
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount of SigGold received
    // with fissionTx and BetaDecay-Tx
    ???

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
  ): Seq[Tx] =
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 5. Create FissionTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create BetaDecay+Tx with SigGoldRsv retrieved
    ???

  /**
    * Mint SigGoldRsv with Erg rate
    *
    * Minting pure SigGoldRsv with Erg requires a fission tx to retrieve both
    * SigGoldRsv and SigGold, and then converting the SigGold into SigGoldRsv
    * Fission -> BetaDecay+
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintSigGoldRsvPrice(ergAmount: Long): Seq[AssetPrice] =
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount of SigGold received
    // with fissionTx and BetaDecay+Tx
    ???
}
