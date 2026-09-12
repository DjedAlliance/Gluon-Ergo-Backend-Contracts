package gluonw.boxes

import commons.configs.MultiSig
import edge.registers.{AddressRegister, Register}
import org.ergoplatform.appkit.{Address, ErgoValue, NetworkType, SigmaProp}
import org.ergoplatform.sdk.JavaHelpers
import org.ergoplatform.sdk.JavaHelpers.SigmaDsl
import sigmastate.eval.CostingSigmaDslBuilder.proveDlog

class SingleAddressRegister(override val address: String)
    extends AddressRegister(address) {
  def this(registerData: Array[Byte]) = this(
    address = AddressRegister.getAddress(registerData).toString
  )
}

class SigmaPropRegister(override val value: SigmaProp) extends Register(value) {
  override def toErgoValue: Option[ErgoValue[_]] =
    Option(ErgoValue.of(value.getSigmaBoolean))
}

object SigmaPropRegister {

  def from(multiSig: MultiSig): SigmaProp = {
    val sigmaProps = multiSig.addresses.map { address =>
      proveDlog(address.getPublicKeyGE)
    }
    val collSigmaProps =
      JavaHelpers.SigmaDsl.Colls.fromArray(sigmaProps)

    val sigmaProp = SigmaDsl.atLeast(multiSig.bound, collSigmaProps)

    new SigmaProp(sigmaProp)
  }
}
