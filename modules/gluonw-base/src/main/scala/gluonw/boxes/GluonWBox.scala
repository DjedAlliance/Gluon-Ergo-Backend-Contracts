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

  /**
    * Get price of Protons.
    * This value can be retrieved via algorithm?
    * @return
    */
  def getProtonsPrice: AssetPrice = ???

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

  // @todo kii: Implement Neutrons and protons asset price.
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
  GoldPriceRegister: LongRegister,
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    GluonWBoxContract.getContract().contract.ergoContract

  def getGoldPrice: Long = GoldPriceRegister.value

  override def R4: Option[Register[_]] = Option(GoldPriceRegister)

  override def toJson(): Json =
    Json.fromFields(
      List()
    )
}

object GoldOracleBox extends BoxWrapperHelper {
  override def from(inputBox: InputBox): GoldOracleBox = ???
}

object GluonWBox extends BoxWrapperHelper {
  override def from(inputBox: InputBox): GluonWBox = ???
}
