package gluonw.common

import commons.node.Client
import io.circe.Json

import javax.inject.Inject

class GluonW @Inject() (
  client: Client
) {

  /**
    * Fission Tx
    * @return
    */
  def fission(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * Fusion Tx
    * @return
    */
  def fusion(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * Beta Decay Plus
    * @return
    */
  def transmuteSigGoldToSigGoldRsv(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * Beta Decay Minus
    * @return
    */
  def transmuteSigGoldRsvToSigGold(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * SigGold to Erg
    * BetaDecay Plus -> Fusion
    * @return
    */
  def redeemSigGold(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * Erg to SigGold
    * Fission -> BetaDecay Minus
    * @return
    */
  def mintSigGold(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * SigGoldRsv to Erg
    * BetaDecay Minus -> Fusion
    * @return
    */
  def redeemSigGoldRsv(): Json =
    Json.fromFields(
      List(
        )
    )

  /**
    * Erg to SigGoldRsv
    * Fission -> BetaDecay Plus
    * @return
    */
  def mintSigGoldRsv(): Json =
    Json.fromFields(
      List(
        )
    )
}
