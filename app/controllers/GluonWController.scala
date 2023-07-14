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
    * Erg to Neutrons and Protons
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  def fission(ergAmount: Long): Action[Json]

  /**
    * Erg to Neutrons and Protons Price
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  def fissionPrice(ergAmount: Long): Action[AnyContent]

  /**
    * Neutrons to Rsv
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  def transmuteNeutronsToProtons(goldAmount: Long): Action[Json]

  /**
    * Neutrons to Rsv rate
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  def transmuteNeutronsToProtonsPrice(goldAmount: Long): Action[AnyContent]

  /**
    * Protons to Neutrons
    * @param rsvAmount the amount of Protons to be converted
    * @return
    */
  def transmuteProtonsToNeutrons(rsvAmount: Long): Action[Json]

  /**
    * Protons to Neutrons rate
    * @param rsvAmount the amount of Protons to be converted
    * @return
    */
  def transmuteProtonsToNeutronsPrice(rsvAmount: Long): Action[AnyContent]

  /**
    * Mint Neutrons
    * @param ergAmount the Erg amount to convert to sigGold
    * @return
    */
  def mintNeutrons(ergAmount: Long): Action[Json]

  /**
    * Mint Neutrons rate
    * @param ergAmount the Erg amount to convert to Neutrons
    * @return
    */
  def mintNeutronsPrice(ergAmount: Long): Action[AnyContent]

  /**
    * Redeem Neutrons for Erg
    * @param goldAmount the amount of Neutrons to be redeemed
    * @return
    */
  def redeemNeutrons(goldAmount: Long): Action[Json]

  /**
    * Redeem Neutrons for Erg rate
    * @param goldAmount the amount of Neutrons to be redeemed
    * @return
    */
  def redeemNeutronsPrice(goldAmount: Long): Action[AnyContent]

  /**
    * Mint Protons
    * @param ergAmount the Erg amount to convert to Protons
    * @return
    */
  def mintProtons(ergAmount: Long): Action[Json]

  /**
    * Mint Protons rate
    * @param ergAmount the Erg amount to convert to Protons
    * @return
    */
  def mintProtonsPrice(ergAmount: Long): Action[AnyContent]

  /**
    * Redeem Protons for Erg
    * @param rsvAmount the amount of Protons to be redeemed
    * @return
    */
  def redeemProtons(rsvAmount: Long): Action[Json]

  /**
    * Redeem Protons for Erg rate
    * @param rsvAmount the amount of Protons to be redeemed
    * @return
    */
  def redeemProtonsPrice(rsvAmount: Long): Action[AnyContent]
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
      Ok(
        gluonWBoxExplorer.getGluonWBox
          .getNeutronsPrice(gluonWBoxExplorer.getGoldOracleBox)
          .toJson
      ).as("application/json")
    }

  override def rsvPrice(): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        gluonWBoxExplorer.getGluonWBox.getProtonsPrice.toJson
      ).as("application/json")
    }

  /**
    * Erg to Neutrons and Protons
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
    * Erg to Neutrons and Protons Price
    *
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  override def fissionPrice(ergAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.fissionPrice(ergAmount).map(rate => rate.toJson)
        )
      ).as("application/json")
    }

  /**
    * Neutrons to Rsv
    *
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  override def transmuteNeutronsToProtons(goldAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, goldAmount, gluonW.transmuteNeutronsToProtons))
        .as("application/json")
    }

  /**
    * Neutrons to Rsv rate
    *
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  override def transmuteNeutronsToProtonsPrice(
    goldAmount: Long
  ): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(
      Json.fromValues(
        gluonW
          .transmuteNeutronsToProtonsPrice(goldAmount)
          .map(rate => rate.toJson)
      )
    ).as("application/json")
  }

  /**
    * Protons to Neutrons
    *
    * @param rsvAmount the amount of Protons to be converted
    * @return
    */
  override def transmuteProtonsToNeutrons(rsvAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, rsvAmount, gluonW.transmuteProtonsToNeutrons))
        .as("application/json")
    }

  /**
    * Protons to Neutrons rate
    *
    * @param rsvAmount the amount of Protons to be converted
    * @return
    */
  override def transmuteProtonsToNeutronsPrice(
    rsvAmount: Long
  ): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW
            .transmuteProtonsToNeutronsPrice(rsvAmount)
            .map(rate => rate.toJson)
        )
      ).as("application/json")
    }

  /**
    * Mint Neutrons
    *
    * @param ergAmount the Erg amount to convert to sigGold
    * @return
    */
  override def mintNeutrons(ergAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, ergAmount, gluonW.mintNeutrons)).as("application/json")
    }

  /**
    * Mint Neutrons rate
    *
    * @param ergAmount the Erg amount to convert to Neutrons
    * @return
    */
  override def mintNeutronsPrice(ergAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.mintNeutronsPrice(ergAmount).map(rate => rate.toJson)
        )
      ).as("application/json")
    }

  /**
    * Redeem Neutrons for Erg
    *
    * @param goldAmount the amount of Neutrons to be redeemed
    * @return
    */
  override def redeemNeutrons(goldAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, goldAmount, gluonW.redeemNeutrons))
        .as("application/json")
    }

  /**
    * Redeem Neutrons for Erg rate
    *
    * @param goldAmount the amount of Neutrons to be redeemed
    * @return
    */
  override def redeemNeutronsPrice(goldAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.redeemNeutronsPrice(goldAmount).map(rate => rate.toJson)
        )
      ).as("application/json")
    }

  /**
    * Mint Protons
    *
    * @param ergAmount the Erg amount to convert to Protons
    * @return
    */
  override def mintProtons(ergAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, ergAmount, gluonW.mintProtons))
        .as("application/json")
    }

  /**
    * Mint Protons rate
    *
    * @param ergAmount the Erg amount to convert to Protons
    * @return
    */
  override def mintProtonsPrice(ergAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.mintProtonsPrice(ergAmount).map(rate => rate.toJson)
        )
      ).as("application/json")
    }

  /**
    * Redeem Protons for Erg
    *
    * @param rsvAmount the amount of Protons to be redeemed
    * @return
    */
  override def redeemProtons(rsvAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, rsvAmount, gluonW.redeemProtons))
        .as("application/json")
    }

  /**
    * Redeem Protons for Erg rate
    *
    * @param rsvAmount the amount of Protons to be redeemed
    * @return
    */
  override def redeemProtonsPrice(rsvAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.redeemProtonsPrice(rsvAmount).map(rate => rate.toJson)
        )
      ).as("application/json")
    }
}
