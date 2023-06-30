package gluonw.common

import commons.configs.GetOracleConfig
import commons.node.{Client, MainNodeInfo}
import errors.ParseException
import explorer.Explorer
import gluonw.boxes.{GluonWBox, GoldOracleBox}
import gluonw.contracts.GluonWBoxContract
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import play.api.libs.json.JsResultException

import javax.inject.Inject

class GluonWBoxExplorer @Inject() (implicit client: Client)
    extends IGluonWBoxExplorer {

  override def getGluonWBox: GluonWBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val gluonWAddress: Address =
          GluonWBoxContract.getContract()(ctx).contract.address

        val gluonWBox: Seq[InputBox] =
          client.getAllUnspentBox(gluonWAddress)

        GluonWBox.from(gluonWBox.head)
      } catch {
        case e: ParseException    => throw ParseException(e.getMessage)
        case e: JsResultException => throw e
        case e: Throwable         => throw e
      }
    }

  override def getGoldOracleBox: GoldOracleBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val oracleAddress: Address = GetOracleConfig.get()

        val oracleBoxes: Seq[InputBox] =
          client.getAllUnspentBox(oracleAddress)

        GoldOracleBox.from(oracleBoxes.head)
      } catch {
        case e: ParseException    => throw ParseException(e.getMessage)
        case e: JsResultException => throw e
        case e: Throwable         => throw e
      }
    }
}

abstract class IGluonWBoxExplorer extends Explorer(nodeInfo = MainNodeInfo()) {
  def getGluonWBox: GluonWBox
  def getOracleBox: GoldOracleBox
}
