package gluonw.common

import edge.commons.ErgCommons
import commons.node.Client
import edge.pay.ErgoPayResponse
import gluonw.boxes.{GluonWBox, OracleBox}
import gluonw.txs.{BetaDecayMinusTx, BetaDecayPlusTx, FissionTx, FusionTx}
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import edge.txs.{TTx, Tx}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import javax.inject.Inject
import scala.jdk.CollectionConverters.SeqHasAsJava

trait TGluonW {

  /**
    * Fission
    * Gets the Fission Tx
    * @param ergAmount Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def fission(ergAmount: Long, walletAddress: Address): Seq[TTx]

  /**
    * Fission Price
    * Gets the rate for the Fission Tx
    * @param ergAmount Amount of Erg to be transacted
    * @return
    */
  def fissionPrice(ergAmount: Long): Seq[AssetPrice]

  /**
    * Fusion
    * Gets the Fusion Tx
    * @param ergAmount Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def fusion(ergAmount: Long, walletAddress: Address): Seq[TTx]

  /**
    * Fusion Price
    * Gets the rate for the Fusion Tx
    * @param ergAmount Amount of Erg to be transacted
    * @return
    */
  def fusionPrice(ergAmount: Long): Seq[AssetPrice]

  /**
    * Transmute Neutrons to Protons
    * Beta Decay Plus Tx
    * @param neutronsAmount Amount of Neutrons to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def transmuteNeutronsToProtons(
    neutronsAmount: Long,
    walletAddress: Address
  ): Seq[TTx]

  /**
    * Transmute Neutrons to Protons Price
    * Beta Decay Plus Tx
    * @param neutronsAmount Amount of Neutrons to be transacted
    * @return
    */
  def transmuteNeutronsToProtonsPrice(neutronsAmount: Long): Seq[AssetPrice]

  /**
    * Transmute Protons to Protons
    * Beta Decay Plus Tx
    * @param protonsAmount Amount of Neutrons to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def transmuteProtonsToNeutrons(
    protonsAmount: Long,
    walletAddress: Address
  ): Seq[TTx]

  /**
    * Transmute Protons to Neutrons Price
    * Beta Decay Plus Tx
    * @param protonsAmount Amount of Protons to be transacted
    * @return
    */
  def transmuteProtonsToNeutronsPrice(protonsAmount: Long): Seq[AssetPrice]

  /**
    * Redeem Neutrons to Erg
    * @param neutronsAmount Amount of Neutrons to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def redeemNeutrons(neutronsAmount: Long, walletAddress: Address): Seq[TTx]

  /**
    * Redeem Neutrons to Erg rate
    * @param neutronsAmount Amount of Neutrons to be redeemed
    * @return
    */
  def redeemNeutronsPrice(neutronsAmount: Long): Seq[AssetPrice]

  /**
    * Redeem Protons to Erg rate
    * @param protonsAmount Amount of Protons to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def redeemProtons(protonsAmount: Long, walletAddress: Address): Seq[TTx]

  /**
    * Redeem Protons to Erg rate
    * @param protonsAmount Amount of Protons to be redeemed
    * @return
    */
  def redeemProtonsPrice(protonsAmount: Long): Seq[AssetPrice]

  /**
    * Mint Neutrons with Erg
    * @param ergAmount Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def mintNeutrons(ergAmount: Long, walletAddress: Address): Seq[TTx]

  /**
    * Mint Neutrons with Erg rate
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  def mintNeutronsPrice(ergAmount: Long): Seq[AssetPrice]

  /**
    * Mint Protons with Erg
    * @param ergAmount Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  def mintProtons(ergAmount: Long, walletAddress: Address): Seq[TTx]

  /**
    * Mint Protons with Erg rate
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  def mintProtonsPrice(ergAmount: Long): Seq[AssetPrice]
}

trait TxConverter {

  def convert(
    txs: Seq[TTx],
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
  val gluonWConstants: GluonWConstants = GluonWConstants()
  val algorithm: TGluonWAlgorithm = GluonWAlgorithm(gluonWConstants)

  def getPriceFromAlgorithm(
    assetAmount: Long,
    algorithmFunc: (GluonWBox, Long) => (
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
    algorithmFunc: (GluonWBox, Long) => (
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
    algorithmFunc: (GluonWBox, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): (GluonWBox, Seq[AssetPrice]) =
    // 2. Use Algorithm to calculate rate
    algorithmFunc(gluonWBox, assetAmount)

  def getPriceFromAlgorithmWithOracleBox(
    assetAmount: Long,
    currentHeight: Long,
    algorithmFunc: (GluonWBox, OracleBox, Long, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): Seq[AssetPrice] = {
    // 1. Get the Latest GluonWBox
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

    getPriceAndGluonWBoxFromAlgorithmWithGluonWBoxAndOracleBox(
      assetAmount,
      gluonWBox,
      currentHeight,
      algorithmFunc
    )._2
  }

  def getPriceAndGluonWBoxFromAlgorithmWithOracleBox(
    assetAmount: Long,
    currentHeight: Long,
    algorithmFunc: (GluonWBox, OracleBox, Long, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): (GluonWBox, Seq[AssetPrice]) = {
    // 1. Get the Latest GluonWBox
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

    getPriceAndGluonWBoxFromAlgorithmWithGluonWBoxAndOracleBox(
      assetAmount,
      gluonWBox,
      currentHeight,
      algorithmFunc
    )
  }

  def getPriceAndGluonWBoxFromAlgorithmWithGluonWBoxAndOracleBox(
    assetAmount: Long,
    gluonWBox: GluonWBox,
    currentHeight: Long,
    algorithmFunc: (GluonWBox, OracleBox, Long, Long) => (
      GluonWBox,
      Seq[AssetPrice]
    )
  ): (GluonWBox, Seq[AssetPrice]) = {
    // 1. Get the Oracle Box
    val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox

    // 2. Use Algorithm to calculate rate
    algorithmFunc(gluonWBox, neutronOracleBox, assetAmount, currentHeight)
  }

  /**
    * Fission
    * Gets the Fission Tx
    *
    * @param ergAmount     Amount of Erg to be transacted
    * @param walletAddress Wallet Address of the user
    * @return Tx that will give the user the amount of Neutrons and
    *         Protons they deserve
    */
  override def fission(ergAmount: Long, walletAddress: Address): Seq[TTx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: java.util.List[InputBox] =
        client.getCoveringBoxesFor(walletAddress, ergAmount).getBoxes

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox

      val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)

      // 4. Create FissionTx
      val fissionTx: FissionTx = FissionTx(
        ergToExchange = ergAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(neutronOracleBox.box.get.input)
      )(ctx, algorithm, gluonWFeesCalculator)

      Seq(fissionTx)
    }

  /**
    * Fission Price
    * Gets the rate for the Fission Tx
    *
    * @param ergAmount Amount of Erg to be transacted
    * @return Neutrons AssetPrice and Protons AssetPrice
    */
  override def fissionPrice(ergAmount: Long): Seq[AssetPrice] =
    // Use Algorithm to calculate Fission rate
    getPriceFromAlgorithm(ergAmount, algorithm.fissionPrice)

  override def fusion(ergAmount: Long, walletAddress: Address): Seq[TTx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: java.util.List[InputBox] =
        client.getCoveringBoxesFor(walletAddress, ergAmount).getBoxes

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox

      val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)

      // 4. Create FissionTx
      val fusionTx: FusionTx = FusionTx(
        ergToRetrieve = ergAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(neutronOracleBox.box.get.input)
      )(ctx, algorithm, gluonWFeesCalculator)

      Seq(fusionTx)
    }

  override def fusionPrice(ergAmount: Long): Seq[AssetPrice] =
    // Use Algorithm to calculate Fission rate
    getPriceFromAlgorithm(ergAmount, algorithm.fusionPrice)

  /**
    * Transmute Neutrons to Protons
    * Beta Decay Minus Tx
    *
    * @param neutronsAmount    Amount of Neutrons to be transacted
    * @param walletAddress Wallet Address of the user
    * @return Tx that will return the amount of Protons the user deserves
    *         back to the user
    */
  override def transmuteNeutronsToProtons(
    neutronsAmount: Long,
    walletAddress: Address
  ): Seq[TTx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: List[InputBox] =
        client.getCoveringBoxesFor(
          walletAddress,
          amount = ErgCommons.MinMinerFee,
          tokensToSpend = Seq(
            GluonWTokens.get(GluonWAsset.NEUTRON.toString, neutronsAmount)
          ).asJava
        )

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
      val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)

      // 4. Create BetaDecayPlusTx
      val betaDecayMinusTx: BetaDecayMinusTx = BetaDecayMinusTx(
        neutronsToTransmute = neutronsAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(neutronOracleBox.box.get.input)
      )(ctx, algorithm, gluonWFeesCalculator, ctx.getHeight)

      Seq(betaDecayMinusTx)
    }

  /**
    * Transmute Neutrons to Protons Price
    * Beta Decay Plus Tx
    *
    * @param neutronsAmount Amount of Neutrons to be transacted
    * @return AssetPrice of Protons
    */
  override def transmuteNeutronsToProtonsPrice(
    neutronsAmount: Long
  ): Seq[AssetPrice] =
    // Use Algorithm to calculate BetaDecayPlus rate
    getPriceFromAlgorithmWithOracleBox(
      neutronsAmount,
      currentHeight = client.getHeight,
      algorithm.betaDecayPlusPrice
    )

  /**
    * Transmute Protons to Protons
    * Beta Decay Plus Tx
    *
    * @param protonsAmount     Amount of Neutrons to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def transmuteProtonsToNeutrons(
    protonsAmount: Long,
    walletAddress: Address
  ): Seq[TTx] =
    client.getClient.execute { (ctx: BlockchainContext) =>
      // 1. Get the box from the user
      val userBoxes: List[InputBox] =
        client.getCoveringBoxesFor(
          walletAddress,
          amount = ErgCommons.MinMinerFee,
          tokensToSpend = Seq(
            GluonWTokens.get(GluonWAsset.PROTON.toString, protonsAmount)
          ).asJava
        )

      // 2. Get the Latest GluonWBox
      val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox

      // 3. Get the Oracle Box
      val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
      val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)

      // 4. Create BetaDecayMinusTx
      val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
        protonsToTransmute = protonsAmount,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ userBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(neutronOracleBox.box.get.input)
      )(ctx, algorithm, gluonWFeesCalculator, ctx.getHeight)

      Seq(betaDecayPlusTx)
    }

  /**
    * Transmute Protons to Neutrons Price
    * Beta Decay Minus Tx
    *
    * @param protonsAmount Amount of Protons to be transacted
    * @return
    */
  override def transmuteProtonsToNeutronsPrice(
    protonsAmount: Long
  ): Seq[AssetPrice] =
    // Use Algorithm to calculate BetaDecayMinus rate
    getPriceFromAlgorithmWithOracleBox(
      protonsAmount,
      currentHeight = client.getHeight,
      algorithm.betaDecayMinusPrice
    )

  /**
    * Redeem Neutrons to Erg
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * Redeeming Erg with purely neutron requires decaying some of the neutron
    * to proton and then consecutively carrying out a fusion tx
    * BetaDecay- -> Fusion
    * @param neutronsAmount    Amount of Neutrons to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def redeemNeutrons(
    neutronsAmount: Long,
    walletAddress: Address
  ): Seq[TTx] =
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Calculate amount required for fusion
    // 5. Create BetaDecayMinusTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create FusionTx
    ???

  /**
    * Redeem Neutrons to Erg rate
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * BetaDecay- -> Fusion
    * @param neutronsAmount Amount of Neutrons to be redeemed
    * @return
    */
  override def redeemNeutronsPrice(neutronsAmount: Long): Seq[AssetPrice] = {
    // Calculate amount required for BetaDecayMinusTx and fusion
    // This has to be done in reverse, for example,
    // 1. Calculate how much of equilibrium to get the value
    val betaDecayMinusPriceAndGluonWBox: (GluonWBox, Seq[AssetPrice]) =
      getPriceAndGluonWBoxFromAlgorithmWithOracleBox(
        neutronsAmount,
        currentHeight = client.getHeight,
        algorithm.betaDecayMinusPrice
      )
    ???
  }

  /**
    * Redeem Protons to Erg
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * Redeeming Erg with purely proton requires decaying some of the proton
    * to neutron and then consecutively carrying out a fusion tx
    * BetaDecay+ -> Fusion
    * @param protonsAmount     Amount of Protons to be redeemed
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def redeemProtons(
    protonsAmount: Long,
    walletAddress: Address
  ): Seq[TTx] =
    // 1. Get the box from the user
    // 2. Get the Latest GluonWBox
    // 3. Get the Oracle Box
    // 4. Calculate amount required for BetaDecay+ and fusion
    // 5. Create BetaDecayPlusTx
    // 6. Get GluonWBox and UserBox from Tx
    // 7. Create FusionTx
    ???

  /**
    * Redeem Protons to Erg rate
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * @param protonsAmount Amount of Protons to be redeemed
    * @return
    */
  override def redeemProtonsPrice(protonsAmount: Long): Seq[AssetPrice] =
    // 1. Get the Latest GluonWBox
    // 2. Get the Oracle Box
    // 3. Calculate amount required for BetaDecayPlusTx and fusion
    ???

  /**
    * Mint Neutrons with Erg
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * Minting pure Neutrons with Erg requires a fission tx to retrieve both
    * Neutrons and Protons, and then converting the Protons into Neutrons
    * Fission -> BetaDecay-
    * @param ergAmount     Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def mintNeutrons(ergAmount: Long, walletAddress: Address): Seq[TTx] =
  {
    // 1. Create FissionTx
    val fissionTx = fission(ergAmount, walletAddress)
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox
    // 2. Get GluonWBox and UserBox from Tx
    // 3. Create BetaDecay-Tx with Protons retrieved
    client.getClient.execute { (ctx: BlockchainContext) =>
      val fissionTxAsInputBoxes = fissionTx.head.buildTx.getInputs

      // 2. Get the Latest GluonWBox
      val outputGluonWBox: GluonWBox = GluonWBox.from(
        fissionTxAsInputBoxes.get(0))

      // 3. Get the Oracle Box
      val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
      val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)

      val protonsTransmuted: Long = gluonWBox.Protons.value - outputGluonWBox.Protons.value

      // 4. Create BetaDecayPlusTx
      val betaDecayPlusTx: BetaDecayPlusTx = BetaDecayPlusTx(
        protonsToTransmute = protonsTransmuted,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ fissionTxAsInputBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(neutronOracleBox.box.get.input)
      )(ctx, algorithm, gluonWFeesCalculator, ctx.getHeight)

      fissionTx ++ Seq(betaDecayPlusTx)
    }
  }

  /**
    * Mint Neutrons with Erg rate
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * Fission -> BetaDecay-
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintNeutronsPrice(ergAmount: Long): Seq[AssetPrice] =
  {
    // 1. Get the Latest GluonWBox
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox
    // 2. Get the Oracle Box
    val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
    // 3. Calculate amount of Protons received
    val fissionTxGluonWBox: GluonWBox = algorithm.fission(gluonWBox, ergAmount)
    val protonsTransmuted: Long = gluonWBox.Protons.value - fissionTxGluonWBox.Protons.value

    // BetaDecay-Tx
    val betaDecayPlusTxGluonWBox: GluonWBox = algorithm.betaDecayPlus(
      fissionTxGluonWBox,
      protonsToTransmute = protonsTransmuted)(neutronOracleBox, client.getHeight)

    Seq(AssetPrice(
      name = GluonWAsset.NEUTRON.toString,
      gluonWBox.Neutrons.getValue - betaDecayPlusTxGluonWBox.Neutrons.getValue,
      GluonWTokens.neutronId
    ))
  }

  /**
    * Mint Protons with Erg
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * Fission -> BetaDecay+
    * @param ergAmount     Amount of Ergs to be transacted
    * @param walletAddress Wallet Address of the user
    * @return
    */
  override def mintProtons(
    ergAmount: Long,
    walletAddress: Address
  ): Seq[TTx] =
  {
    // 1. Create FissionTx
    val fissionTx = fission(ergAmount, walletAddress)
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox
    // 2. Get GluonWBox and UserBox from Tx
    // 3. Create BetaDecay-Tx with Protons retrieved
    client.getClient.execute { (ctx: BlockchainContext) =>
      val fissionTxAsInputBoxes = fissionTx.head.buildTx.getInputs

      // 2. Get the Latest GluonWBox
      val outputGluonWBox: GluonWBox = GluonWBox.from(
        fissionTxAsInputBoxes.get(0))

      // 3. Get the Oracle Box
      val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
      val gluonWFeesCalculator: GluonWFeesCalculator =
        GluonWFeesCalculator()(gluonWBox, gluonWConstants)

      val neutronsTransmuted: Long = gluonWBox.Neutrons.value - outputGluonWBox.Neutrons.value

      // 4. Create BetaDecayMinusTx
      val betaDecayMinusTx: BetaDecayMinusTx = BetaDecayMinusTx(
        neutronsToTransmute = neutronsTransmuted,
        inputBoxes = Seq(gluonWBox.box.get.input) ++ fissionTxAsInputBoxes.toSeq,
        changeAddress = walletAddress,
        dataInputs = Seq(neutronOracleBox.box.get.input)
      )(ctx, algorithm, gluonWFeesCalculator, ctx.getHeight)

      fissionTx ++ Seq(betaDecayMinusTx)
    }
  }

  /**
    * Mint Protons with Erg rate
    * @todo v2: Implement mint and redeem protons and neutrons directly with Erg
    *
    * Minting pure Protons with Erg requires a fission tx to retrieve both
    * Protons and Neutrons, and then converting the Neutrons into Protons
    * Fission -> BetaDecay+
    * @param ergAmount Amount of Ergs to be transacted
    * @return
    */
  override def mintProtonsPrice(ergAmount: Long): Seq[AssetPrice] =
  {
    // 1. Get the Latest GluonWBox
    val gluonWBox: GluonWBox = gluonWBoxExplorer.getGluonWBox
    // 2. Get the Oracle Box
    val neutronOracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
    // 3. Calculate amount of Neutrons received
    val fissionTxGluonWBox: GluonWBox = algorithm.fission(gluonWBox, ergAmount)
    val neutronsTransmuted: Long = gluonWBox.Neutrons.value - fissionTxGluonWBox.Neutrons.value

    // BetaDecay-Tx
    val betaDecayPlusTxGluonWBox: GluonWBox = algorithm.betaDecayMinus(
      fissionTxGluonWBox,
      neutronsToTransmute = neutronsTransmuted)(neutronOracleBox, client.getHeight)

    Seq(AssetPrice(
      name = GluonWAsset.PROTON.toString,
      gluonWBox.Protons.getValue - betaDecayPlusTxGluonWBox.Protons.getValue,
      GluonWTokens.protonId
    ))
  }
}
