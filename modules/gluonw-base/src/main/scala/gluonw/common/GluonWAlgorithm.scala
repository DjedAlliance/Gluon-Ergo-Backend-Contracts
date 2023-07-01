package gluonw.common

import gluonw.boxes.GluonWBox

trait TGluonWAlgorithm {

  def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox
  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox
  def betaDecayPlus(inputGluonWBox: GluonWBox, goldAmount: Long): GluonWBox
  def betaDecayMinus(inputGluonWBox: GluonWBox, rsvAmount: Long): GluonWBox
}

object GluonWAlgorithm extends TGluonWAlgorithm {
  def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox = ???
  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox = ???

  def betaDecayPlus(inputGluonWBox: GluonWBox, goldAmount: Long): GluonWBox =
    ???

  def betaDecayMinus(inputGluonWBox: GluonWBox, rsvAmount: Long): GluonWBox =
    ???
}
