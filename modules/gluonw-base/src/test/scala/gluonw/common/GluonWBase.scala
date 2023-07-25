package gluonw.common

import edge.commons.ErgCommons
import edge.node.{BaseClient, DefaultNodeInfo}
import edge.registers.{IntRegister, LongRegister, StringRegister}
import edge.tokens.Tokens
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{
  Address,
  ErgoContract,
  ErgoProver,
  InputBox,
  NetworkType,
  Parameters
}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

abstract class UnitSpec extends AnyWordSpec with Matchers {}

trait GluonWBase extends UnitSpec {
  val networkType: NetworkType = NetworkType.TESTNET

  // Set client in test that inherits
  val client: BaseClient = new BaseClient(
    nodeInfo = DefaultNodeInfo(networkType)
  )

  // contract: true
  val trueAddress: Address = Address.create("4MQyML64GnzMxZgm")

  // contract: HEIGHT < 4000000
  val heightSmallerThanXAddress: Address =
    Address.create("2fp75qcgMrTNR2vuLhiJYQt")

  // contract: true && false
  val trueAndFalseAddress: Address = Address.create("m3iBKr65o53izn")

  val exleDevAddress: Address =
    Address.create("9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn")

  val dummyAddress: Address = trueAddress

  val dummyTxId: String =
    "ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d"

  val minFee: Long = ErgCommons.MinMinerFee
  val oneErg: Long = Parameters.OneErg

  def dummyProver: ErgoProver =
    client.getClient.execute { ctx =>
      val prover = ctx
        .newProverBuilder()
        .withDLogSecret(BigInt.apply(0).bigInteger)
        .build()

      return prover
    }

  def buildUserBox(
    value: Long,
    index: Short = 0,
    address: Address = dummyAddress,
    txId: String = dummyTxId,
    networkType: NetworkType = NetworkType.MAINNET
  ): InputBox =
    client.getClient.execute { ctx =>
      val inputBox = ctx
        .newTxBuilder()
        .outBoxBuilder()
        .value(value)
        .contract(
          new ErgoTreeContract(address.getErgoAddress.script, networkType)
        )
        .build()
        .convertToInputWith(txId, index)

      return inputBox
    }

  /**
    * Create Payment Box
    *
    * creates a dummy payment box using ErgoContract
    * @param contract Proxy Contract
    * @param value Value of the input box
    * @return InputBox generated using dummy tx
    */
  def createPaymentBox(
    contract: ErgoContract = trueAddress.toErgoContract,
    value: Long = minFee,
    neutronsValue: Long = 0L,
    protonsValue: Long = 0L
  ): InputBox =
    client.getClient.execute { ctx =>
      val neutrons: ErgoToken =
        if (neutronsValue > 0)
          ErgoToken(GluonWTokens.neutronId, neutronsValue)
        else null

      val protons: ErgoToken =
        if (neutronsValue > 0)
          ErgoToken(GluonWTokens.protonId, protonsValue)
        else null

      val tokens: Seq[ErgoToken] = Seq(neutrons, protons)

      ctx
        .newTxBuilder()
        .outBoxBuilder()
        .contract(contract)
        .value(value)
        .tokens(tokens: _*)
        .build()
        .convertToInputWith(dummyTxId, 0)
    }

  def createGluonWBox: GluonWBox = GluonWBox.create()

  def createTestOracleBox: OracleBox =
    OracleBox(
      value = 10000000L,
      epochIdRegister = new IntRegister(1396),
      priceRegister = new LongRegister(52594551964068L),
      groupElementRegister = new StringRegister("random string"),
      tokens = Seq(
        new ErgoToken(
          ErgoId.create(
            "001e182cc3f04aec4486c7a5018d198e9591a7cfb0b372f5f95fa3e5ddbd24d3"
          ),
          1
        ),
        new ErgoToken(
          ErgoId.create(
            "56aeed3ba3f677ffb5462b0b1f83da3e1d06c8946ba978ef7e706221bac5e982"
          ),
          295
        )
      )
    )

  def genesisGluonWBox: GluonWBox = GluonWBox.create(
    protonAmount =
      GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY - (1 * GluonWBoxConstants.PRECISION).toLong,
    neutronAmount =
      GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY - (1 * GluonWBoxConstants.PRECISION).toLong,
    ergAmount = 6000 * Parameters.OneErg
  )

  def getManipulatedToken(
    tokens: Seq[ErgoToken],
    tokenIdToChange: ErgoId,
    amountToChange: Long
  ): Seq[ErgoToken] =
    tokens.map { token =>
      if (token.getId.equals(tokenIdToChange)) {
        ErgoToken(token.getId, token.value + amountToChange)
      } else token
    }

}
