package controllers

import commons.node.Client
import edge.pay.ErgoPayResponse
import errors.ExceptionThrowable
import gluonw.common.{GluonW, GluonWBoxExplorer, TxConverter}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.ergoplatform.appkit.Address
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc._
import txs.Tx

import javax.inject.Inject

/**
  * common.GluonW Controller trait
  */
trait TGluonWController {

  /**
    * Price of gold against erg
    * @return
    */
  def goldPrice(): Action[AnyContent]

  /**
    * Price of Rsv against erg
    * @return
    */
  def rsvPrice(): Action[AnyContent]

  /**
    * Erg to SigGold and SigGoldRsv
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  def fission(ergAmount: Long): Action[Json]

  /**
    * Erg to SigGold and SigGoldRsv Rate
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  def fissionRate(ergAmount: Long): Action[AnyContent]

  /**
    * SigGold to Rsv
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  def transmuteSigGoldToSigGoldRsv(goldAmount: Long): Action[Json]

  /**
    * SigGold to Rsv rate
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  def transmuteSigGoldToSigGoldRsvRate(goldAmount: Long): Action[AnyContent]

  /**
    * SigGoldRsv to SigGold
    * @param rsvAmount the amount of SigGoldRsv to be converted
    * @return
    */
  def transmuteSigGoldRsvToSigGold(rsvAmount: Long): Action[Json]

  /**
    * SigGoldRsv to SigGold rate
    * @param rsvAmount the amount of SigGoldRsv to be converted
    * @return
    */
  def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): Action[AnyContent]

  /**
    * Mint SigGold
    * @param ergAmount the Erg amount to convert to sigGold
    * @return
    */
  def mintSigGold(ergAmount: Long): Action[Json]

  /**
    * Mint SigGold rate
    * @param ergAmount the Erg amount to convert to SigGold
    * @return
    */
  def mintSigGoldRate(ergAmount: Long): Action[AnyContent]

  /**
    * Redeem SigGold for Erg
    * @param goldAmount the amount of SigGold to be redeemed
    * @return
    */
  def redeemSigGold(goldAmount: Long): Action[Json]

  /**
    * Redeem SigGold for Erg rate
    * @param goldAmount the amount of SigGold to be redeemed
    * @return
    */
  def redeemSigGoldRate(goldAmount: Long): Action[AnyContent]

  /**
    * Mint SigGoldRsv
    * @param ergAmount the Erg amount to convert to SigGoldRsv
    * @return
    */
  def mintSigGoldRsv(ergAmount: Long): Action[Json]

  /**
    * Mint SigGoldRsv rate
    * @param ergAmount the Erg amount to convert to SigGoldRsv
    * @return
    */
  def mintSigGoldRsvRate(ergAmount: Long): Action[AnyContent]

  /**
    * Redeem SigGoldRsv for Erg
    * @param rsvAmount the amount of SigGoldRsv to be redeemed
    * @return
    */
  def redeemSigGoldRsv(rsvAmount: Long): Action[Json]

  /**
    * Redeem SigGoldRsv for Erg rate
    * @param rsvAmount the amount of SigGoldRsv to be redeemed
    * @return
    */
  def redeemSigGoldRsvRate(rsvAmount: Long): Action[AnyContent]
}

class GluonWController @Inject() (
  val client: Client,
  val gluonW: GluonW,
  val gluonWBoxExplorer: GluonWBoxExplorer,
  val controllerComponents: ControllerComponents
) extends BaseController
    with Circe
    with ExceptionThrowable
    with TGluonWController
    with TxConverter {
  private val logger: Logger = Logger(this.getClass)

  def TxCall(
    request: Request[Json],
    assetAmount: Long,
    txFunc: (Long, Address) => Seq[Tx]
  ): Json = {
    // Get the wallet address from the request body
    val walletAddress: Address =
      Address.create(getRequestBodyAsString(request, "walletAddress"))

    // Set up fission tx and get response
    val fissionTxs: Seq[Tx] = txFunc(assetAmount, walletAddress)
    // Send ergoPayResponse back
    val ergoPayResponses: Seq[ErgoPayResponse] =
      convert(fissionTxs, walletAddress)

    Json.fromValues(ergoPayResponses.map(r => r.asJson))
  }

  override def goldPrice(): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonWBoxExplorer.getGoldOracleBox.getGoldRate.toJson)
        .as("application/json")
    }

  override def rsvPrice(): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        gluonWBoxExplorer.getGluonWBox.getRsvRate.toJson
      ).as("application/json")
    }

  /**
    * Erg to SigGold and SigGoldRsv
    *
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  override def fission(ergAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      try {
        Ok(TxCall(request, ergAmount, gluonW.fission)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
    }

  /**
    * Erg to SigGold and SigGoldRsv Rate
    *
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  override def fissionRate(ergAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonW.fissionRate(ergAmount).toJson).as("application/json")
    }

  /**
    * SigGold to Rsv
    *
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  override def transmuteSigGoldToSigGoldRsv(goldAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, goldAmount, gluonW.transmuteSigGoldToSigGoldRsv))
        .as("application/json")
    }

  /**
    * SigGold to Rsv rate
    *
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  override def transmuteSigGoldToSigGoldRsvRate(
    goldAmount: Long
  ): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(gluonW.transmuteSigGoldToSigGoldRsvRate(goldAmount).toJson)
      .as("application/json")
  }

  /**
    * SigGoldRsv to SigGold
    *
    * @param rsvAmount the amount of SigGoldRsv to be converted
    * @return
    */
  override def transmuteSigGoldRsvToSigGold(rsvAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, rsvAmount, gluonW.transmuteSigGoldRsvToSigGold))
        .as("application/json")
    }

  /**
    * SigGoldRsv to SigGold rate
    *
    * @param rsvAmount the amount of SigGoldRsv to be converted
    * @return
    */
  override def transmuteSigGoldRsvToSigGoldRate(
    rsvAmount: Long
  ): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonW.transmuteSigGoldRsvToSigGoldRate(rsvAmount).toJson)
        .as("application/json")
    }

  /**
    * Mint SigGold
    *
    * @param ergAmount the Erg amount to convert to sigGold
    * @return
    */
  override def mintSigGold(ergAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, ergAmount, gluonW.mintSigGold)).as("application/json")
    }

  /**
    * Mint SigGold rate
    *
    * @param ergAmount the Erg amount to convert to SigGold
    * @return
    */
  override def mintSigGoldRate(ergAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonW.mintSigGoldRate(ergAmount).toJson).as("application/json")
    }

  /**
    * Redeem SigGold for Erg
    *
    * @param goldAmount the amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGold(goldAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, goldAmount, gluonW.redeemSigGold))
        .as("application/json")
    }

  /**
    * Redeem SigGold for Erg rate
    *
    * @param goldAmount the amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGoldRate(goldAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonW.redeemSigGoldRate(goldAmount).toJson).as("application/json")
    }

  /**
    * Mint SigGoldRsv
    *
    * @param ergAmount the Erg amount to convert to SigGoldRsv
    * @return
    */
  override def mintSigGoldRsv(ergAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, ergAmount, gluonW.mintSigGoldRsv))
        .as("application/json")
    }

  /**
    * Mint SigGoldRsv rate
    *
    * @param ergAmount the Erg amount to convert to SigGoldRsv
    * @return
    */
  override def mintSigGoldRsvRate(ergAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonW.mintSigGoldRsvRate(ergAmount).toJson).as("application/json")
    }

  /**
    * Redeem SigGoldRsv for Erg
    *
    * @param rsvAmount the amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsv(rsvAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, rsvAmount, gluonW.redeemSigGoldRsv))
        .as("application/json")
    }

  /**
    * Redeem SigGoldRsv for Erg rate
    *
    * @param rsvAmount the amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsvRate(rsvAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(gluonW.redeemSigGoldRsvRate(rsvAmount).toJson).as("application/json")
    }
}
