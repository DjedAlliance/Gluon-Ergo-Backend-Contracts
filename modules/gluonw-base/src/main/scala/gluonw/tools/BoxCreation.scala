package gluonw.tools

import commons.node.TestClient
import edge.boxes.BoxWrapper
import edge.node.BaseClient
import edge.tools.BoxTools
import edge.tools.BoxTools.{getProver, mergeBox, mintTokens}
import edge.txs.Tx
import gluonw.common.GluonWTokens
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoProver, InputBox, Parameters, SignedTransaction}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants}
import org.ergoplatform.sdk.ErgoToken
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
    val runTx = "signReduced"

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

        sigGoldRsvMintTx
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
      case "mutate" => {
        val boxIdToMutate: String = "fb860b9d14dd6b210294f9fc1197ba687618ddbc021bfcdf308830f301032569"
        val gluonWBox: InputBox = ctx.getBoxesById(boxIdToMutate).head
        val mutatedGluonWBox: GluonWBox = GluonWBox.from(gluonWBox)

        mutate(boxIdToMutate = boxIdToMutate, mutatedGluonWBox)(client, conf, nodeConf)
      }
      case "signReduced" => {
        BoxTools.signReducedTx(
          Seq("rwoCuFzI3Zxt-QE9EZQfbH3PAkTqi_tTsmQaJmhgV2sJIAsAAFqv9BOmBM-8eHN2z_YpD-eNED1gXeVlH1cmcIYb5ptJAAABXE84PBIk5HqUAQIqHi2wPwtoa7zyUxvgS_ycVhEFKrkFXdCrRTGOFETnTusaEU9u6iOUVtPq02Yk1-L_ORqRZ_hbboRukDYoYgyV0Z8p5txCAMfbQUF5SgTJNnw8Ri2gkpbBOjSZ-FqGmAkLHnTv1oiVIxEpSJjijuL_y6IwK-rLWGQSoptAlQyGmFq_-6LFd-vzHO8Hzv8BSRgDgloSUCqpvKbIN9qMdjchIZVrdT1djwNCGAGV_vUCEeds9d-AKwPA3MvhbxAVBAAEAgQCBAQEBAQABAAEgKjWuQcGAWQFgIl6BgFCBAAGAgPoAQEBAQYBAgEBAQEGAQIIzQNVIwyyP55qXvBVeLc6hq32K8FVFdHRco_BtCkY6fs_mAEA2BrWAdtjCKfWArKlcwAA1gPbYwhyAtYEsnIBcwEA1gWycgNzAgDWBrJyAXMDANYHsnIDcwQA1gjkxqcEWdYJloMGAZOMsnIBcwUAAYyycgNzBgABk4xyBAGMcgUBk4xyBgGMcgcBk8KnwnICk3II5MZyAgRZk-TGpwU8Dg7kxnICBTwODtYKjHIEAtYLjHIFAtYMkXIKcgvWDYxyBgLWDoxyBwLWD5FyDXIO1hDBp9YRwXIC1hJ-mYxyCAFyCgbWE3MH1hRzCNYVfplyEHMJBtYWfpmMcggCcg0G1hehnZxzCn5yEwZyFJ2cchKdfuTGsttlAf5zCwAEBQZzDHIV1hiTchByEdYZj3IKcgvWGo9yDXIOlZaDBAFyCXIMcg-PchByEdgC1ht-mXIRchAG1hyZfnITBp1-chMGchTRloMEAXIJk36ZcgpyCwadnZycchtyEnIcchV-chMGk36Zcg1yDgadnZycchtyFnIcchV-chMGcw2VloMEAXIJchlyGpFyEHIR2APWG5l-chMGnX5yEwZyFNYcnH6ZchFyEAadfnITBnIb1h2cchVyG9GWgwQBcgmTfplyCnILBp2cnHIcchJ-chMGch2TfplyDXIOBp2cnHIcchZ-chMGch1zDpWWgwQBcglyDHIachjRloMEAXIJk36ZcgpyCwadnJ2cfplyDnINBp2cmX5yEwadnHMPfnITBnIUmX5yEwZyF35yEwZyF3ISchZzEJN-chEGfnIQBpWWgwQBcglyGXIPchjRloMEAXIJcxGTfplyDXIOBp2cnZydnH6ZcgtyCgaZfnITBp2ccxJ-chMGchR-chMGchZyEnIXmX5yEwZyF5N-chEGfnIQBpWWgwQBcgmTcgpyC5NyDXIOchhzE9FzFPvhJQMAAQH5_oKvhK_RsQEC5_z20oWv0bEBAlmAgNDYi96i4wKAgNDYi96i4wI8Dg4gW26EbpA2KGIMldGfKebcQgDH20FBeUoEyTZ8PEYtoJIglsE6NJn4WoaYCQsedO_WiJUjESlImOKO4v_LojAr6suAq472vQEACM0DVSMMsj-eal7wVXi3Ooat9ivBVRXR0XKPwbQpGOn7P5j74SUEA4DC1y8CmYOxGQGHgaW9AQSAwtcvAMCEPRAFBAAEAA42EAIEoAsIzQJ5vmZ--dy7rFWgYpXOhwsHApv82y3OKNlZ8oFbFvgXmOoC0ZKjmozHpwFzAHMBEAECBALRloMDAZOjjMeypXMAAAGTwrKlcwEAdHMCcwODAQjN7qyTsaVzBPvhJQAA05GWA80DVSMMsj-eal7wVXi3Ooat9ivBVRXR0XKPwbQpGOn7P5jmlgPmlgM=")
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

  def mutate(boxIdToMutate: String, outBox: BoxWrapper)(client: BaseClient,
               config: ErgoToolConfig,
               nodeConfig: ErgoNodeConfig): Seq[SignedTransaction] = {
    client.getClient.execute { ctx => {
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val spendingBoxes: java.util.List[InputBox] = ctx.getDataSource.getUnspentBoxesFor(ownerAddress, 0, 500)

      val boxesToMutate: Seq[InputBox] = ctx.getBoxesById(boxIdToMutate)

      val tx: Tx = Tx(
        inputBoxes = boxesToMutate ++ spendingBoxes,
        outBoxes = Seq(outBox),
        changeAddress = ownerAddress
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }}
  }
}
