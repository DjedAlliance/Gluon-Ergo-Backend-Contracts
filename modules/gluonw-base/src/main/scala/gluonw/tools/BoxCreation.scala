package gluonw.tools

import commons.configs.ServiceConfig
import commons.node.{Client, TestClient}
import edge.boxes.FundsToAddressBox
import edge.node.BaseClient
import edge.registers.LongRegister
import edge.tools.BoxTools
import edge.tools.BoxTools.{mergeBox, mintTokens}
import gluonw.common.{GluonWBoxExplorer, GluonWTokens}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  InputBox,
  NetworkType,
  Parameters,
  SignedTransaction
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import org.ergoplatform.sdk.{ErgoToken, SecretString}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object BoxCreation extends App {

  val isMainNet: Boolean = false
  val configFileName = "ergo_config.json"
  val testNetConfigFileName = "ergo_config_testnet.json"

  val conf: ErgoToolConfig = if (isMainNet) {
    ErgoToolConfig.load(configFileName)
  } else {
    ErgoToolConfig.load(testNetConfigFileName)
  }
  val nodeConf: ErgoNodeConfig = conf.getNode
  val client: BaseClient = new TestClient(nodeConf.getNetworkType)

//  val explorer: GluonWBoxExplorer = new GluonWBoxExplorer()(client)
  val reducedTxBytes: Seq[String] = Seq(
    ""
  )

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
      "GluonW Test GAU",
      (
        "GluonW GAU Neutrons by DJed Alliance v1.2: VarPhiBeta Implemented. This is a test Token.",
        GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY
      )
    ),
    (
      "GluonW Test GAUC",
      (
        "GluonW GAUC Protons by DJed Alliance v1.2: VarPhiBeta Implemented. This is a test Token.",
        GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY
      )
    )
  )

  val txJson: Seq[Unit] = client.getClient.execute { (ctx: BlockchainContext) =>
    val SIGN_REDUCED: String = "signReduced"
    val MINT: String = "mint"
    val MUTATE: String = "mutate"
    val MERGE: String = "merge"
    val BURN: String = "burn"

    // SET RUN TX HERE
    val runTx: String = MUTATE

    System.out.println(s"Running $runTx tx")
    val totalSupply: Long = GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY

    val signedTxs: Seq[SignedTransaction] = runTx match {
      case MINT => {
        val neutronsMintTx = mintTokens(
          tokens(1)._1,
          tokens(1)._2._1,
          tokens(1)._2._2,
          decimals = 9
        )(client, conf, nodeConf)
        val protonsMintTx = mintTokens(
          tokens(2)._1,
          tokens(2)._2._1,
          tokens(2)._2._2,
          decimals = 9
        )(client, conf, nodeConf)
        val gluonWNFTMintTx = mintTokens(
          tokens.head._1,
          tokens.head._2._1,
          tokens.head._2._2
        )(client, conf, nodeConf)

        gluonWNFTMintTx
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
          "5778651e6e9a3748da4158b644ee649d4794e2057f3b999d9f21fb8652d12cb9"
        val gluonWBox: InputBox = ctx.getBoxesById(boxIdToMutate).head
        val mutatedGluonWBox: GluonWBox = GluonWBox.from(gluonWBox)
        val ownerAddress: Address = Address.createEip3Address(
          0,
          nodeConf.getNetworkType,
          SecretString.create(nodeConf.getWallet.getMnemonic),
          SecretString.create(""),
          false
        )
        // get some fee for spending
        val spendingBoxes =
          ctx.getDataSource
            .getUnspentBoxesFor(ownerAddress, 0, 500)
            .asScala
            .toSeq
            .filter(_.getTokens.isEmpty)
//        val toUserBox: FundsToAddressBox = FundsToAddressBox(
//          address =
//            if (nodeConf.getNetworkType == NetworkType.MAINNET)
//              ServiceConfig.mainNetServiceOwner
//            else ServiceConfig.testNetServiceOwner,
//          value = mutatedGluonWBox.value - 2 * Parameters.MinFee,
//          tokens = mutatedGluonWBox.tokens,
//          R4 = Option(mutatedGluonWBox.totalSupplyRegister),
//          R5 = Option(mutatedGluonWBox.tokenIdRegister),
//          R6 = Option(mutatedGluonWBox.feeRegister),
//          R7 = Option(mutatedGluonWBox.volumePlusRegister),
//          R8 = Option(mutatedGluonWBox.volumeMinusRegister),
//          R9 = Option(mutatedGluonWBox.lastDayBlockRegister)
//        )
//        val oracleBox: OracleBox = explorer.getOracleBox

        BoxTools.mutate(
          boxIdToMutate = boxIdToMutate,
          Seq(mutatedGluonWBox),
//          dataInputs = Seq(oracleBox.box.get.input),
          inputBoxes = spendingBoxes
        )(
          client,
          conf,
          nodeConf
        )
      }
      case SIGN_REDUCED => {
        BoxTools.signReducedTx(
          reducedTxBytes
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

        BoxTools.consolidateBoxes(ergValue = ergValue, tokens = tokens)(
          client,
          conf,
          nodeConf
        )
      }
      case BURN => {
        val tokens = Seq(
          ErgoToken(
            GluonWTokens.neutronId,
            (GluonWBoxConstants.PRECISION / 100)
          ),
          ErgoToken(
            GluonWTokens.protonId,
            (GluonWBoxConstants.PRECISION / 100)
          )
        )

        BoxTools.burnTokens(tokens)(client, conf, nodeConf)
      }
    }

    signedTxs.map { signedTx =>
      ctx.sendTransaction(signedTx)
      val jsonVal = signedTx.toJson(true)
      System.out.println(jsonVal)
    }
  }

  System.out.println("Completed Transaction")
}
