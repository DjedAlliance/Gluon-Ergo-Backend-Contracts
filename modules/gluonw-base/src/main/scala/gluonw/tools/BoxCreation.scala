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
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoProver, InputBox, NetworkType, Parameters, SignedTransaction}
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
  val reducedTxBytes: String = "lCEDUnjuWFrzSjl_NNsWxnR3Qq74yKfvfCkbqRlHbyMBbQ8AAGnSZVgN5Lh8tuYQTrCLX1v8PDmasr9YCrkOk8d0sjRoAAB9QkBjVKp5mD6jue4in4IGG4J4TSoPd6qFmrpp2Bi9BQAAAdoTDcMuZ75uMbJcIR2vdEPRKlhLBrV5MLtoFNY0vTIDDAHa6xOYrSo-uWvtdAK7pUJ6bEUMqsw_QxmztzsZp-citETxm_PORT1Q7-uxxmidYII_-xExHzqhH3qeD_HivQUDZbu5ufIeu36g07DPKxwnRXOehhmectS7DC0EOLNlEFhkEqKbQJUMhphav_uixXfr8xzvB87_AUkYA4JaElAqSIgyYz2O8YNn-KgHIvxFJk9sobVqdqUcfxyad40pnxWJhbOAjt53N8h4D1tSedqpa5HJ30nV7ufVs_KIp9b9smCNj1L7IxJ9kAAmL5pk61coLgxyVCfTXpUN1vGBFOMAdpT85si3-5s0-o2QMhdOamN9ZQGTbCDy_MBMfJJSr8xbboRukDYoYgyV0Z8p5txCAMfbQUF5SgTJNnw8Ri2gkqm8psg32ox2NyEhlWt1PV2PA0IYAZX-9QIR52z134ArlsE6NJn4WoaYCQsedO_WiJUjESlImOKO4v_LojAr6stjft0JB1ZFKeqCSwIabWJkPCnTkXd0Pq6GkbYGuHiv2QXAvPL6URBpBAAEAgQCBAQEBAQABAAIzQNVIwyyP55qXvBVeLc6hq32K8FVFdHRco_BtCkY6fs_mASAqNa5BwYBZAWAiXoGAgPoBAAGAUIGAQQGAQAGAQUO1AQQIwQABAAEAgQCBAIEAA4gBD6hLwN2l0jkNsADiGxFXd8afNSq-9IUYCgi1SE_TmgEBAQABAAEAAQCDiBWru07o_Z3_7VGKwsfg9o-HQbIlGupeO9-cGIhusXpggUABAQFyAEF0gEEAAQCBAQEBAQABAAEAAQADiDZS_rEC1FjU5g0QyCRBNzdW3yiMqAcyzdu6AFN9jMJBwQABAIEAgQCBDwEAA4gAB4YLMPwSuxEhselAY0ZjpWRp8-ws3L1-V-j5d29JNMBAAQE2AHWAeTjAASVk3IBcwDYCNYCsqRzAQDWA9tjCHIC1gSypXMCANYF22MIcgTWBttjCKfWB5mMsnIFcwMAAoyycgZzBAAC1giZwafBcgTWCcFyAtHt7e2TjLJyA3MFAAFzBq-0pXMHsaXZAQpjk7HbYwhyCnMI7ZOMsnIFcwkAAYyycgZzCgABk4yycgVzCwABcwzt7ZFyB3MNkHIInJ2cnXIJjLJyA3MOAAJyB3MPcxCTmcGypXMRAHIJcgiVk3IBcxLYAdYCsqVzEwDR7e2T22MIcgLbYwink8JyAsKnj8GnwXIC2ATWArKlcxQA1gPbYwin1gSypHMVANYF22MIcgTR7e3tk7LbYwhyAnMWALJyA3MXAJPCcgLCp5PBp8FyAu2TjLJyBXMYAAFzGZKMsttjCLKlcxoAcxsAApmajLJyBXMcAAKMsnIDcx0AAn6csbWk2QEGY5XmxnIGBgXt7ZKMx3IGAZmjcx6TjLLbYwhyBnMfAAFzIJPkxnIGBQTkxnIEBQRzIXMiBQYBAQIBBAAEAgQIBAYEBAQEBgMPQkABAQQGBgMPQkABAQEBBgMPQkABAQEBBQABAQEBBKALBAAEAAQcBgECAQEEHAQABQAEAAQcBAAEAgQcBAIEHAQCBQAEHAQABAAFAAQcBAAEAgQcBAIEHAQCBQAEoAsEoAsEoAsEHAQcBAAEAAQcAQEGAQIEHAQABQAEAAQcBAAEAgQcBAIEHAQCBQAEHAQABAAFAAQcBAAEAgQcBAIEHAQCBQAEoAsEoAsBANgk1gHbYwin1gKypXMAANYD22MIcgLWBLJyAXMBANYFsnIDcwIA1gaycgFzAwDWB7JyA3MEANYI5ManBFnWCeTGpwZZ1gqMcgkC1gvkxnICBlnWDIxyCwLWDZaDBwGTjLJyAXMFAAGMsnIDcwYAAZOMcgQBjHIFAZOMcgYBjHIHAZPCp8JyApNyCOTGcgIEWZPkxqcFPA4O5MZyAgU8Dg6TcgpyDNYOjHIEAtYPjHIFAtYQkXIOcg_WEYxyBgLWEoxyBwLWE5FyEXIS1hTBp9YVwXIC1haWgwQBcg1yEHITj3IUchXWF49yDnIP1hiPchFyEtYZloMEAXINchdyGJFyFHIV1hqTchRyFdYbloMEAXINchByGHIa1hyWgwQBcg1yF3ITchrWHXMH1h6McgkB1h_kxqcHEdYg5MZyAgcR1iHkxqcIEdYi5MZyAggR1iPkxqcJBdYk5MZyAgkFlZeDBAFyFnIZchtyHNge1iV-mYxyCAFyDgbWJnMI1idzCdYofplyFHMKBtYpfpmMcggCchEG1irjAAjWK3ML1iydfuTGsttlAf5zDAAEBQZyK9YtoZ2ccw1-ciYGciednHIlcixyKNYumX5yJgZyLdYv2QEvBZ2cfnIvBp2cci5yKHIpfnImBtYw2QEwBZ2cfnIwBnIsfnImBtYxlXIWfplyFXIUBpVyGX6ZchRyFQaVchvaci8BmXISchHacjABmXIPcg7WMp2ccw5yMXIr1jNzD9Y0hgLQch2Vj3IecgqdnJ2ccxByMXIrfplyCnIeBn5yCgZyM9Y1hgJzEZ2ccxJyMXIr1jaGAoMBAnMTcjPWN5XschxyG5XmciqDA04OcjSGAtDkcipyMnI1gwNODnI0cjZyNZXmciqDA04OcjSGAtDkcipyMnI2gwNODnI0cjZyNtY4snI3cxQA1jmMcjgC1jqycjdzFQDWO4xyOgLWPJFyO3Iz1j2VcjyypXMWALKlcxcA1j6ycjdzGADWP4xyPgLWQJaDAwHt7ZWRcjlyM9gB1kCypXMZAJaDAgGTwnJAjHI4AZN-wXJABppyOXMacxuV5nIqlXI82AHWQLKlcxwAloMCAZPCckCMcjoBk37BckAGmnI7cx1zHnMflexyG3IclZFyP3IzloMCAZPCcj2Mcj4Bk37Bcj0GmnI_cyBzIXMik36ZjHILAXIeBnI5k3IMcgrWQZaDAwGTch9yIJNyIXIik3IjciTWQtkBQhF-sHJCcyPZAURZmoxyRAGMckQCBpVyFtgC1kN-mXIVchQG1kSZfnImBp1-ciYGcifRloMGAXINk36Zcg5yDwadnZycckNyJXJEcih-ciYGk36ZchFyEgadnZycckNyKXJEcih-ciYGcyRyQHJBlXIZ2APWQ5l-ciYGnX5yJgZyJ9ZEnH6ZchVyFAadfnImBnJD1kWccihyQ9GWgwYBcg2TfplyDnIPBp2cnHJEciV-ciYGckWTfplyEXISBp2cnHJEcil-ciYGckVzJXJAckGVchvYB9ZDmXISchHWRNpyQgFyItZF2nJCAXIg1kZ9nZl-owVyI35zJgUE1keTckZzJ9ZIkXJGcyjWSZlzKXJG0ZaDCAFyDZN-mXIOcg8GnZydnH5yQwadnJl-ciYGmp1-ciYGciednJ1-ciYGcyqVkXJEckVyM5lyRXJEcihyLn5yJgZyLXIlcilzK5N-chUGfnIUBnJAloMEAZOxciJzLJOVckeyciFzLQBzLrJyInMvAJVySJO0ciJyRnMwtHIhczFySZO0ciJzMnMztHIhczRzNa-0ciJzNnJG2QFKBZNySnM3loMEAZOxciBzOJN-snIgczkABpp-lXJHsnIfczoAczsG2nIvAXJDlXJIk7RyIHJGczy0ch9zPXJJk7RyIHM-cz-0ch9zQHNBr7RyIHNCckbZAUoFk3JKc0OTciR-nJ2jc0RzRQWVchzYCNZDmXIPcg7WRNpyQgFyINZF2nJCAXIi1kZ9nZl-owVyI35zRgUE1keVknJGc0dzSHJG1kiTckdzSdZJkXJHc0rWSplzS3JH0ZaDCAFyDXNMk36ZchFyEgadnJ2cnZx-ckMGmX5yJgaanX5yJgZyJ52cnX5yJgZzTZWRckRyRXIzmXJFckRyKH5yJgZyKXIlci1yLpN-chUGfnIUBnJAloMEAZOxciBzTpOVckiych9zTwBzULJyIHNRAJVySZO0ciByR3NStHIfc1NySpO0ciBzVHNVtHIfc1ZzV6-0ciBzWHJH2QFLBZNyS3NZloMEAZOxciJzWpN-snIic1sABpp-lXJIsnIhc1wAc10G2nIwAXJDlXJJk7RyInJHc160ciFzX3JKk7RyInNgc2G0ciFzYnNjr7RyInNkckfZAUsFk3JLc2WTciR-nJ2jc2ZzZwXRc2hyHejgLQMAAQHCufa5ha_RsQEC5Lfvt4Wv0bEBBlmAgNDYi96i4wKAgNDYi96i4wI8Dg4gtETxm_PORT1Q7-uxxmidYII_-xExHzqhH3qeD_HivQUgA2W7ubnyHrt-oNOwzyscJ0VznoYZnnLUuwwtBDizZRBZuJ-rYICAgr-T7_AIEQ7a_poFAAAAAAAAAAAAAAAAABEOrJDwvAEAAAAAAAAAAAAAAAAABaC_W__u1OSAdAAIzQNVIwyyP55qXvBVeLc6hq32K8FVFdHRco_BtCkY6fs_mOjgLQsDgMLXLwTViPggBeDgpEcBvsaxMgbg4KRHB4L1thkIh4GlvQEJgMLXLwqZg7EZCwECnMi4NACjl1EACM0DVSMMsj-eal7wVXi3Ooat9ivBVRXR0XKPwbQpGOn7P5jo4C0AAKCIQRAjBAAEAAQCBAIEAgQADiAEPqEvA3aXSOQ2wAOIbEVd3xp81Kr70hRgKCLVIT9OaAQEBAAEAAQABAIOIFau7Tuj9nf_tUYrCx-D2j4dBsiUa6l4735wYiG6xemCBQAEBAXIAQXSAQQABAIEBAQEBAAEAAQABAAOINlL-sQLUWNTmDRDIJEE3N1bfKIyoBzLN27oAU32MwkHBAAEAgQCBAIEPAQADiAAHhgsw_BK7ESGx6UBjRmOlZGnz7CzcvX5X6Pl3b0k0wEABATYAdYB5OMABJWTcgFzANgI1gKypHMBANYD22MIcgLWBLKlcwIA1gXbYwhyBNYG22MIp9YHmYyycgVzAwACjLJyBnMEAALWCJnBp8FyBNYJwXIC0e3t7ZOMsnIDcwUAAXMGr7SlcwexpdkBCmOTsdtjCHIKcwjtk4yycgVzCQABjLJyBnMKAAGTjLJyBXMLAAFzDO3tkXIHcw2QcgicnZydcgmMsnIDcw4AAnIHcw9zEJOZwbKlcxEAcglyCJWTcgFzEtgB1gKypXMTANHt7ZPbYwhyAttjCKeTwnICwqePwafBcgLYBNYCsqVzFADWA9tjCKfWBLKkcxUA1gXbYwhyBNHt7e2TsttjCHICcxYAsnIDcxcAk8JyAsKnk8GnwXIC7ZOMsnIFcxgAAXMZkoyy22MIsqVzGgBzGwACmZqMsnIFcxwAAoyycgNzHQACfpyxtaTZAQZjlebGcgYGBe3tkozHcgYBmaNzHpOMsttjCHIGcx8AAXMgk-TGcgYFBOTGcgQFBHMhcyIF6OAtAADAhD0QBQQABAAONhACBKALCM0Ceb5mfvncu6xVoGKVzocLBwKb_NstzijZWfKBWxb4F5jqAtGSo5qMx6cBcwBzARABAgQC0ZaDAwGTo4zHsqVzAAABk8KypXMBAHRzAnMDgwEIze6sk7GlcwTo4C0AANPLsgnNA1UjDLI_nmpe8FV4tzqGrfYrwVUV0dFyj8G0KRjp-z-YoLMJzQNVIwyyP55qXvBVeLc6hq32K8FVFdHRco_BtCkY6fs_mPWzCfWzCQ=="

  client.setClient()

  val tokens: Seq[(String, (String, Long))] = Seq(
    ("GluonW Test NFT", ("GluonW NFTby DJed Alliance v1.2: VarPhiBeta Implemented. This is a test Token.", 1L)),
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
    val runTx: String = SIGN_REDUCED

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
            ),
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
          "8ffde6332af52b57b5b26560ffee4951286146f07edd2350f6dcf244efde52a4"
        val gluonWBox: InputBox = ctx.getBoxesById(boxIdToMutate).head
        val mutatedGluonWBox: GluonWBox = GluonWBox.from(gluonWBox)
        val toUserBox: FundsToAddressBox = FundsToAddressBox(
          address = if (nodeConf.getNetworkType == NetworkType.MAINNET) ServiceConfig.mainNetServiceOwner else ServiceConfig.testNetServiceOwner,
          value = mutatedGluonWBox.value - 2*Parameters.MinFee,
          tokens = mutatedGluonWBox.tokens,
          R4 = Option(mutatedGluonWBox.totalSupplyRegister),
          R5 = Option(mutatedGluonWBox.tokenIdRegister),
          R6 = Option(mutatedGluonWBox.feeRegister),
          R7 = Option(mutatedGluonWBox.volumePlusRegister),
          R8 = Option(mutatedGluonWBox.volumeMinusRegister),
          R9 = Option(mutatedGluonWBox.lastDayBlockRegister))
//        val oracleBox: OracleBox = explorer.getOracleBox

        mutate(
          boxIdToMutate = boxIdToMutate,
          Seq(toUserBox),
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
          ),
        )

        consolidateBoxes(ergValue = ergValue, tokens = tokens)(
          client, conf, nodeConf
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
      .getCoveringBoxesFor(ownerAddress, ergValue + Parameters.MinFee, tokens.toList.asJava)

    val consolidatedBox: FundsToAddressBox = FundsToAddressBox(
      address = ownerAddress,
      value = ergValue,
      tokens = tokens
    )

    val tx: Tx = Tx(
      inputBoxes = coveringBoxes,
      outBoxes = Seq(consolidatedBox),
      changeAddress = ownerAddress,
    )(ctx)

    val signed: SignedTransaction = tx.signTxWithProver(prover)

    Seq(signed)
  }
}
