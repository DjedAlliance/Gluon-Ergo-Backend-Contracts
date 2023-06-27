package commons.boxes

import org.ergoplatform.appkit.{BlockchainContext, ErgoContract}
import scorex.crypto.hash.Digest32

trait Box

abstract class Contract {
  def getContract(ctx: BlockchainContext): ErgoContract

  def getContractScriptHash(ctx: BlockchainContext): Digest32 =
    Addresses.getContractScriptHash(getContract(ctx))
}

object Addresses {

  def getContractScriptHash(contract: ErgoContract): Digest32 =
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
}
