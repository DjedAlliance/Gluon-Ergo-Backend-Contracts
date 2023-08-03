package gluonw.tools

import commons.configs.NodeConfig
import edge.commons.ErgCommons
import commons.node.{Client, TestClient}
import edge.node.BaseClient
import edge.tools.BoxTools.{mergeBox, mintTokens}
import gluonw.common.GluonWTokens
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  BoxOperations,
  InputBox,
  OutBox,
  Parameters,
  SignedTransaction
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants}
import org.ergoplatform.sdk.{ErgoToken, SecretString}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

object BoxCreation extends App {

  val configFileName = "ergo_config.json"
  val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
  val nodeConf: ErgoNodeConfig = conf.getNode
  val client: BaseClient = new TestClient(nodeConf.getNetworkType)
  client.setClient()

  val tokens: Seq[(String, (String, Long))] = Seq(
    ("GluonW NFT", ("GluonW NFTby DJed Alliance", 1L)),
    (
      "GluonW Neutrons",
      (
        "GluonW Neutrons by DJed Alliance",
        GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY
      )
    ),
    (
      "GluonW Protons",
      (
        "GluonW Protons by DJed Alliance",
        GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY
      )
    )
  )

  val txJson: Seq[Unit] = client.getClient.execute { (ctx: BlockchainContext) =>
    val runTx = "merge"

    System.out.println(s"Running $runTx tx")
    val totalSupply: Long = GluonWBoxConstants.TOTAL_CIRCULATING_SUPPLY

    val signedTxs: Seq[SignedTransaction] = runTx match {
      case "mint" => {
        val sigGoldMintTx = mintTokens(
          tokens(1)._1,
          tokens(1)._2._1,
          tokens(1)._2._2
        )(client, conf, nodeConf)
        val sigGoldRsvMintTx = mintTokens(
          tokens(2)._1,
          tokens(2)._2._1,
          tokens(2)._2._2
        )(client, conf, nodeConf)
        val sigGoldNFTMintTx = mintTokens(
          tokens.head._1,
          tokens.head._2._1,
          tokens.head._2._2
        )(client, conf, nodeConf)

        sigGoldNFTMintTx
      }
      case "merge" => {
        val nftToken = ErgoToken(GluonWTokens.gluonWBoxNFTId, 1)
        val sigGoldToken = ErgoToken(GluonWTokens.neutronId, totalSupply)
        val sigGoldRsvToken =
          ErgoToken(GluonWTokens.protonId, totalSupply)
        val gluonWBox: GluonWBox = GluonWBox(
          value =
            20 * Parameters.OneErg + GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE,
          tokens = Seq(
            ErgoToken(GluonWTokens.gluonWBoxNFTId, 1),
            ErgoToken(
              GluonWTokens.neutronId,
              totalSupply - (GluonWBoxConstants.PRECISION / 10)
            ),
            ErgoToken(
              GluonWTokens.protonId,
              totalSupply - (GluonWBoxConstants.PRECISION / 10)
            )
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
}
