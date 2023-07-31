package gluonw.boxes

import commons.configs.OracleConfig
import edge.boxes.{Box, BoxWrapperHelper, BoxWrapperJson}
import edge.registers.{
  CollBytePairRegister,
  IntRegister,
  LongPairRegister,
  LongRegister,
  Register
}
import gluonw.common.{AssetPrice, GluonWAsset, GluonWTokens}
import gluonw.contracts.GluonWBoxContract
import io.circe.Json
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  InputBox,
  Parameters
}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import special.collection.Coll

import scala.jdk.CollectionConverters.ListHasAsScala

object GluonWBoxConstants {
  val PRECISION: Long = 1_000_000_000L
  val TOTAL_CIRCULATING_SUPPLY: Long = 100_000_000L * PRECISION
  val PROTONS_TOTAL_CIRCULATING_SUPPLY: Long = TOTAL_CIRCULATING_SUPPLY
  val NEUTRONS_TOTAL_CIRCULATING_SUPPLY: Long = TOTAL_CIRCULATING_SUPPLY
  // This is the required fee for the box to be in existence. It's the minimum
  // amount a box require to have to exist on the ergo blockchain
  val GLUONWBOX_BOX_EXISTENCE_FEE: Long = Parameters.MinFee
}

case class GluonWBox(
  value: Long,
  totalSupplyRegister: LongPairRegister = new LongPairRegister(
    (
      GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY,
      GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY
    )
  ),
  tokenIdRegister: CollBytePairRegister = new CollBytePairRegister(
    (GluonWTokens.neutronId.getBytes, GluonWTokens.protonId.getBytes)
  ),
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    GluonWBoxContract.getContract().contract.ergoContract

  def Neutrons: ErgoToken = tokens.tail.head

  def Protons: ErgoToken = tokens.tail.tail.head

  def getNeutronsPrice(oracleBox: OracleBox): AssetPrice = AssetPrice(
    name = GluonWAsset.NEUTRON.toString,
    price = oracleBox.getPrice,
    id = GluonWTokens.neutronId
  )

  // @todo kii: Implement this
  def getProtonsPrice: AssetPrice = ???

  def neutronsTotalSupply: Long = totalSupplyRegister.value._1
  def protonsTotalSupply: Long = totalSupplyRegister.value._2

  def neutronsCirculatingSupply: Long =
    neutronsTotalSupply - Neutrons.getValue

  def protonsCirculatingSupply: Long =
    protonsTotalSupply - Protons.getValue

  def ergFissioned: Long = value - GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE

  override def R4: Option[Register[_]] = Option(totalSupplyRegister)

  override def R5: Option[Register[_]] = Option(tokenIdRegister)

  // @todo kii: do we need to Implement Neutrons and protons asset price?
  override def toJson(): Json =
    Json.fromFields(
      List(
        ("boxId", Json.fromString(this.id.toString)),
        (
          "assets",
          Json.fromFields(
            List(
              (
                "neutrons",
                Json.fromFields(
                  List(
                    ("tokenId", Json.fromString(Neutrons.getId.toString)),
                    (
                      "circulatingSupply",
                      Json.fromLong(neutronsCirculatingSupply)
                    )
                  )
                )
              ),
              (
                "protons",
                Json.fromFields(
                  List(
                    ("tokenId", Json.fromString(Protons.getId.toString)),
                    (
                      "circulatingSupply",
                      Json.fromLong(protonsCirculatingSupply)
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
}

case class OracleBox(
  value: Long,
  priceRegister: LongRegister,
  epochIdRegister: IntRegister,
  groupElementRegister: LongRegister,
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    Address.create(OracleConfig.get().address).toErgoContract

  def getPrice: Long = priceRegister.value

  def getEpochId: Int = epochIdRegister.value

  override def R4: Option[Register[_]] = Option(groupElementRegister)
  override def R6: Option[Register[_]] = Option(priceRegister)

  override def R5: Option[Register[_]] = Option(epochIdRegister)

  override def toJson(): Json =
    Json.fromFields(
      List(
        ("name", Json.fromString("SigGold")),
        ("tokenId", Json.fromString(tokens.tail.head.getId.toString)),
        ("epochId", Json.fromLong(getEpochId)),
        ("priceInNanoErgPerKg", Json.fromLong(getPrice))
      )
    )
}

object OracleBox extends BoxWrapperHelper {

  override def from(inputBox: InputBox): OracleBox =
    OracleBox(
      value = inputBox.getValue,
      id = inputBox.getId,
      tokens = inputBox.getTokens.asScala.toSeq,
      box = Option(Box(inputBox)),
      priceRegister = new LongRegister(
        inputBox.getRegisters.get(2).getValue.asInstanceOf[Long]
      ),
      epochIdRegister = new IntRegister(
        inputBox.getRegisters.get(1).getValue.asInstanceOf[Int]
      ),
      // We can't figure out how to serialize a groupElement data,
      // and since we don't need this, we're just going to put in
      // dummy data
      groupElementRegister = new LongRegister(
        1L
      )
    )
}

object GluonWBox extends BoxWrapperHelper {

  override def from(inputBox: InputBox): GluonWBox = {
    val tokenIdRegisterTuple: (Coll[Byte], Coll[Byte]) =
      inputBox.getRegisters
        .get(1)
        .getValue
        .asInstanceOf[(Coll[Byte], Coll[Byte])]

    GluonWBox(
      value = inputBox.getValue,
      id = inputBox.getId,
      tokens = inputBox.getTokens.asScala.toSeq,
      box = Option(Box(inputBox)),
      totalSupplyRegister = new LongPairRegister(
        inputBox.getRegisters.get(0).getValue.asInstanceOf[(Long, Long)]
      ),
      tokenIdRegister = new CollBytePairRegister(
        (tokenIdRegisterTuple._1.toArray, tokenIdRegisterTuple._2.toArray)
      )
    )
  }

  def create(
    neutronAmount: Long = GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY,
    protonAmount: Long = GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY,
    ergAmount: Long = GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE
  ): GluonWBox =
    GluonWBox(
      value = ergAmount,
      tokens = Seq(
        ErgoToken(GluonWTokens.gluonWBoxNFTId, 1),
        ErgoToken(GluonWTokens.neutronId, protonAmount),
        ErgoToken(GluonWTokens.protonId, neutronAmount)
      )
    )
}
