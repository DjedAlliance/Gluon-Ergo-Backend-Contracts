package controllers

import commons.node.Client
import errors.ExceptionThrowable
import gluonw.common.GluonW
import io.circe.Json
import play.api.libs.circe.Circe
import play.api.mvc._

/**
  * common.GluonW Controller trait
  */
trait TGluonWController {

  /**
    * Erg to SigGold and SigGoldRsv
    * @return
    */
  def fission(ergAmount: Long): Action[Json]
  def fissionRate(ergAmount: Long): Action[Json]

  /**
    * SigGold and SigGoldRsv to Erg
    * @return
    */
  def fusion(): Action[Json]

  /**
    * SigGold to SigGoldRsv
    * @return
    */
  def transmuteSigGoldToSigGoldRsv(goldAmount: Long): Action[Json]
  def transmuteSigGoldToSigGoldRsvRate(goldAmount: Long): Action[Json]

  /**
    * SigGoldRsv to SigGold
    * @return
    */
  def transmuteSigGoldRsvToSigGold(rsvAmount: Long): Action[Json]
  def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): Action[Json]

  /**
    * Erg to SigGold
    * @return
    */
  def mintSigGold(ergAmount: Long): Action[Json]
  def mintSigGoldRate(ergAmount: Long): Action[Json]

  /**
    * SigGold to Erg
    * @return
    */
  def redeemSigGold(goldAmount: Long): Action[Json]
  def redeemSigGoldRate(goldAmount: Long): Action[Json]

  /**
    * Erg to SigGoldRsv
    * @return
    */
  def mintSigGoldRsv(ergAmount: Long): Action[Json]
  def mintSigGoldRsvRate(ergAmount: Long): Action[Json]

  /**
    * SigGoldRsv to Erg
    * @return
    */
  def redeemSigGoldRsv(rsvAmount: Long): Action[Json]
  def redeemSigGoldRsvRate(rsvAmount: Long): Action[Json]
}

class GluonWController(
  val client: Client,
  val gluonW: GluonW,
  val controllerComponents: ControllerComponents
) extends BaseController
    with Circe
    with ExceptionThrowable
    with TGluonWController {

  /**
    * Erg to SigGold and SigGoldRsv
    *
    * @return
    */
  override def fission(ergAmount: Long): Action[Json] = ???

  /**
    * SigGold and SigGoldRsv to Erg
    *
    * @return
    */
  override def fusion(): Action[Json] = ???

  /**
    * SigGold to SigGoldRsv
    *
    * @return
    */
  override def transmuteSigGoldToSigGoldRsv(goldAmount: Long): Action[Json] =
    ???

  override def fissionRate(ergAmount: Long): Action[Json] = ???

  override def transmuteSigGoldToSigGoldRsvRate(
    goldAmount: Long
  ): Action[Json] = ???

  override def transmuteSigGoldRsvToSigGoldRate(rsvAmount: Long): Action[Json] =
    ???

  override def mintSigGoldRate(ergAmount: Long): Action[Json] = ???

  override def redeemSigGoldRate(goldAmount: Long): Action[Json] = ???

  override def mintSigGoldRsvRate(ergAmount: Long): Action[Json] = ???

  override def redeemSigGoldRsvRate(rsvAmount: Long): Action[Json] = ???

  /**
    * SigGoldRsv to SigGold
    *
    * @return
    */
  override def transmuteSigGoldRsvToSigGold(rsvAmount: Long): Action[Json] = ???

  /**
    * Erg to SigGold
    *
    * @return
    */
  override def mintSigGold(ergAmount: Long): Action[Json] = ???

  /**
    * SigGold to Erg
    *
    * @return
    */
  override def redeemSigGold(goldAmount: Long): Action[Json] = ???

  /**
    * Erg to SigGoldRsv
    *
    * @return
    */
  override def mintSigGoldRsv(ergAmount: Long): Action[Json] = ???

  /**
    * SigGoldRsv to Erg
    *
    * @return
    */
  override def redeemSigGoldRsv(rsvAmount: Long): Action[Json] = ???
}
