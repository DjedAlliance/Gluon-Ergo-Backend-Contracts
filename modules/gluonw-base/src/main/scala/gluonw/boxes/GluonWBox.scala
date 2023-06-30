package gluonw.boxes

import boxes.{Box, BoxWrapper, BoxWrapperHelper, BoxWrapperJson}
import gluonw.common.AssetRate
import gluonw.contracts.GluonWBoxContract
import io.circe.Json
import org.ergoplatform.appkit.{
  BlockchainContext,
  ErgoContract,
  ErgoId,
  ErgoToken,
  InputBox
}

case class GluonWBox(
  value: Long,
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    GluonWBoxContract.getContract().contract.ergoContract

  def getRsvRate: AssetRate = ???

  def getRsvAmount: Long = ???

  def getSigGoldCirculatingSupply: Long = ???

  def getSigGoldRsvCirculatingSupply: Long = ???

  override def toJson(): Json =
    Json.fromFields(
      List()
    )
}

case class GoldOracleBox(
  value: Long,
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapperJson {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    GluonWBoxContract.getContract().contract.ergoContract

  def getGoldRate: AssetRate = ???

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
