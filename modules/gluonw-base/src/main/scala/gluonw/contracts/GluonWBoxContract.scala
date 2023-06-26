package gluonw.contracts

import commons.ErgCommons
import commons.contracts.ContractScripts
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoContract, ErgoId}

case class GluonWBoxContract(
  contract: Contract,
  minFee: Long
)

object GluonWBoxContract {

  def build(
    minFee: Long
  )(implicit ctx: BlockchainContext): GluonWBoxContract =
    GluonWBoxContract(
      Contract.build(
        ContractScripts.GluonWBoxGuardScript.contractScript
      ),
      minFee
    )

  def getContract()(implicit ctx: BlockchainContext): GluonWBoxContract =
    this.build(
      ErgCommons.MinBoxFee
    )
}
