package gluonw.tools

import edge.commons.ErgCommons
import commons.node.{Client, TestClient}
import edge.boxes.BoxWrapper
import edge.node.BaseClient
import edge.txs.Tx
import gluonw.common.{GluonWAsset, GluonWTokens}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  Eip4Token,
  ErgoId,
  ErgoProver,
  ErgoToken,
  InputBox,
  OutBox,
  Parameters,
  SecretString,
  SignedTransaction,
  UnsignedTransaction,
  UnsignedTransactionBuilder
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import edge.utils.ContractUtils
import gluonw.boxes.GluonWBox

import java.util.stream.Collectors
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

object BoxCreation extends App {

  val configFileName = "ergo_config.json"
  val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
  val nodeConf: ErgoNodeConfig = conf.getNode
  val client: BaseClient = new TestClient(nodeConf.getNetworkType)
  client.setClient()

  val tokens: Seq[(String, (String, Long))] = Seq(
    ("SIGGoldNFT", ("SigGold NFTby DJed Alliance", 1L)),
    ("SIGGold", ("SigGold by DJed Alliance", 100000000L)),
    ("SIGGoldRsv", ("SigGoldRsv by DJed Alliance", 100000000L))
  )

  val txJson: Seq[Unit] = client.getClient.execute { (ctx: BlockchainContext) =>
    val runTx = "merge"

    System.out.println(s"Running $runTx tx")
//      val sigGoldMintTx = mintTokens(tokens(1)._1, tokens(1)._2._1, tokens(1)._2._2)(client, conf, nodeConf)
//      val sigGoldRsvMintTx = mintTokens(tokens(2)._1, tokens(2)._2._1, tokens(2)._2._2)(client, conf, nodeConf)
//      val sigGoldNFTMintTx = mintTokens(tokens.head._1, tokens.head._2._1, tokens.head._2._2)(client, conf, nodeConf)
    val totalSupply: Long = 100000000L

    val signedTxs: Seq[SignedTransaction] = runTx match {
//        case "mint" => sigGoldNFTMintTx
      case "merge" => {
        val nftToken = new ErgoToken(GluonWTokens.gluonWBoxNFTId, 1)
        val sigGoldToken = new ErgoToken(GluonWTokens.sigGoldId, totalSupply)
        val sigGoldRsvToken =
          new ErgoToken(GluonWTokens.sigGoldRsvId, totalSupply)
        val gluonWBox: GluonWBox = GluonWBox(
          value = ErgCommons.MinBoxFee,
          tokens = Seq(
            new ErgoToken(GluonWTokens.gluonWBoxNFTId, 1),
            new ErgoToken(GluonWTokens.sigGoldId, totalSupply),
            new ErgoToken(GluonWTokens.sigGoldRsvId, totalSupply)
          )
        )

        mergeBox(
          Seq(nftToken, sigGoldToken, sigGoldRsvToken),
          Seq(gluonWBox)
        )(client, conf, nodeConf)
      }
    }

    signedTxs.map { signedTx =>
      ctx.sendTransaction(signedTx)
      val jsonVal = signedTx.toJson(true)
      System.out.println(jsonVal)
    }
  }

  System.out.println("Completed Transaction")

  def mintTokens(
    tokenName: String,
    tokenDesc: String,
    amount: Long = 1L,
    decimals: Int = 0
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val addressIndex: Int = config.getParameters.get("addressIndex").toInt

      val prover: ErgoProver = ctx
        .newProverBuilder()
        .withMnemonic(
          SecretString.create(nodeConfig.getWallet.getMnemonic),
          SecretString.create(""),
          false
        )
        .withEip3Secret(addressIndex)
        .build()

      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val directBox = client
        .getCoveringBoxesFor(ownerAddress, ErgCommons.MinMinerFee * 10)
        .getBoxes
      System.out.println("boxId: " + directBox.get(0).getId)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      def eip4Token: Eip4Token = new Eip4Token(
        directBox.get(0).getId.toString,
        amount,
        tokenName,
        tokenDesc,
        decimals
      )

      val tokenBox: OutBox = txB
        .outBoxBuilder()
        .value(ErgCommons.MinBoxFee)
        .mintToken(eip4Token)
        .contract(ContractUtils.sendToPK(ownerAddress))
        .build()

      val inputBoxes = directBox

      val tx = txB
        .addInputs(inputBoxes.toSeq: _*)
        .addOutputs(tokenBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(ownerAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      Seq(signed)
    }

  def mergeBox(tokensToMerge: Seq[ErgoToken], outBox: Seq[BoxWrapper])(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val addressIndex: Int = config.getParameters.get("addressIndex").toInt
      val ownerAddress: Address = Address.createEip3Address(
        0,
        nodeConfig.getNetworkType,
        SecretString.create(nodeConfig.getWallet.getMnemonic),
        SecretString.create(""),
        false
      )

      val prover: ErgoProver = ctx
        .newProverBuilder()
        .withMnemonic(
          SecretString.create(nodeConfig.getWallet.getMnemonic),
          SecretString.create(""),
          false
        )
        .withEip3Secret(addressIndex)
        .build()

      val spendingBoxes =
        ctx.getDataSource.getUnspentBoxesFor(ownerAddress, 0, 500)

      val spendingBoxesWithTokens: java.util.List[InputBox] = spendingBoxes
        .stream()
        .filter(!_.getTokens.isEmpty)
        .collect(Collectors.toList())

      // Put the boxes with the seqId tokens tokens together in a sequence
      val boxesToMerge: Seq[InputBox] = spendingBoxesWithTokens.filter {
        spendingBox =>
          val hasTokens: Boolean = spendingBox.getTokens.exists(token =>
            tokensToMerge.exists(_.getId.equals(token.getId))
          )
          hasTokens
      }.toSeq

      // Merge the boxes together into the box expected.
      val inputBoxes = List(
        boxesToMerge: _*
      )

      val tx: Tx = Tx(
        inputBoxes = inputBoxes.toSeq,
        changeAddress = ownerAddress,
        outBoxes = outBox
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }
}
