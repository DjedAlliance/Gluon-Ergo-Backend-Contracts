package gluonw.txs

import edge.registers.{LongPairRegister, LongRegister}
import gluonw.boxes.{GluonWBox, GluonWBoxConstants, OracleBox}
import gluonw.common.{
  GluonWAlgorithm,
  GluonWBase,
  GluonWConstants,
  TGluonWConstants
}
import org.ergoplatform.appkit.{Address, Parameters}
import edge.txs.Tx

/**
  * AdjustPegTxSpec
  *
  * Tests for Bruno's adjustPeg design:
  * - alpha is stored in R9._2 (lastDayBlockRegister._2)
  * - lastBucketBlockHeight is stored in R9._1 and must never change during adjustPeg
  * - healthy range: 0.50 <= q <= 0.98  (q = SNeutrons * P' / RErg)
  * - outside healthy range:
  *     q > 0.98 (r < 102%): alpha *= 0.99
  *     q < 0.50 (r > 200%): alpha *= 1.01
  */
class AdjustPegTxSpec extends GluonWBase {
  client.setClient()

  val PRECISION: Long = GluonWBoxConstants.PRECISION

  /** Oracle box that yields getPricePerGram = pricePerGram. */
  def oracleWithPrice(pricePerGram: Long): OracleBox =
    createTestOracleBox.copy(
      priceRegister = new LongRegister(pricePerGram * 1000L)
    )

  def healthyOracle: OracleBox = createHealthyOracleBox

  /** Build a genesis box with a custom alpha in R9._2. */
  def genesisBoxWithAlpha(alpha: Long): GluonWBox = {
    val base = genesisGluonWBox()
    base.copy(
      lastDayBlockRegister = new LongPairRegister(
        (base.lastDayBlockRegister.value._1, alpha)
      )
    )
  }

  /**
    * Box where q > 0.98 (r < 102%).
    * 10_000 neutrons circulating, 1_000 ERG fissioned:
    * q = 10_000 * PREC * 132_000_000 / (1_000 * 1e9) = 1.32 > 0.98
    */
  def lowReserveBox(alpha: Long = PRECISION): GluonWBox = {
    val neutronCirculating = 10_000L * PRECISION
    val ergFissioned = 1_000L * Parameters.OneErg
    GluonWBox.create(
      neutronAmount = GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY - neutronCirculating,
      protonAmount  = GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY,
      ergAmount     = ergFissioned + GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE
    ).copy(lastDayBlockRegister = new LongPairRegister((0L, alpha)))
  }

  /**
    * Box where q < 0.50 (r > 200%).
    * 1_000 neutrons circulating, 200_000 ERG fissioned:
    * q = 1_000 * PREC * 132_000_000 / (200_000 * 1e9) ~= 0.00066 < 0.50
    */
  def highReserveBox(alpha: Long = PRECISION): GluonWBox = {
    val neutronCirculating = 1_000L * PRECISION
    val ergFissioned = 200_000L * Parameters.OneErg
    GluonWBox.create(
      neutronAmount = GluonWBoxConstants.NEUTRONS_TOTAL_CIRCULATING_SUPPLY - neutronCirculating,
      protonAmount  = GluonWBoxConstants.PROTONS_TOTAL_CIRCULATING_SUPPLY,
      ergAmount     = ergFissioned + GluonWBoxConstants.GLUONWBOX_BOX_EXISTENCE_FEE
    ).copy(lastDayBlockRegister = new LongPairRegister((0L, alpha)))
  }

  // ===========================================================================
  // Tests
  // ===========================================================================

  "AdjustPegTx" should {

    val gluonWConstants: TGluonWConstants = GluonWConstants()
    implicit val gluonWAlgorithm: GluonWAlgorithm = GluonWAlgorithm(gluonWConstants)

    // 1. Decrease alpha when r < 102%

    "decrease alpha by 1% when q > 0.98 (r < 102%)" in {
      val inBox = lowReserveBox()
      implicit val oracle: OracleBox = healthyOracle

      val outBox = gluonWAlgorithm.adjustPeg(inBox)

      val expectedAlpha = (BigInt(PRECISION) * 99 / 100).toLong
      assert(outBox.alpha == expectedAlpha,
        s"Expected alpha=$expectedAlpha but got ${outBox.alpha}")
    }

    // 2. Increase alpha when r > 200%

    "increase alpha by 1% when q < 0.50 (r > 200%)" in {
      val inBox = highReserveBox()
      implicit val oracle: OracleBox = healthyOracle

      val outBox = gluonWAlgorithm.adjustPeg(inBox)

      val expectedAlpha = (BigInt(PRECISION) * 101 / 100).toLong
      assert(outBox.alpha == expectedAlpha,
        s"Expected alpha=$expectedAlpha but got ${outBox.alpha}")
    }

    // 3. Reject when inside healthy range

    "throw when called while box is within the healthy range" in {
      val inBox = genesisGluonWBox()   // q ~= 0.66 -> inside [0.50, 0.98]
      implicit val oracle: OracleBox = healthyOracle

      assertThrows[Exception] {
        gluonWAlgorithm.adjustPeg(inBox)
      }
    }

    // 4. R9._1 preserved; only R9._2 changes

    "preserve R9._1 (lastBucketBlockHeight) and change only R9._2 (alpha)" in {
      val sentinelBlockHeight = 123456L
      val inBox = lowReserveBox().copy(
        lastDayBlockRegister = new LongPairRegister((sentinelBlockHeight, PRECISION))
      )
      implicit val oracle: OracleBox = healthyOracle

      val outBox = gluonWAlgorithm.adjustPeg(inBox)

      assert(outBox.lastBucketBlock == sentinelBlockHeight,
        s"R9._1 must be preserved: expected $sentinelBlockHeight but got ${outBox.lastBucketBlock}")
      assert(outBox.alpha != PRECISION,
        s"R9._2 must have changed from $PRECISION")
    }

    // 5. Non-default alpha persists through normal operations

    "persist a non-default alpha through subsequent normal operations (fission)" in {
      val customAlpha = (BigInt(PRECISION) * 95 / 100).toLong
      val inBox = genesisBoxWithAlpha(customAlpha)

      val outBox = gluonWAlgorithm.fission(inBox, Parameters.OneErg)

      assert(outBox.alpha == customAlpha,
        s"Alpha must survive fission: expected $customAlpha but got ${outBox.alpha}")
    }

    // 6a. Non-default alpha: decrease direction

    "correctly decrease a non-default alpha (not 1.0) when q > 0.98" in {
      val customAlpha = (BigInt(PRECISION) * 110 / 100).toLong
      val inBox = lowReserveBox(alpha = customAlpha)
      implicit val oracle: OracleBox = healthyOracle

      val outBox = gluonWAlgorithm.adjustPeg(inBox)

      val expectedAlpha = (BigInt(customAlpha) * 99 / 100).toLong
      assert(outBox.alpha == expectedAlpha,
        s"Expected alpha=$expectedAlpha but got ${outBox.alpha}")
    }

    // 6b. Non-default alpha: increase direction

    "correctly increase a non-default alpha (not 1.0) when q < 0.50" in {
      val customAlpha = (BigInt(PRECISION) * 85 / 100).toLong
      val inBox = highReserveBox(alpha = customAlpha)
      implicit val oracle: OracleBox = healthyOracle

      val outBox = gluonWAlgorithm.adjustPeg(inBox)

      val expectedAlpha = (BigInt(customAlpha) * 101 / 100).toLong
      assert(outBox.alpha == expectedAlpha,
        s"Expected alpha=$expectedAlpha but got ${outBox.alpha}")
    }
    
    // ===========================================================================
    // On-Chain Enforcement Tests (Signing via ErgoScript Guard)
    // ===========================================================================

    "On-chain: accept valid downward adjustPeg (alpha * 0.99) when q > 0.98" in {
      client.getClient.execute { implicit ctx =>
        val inBox = lowReserveBox()
        val oracleBox = healthyOracle
        val paymentBox = createPaymentBox(value = Parameters.MinFee)

        val outBox = gluonWAlgorithm.adjustPeg(inBox)(oracleBox)
        
        val adjustTx: Tx = Tx(
          inputBoxes = Seq(inBox.getAsInputBox(), paymentBox),
          changeAddress = trueAddress,
          dataInputs = Seq(oracleBox.getAsInputBox()),
          outBoxes = Seq(outBox)
        )
        // Should sign successfully
        adjustTx.signTx
      }
    }

    "On-chain: REJECT tampered downward adjustPeg (alpha * 0.98 instead of 0.99) when q > 0.98" in {
      client.getClient.execute { implicit ctx =>
        val inBox = lowReserveBox()
        val oracleBox = healthyOracle
        val paymentBox = createPaymentBox(value = Parameters.MinFee)

        val validOutBox = gluonWAlgorithm.adjustPeg(inBox)(oracleBox)
        val tamperedAlpha = (BigInt(PRECISION) * 98 / 100).toLong
        val tamperedOutBox = validOutBox.copy(
          lastDayBlockRegister = new LongPairRegister(
            (validOutBox.lastBucketBlock, tamperedAlpha)
          )
        )
        
        val hackTx: Tx = Tx(
          inputBoxes = Seq(inBox.getAsInputBox(), paymentBox),
          changeAddress = trueAddress,
          dataInputs = Seq(oracleBox.getAsInputBox()),
          outBoxes = Seq(tamperedOutBox)
        )
        // ErgoScript guard should reject the signature
        assertThrows[Throwable] {
          hackTx.signTx
        }
      }
    }

    "On-chain: accept valid upward adjustPeg (alpha * 1.01) when q < 0.50" in {
      client.getClient.execute { implicit ctx =>
        val inBox = highReserveBox()
        val oracleBox = healthyOracle
        val paymentBox = createPaymentBox(value = Parameters.MinFee)

        val outBox = gluonWAlgorithm.adjustPeg(inBox)(oracleBox)
        
        val adjustTx: Tx = Tx(
          inputBoxes = Seq(inBox.getAsInputBox(), paymentBox),
          changeAddress = trueAddress,
          dataInputs = Seq(oracleBox.getAsInputBox()),
          outBoxes = Seq(outBox)
        )
        // Should sign successfully
        adjustTx.signTx
      }
    }

    "On-chain: REJECT tampered upward adjustPeg (alpha * 1.02 instead of 1.01) when q < 0.50" in {
      client.getClient.execute { implicit ctx =>
        val inBox = highReserveBox()
        val oracleBox = healthyOracle
        val paymentBox = createPaymentBox(value = Parameters.MinFee)

        val validOutBox = gluonWAlgorithm.adjustPeg(inBox)(oracleBox)
        val tamperedAlpha = (BigInt(PRECISION) * 102 / 100).toLong
        val tamperedOutBox = validOutBox.copy(
          lastDayBlockRegister = new LongPairRegister(
            (validOutBox.lastBucketBlock, tamperedAlpha)
          )
        )
        
        val hackTx: Tx = Tx(
          inputBoxes = Seq(inBox.getAsInputBox(), paymentBox),
          changeAddress = trueAddress,
          dataInputs = Seq(oracleBox.getAsInputBox()),
          outBoxes = Seq(tamperedOutBox)
        )
        // ErgoScript guard should reject the signature
        assertThrows[Throwable] {
          hackTx.signTx
        }
      }
    }
  }
}
