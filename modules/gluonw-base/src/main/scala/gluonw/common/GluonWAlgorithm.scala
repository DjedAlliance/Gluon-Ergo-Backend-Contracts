package gluonw.common

import edge.registers.{LongPairRegister, LongRegister, NumbersRegister}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import io.circe.Json
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

import scala.math.BigDecimal.long2bigDecimal

/**
  * AssetPrice
  * Gives the price of the asset
  */
case class AssetPrice(name: String, price: Long, id: ErgoId) {

  def toJson: Json =
    Json.fromFields(
      List(
        ("assetName", Json.fromString(name)),
        ("id", Json.fromString(id.toString)),
        ("price", Json.fromLong(price)),
        ("decimals", Json.fromLong(9))
      )
    )
}

trait TGluonWConstants {
  def qStar: Long

  def fusionRatio(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): BigInt

  def phiFission: Long

  def phiFusion: Long

  def varPhiBeta(
    rErg: Long,
    volumeToBeNegate: List[Long],
    volumeToMinus: List[Long]
  ): Long

  val precision: Long

  def neutronsToNanoErg(
    neutronsInCirculation: Long,
    neutronsAmount: Long,
    fissionedErg: Long,
    goldPriceNanoErgPerGram: Long
  ): Long

  def protonsToNanoErg(
    neutronsInCirculation: Long,
    protonsInCirculation: Long,
    protonsAmount: Long,
    fissionedErg: Long,
    goldPriceNanoErgPerGram: Long
  ): Long
}

case class GluonWConstants(precision: Long = GluonWBoxConstants.PRECISION)
    extends TGluonWConstants {
  override def qStar: Long = (0.66 * precision).toLong

  override def fusionRatio(
    neutronsInCirculation: Long,
    pt: Long,
    fissionedErg: Long
  ): BigInt = {
    // We divide pt by 1000 because it is kilograms. And we want to do it in grams.
    val rightHandMinVal: BigInt = {
      (BigInt(neutronsInCirculation) * BigInt(pt) / fissionedErg).toLong
    }

    val ratio = rightHandMinVal.min(BigInt(qStar))

    // Begin logging
    val r = ratio.toFloat
    val nPrice = fissionedErg * r / neutronsInCirculation;
    System.out.println("Fusion Ratio: " + r/1E9 + "; Reserve Ratio: " + 1E9 / r + "; Neutron price: " + nPrice)
    // End logging
    
    ratio
  }

  override def phiFission: Long = (0.001 * precision).toLong

  override def phiFusion: Long = (0.005 * precision).toLong

  override def varPhiBeta(
    rErg: Long,
    volumeToBeNegate: List[Long],
    volumeToMinus: List[Long]
  ): Long = {
    val phi0: Long = (0.005 * precision).toLong
    val phi1: Long = (precision * 0.5).toLong

    val sumVolumeToBeNegate: BigInt =
      volumeToBeNegate.fold(0L)((acc: Long, x: Long) => acc + x).toBigInt
    val sumVolumeToMinus: BigInt =
      volumeToMinus.fold(0L)((acc: Long, x: Long) => acc + x).toBigInt

    val volume: BigInt = if (sumVolumeToBeNegate < sumVolumeToMinus) {
      BigInt(0)
    } else {
      sumVolumeToBeNegate - sumVolumeToMinus
    }

    val phi = (phi0 + phi1 * volume / rErg).toLong
    System.out.println("Volume: " + volume.toFloat/1E9 + "; R:" + rErg.toFloat/1E9 + "; TransmutationFee: " + phi.toFloat/1E9)
    phi
  }

  def neutronsToNanoErg(
    neutronsInCirculation: Long,
    neutronsAmount: Long,
    fissionedErg: Long,
    goldPriceNanoErgPerGram: Long
  ): Long = {
    val fusRatio: BigInt =
      fusionRatio(
        neutronsInCirculation,
        goldPriceNanoErgPerGram,
        fissionedErg
      )

    // Equations for determining the neutron price, Pn, and neutron volume, Vn, given N neutrons.
    // Note that the target price, Pt, i.e. oracle price, is not the same as the neutron price.
    // q = min(q*, Sn*Pt/R)
    // Pn = q * R / Sn
    // Vn = N*Pn
    val neutronsPrice: BigInt =
      (fusRatio * fissionedErg) / neutronsInCirculation

    ((BigInt(neutronsAmount) * neutronsPrice) / GluonWBoxConstants.PRECISION).toLong
  }

  def protonsToNanoErg(
    neutronsInCirculation: Long,
    protonsInCirculation: Long,
    protonsAmount: Long,
    fissionedErg: Long,
    goldPriceNanoErgPerGram: Long
  ): Long = {
    val fusRatio: BigInt =
      fusionRatio(
        neutronsInCirculation,
        goldPriceNanoErgPerGram,
        fissionedErg
      )

    // Equations for determining the proton price, Pp, and proton volume, Vp, given N protons.
    // q  = min(q*, Sn*Pt/R)
    // Pp = (1-q) * R / Sp
    // Vp = N*Pp
    val oneMinusFusionRatio: BigInt = GluonWBoxConstants.PRECISION - fusRatio
    val protonsPrice: BigInt =
      (oneMinusFusionRatio * fissionedErg) / protonsInCirculation

    System.out.println("Proton Price: " + protonsPrice.toFloat / 1E9)

    ((BigInt(protonsAmount) * protonsPrice) / GluonWBoxConstants.PRECISION).toLong
  }

}

/**
  * Algorithm and rates
  *
  * Note regarding Prices:
  * When getting rate, we will get rates for both Protons and Neutrons
  * most of the time. Unless it's a one to one tx.
  * For fusion, we will take the amount of Erg to be redeemed, and
  * return the amount of Protons and Neutrons needed for fusion. This
  * will be further improve to aid user experience.
  */
trait TGluonWAlgorithm {
  def fission(inputGluonWBox: GluonWBox, ergAmount: Long): GluonWBox

  def fusion(inputGluonWBox: GluonWBox, ergRedeemed: Long): GluonWBox

  def betaDecayPlus(inputGluonWBox: GluonWBox, protonsToTransmute: Long)(
    implicit oracleBox: OracleBox,
    currentHeight: Long
  ): GluonWBox

  def betaDecayMinus(inputGluonWBox: GluonWBox, neutronsToTransmute: Long)(
    implicit oracleBox: OracleBox,
    currentHeight: Long
  ): GluonWBox

  def fissionPrice(
    inputGluonWBox: GluonWBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice])

  def fusionPrice(
    inputGluonWBox: GluonWBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice])

  def betaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    protonsToTransmute: Long,
    currentHeight: Long
  ): (GluonWBox, Seq[AssetPrice])

  def betaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    neutronsToTransmute: Long,
    currentHeight: Long
  ): (GluonWBox, Seq[AssetPrice])
}

/**
  * @param sProtons Protons Circulating Supply
  * @param sNeutrons Neutrons Circulating Supply
  * @param rErg Erg fissioned amount
  */
case class GluonWCalculator(
  sProtons: Long,
  sNeutrons: Long,
  rErg: Long,
  gluonWConstants: TGluonWConstants
) {

  def fission(ergToFission: Long): GluonWBoxOutputAssetAmount = {
    val outNeutronsAmount: Long =
      ((BigInt(ergToFission) * BigInt(sNeutrons) * (gluonWConstants.precision - gluonWConstants.phiFission) / rErg) / gluonWConstants.precision).toLong
    val outProtonsAmount: Long =
      ((BigInt(ergToFission) * BigInt(sProtons) * (gluonWConstants.precision - gluonWConstants.phiFission) / rErg) / gluonWConstants.precision).toLong

    GluonWBoxOutputAssetAmount(
      ergAmount = -ergToFission,
      neutronsAmount = outNeutronsAmount,
      protonsAmount = outProtonsAmount
    )
  }

  def fusion(ergFusioned: Long): GluonWBoxOutputAssetAmount = {
    val inNeutronsNumerator: BigInt =
      BigInt(ergFusioned) * BigInt(sNeutrons) * gluonWConstants.precision

    val inProtonsNumerator: BigInt =
      BigInt(ergFusioned) * BigInt(sProtons) * gluonWConstants.precision

    val denominator: BigInt =
      BigInt(rErg) * (gluonWConstants.precision - gluonWConstants.phiFusion)

    val inNeutronsAmount: Long = (inNeutronsNumerator / denominator).toLong
    val inProtonsAmount: Long = (inProtonsNumerator / denominator).toLong

    GluonWBoxOutputAssetAmount(
      ergAmount = ergFusioned,
      neutronsAmount = -inNeutronsAmount,
      protonsAmount = -inProtonsAmount
    )
  }

  def betaDecayPlus(
    rErg: Long,
    volumePlus: List[Long],
    volumeMinus: List[Long],
    protonsToDecay: Long
  )(goldPrice: Long): GluonWBoxOutputAssetAmount = {
    val fusionRatio: BigInt =
      gluonWConstants.fusionRatio(sNeutrons, goldPrice, rErg)

    val oneMinusPhiBeta: BigInt =
      BigInt(gluonWConstants.precision) - BigInt(
        gluonWConstants.varPhiBeta(
          rErg,
          volumeToBeNegate = volumePlus,
          volumeToMinus = volumeMinus
        )
      )
    val oneMinusFusionRatio: BigInt =
      BigInt(gluonWConstants.precision) - fusionRatio
    val ratio1: BigInt =
      (BigInt(protonsToDecay) * oneMinusPhiBeta) / BigInt(sProtons)
    val ratio2: BigInt = (oneMinusFusionRatio * BigInt(sNeutrons)) / BigInt(
      gluonWConstants.precision
    )
    val outNeutronsAmount: BigInt = (ratio1 * ratio2) / fusionRatio

    GluonWBoxOutputAssetAmount(
      ergAmount = 0,
      neutronsAmount = outNeutronsAmount.toLong,
      protonsAmount = -protonsToDecay
    )
  }

  def betaDecayMinus(
    rErg: Long,
    volumeMinus: List[Long],
    volumePlus: List[Long],
    neutronsToDecay: Long
  )(goldPrice: Long): GluonWBoxOutputAssetAmount = {
    val fusionRatio: BigInt =
      gluonWConstants.fusionRatio(sNeutrons, goldPrice, rErg)

    val oneMinusPhiBeta: BigInt =
      BigInt(gluonWConstants.precision) - BigInt(
        gluonWConstants.varPhiBeta(
          rErg,
          volumeToBeNegate = volumeMinus,
          volumeToMinus = volumePlus
        )
      )
    val oneMinusFusionRatio: BigInt =
      BigInt(gluonWConstants.precision) - fusionRatio
    val ratio1: BigInt =
      (BigInt(neutronsToDecay) * oneMinusPhiBeta) / BigInt(sNeutrons)
    val ratio2: BigInt =
      (fusionRatio * BigInt(sProtons)) / BigInt(gluonWConstants.precision)
    val outProtonsAmount: BigInt = (ratio1 * ratio2) / oneMinusFusionRatio

    GluonWBoxOutputAssetAmount(
      ergAmount = 0,
      neutronsAmount = -neutronsToDecay,
      protonsAmount = outProtonsAmount.toLong
    )
  }
}

/**
  * @param ergAmount Amount of Ergs
  * @param neutronsAmount Amount of Neutrons
  * @param protonsAmount Amount of Protons
  */
case class GluonWBoxOutputAssetAmount(
  ergAmount: Long,
  neutronsAmount: Long,
  protonsAmount: Long
)

case class GluonWAlgorithm(gluonWConstants: TGluonWConstants)
    extends TGluonWAlgorithm {

  val BLOCKS_PER_VOLUME_BUCKET: Long =
    GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET

  def getVolumes(
    currentHeight: Long,
    lastDayBlockHeight: Long,
    mVolumeInErgs: Long,
    volumeListToAdd: List[Long],
    volumeListToPreserved: List[Long]
  ): (List[Long], List[Long]) = {
    // 1. We get the Ndays from previousLastDayBlock
    val getNDaysPreFilteredValue: Int =
      ((currentHeight - lastDayBlockHeight) / BLOCKS_PER_VOLUME_BUCKET).toInt

    val nDays: Int =
      if (getNDaysPreFilteredValue >= GluonWBoxConstants.BUCKETS) {
        GluonWBoxConstants.BUCKETS
      } else getNDaysPreFilteredValue
    val outVolumeListToAddExpectedValue: Long =
      (if (nDays == 0) { volumeListToAdd.head }
       else { 0L }) + mVolumeInErgs
    // We're always adding one to the front when we prepend, so we only need n-1
    val listToPrepend: List[Long] = List.fill(nDays - 1)(0L)
    val volumeListToAddResult: List[Long] = if (nDays == 0) {
      volumeListToAdd.updated(0, outVolumeListToAddExpectedValue)
    } else {
      List(outVolumeListToAddExpectedValue) ++ listToPrepend ++ volumeListToAdd
        .slice(0, GluonWBoxConstants.BUCKETS - nDays)
    }

    val outVolumeListToPreservedExpectedValue: Long = if (nDays == 0) {
      volumeListToPreserved.head
    } else { 0L }
    val volumeListToPreservedResult: List[Long] = if (nDays == 0) {
      volumeListToPreserved
    } else {
      List(outVolumeListToPreservedExpectedValue) ++ listToPrepend ++ volumeListToPreserved
        .slice(0, GluonWBoxConstants.BUCKETS - nDays)
    }

    val volumes = (volumeListToAddResult, volumeListToPreservedResult)
    System.out.println("Volumes Plus: " + volumes._1.map(_.toFloat/1e9) + " ; Total: " + volumes._1.sum.toFloat/1e9)
    System.out.println("Volumes Minus: " + volumes._2.map(_.toFloat/1e9) + " ; Total: " + volumes._2.sum.toFloat/1e9)
    volumes
  }

  def outputGluonWBox(
    inputGluonWBox: GluonWBox,
    gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount,
    volumePlus: List[Long] = null,
    volumeMinus: List[Long] = null,
    dayBlockHeight: Long = 0L
  ): GluonWBox = {
    val tokens: Seq[ErgoToken] = inputGluonWBox.tokens.map { token =>
      token.getId match {
        case GluonWTokens.neutronId =>
          ErgoToken(
            token.getId,
            token.getValue - gluonWBoxOutputAssetAmount.neutronsAmount
          )
        case GluonWTokens.protonId =>
          ErgoToken(
            token.getId,
            token.getValue - gluonWBoxOutputAssetAmount.protonsAmount
          )
        case _ => token
      }
    }

    inputGluonWBox.copy(
      value = inputGluonWBox.value - gluonWBoxOutputAssetAmount.ergAmount,
      tokens = tokens,
      volumePlusRegister = if (volumePlus != null) {
        new NumbersRegister(volumePlus.toArray)
      } else inputGluonWBox.volumePlusRegister,
      volumeMinusRegister = if (volumeMinus != null) {
        new NumbersRegister(volumeMinus.toArray)
      } else inputGluonWBox.volumeMinusRegister,
      lastDayBlockRegister = new LongRegister(dayBlockHeight)
    )
  }

  override def fission(
    inputGluonWBox: GluonWBox,
    ergAmount: Long
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.ergFissioned

    val ergToChange: Long = ergAmount

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).fission(ergToChange)

    outputGluonWBox(
      inputGluonWBox,
      gluonWBoxOutputAssetAmount,
      dayBlockHeight = inputGluonWBox.lastDayBlockRegister.value
    )
  }

  override def fusion(
    inputGluonWBox: GluonWBox,
    ergRedeemed: Long
  ): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.ergFissioned

    val ergToChange: Long = ergRedeemed

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).fusion(ergToChange)

    outputGluonWBox(
      inputGluonWBox,
      gluonWBoxOutputAssetAmount,
      dayBlockHeight = inputGluonWBox.lastDayBlockRegister.value
    )
  }

  override def betaDecayPlus(
    inputGluonWBox: GluonWBox,
    protonsToTransmute: Long
  )(implicit oracleBox: OracleBox, currentHeight: Long): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.ergFissioned
    val (volumePlus, volumeMinus): (List[Long], List[Long]) = getVolumes(
      currentHeight = currentHeight,
      lastDayBlockHeight = inputGluonWBox.lastDayBlockRegister.value,
      mVolumeInErgs = gluonWConstants.protonsToNanoErg(
        neutronsInCirculation = sNeutrons,
        protonsInCirculation = sProtons,
        fissionedErg = rErg,
        goldPriceNanoErgPerGram = oracleBox.getPricePerGram,
        protonsAmount = protonsToTransmute
      ),
      volumeListToAdd = inputGluonWBox.volumePlusRegister.value.toList,
      volumeListToPreserved = inputGluonWBox.volumeMinusRegister.value.toList
    )

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).betaDecayPlus(
        protonsToDecay = protonsToTransmute,
        rErg = rErg,
        volumePlus = volumePlus,
        volumeMinus = volumeMinus
      )(oracleBox.getPricePerGram)

    val dayBlockHeight: Long =
      (currentHeight / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET) * GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET

    System.out.println("Oracle: " + oracleBox.getPricePerGram.toFloat/1e9)

    outputGluonWBox(
      inputGluonWBox,
      gluonWBoxOutputAssetAmount,
      volumePlus = volumePlus,
      volumeMinus = volumeMinus,
      dayBlockHeight = dayBlockHeight
    )
  }

  override def betaDecayMinus(
    inputGluonWBox: GluonWBox,
    neutronsToTransmute: Long
  )(implicit oracleBox: OracleBox, currentHeight: Long): GluonWBox = {
    val sProtons: Long = inputGluonWBox.protonsCirculatingSupply
    val sNeutrons: Long = inputGluonWBox.neutronsCirculatingSupply

    val rErg: Long = inputGluonWBox.ergFissioned

    val (volumeMinus, volumePlus): (List[Long], List[Long]) = getVolumes(
      currentHeight = currentHeight,
      lastDayBlockHeight = inputGluonWBox.lastDayBlockRegister.value,
      mVolumeInErgs = gluonWConstants
        .neutronsToNanoErg(
          neutronsInCirculation = sNeutrons,
          neutronsAmount = neutronsToTransmute,
          fissionedErg = rErg,
          goldPriceNanoErgPerGram = oracleBox.getPricePerGram
        ),
      volumeListToAdd = inputGluonWBox.volumeMinusRegister.value.toList,
      volumeListToPreserved = inputGluonWBox.volumePlusRegister.value.toList
    )

    val gluonWBoxOutputAssetAmount: GluonWBoxOutputAssetAmount =
      GluonWCalculator(
        sNeutrons = sNeutrons,
        sProtons = sProtons,
        rErg = rErg,
        gluonWConstants = gluonWConstants
      ).betaDecayMinus(
        rErg = rErg,
        volumePlus = volumePlus,
        volumeMinus = volumeMinus,
        neutronsToDecay = neutronsToTransmute
      )(oracleBox.getPricePerGram)

    val dayBlockHeight: Long =
      (currentHeight / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET) * GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET

    System.out.println("Oracle: " + oracleBox.getPricePerGram)

    outputGluonWBox(
      inputGluonWBox,
      gluonWBoxOutputAssetAmount,
      volumePlus = volumePlus,
      volumeMinus = volumeMinus,
      dayBlockHeight = dayBlockHeight
    )
  }

  override def fissionPrice(
    inputGluonWBox: GluonWBox,
    ergAmount: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fission(inputGluonWBox, ergAmount)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.NEUTRON.toString,
          inputGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue,
          GluonWTokens.neutronId
        ),
        AssetPrice(
          name = GluonWAsset.PROTON.toString,
          inputGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue,
          GluonWTokens.protonId
        )
      )
    )
  }

  /**
    *
    * @param inputGluonWBox GluonWBox to undergo tx
    * @param ergRedeemed erg to be redeemed. This is a positive amount
    * @return
    */
  override def fusionPrice(
    inputGluonWBox: GluonWBox,
    ergRedeemed: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      fusion(inputGluonWBox, ergRedeemed)

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.NEUTRON.toString,
          outGluonWBox.Neutrons.getValue - inputGluonWBox.Neutrons.getValue,
          GluonWTokens.neutronId
        ),
        AssetPrice(
          name = GluonWAsset.PROTON.toString,
          outGluonWBox.Protons.getValue - inputGluonWBox.Protons.getValue,
          GluonWTokens.protonId
        )
      )
    )
  }

  override def betaDecayPlusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    protonsToTransmute: Long,
    currentHeight: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      betaDecayPlus(inputGluonWBox, protonsToTransmute)(
        oracleBox,
        currentHeight
      )

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.NEUTRON.toString,
          inputGluonWBox.Neutrons.getValue - outGluonWBox.Neutrons.getValue,
          GluonWTokens.neutronId
        )
      )
    )
  }

  override def betaDecayMinusPrice(
    inputGluonWBox: GluonWBox,
    oracleBox: OracleBox,
    neutronsToTransmute: Long,
    currentHeight: Long
  ): (GluonWBox, Seq[AssetPrice]) = {
    val outGluonWBox: GluonWBox =
      betaDecayMinus(inputGluonWBox, neutronsToTransmute)(
        oracleBox,
        currentHeight
      )

    (
      outGluonWBox,
      Seq(
        AssetPrice(
          name = GluonWAsset.PROTON.toString,
          inputGluonWBox.Protons.getValue - outGluonWBox.Protons.getValue,
          GluonWTokens.protonId
        )
      )
    )
  }
}
