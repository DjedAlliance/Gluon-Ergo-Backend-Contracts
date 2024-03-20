package gluonw.common

import commons.configs.{OracleConfig, TOracleConfig}
import commons.node.{Client, MainNodeInfo}
import edge.errors.ParseException
import edge.explorer.Explorer
import edge.json.BoxData
import gluonw.boxes.{BoxHelper, GluonWBox, OracleBox, OracleBuybackBox}
import org.ergoplatform.appkit.{BlockchainContext, InputBox}
import play.api.libs.json.JsResultException

import javax.inject.Inject
import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class GluonWBoxExplorer @Inject() (implicit client: Client)
    extends IGluonWBoxExplorer {

  /**
    * @TODO kii
    * Make GluonWBox and BuyBackBox retrieve mempool
    * Also make sure the box that is retrieved is the right box
    * 2. Get GluonBox with NFTID and BuyBackBox via NFTID
    * 3. Check and Get GluonBox and BuyBackBox from mempool
    * 4. Integrate get BuyBackBox and GluonBox from mempool else get from confirmed.
    * @return
    */
  override def getGluonWBox: GluonWBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val gluonNFTId = GluonWTokens.gluonWBoxNFTId.toString()
        val mempoolGluonBox =
          getMempoolBoxesByTokenIdAsInputBoxes(gluonNFTId, ctx)
        val gluonWBox: InputBox = if (mempoolGluonBox.nonEmpty) {
          mempoolGluonBox.head
        } else {
          // Currently returns Json, change it to inputBox
          val blockchainBox = getUnspentTokenBoxes(gluonNFTId)
          val boxId: String = BoxHelper.getIdFromJson(blockchainBox)
          ctx.getBoxesById(boxId).head
        }

        GluonWBox.from(gluonWBox)
      } catch {
        case e: ParseException    => throw ParseException(e.getMessage)
        case e: JsResultException => throw e
        case e: Throwable         => throw e
      }
    }

  def getOracleBuybackBox: OracleBuybackBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val buybackBoxNFTId: String =
          OracleConfig.get().paymentNft.id.toString()
        val mempoolBuybackBox: Seq[InputBox] =
          getMempoolBoxesByTokenIdAsInputBoxes(buybackBoxNFTId, ctx)
        val gluonWBox: InputBox = if (mempoolBuybackBox.nonEmpty) {
          mempoolBuybackBox.head
        } else {
          // Currently returns Json, change it to inputBox
          val blockchainBox = getUnspentTokenBoxes(buybackBoxNFTId)
          val boxId: String = BoxHelper.getIdFromJson(blockchainBox)
          ctx.getBoxesById(boxId).head
        }

        OracleBuybackBox.from(gluonWBox)
      } catch {
        case e: ParseException    => throw ParseException(e.getMessage)
        case e: JsResultException => throw e
        case e: Throwable         => throw e
      }
    }

  override def getOracleBox: OracleBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val oracleConfig: TOracleConfig = OracleConfig.get()

        val oracleBoxesJson =
          getUnspentTokenBoxes(oracleConfig.nft.id.toString())
        val oracleBox = OracleBox.from(oracleBoxesJson)
        val oracleInputBox: InputBox =
          ctx.getBoxesById(oracleBox.id.toString()).head

        OracleBox.from(oracleInputBox)
      } catch {
        case e: ParseException    => throw ParseException(e.getMessage)
        case e: JsResultException => throw e
        case e: Throwable         => throw e
      }
    }

}

abstract class IGluonWBoxExplorer extends Explorer(nodeInfo = MainNodeInfo()) {

  def getGluonWBox: GluonWBox
  def getOracleBox: OracleBox
  def getOracleBuybackBox: OracleBuybackBox
}
