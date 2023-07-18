package gluonw.boxes

import edge.boxes.{Box, BoxWrapperHelper, BoxWrapperJson}
import edge.registers.{
  CollBytePairRegister,
  LongPairRegister,
  LongRegister,
  Register
}
import gluonw.common.{AssetPrice, GluonWAsset, GluonWTokens}
import gluonw.contracts.GluonWBoxContract
import io.circe.Json
import org.ergoplatform.appkit.{
  BlockchainContext,
  ErgoContract,
  ErgoId,
  ErgoToken,
  InputBox
}
import special.collection.Coll

import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

case class GluonWBox(
  value: Long,
  totalSupplyRegister: LongPairRegister = new LongPairRegister(
    (100000000L, 100000000L)
  ),
  tokenIdRegister: CollBytePairRegister = new CollBytePairRegister(
    (GluonWTokens.sigGoldId.getBytes, GluonWTokens.sigGoldRsvId.getBytes)
  ),
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    GluonWBoxContract.getContract().contract.ergoContract

  def Neutrons: ErgoToken = tokens.tail.head

  def Protons: ErgoToken = tokens.tail.tail.head

  def getNeutronsPrice(goldOracleBox: GoldOracleBox): AssetPrice = AssetPrice(
    name = GluonWAsset.SIGGOLD.toString,
    price = goldOracleBox.getGoldPrice,
    id = GluonWTokens.sigGoldId
  )

  def neutronsTotalSupply: Long = totalSupplyRegister.value._1
  def protonsTotalSupply: Long = totalSupplyRegister.value._2

  def getNeutronsCirculatingSupply: Long =
    neutronsTotalSupply - Neutrons.getValue

  def getProtonsCirculatingSupply: Long =
    protonsTotalSupply - Protons.getValue

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
                      Json.fromLong(getNeutronsCirculatingSupply)
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
                      Json.fromLong(getProtonsCirculatingSupply)
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

case class GoldOracleBox(
  value: Long,
  goldPriceRegister: LongRegister,
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    GluonWBoxContract.getContract().contract.ergoContract

  def getGoldPrice: Long = goldPriceRegister.value

  override def R4: Option[Register[_]] = Option(goldPriceRegister)

  override def toJson(): Json =
    Json.fromFields(
      List(
        ("name", Json.fromString("SigGold")),
        ("tokenId", Json.fromString(tokens.tail.head.getId.toString)),
        ("priceInNanoErgPerKg", Json.fromLong(getGoldPrice))
      )
    )
}

object GoldOracleBox extends BoxWrapperHelper {

  override def from(inputBox: InputBox): GoldOracleBox =
    GoldOracleBox(
      value = inputBox.getValue,
      id = inputBox.getId,
      tokens = inputBox.getTokens.asScala.toSeq,
      box = Option(Box(inputBox)),
      goldPriceRegister = new LongRegister(
        inputBox.getRegisters.get(0).getValue.asInstanceOf[Long]
      )
    )
}

object GluonWBox extends BoxWrapperHelper {

  override def from(inputBox: InputBox): GluonWBox = {
    val tokenIdRegisterTuple: (Coll[Byte], Coll[Byte]) =
      inputBox.getRegisters
        .get(0)
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
}
