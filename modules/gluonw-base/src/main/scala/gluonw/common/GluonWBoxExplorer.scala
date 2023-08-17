package gluonw.common

import commons.configs.{OracleConfig, TOracleConfig}
import commons.node.{Client, MainNodeInfo}
import edge.errors.ParseException
import edge.explorer.Explorer
import gluonw.boxes.{GluonWBox, OracleBox}
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

  override def getOracleBox: OracleBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val oracleConfig: TOracleConfig = OracleConfig.get()

        val oracleBoxes: Seq[InputBox] =
          client.getAllUnspentBox(oracleConfig.address)

        val priceOracleBoxes: Seq[InputBox] =
          oracleBoxes.filter(_.getRegisters.size() == 2)
        OracleBox.from(priceOracleBoxes.head)
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
}
