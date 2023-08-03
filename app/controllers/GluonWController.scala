package controllers

import commons.node.Client
import edge.errors.ExceptionThrowable
import edge.pay.ErgoPayResponse
import edge.txs.TTx
import gluonw.boxes.OracleBox
import gluonw.common.{GluonW, GluonWBoxExplorer, TxConverter}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.ergoplatform.appkit.Address
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc._

import javax.inject.Inject

/**
  * common.GluonW Controller trait
  */
trait TGluonWController {

  /**
    * Price of neutrons against erg
    * @return
    */
  def neutronPrice(): Action[AnyContent]

  /**
    * Price of protons against erg
    * @return
    */
  def protonPrice(): Action[AnyContent]

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
    * Neutrons to Protons
    * @param neutronsAmount the amount of neutrons to be converted
    * @return
    */
  def transmuteNeutronsToProtons(neutronsAmount: Long): Action[Json]

  /**
    * Neutrons to Protons rate
    * @param neutronsAmount the amount of neutrons to be converted
    * @return
    */
  def transmuteNeutronsToProtonsPrice(neutronsAmount: Long): Action[AnyContent]

  /**
    * Protons to Neutrons
    * @param protonsAmount the amount of Protons to be converted
    * @return
    */
  def transmuteProtonsToNeutrons(protonsAmount: Long): Action[Json]

  /**
    * Protons to Neutrons rate
    * @param protonsAmount the amount of Protons to be converted
    * @return
    */
  def transmuteProtonsToNeutronsPrice(protonsAmount: Long): Action[AnyContent]

  /**
    * Mint Neutrons
    * @param ergAmount the Erg amount to convert to neutrons
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
    * @param neutronsAmount the amount of Neutrons to be redeemed
    * @return
    */
  def redeemNeutrons(neutronsAmount: Long): Action[Json]

  /**
    * Redeem Neutrons for Erg rate
    * @param neutronsAmount the amount of Neutrons to be redeemed
    * @return
    */
  def redeemNeutronsPrice(neutronsAmount: Long): Action[AnyContent]

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
    * @param protonsAmount the amount of Protons to be redeemed
    * @return
    */
  def redeemProtons(protonsAmount: Long): Action[Json]

  /**
    * Redeem Protons for Erg rate
    * @param protonsAmount the amount of Protons to be redeemed
    * @return
    */
  def redeemProtonsPrice(protonsAmount: Long): Action[AnyContent]
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
  client.setClient()

  def TxCall(
    request: Request[Json],
    assetAmount: Long,
    txFunc: (Long, Address) => Seq[TTx]
  ): Json = {
    // Get the wallet address from the request body
    val walletAddress: Address =
      Address.create(getRequestBodyAsString(request, "walletAddress"))

    // Set up fission tx and get response
    val txs: Seq[TTx] = txFunc(assetAmount, walletAddress)

    txs.map(tx => println(tx.visualizeTx))
    // Send ergoPayResponse back
    val ergoPayResponses: Seq[ErgoPayResponse] =
      convert(txs, walletAddress)

    Json.fromValues(ergoPayResponses.map(r => r.asJson))
  }

  override def neutronPrice(): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        gluonWBoxExplorer.getOracleBox
          .toJson()
      ).as("application/json")
    }

  override def protonPrice(): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      val oracleBox: OracleBox = gluonWBoxExplorer.getOracleBox
      Ok(
        gluonWBoxExplorer.getGluonWBox.getProtonsPrice(oracleBox).toJson
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
    * Neutrons to Protons
    *
    * @param neutronsAmount the amount of neutrons to be converted
    * @return
    */
  override def transmuteNeutronsToProtons(neutronsAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, neutronsAmount, gluonW.transmuteNeutronsToProtons))
        .as("application/json")
    }

  /**
    * Neutrons to Protons rate
    *
    * @param neutronsAmount the amount of neutrons to be converted
    * @return
    */
  override def transmuteNeutronsToProtonsPrice(
    neutronsAmount: Long
  ): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(
      Json.fromValues(
        gluonW
          .transmuteNeutronsToProtonsPrice(neutronsAmount)
          .map(rate => rate.toJson)
      )
    ).as("application/json")
  }

  /**
    * Protons to Neutrons
    *
    * @param protonsAmount the amount of Protons to be converted
    * @return
    */
  override def transmuteProtonsToNeutrons(protonsAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, protonsAmount, gluonW.transmuteProtonsToNeutrons))
        .as("application/json")
    }

  /**
    * Protons to Neutrons rate
    *
    * @param protonsAmount the amount of Protons to be converted
    * @return
    */
  override def transmuteProtonsToNeutronsPrice(
    protonsAmount: Long
  ): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW
            .transmuteProtonsToNeutronsPrice(protonsAmount)
            .map(rate => rate.toJson)
        )
      ).as("application/json")
    }

  /**
    * Mint Neutrons
    *
    * @param ergAmount the Erg amount to convert to neutrons
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
    * @param neutronsAmount the amount of Neutrons to be redeemed
    * @return
    */
  override def redeemNeutrons(neutronsAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, neutronsAmount, gluonW.redeemNeutrons))
        .as("application/json")
    }

  /**
    * Redeem Neutrons for Erg rate
    *
    * @param neutronsAmount the amount of Neutrons to be redeemed
    * @return
    */
  override def redeemNeutronsPrice(neutronsAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.redeemNeutronsPrice(neutronsAmount).map(rate => rate.toJson)
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
    * @param protonsAmount the amount of Protons to be redeemed
    * @return
    */
  override def redeemProtons(protonsAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      Ok(TxCall(request, protonsAmount, gluonW.redeemProtons))
        .as("application/json")
    }

  /**
    * Redeem Protons for Erg rate
    *
    * @param protonsAmount the amount of Protons to be redeemed
    * @return
    */
  override def redeemProtonsPrice(protonsAmount: Long): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        Json.fromValues(
          gluonW.redeemProtonsPrice(protonsAmount).map(rate => rate.toJson)
        )
      ).as("application/json")
    }
}
