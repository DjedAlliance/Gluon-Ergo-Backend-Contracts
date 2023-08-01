package gluonw.boxes

import commons.math.MathUtils
import gluonw.common.{AssetPrice, GluonWBase}
import org.ergoplatform.appkit.Parameters

import scala.util.Random

class GluonWBoxSpec extends GluonWBase {
  client.setClient()

  "GluonWBox" should {
    val oracleBox: OracleBox = createTestOracleBox
    "Get Right ProtonPrice that are set" in {
      case class ErgNeutronProtonAndPrice(
        ergAmount: Double,
        neutronAmount: Double,
        protonAmount: Double,
        protonPrice: Double
      )

      val ergNeutronProtonAndPrices: Seq[ErgNeutronProtonAndPrice] = Seq(
        ErgNeutronProtonAndPrice(200, 1.137347, 0.95, 147.57),
        ErgNeutronProtonAndPrice(200, 1.006064, 0.995852, 147.70),
        ErgNeutronProtonAndPrice(210, 1.056, 1.045, 147.80),
        ErgNeutronProtonAndPrice(310, 1.554, 1.538, 148.45),
        ErgNeutronProtonAndPrice(7062, 35.054, 34.698, 150.40),
        ErgNeutronProtonAndPrice(7062, 63.080096, 24.698441, 151.61),
        ErgNeutronProtonAndPrice(5062, 45.034990, 17.633043, 152.76),
        ErgNeutronProtonAndPrice(9449, 83.674, 32.762, 154.10)
      )

      ergNeutronProtonAndPrices.foreach { ergNeutronProtonAndPrice =>
        val gluonWBox: GluonWBox = genesisGluonWBox(
          ergAmount = ergNeutronProtonAndPrice.ergAmount,
          neutronAmount = ergNeutronProtonAndPrice.neutronAmount,
          protonAmount = ergNeutronProtonAndPrice.protonAmount
        )

        val protonPrice: AssetPrice = gluonWBox.getProtonsPrice(oracleBox)
        val protonPriceAtDollars: Double =
          protonPrice.price.toDouble / GluonWBoxConstants.PRECISION

        def getRoundUpValue(x: Double, y: Double): Boolean = {
          val xCentValue: Long = Math.round(x * 100).toLong
          val yCentValue: Long = Math.round(y * 100).toLong

          Math.abs(xCentValue - yCentValue) < 10
        }
        assert(
          getRoundUpValue(
            ergNeutronProtonAndPrice.protonPrice,
            protonPriceAtDollars
          ),
          s"Expected: ${ergNeutronProtonAndPrice.protonPrice}, Actual: ${protonPriceAtDollars}"
        )
      }

    }

    "Get Right ProtonPrice" in {

      val maxErgs: Long = 1_000_000L
      val minErgs: Long = 1000
      val maxErgsInNanoErgs: Long = maxErgs

      val maxTokens: Long = 1_000L
      val minTokens: Long = 1L
      val maxTokensInPrecision: Long = maxTokens

      (1 to 100).foreach { _ =>
        val randomErgs: Double = new Random().nextDouble()
        val randomNeutrons: Double = new Random().nextDouble()
        val randomProtons: Double = new Random().nextDouble()

        val totalErgs: Long = (maxErgsInNanoErgs * randomErgs).toLong + minErgs
        val totalNeutrons: Long =
          (maxTokensInPrecision * randomNeutrons).toLong + minTokens
        val totalProtons: Long =
          (maxTokensInPrecision * randomProtons).toLong + minTokens

        val gluonWBox: GluonWBox = genesisGluonWBox(
          ergAmount = totalErgs,
          neutronAmount = totalNeutrons,
          protonAmount = totalProtons
        )

        val protonPrice: AssetPrice = gluonWBox.getProtonsPrice(oracleBox)

        val expectedErgsFissioned: Long = totalErgs * Parameters.OneErg
        val expectedNeutronsCirculatingSupply: Long =
          totalNeutrons * GluonWBoxConstants.PRECISION
        val expectedProtonsCirculatingSupply: Long =
          totalProtons * GluonWBoxConstants.PRECISION

        assert(gluonWBox.ergFissioned == expectedErgsFissioned)
        assert(
          gluonWBox.protonsCirculatingSupply == expectedProtonsCirculatingSupply
        )
        assert(
          gluonWBox.neutronsCirculatingSupply == expectedNeutronsCirculatingSupply
        )

        val expectedPrice: Long =
          ((expectedErgsFissioned - ((expectedNeutronsCirculatingSupply * BigInt(
            oracleBox.getPrice / 1000
          )) / GluonWBoxConstants.PRECISION)) * GluonWBoxConstants.PRECISION / expectedProtonsCirculatingSupply).toLong

        assert(protonPrice.price == expectedPrice)
      }
    }
  }
}
