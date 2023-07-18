package gluonw.contracts

import commons.configs.GetServiceConfig
import edge.commons.ErgCommons
import commons.contracts.ContractScripts
import edge.contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext}

case class GluonWBoxContract(
  contract: Contract,
  ownerAddress: Address,
  minFee: Long
)

object GluonWBoxContract {

  def build(
    minFee: Long,
    serviceOwner: Address = GetServiceConfig.getServiceOwner()
  )(implicit ctx: BlockchainContext): GluonWBoxContract =
    GluonWBoxContract(
      Contract.build(
        ContractScripts.GluonWBoxGuardScript.contractScript,
        "_MinFee" -> minFee,
        "_MutatePk" -> serviceOwner.getPublicKey
      ),
      ownerAddress = serviceOwner,
      minFee
    )

  def getContract()(implicit ctx: BlockchainContext): GluonWBoxContract =
    this.build(
      ErgCommons.MinBoxFee,
      serviceOwner = GetServiceConfig.getServiceOwner()
    )
}
