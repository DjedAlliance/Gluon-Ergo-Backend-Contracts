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
    "oh8DrTMSUY8KLS2CK7JeWorwGDlk41puOXTTowV2pV62M1EAACfUZXMxdLwtSuZRL0M7JUZVKJrHA_cY5RLJv0h4jKbZAADqd6HeEpyLkO7RGaKYTnvbAQa8f_2-XDLGUNlM6-LPPQABAAQCAZ3gY33OKoNHtngjynrK19tgXLSpXGPnWQ4mxm-tx_qIBRyXObkOGn-2UBg-IzeXN1cCdpHJc7wfJ9mkh-aQ0opAtxBrdUcSxPxFqiqEW6ZSo9d5WqMimLe0U2IwhtSumhQA-tkFqnIQWQCUGTsnQ7FjFJ7ulcDurIcy-mn7W_d8RAAU3-A8KljWBOjY4fxzto-sdRLwZy2OCGoa9NDEAbbh_7WHtNHZsH2OEE09uZz6LKS76VQH5hRvyJquyXoJ8ewFwNCgk5gHEGsEAAQCBAIEBAQEBAAEAAjNA1UjDLI_nmpe8FV4tzqGrfYrwVUV0dFyj8G0KRjp-z-YBgQ7msoABgFkBYCJegYCA-gEAAYBQgYBBAYBAAYBBQ7UBBAjBAAEAAQCBAIEAgQADiAEPqEvA3aXSOQ2wAOIbEVd3xp81Kr70hRgKCLVIT9OaAQEBAAEAAQABAIOIFau7Tuj9nf_tUYrCx-D2j4dBsiUa6l4735wYiG6xemCBQAEBAXIAQXSAQQABAIEBAQEBAAEAAQABAAOINlL-sQLUWNTmDRDIJEE3N1bfKIyoBzLN27oAU32MwkHBAAEAgQCBAIEPAQADiAAHhgsw_BK7ESGx6UBjRmOlZGnz7CzcvX5X6Pl3b0k0wEABATYAdYB5OMABJWTcgFzANgI1gKypHMBANYD22MIcgLWBLKlcwIA1gXbYwhyBNYG22MIp9YHmYyycgVzAwACjLJyBnMEAALWCJnBp8FyBNYJwXIC0e3t7ZOMsnIDcwUAAXMGr7SlcwexpdkBCmOTsdtjCHIKcwjtk4yycgVzCQABjLJyBnMKAAGTjLJyBXMLAAFzDO3tkXIHcw2QcgicnZydcgmMsnIDcw4AAnIHcw9zEJOZwbKlcxEAcglyCJWTcgFzEtgB1gKypXMTANHt7ZPbYwhyAttjCKeTwnICwqePwafBcgLYBNYCsqVzFADWA9tjCKfWBLKkcxUA1gXbYwhyBNHt7e2TsttjCHICcxYAsnIDcxcAk8JyAsKnk8GnwXIC7ZOMsnIFcxgAAXMZkoyy22MIsqVzGgBzGwACmZqMsnIFcxwAAoyycgNzHQACfpyxtaTZAQZjlebGcgYGBe3tkozHcgYBmaNzHpOMsttjCHIGcx8AAXMgk-TGcgYFBOTGcgQFBHMhcyIFBgEBAgEEAAQCBAQEBgYDD0JAAQEEBgQIBgMPQkABAQEBBAIEAA4g_7WHtNHZsH2OEE09uZz6LKS76VQH5hRvyJquyXoJ8ewGAw9CQAEBAQEFAAEBAQEEoAsEAAQABBwGAQIBAQQcBAAFAAQABBwEAAQCBBwEAgQcBAIFAAQcBAAEAAUABBwEAAQCBBwEAgQcBAIFAASgCwSgCwSgCwQcBBwEAAQABBwBAQYBAgQcBAAFAAQABBwEAAQCBBwEAgQcBAIFAAQcBAAEAAUABBwEAAQCBBwEAgQcBAIFAASgCwSgCwEA2CPWAdtjCKfWArKlcwAA1gPbYwhyAtYEsnIBcwEA1gWycgNzAgDWBrJyAXMDANYHsnIDcwQA1gjkxqcEWdYJ5ManBlnWCoxyCQLWC4zkxnICBlkC1gyWgwcBk4yycgFzBQABjLJyA3MGAAGTjHIEAYxyBQGTjHIGAYxyBwGTwqfCcgKTcgjkxnICBFmT5ManBTwODuTGcgIFPA4Ok3IKcgvWDYxyBALWDoxyBQLWD5FyDXIO1hCMcgYC1hGMcgcC1hKRchByEdYTwafWFMFyAtYVloMEAXIMcg9yEo9yE3IU1haPcg1yDtYXj3IQchHWGJaDBAFyDHIWcheRchNyFNYZk3ITchTWGpaDBAFyDHIPchdyGdYbloMEAXIMchZyEnIZ1hxzB9YdjHIJAdYe5ManBxHWH-TGcgIHEdYg5ManCBHWIeTGcgIIEdYi5ManCQXWI-TGcgIJBZWXgwQBchVyGHIachvYHdYkfpmMcggBcg0G1iVzCNYmcwnWJ36ZchNzCgbWKH6ZjHIIAnIQBtYp4wAI1ipzC9YrnX7kxrLbZQH-cwwABAUGcirWLKGdnHMNciVyJp2cciRyK3In1i2ZciVyLNYu2QEuBZ2cfnIuBp2cci1yJ3IociXWL9kBLwWdnH5yLwZyK3Il1jCVchV-mXIUchMGlXIYfplyE3IUBpVyGtpyLgGZchFyENpyLwGZcg5yDdYxnZxzDnIwcirWMnMP1jOGAtByHJWPch1yCp2cnZxzEHIwcip-mXIKch0GfnIKBnIy1jSGAnMRnZxzEnIwcirWNYYCgwECcxNyMtY2lexyG3IaleZyKYMDTg5yM3I0hgLQ5HIpcjGDA04OcjNyNHI1leZyKYMDTg5yM4YC0ORyKXIxcjWDA04OcjNyNXI11jeycjZzFADWOIxyNwLWObJyNnMVANY6jHI5AtY7sqVzFgDWPIxyOQHWPZFyOnIy1j6WgwIB7e2VkXI4cjLYAdY-lZByOnIycjuypXMXAJaDAgGTwnI-jHI3AZN-wXI-BppyOHMYcxmV5nIplXI92AHWPpWQcjpyMrKlcxoAsqVzGwCWgwIBk8JyPnI8k37Bcj4GmnI6cxxzHXMelexyGnIblXI92ALWPsJyO9Y_sqSZsaRzHwCWgwQBk3I-cjyTcj7Ccj-TjLLbYwhyO3MgAAFzIZN-wXI7BpqafsFyPwZyOnMicyNzJJNyC3IK1j-WgwMBk3Iech-TciByIZNyInIj1kDZAUARfrByQHMl2QFCWZqMckIBjHJCAgaVchXYAtZBfplyFHITBtZCmXIlnXIlcibRloMGAXIMk36Zcg1yDgadnZycckFyJHJCcidyJZN-mXIQchEGnZ2cnHJBcihyQnInciVzJnI-cj-VchjYAtZBfplyE3IUBtZCnHInmXIlnXIlcibRloMGAXIMk36Zcg5yDQadnJxyQXIkciVyQpN-mXIRchAGnZycckFyKHIlckJzJ3I-cj-VchrYB9ZBmXIRchDWQtpyQAFyIdZD2nJAAXIf1kR9nZl-owVyIn5zKAUE1kWTckRzKdZGkXJEcyrWR5lzK3JE0ZaDCAFyDJN-mXINcg4GnZydnH5yQQaZciWanXIlciadnJ1yJXMslZFyQnJDcjKZckNyQnInciidnHItciRyJXIscy2TfnIUBn5yEwZyPpaDBAGTsXIhcy6TlXJFsnIgcy8AczCyciFzMQCVckaTtHIhckRzMrRyIHMzckeTtHIhczRzNbRyIHM2czevtHIhczhyRNkBSAWTckhzOZaDBAGTsXIfczqTfrJyH3M7AAaafpVyRbJyHnM8AHM9BtpyLgFyQZVyRpO0ch9yRHM-tHIecz9yR5O0ch9zQHNBtHIec0JzQ6-0ch9zRHJE2QFIBZNySHNFk3Ijfpydo3NGc0cFlXIb2AjWQZlyDnIN1kLackABch_WQ9pyQAFyIdZEfZ2ZfqMFciJ-c0gFBNZFlZJyRHNJc0pyRNZGk3JFc0vWR5FyRXNM1kiZc01yRdGWgwgBcgxzTpN-mXIQchEGnZydnH5yQQaZciWanXIlciadnJ1yJXNPlZFyQnJDcjKZckNyQnInciSdnHIscihyJXItk35yFAZ-chMGcj6WgwQBk7FyH3NQk5VyRrJyHnNRAHNSsnIfc1MAlXJHk7RyH3JFc1S0ch5zVXJIk7RyH3NWc1e0ch5zWHNZr7RyH3NackXZAUkFk3JJc1uWgwQBk7FyIXNck36yciFzXQAGmn6VckayciBzXgBzXwbaci8BckGVckeTtHIhckVzYLRyIHNhckiTtHIhc2JzY7RyIHNkc2WvtHIhc2ZyRdkBSQWTcklzZ5NyI36cnaNzaHNpBdFzanIc0Ko_AwABAaTgn9mCr9GxAQKItrqOga_RsQEGWYCA0NiL3qLjAoCA0NiL3qLjAjwODiC3EGt1RxLE_EWqKoRbplKj13laoyKYt7RTYjCG1K6aFCAA-tkFqnIQWQCUGTsnQ7FjFJ7ulcDurIcy-mn7W_d8RFm0nJe3C4CAgr-T7_AIEQ68iJIDAAAAAAAAAAAAAAAAguHMl9ABEQ4AAAAAAAAAAAAAAAAAAAWg03652f3GlYYBAAjNA1UjDLI_nmpe8FV4tzqGrfYrwVUV0dFyj8G0KRjp-z-Y0Ko_AwHQqbX3AgPd-bWVTgLs05rCBAC-yuTdAxAjBAAEAAQCBAIEAgQADiAEPqEvA3aXSOQ2wAOIbEVd3xp81Kr70hRgKCLVIT9OaAQEBAAEAAQABAIOIFau7Tuj9nf_tUYrCx-D2j4dBsiUa6l4735wYiG6xemCBQAEBAXIAQXSAQQABAIEBAQEBAAEAAQABAAOINlL-sQLUWNTmDRDIJEE3N1bfKIyoBzLN27oAU32MwkHBAAEAgQCBAIEPAQADiAAHhgsw_BK7ESGx6UBjRmOlZGnz7CzcvX5X6Pl3b0k0wEABATYAdYB5OMABJWTcgFzANgI1gKypHMBANYD22MIcgLWBLKlcwIA1gXbYwhyBNYG22MIp9YHmYyycgVzAwACjLJyBnMEAALWCJnBp8FyBNYJwXIC0e3t7ZOMsnIDcwUAAXMGr7SlcwexpdkBCmOTsdtjCHIKcwjtk4yycgVzCQABjLJyBnMKAAGTjLJyBXMLAAFzDO3tkXIHcw2QcgicnZydcgmMsnIDcw4AAnIHcw9zEJOZwbKlcxEAcglyCJWTcgFzEtgB1gKypXMTANHt7ZPbYwhyAttjCKeTwnICwqePwafBcgLYBNYCsqVzFADWA9tjCKfWBLKkcxUA1gXbYwhyBNHt7e2TsttjCHICcxYAsnIDcxcAk8JyAsKnk8GnwXIC7ZOMsnIFcxgAAXMZkoyy22MIsqVzGgBzGwACmZqMsnIFcxwAAoyycgNzHQACfpyxtaTZAQZjlebGcgYGBe3tkozHcgYBmaNzHpOMsttjCHIGcx8AAXMgk-TGcgYFBOTGcgQFBHMhcyIF0Ko_AQQBAJOFPgAIzQNVIwyyP55qXvBVeLc6hq32K8FVFdHRco_BtCkY6fs_mNCqPwAAwIQ9EAUEAAQADjYQAgSgCwjNAnm-Zn753LusVaBilc6HCwcCm_zbLc4o2VnygVsW-BeY6gLRkqOajMenAXMAcwEQAQIEAtGWgwMBk6OMx7KlcwAAAZPCsqVzAQB0cwJzA4MBCM3urJOxpXME0Ko_AADT-pkJzQNVIwyyP55qXvBVeLc6hq32K8FVFdHRco_BtCkY6fs_mM-aCdPS6QzS6Qw="
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
    val runTx: String = SIGN_REDUCED

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
