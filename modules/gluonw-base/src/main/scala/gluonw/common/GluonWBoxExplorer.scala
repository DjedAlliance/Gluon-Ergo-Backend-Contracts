package gluonw.common

import commons.node.{Client, MainNodeInfo}
import errors.ParseException
import explorer.Explorer
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox}
import play.api.libs.json.JsResultException

import javax.inject.Inject

class GluonWBoxExplorer @Inject() (implicit client: Client)
    extends IGluonWBoxExplorer {

  override def getGluonWBox: InputBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val gluonWAddress: Address = Address.create("")

        val gluonWBox: Seq[InputBox] =
          client.getAllUnspentBox(gluonWAddress)

        gluonWBox.head
      } catch {
        case e: ParseException    => throw ParseException(e.getMessage)
        case e: JsResultException => throw e
        case e: Throwable         => throw e
      }
    }
}

abstract class IGluonWBoxExplorer extends Explorer(nodeInfo = MainNodeInfo()) {
  def getGluonWBox: InputBox
}
