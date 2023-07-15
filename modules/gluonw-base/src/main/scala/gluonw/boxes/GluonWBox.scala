package gluonw.boxes

import boxes.{Box, BoxWrapperHelper, BoxWrapperJson}
import edge.registers.LongRegister
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
import registers.Register

import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

case class GluonWBox(
  value: Long,
  neutronsTotalSupplyRegister: LongRegister = new LongRegister(100000000L),
  protonsTotalSupplyRegister: LongRegister = new LongRegister(100000000L),
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

  def getNeutronsCirculatingSupply: Long =
    neutronsTotalSupplyRegister.value - Neutrons.getValue

  def getProtonsCirculatingSupply: Long =
    protonsTotalSupplyRegister.value - Protons.getValue

  override def R4: Option[Register[_]] = Option(neutronsTotalSupplyRegister)

  override def R5: Option[Register[_]] = Option(protonsTotalSupplyRegister)

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

  override def from(inputBox: InputBox): GluonWBox =
    GluonWBox(
      value = inputBox.getValue,
      id = inputBox.getId,
      tokens = inputBox.getTokens.asScala.toSeq,
      box = Option(Box(inputBox)),
      neutronsTotalSupplyRegister = new LongRegister(
        inputBox.getRegisters.get(0).getValue.asInstanceOf[Long]
      ),
      protonsTotalSupplyRegister = new LongRegister(
        inputBox.getRegisters.get(0).getValue.asInstanceOf[Long]
      )
    )
}
