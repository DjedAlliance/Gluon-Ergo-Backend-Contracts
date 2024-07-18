package gluonw.boxes

import commons.configs.OracleConfig
import commons.node.Client
import edge.boxes.{Box, BoxWrapper, BoxWrapperHelper}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ContextVar,
  ErgoContract,
  InputBox,
  Parameters
}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

case class OracleBuybackBox(
  value: Long,
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null),
  override val tokens: Seq[ErgoToken] = Seq(OracleConfig.get().paymentNft)
) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    OracleConfig.get().paymentAddress.toErgoContract
}

object OracleBuybackBox extends BoxWrapperHelper {

  override def from(inputBox: InputBox): OracleBuybackBox =
    OracleBuybackBox(
      value = inputBox.getValue,
      id = inputBox.getId,
      box = Option(Box(inputBox)),
      tokens = inputBox.getTokens.toSeq
    )

  /**
    * 1 is top up route
    * @param inputBox
    * @return
    */
  def setTopUp(inputBox: InputBox): InputBox =
    inputBox.withContextVars(ContextVar.of(0.toByte, 1))

  def testBox(
    value: Long = Parameters.OneErg,
    id: ErgoId = ErgoId.create("")
  ): OracleBuybackBox =
    OracleBuybackBox(
      value = value,
      id = id
    )
}
