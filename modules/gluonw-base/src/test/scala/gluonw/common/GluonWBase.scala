package gluonw.common

import edge.commons.ErgCommons
import edge.node.{BaseClient, DefaultNodeInfo}
import edge.registers.{
  GroupElementRegister,
  IntRegister,
  LongRegister,
  StringRegister
}
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
import org.ergoplatform.settings.ErgoAlgos
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scorex.util.serialization.Reader
import sigmastate.SGroupElement
import sigmastate.Values.Constant
import sigmastate.lang.DeserializationSigmaBuilder
import sigmastate.serialization.{
  ConstantSerializer,
  ConstantStore,
  DataSerializer,
  SigmaSerializer
}
import sigmastate.utils.SigmaByteReader
import special.sigma.GroupElement

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
    value: Long = minFee * 2,
    neutronsValue: Long = 0L,
    protonsValue: Long = 0L
  ): InputBox =
    client.getClient.execute { ctx =>
      val neutrons: Option[ErgoToken] =
        if (neutronsValue > 0)
          Option(ErgoToken(GluonWTokens.neutronId, neutronsValue))
        else None

      val protons: Option[ErgoToken] =
        if (protonsValue > 0)
          Option(ErgoToken(GluonWTokens.protonId, protonsValue))
        else None

      val tokens: Seq[ErgoToken] = Seq(neutrons, protons).flatten

      if (tokens.isEmpty) {
        ctx
          .newTxBuilder()
          .outBoxBuilder()
          .contract(contract)
          .value(value)
          .build()
          .convertToInputWith(dummyTxId, 0)
      } else {
        ctx
          .newTxBuilder()
          .outBoxBuilder()
          .contract(contract)
          .value(value)
          .tokens(tokens: _*)
          .build()
          .convertToInputWith(dummyTxId, 0)
      }
    }

  def createGluonWBox: GluonWBox = GluonWBox.create()

  def createTestOracleBox: OracleBox = {
    val groupElementByteArray: String =
      "0702585f76d59500ba217152083e11aeabafd3ad7678e093ab26ab25d623ffcefe09"
//    val S = ConstantSerializer(DeserializationSigmaBuilder)
//    val c = S.deserialize(SigmaSerializer.startReader(groupElementByteArray.getBytes()))
    OracleBox(
      value = 10000000L,
      epochIdRegister = new IntRegister(1396),
      priceRegister = new LongRegister(52594551964068L),
      groupElementRegister = new LongRegister(
        1L
      ),
      tokens = Seq(
        ErgoToken(
          ErgoId.create(
            "001e182cc3f04aec4486c7a5018d198e9591a7cfb0b372f5f95fa3e5ddbd24d3"
          ),
          1
        ),
        ErgoToken(
          ErgoId.create(
            "56aeed3ba3f677ffb5462b0b1f83da3e1d06c8946ba978ef7e706221bac5e982"
          ),
          295
        )
      )
    )
  }

  def genesisGluonWBox(
    ergAmount: Long = 200_000L,
    neutronAmount: Long = 1_000L,
    protonAmount: Long = 1_000L
  ): GluonWBox = GluonWBox.create(
    protonAmount =
      GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY - (protonAmount * GluonWBoxConstants.PRECISION).toLong,
    neutronAmount =
      GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY - (neutronAmount * GluonWBoxConstants.PRECISION).toLong,
    ergAmount = ergAmount * Parameters.OneErg + Parameters.MinFee
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
