package gluonw.boxes

import boxes.{Box, BoxWrapper, BoxWrapperHelper, BoxWrapperJson}
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

  override def toJson(): Json =
    Json.fromFields(
      List()
    )
}

object GluonWBox extends BoxWrapperHelper {
  override def from(inputBox: InputBox): GluonWBox = ???
}
