package gluonw.tools

import commons.configs.ServiceConfig
import commons.node.{Client, TestClient}
import edge.boxes.{BoxWrapper, FundsToAddressBox}
import edge.node.BaseClient
import edge.registers.LongRegister
import edge.tools.BoxTools
import edge.tools.BoxTools.{getProver, mergeBox, mintTokens}
import edge.txs.Tx
import gluonw.common.{GluonWBoxExplorer, GluonWTokens}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoProver,
  InputBox,
  NetworkType,
  Parameters,
  SignedTransaction
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import org.ergoplatform.sdk.ErgoToken
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import scala.jdk.CollectionConverters.SeqHasAsJava

object BoxCreation extends App {

  val configFileName = "ergo_config.json"
  val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
  val nodeConf: ErgoNodeConfig = conf.getNode
  val client: BaseClient = new TestClient(nodeConf.getNetworkType)
//  val explorer: GluonWBoxExplorer = new GluonWBoxExplorer()(client)
  val reducedTxBytes: String = ""

  client.setClient()

  val tokens: Seq[(String, (String, Long))] = Seq(
    (
      "GluonW Test NFT",
      (
        "GluonW NFTby DJed Alliance v1.2: VarPhiBeta Implemented. This is a test Token.",
        1L
      )
    ),
    (
      "GluonW Test Neutrons",
      (
        "GluonW Neutrons by DJed Alliance v1.2: VarPhiBeta Implemented. This is a test Token.",
        GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY
      )
    ),
    (
      "GluonW Test Protons",
      (
        "GluonW Protons by DJed Alliance v1.2: VarPhiBeta Implemented. This is a test Token.",
        GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY
      )
    )
  )

  val txJson: Seq[Unit] = client.getClient.execute { (ctx: BlockchainContext) =>
    val SIGN_REDUCED: String = "signReduced"
    val MINT: String = "mint"
    val MUTATE: String = "mutate"
    val MERGE: String = "merge"

    // SET RUN TX HERE
    val runTx: String = MERGE

    System.out.println(s"Running $runTx tx")
    val totalSupply: Long = GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY

    val signedTxs: Seq[SignedTransaction] = runTx match {
      case MINT => {
        val neutronsMintTx = mintTokens(
          tokens(1)._1,
          tokens(1)._2._1,
          tokens(1)._2._2
        )(client, conf, nodeConf)
        val protonsMintTx = mintTokens(
          tokens(2)._1,
          tokens(2)._2._1,
          tokens(2)._2._2
        )(client, conf, nodeConf)
        val gluonWNFTMintTx = mintTokens(
          tokens.head._1,
          tokens.head._2._1,
          tokens.head._2._2
        )(client, conf, nodeConf)

        protonsMintTx
      }
      case MERGE => {
        val nftToken = ErgoToken(GluonWTokens.gluonWBoxNFTId, 1)
        val sigGoldToken = ErgoToken(GluonWTokens.neutronId, totalSupply)
        val sigGoldRsvToken =
          ErgoToken(GluonWTokens.protonId, totalSupply)
        val gluonWBox: GluonWBox = GluonWBox(
          value =
            2 * Parameters.OneErg + GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE,
          tokens = Seq(
            ErgoToken(GluonWTokens.gluonWBoxNFTId, 1),
            ErgoToken(
              GluonWTokens.neutronId,
              totalSupply - (GluonWBoxConstants.PRECISION / 100)
            ),
            ErgoToken(
              GluonWTokens.protonId,
              totalSupply - (GluonWBoxConstants.PRECISION / 100)
            )
          ),
          lastDayBlockRegister = new LongRegister(client.getHeight)
        )

        mergeBox(
          Seq(nftToken, sigGoldToken, sigGoldRsvToken),
          Seq(gluonWBox)
        )(client, conf, nodeConf)
      }
      case MUTATE => {
        val boxIdToMutate: String =
          "1e1449da157a51d5d7f43d2bd3a35fa9cfc6b82cae9ff33c1d8d5bced4a875ad"
        val gluonWBox: InputBox = ctx.getBoxesById(boxIdToMutate).head
        val mutatedGluonWBox: GluonWBox = GluonWBox.from(gluonWBox)
        val toUserBox: FundsToAddressBox = FundsToAddressBox(
          address =
            if (nodeConf.getNetworkType == NetworkType.MAINNET)
              ServiceConfig.mainNetServiceOwner
            else ServiceConfig.testNetServiceOwner,
          value = mutatedGluonWBox.value - 2 * Parameters.MinFee,
          tokens = mutatedGluonWBox.tokens,
          R4 = Option(mutatedGluonWBox.totalSupplyRegister),
          R5 = Option(mutatedGluonWBox.tokenIdRegister),
          R6 = Option(mutatedGluonWBox.feeRegister),
          R7 = Option(mutatedGluonWBox.volumePlusRegister),
          R8 = Option(mutatedGluonWBox.volumeMinusRegister),
          R9 = Option(mutatedGluonWBox.lastDayBlockRegister)
        )
//        val oracleBox: OracleBox = explorer.getOracleBox

        mutate(
          boxIdToMutate = boxIdToMutate,
          Seq(toUserBox)
//          dataInputs = Seq(oracleBox.box.get.input)
        )(
          client,
          conf,
          nodeConf
        )
      }
      case SIGN_REDUCED => {
        BoxTools.signReducedTx(
          Seq(
            reducedTxBytes
          )
        )(client, conf, nodeConf)
      }
      case "consolidate" => {
        val ergValue =
          2 * Parameters.OneErg + GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE
        val tokens = Seq(
          ErgoToken(GluonWTokens.gluonWBoxNFTId, 1),
          ErgoToken(
            GluonWTokens.neutronId,
            totalSupply - (GluonWBoxConstants.PRECISION / 100)
          ),
          ErgoToken(
            GluonWTokens.protonId,
            totalSupply - (GluonWBoxConstants.PRECISION / 100)
          )
        )

        consolidateBoxes(ergValue = ergValue, tokens = tokens)(
          client,
          conf,
          nodeConf
        )
      }
    }

    signedTxs.map { signedTx =>
      ctx.sendTransaction(signedTx)
      val jsonVal = signedTx.toJson(true)
      System.out.println(jsonVal)
    }
  }

  System.out.println("Completed Transaction")

  def mutate(
    boxIdToMutate: String,
    outBoxes: Seq[BoxWrapper],
    dataInputs: Seq[InputBox] = Seq()
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val boxesToMutate: Seq[InputBox] = ctx.getBoxesById(boxIdToMutate)

      val tx: Tx = Tx(
        inputBoxes = boxesToMutate,
        outBoxes = outBoxes,
        changeAddress = ownerAddress,
        dataInputs = dataInputs
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }

  def consolidateBoxes(
    ergValue: Long,
    tokens: Seq[ErgoToken]
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val coveringBoxes: List[InputBox] = client
        .getCoveringBoxesFor(
          ownerAddress,
          ergValue + Parameters.MinFee,
          tokens.toList.asJava
        )

      val consolidatedBox: FundsToAddressBox = FundsToAddressBox(
        address = ownerAddress,
        value = ergValue,
        tokens = tokens
      )

      val tx: Tx = Tx(
        inputBoxes = coveringBoxes,
        outBoxes = Seq(consolidatedBox),
        changeAddress = ownerAddress
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }
}
