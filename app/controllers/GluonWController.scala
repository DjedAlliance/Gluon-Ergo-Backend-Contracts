package controllers

import commons.node.Client
import errors.ExceptionThrowable
import gluonw.common.GluonW
import io.circe.Json
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc._

import javax.inject.Inject

/**
  * common.GluonW Controller trait
  */
trait TGluonWController {

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
  val controllerComponents: ControllerComponents
) extends BaseController
    with Circe
    with ExceptionThrowable
    with TGluonWController {
  private val logger: Logger = Logger(this.getClass)

  /**
    * Erg to SigGold and SigGoldRsv
    *
    * @param ergAmount the amount of erg to be converted
    * @return
    */
  override def fission(ergAmount: Long): Action[Json] =
    Action(circe.json) { implicit request: Request[Json] =>
      try {
        // Get the wallet address from the request body
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        // Set up fission tx and get response
        // Send ergoPayResponse back

        Ok(walletAddress).as("application/json")
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
  Action {
    implicit request: Request[AnyContent] => {
      Ok(Json.fromLong(ergAmount)).as("application/json")
    }
  }

  /**
    * SigGold to Rsv
    *
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  override def transmuteSigGoldToSigGoldRsv(goldAmount: Long): Action[Json] =
    Action(circe.json) {
      implicit request: Request[Json] => {
        Ok("transmuteSigGoldToSigGoldRsv").as("application/json")
      }
    }

  /**
    * SigGold to Rsv rate
    *
    * @param goldAmount the amount of sigGold to be converted
    * @return
    */
  override def transmuteSigGoldToSigGoldRsvRate(
    goldAmount: Long
  ): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => {
      Ok("transmuteSigGoldToSigGoldRsvRate").as("application/json")
    }
  }

  /**
    * SigGoldRsv to SigGold
    *
    * @param rsvAmount the amount of SigGoldRsv to be converted
    * @return
    */
  override def transmuteSigGoldRsvToSigGold(rsvAmount: Long): Action[Json] =
    Action(circe.json) {
      implicit request: Request[Json] => {
        Ok("transmuteSigGoldRsvToSigGold").as("application/json")
      }
    }

  /**
    * SigGoldRsv to SigGold rate
    *
    * @param rsvAmount the amount of SigGoldRsv to be converted
    * @return
    */
  override def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): Action[AnyContent] =
    Action {
      implicit request: Request[AnyContent] => {
        Ok("transmuteSigGoldRsvToSigGoldRate").as("application/json")
      }
    }

  /**
    * Mint SigGold
    *
    * @param ergAmount the Erg amount to convert to sigGold
    * @return
    */
  override def mintSigGold(ergAmount: Long): Action[Json] =
    Action(circe.json) {
      implicit request: Request[Json] => {
        Ok("mintSigGold").as("application/json")
      }
    }

  /**
    * Mint SigGold rate
    *
    * @param ergAmount the Erg amount to convert to SigGold
    * @return
    */
  override def mintSigGoldRate(ergAmount: Long): Action[AnyContent] =
    Action {
      implicit request: Request[AnyContent] => {
        Ok("mintSigGoldRate").as("application/json")
      }
    }

  /**
    * Redeem SigGold for Erg
    *
    * @param goldAmount the amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGold(goldAmount: Long): Action[Json] = {
    Action(circe.json) {
      implicit request: Request[Json] => {
        Ok("redeemSigGold").as("application/json")
      }
    }
  }

  /**
    * Redeem SigGold for Erg rate
    *
    * @param goldAmount the amount of SigGold to be redeemed
    * @return
    */
  override def redeemSigGoldRate(goldAmount: Long): Action[AnyContent] =
    Action {
      implicit request: Request[AnyContent] => {
        Ok("redeemSigGoldRate").as("application/json")
      }
    }

  /**
    * Mint SigGoldRsv
    *
    * @param ergAmount the Erg amount to convert to SigGoldRsv
    * @return
    */
  override def mintSigGoldRsv(ergAmount: Long): Action[Json] =
    Action(circe.json) {
      implicit request: Request[Json] => {
        Ok("mintSigGoldRsv").as("application/json")
      }
    }

  /**
    * Mint SigGoldRsv rate
    *
    * @param ergAmount the Erg amount to convert to SigGoldRsv
    * @return
    */
  override def mintSigGoldRsvRate(ergAmount: Long): Action[AnyContent] =
    Action {
      implicit request: Request[AnyContent] => {
        Ok("mintSigGoldRsvRate").as("application/json")
      }
    }

  /**
    * Redeem SigGoldRsv for Erg
    *
    * @param rsvAmount the amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsv(rsvAmount: Long): Action[Json] = {
    Action(circe.json) {
      implicit request: Request[Json] => {
        Ok("redeemSigGoldRsv").as("application/json")
      }
    }
  }

  /**
    * Redeem SigGoldRsv for Erg rate
    *
    * @param rsvAmount the amount of SigGoldRsv to be redeemed
    * @return
    */
  override def redeemSigGoldRsvRate(rsvAmount: Long): Action[AnyContent] =
    Action {
      implicit request: Request[AnyContent] => {
        Ok("redeemSigGoldRsvRate").as("application/json")
      }
    }
}
