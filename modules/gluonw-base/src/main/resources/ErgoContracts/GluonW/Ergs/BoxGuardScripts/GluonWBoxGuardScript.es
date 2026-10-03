{
    // ===== Contract Info ===== //
    // Name             : Gluon Reactor
    // Type             : Guard Script
    // Author           : Kii, LGD, Zahnentferner
    // Last Modified    : 2026-10-02
    // Version          : v 3.0

    // ===== Version Logs ===== //
    // - 1.0: Basic Gluon without dev fees and UI fees
    // - 1.1: Dev fees and UI fees implemented
    // - 2.0: Oracle check enforced
    // - 3.0: Peg adjustment implemented

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _OracleFeePk:                Coll[Byte]
    // val _OraclePoolNFT:              Coll[Byte]
    // val _OracleBuybackNFT:           Coll[Byte]

    // ===== Box Contents ===== //
    // Tokens
    // 1. (GluonNFT, 1)
    // 2. (Neutrons, Long)
    // 3. (Protons, Long)
    //
    // Registers
    // R4 - (Total Neutrons Supply, Total Protons Supply): (Long, Long)
    // R5 - TreasuryMultisig: SigmaProp
    // R6 - (TotalDevFeesPaid, MaxAmountDevFeesPaid): (Long, Long)
    // R7 - BetaPlusVolume: Coll[Long]
    // R8 - BetaMinusVolume: Coll[Long]
    // R9 - (LastBucketBlock, PegFactor): (Long, Long)

    // ===== Context Vars ===== //
    // val _optUIFeeAddress                    SigmaProp

    // ===== Transaction Types ===== //
    // 1. Fission                   - The user sends Ergs to the reactor (bank) and receives Neutrons and Protons
    // 2. Fusion                    - The user sends Neutrons and Protons to the reactor and receives Ergs
    // 3. Beta Decay +              - The user sends Protons to the reactor and receives Neutrons
    // 4. Beta Decay -              - The user sends Neutrons to the reactor and receives Protons
    // 5. Adjust Peg Factor         - Any user readjusts the peg if the fusion ratio is outside the bounds of the healthy range.
    // 6. Update Treasury Multisig  - The current treasury multisig is used to create a new box containing the updated treasury multisig address.

    // For all of the first five transactions:
    // Inputs: GluonBox, UserPk
    // DataInputs: GoldOracle
    // Outputs: GluonBox, UserPk

    // For the sixth transaction:
    // Inputs: GluonBox, MultisigUtxo
    // DataInputs: None
    // Outputs: GluonBox

    val TREASURY_MULTISIG: SigmaProp = SELF.R5[SigmaProp].get
    val IN_GLUON_BOX: Box = SELF
    val OUT_GLUON_BOX: Box = OUTPUTS(0)
    val ORACLE_BOX: Box = CONTEXT.dataInputs(0)
    val ASSET_TOTAL_SUPPLY_REGISTER: (Long, Long) = IN_GLUON_BOX.R4[(Long, Long)].get
    val ASSET_MAX_DEV_FEE_THRESHOLD: (Long, Long) = IN_GLUON_BOX.R6[(Long, Long)].get
    val OUT_ASSET_MAX_DEV_FEE_THRESHOLD: (Long, Long) = OUT_GLUON_BOX.R6[(Long, Long)].get
    val NEUTRONS_TOTAL_SUPPLY: Long = ASSET_TOTAL_SUPPLY_REGISTER._1
    val PROTONS_TOTAL_SUPPLY: Long = ASSET_TOTAL_SUPPLY_REGISTER._2
    val DEV_FEE_REPAID: Long = ASSET_MAX_DEV_FEE_THRESHOLD._1
    val MAX_DEV_FEE_THRESHOLD: Long = ASSET_MAX_DEV_FEE_THRESHOLD._2
    val OUT_DEV_FEE_REPAID: Long = OUT_ASSET_MAX_DEV_FEE_THRESHOLD._1
    val OUT_MAX_DEV_FEE_THRESHOLD: Long = OUT_ASSET_MAX_DEV_FEE_THRESHOLD._2

    val IN_GLUON_NEUTRONS_TOKEN: (Coll[Byte], Long) = IN_GLUON_BOX.tokens(1)
    val IN_GLUON_PROTONS_TOKEN: (Coll[Byte], Long) = IN_GLUON_BOX.tokens(2)

    val OUT_GLUON_NEUTRONS_TOKEN: (Coll[Byte], Long) = OUT_GLUON_BOX.tokens(1)
    val OUT_GLUON_PROTONS_TOKEN: (Coll[Byte], Long) = OUT_GLUON_BOX.tokens(2)

    val inVolumePlus: Coll[Long] = IN_GLUON_BOX.R7[Coll[Long]].get
    val inVolumeMinus: Coll[Long] = IN_GLUON_BOX.R8[Coll[Long]].get
    val outVolumePlus: Coll[Long] = OUT_GLUON_BOX.R7[Coll[Long]].get
    val outVolumeMinus: Coll[Long] = OUT_GLUON_BOX.R8[Coll[Long]].get

    val inR9: (Long, Long) = IN_GLUON_BOX.R9[(Long, Long)].get
    val outR9: (Long, Long) = OUT_GLUON_BOX.R9[(Long, Long)].get
    val inLastBucketBlock: Long = inR9._1
    val outLastBucketBlock: Long = outR9._1
    val inPegFactor: Long = inR9._2
    val outPegFactor: Long = outR9._2

    val BLOCKS_PER_VOLUME_BUCKET: Int = 720 // Approximately 1 day per volume bucket
    val BUCKETS: Int = 14 // Tracking volume of approximately 14 days

    // ===== (START) Oracle Checks ===== //
    // The two checks for the oracle is:
    // 1. It has the right NFT on it
    // 2. Its height is within 70 min, (35 blocks)
    val oracleBoxCreationHeightDifferenceFromNow: Int = CONTEXT.HEIGHT - ORACLE_BOX.creationInfo._1
    val oracleBoxPoolNFT: (Coll[Byte], Long) = ORACLE_BOX.tokens(0)

    val __oracleCheck: Boolean = allOf(Coll(
        oracleBoxCreationHeightDifferenceFromNow < 35 && oracleBoxCreationHeightDifferenceFromNow >= 0,
        oracleBoxPoolNFT._1 == _OraclePoolNFT
    ))
    // ===== (END) Oracle Checks ===== //

    val one: BigInt                = (1000000000).toBigInt // one is 1,000,000,000 because we are using 9 decimal digits.

    // # Parameters
    val qStar: BigInt           = (99 * one / 100) // q* = 99%
    val qUpperThreshold: BigInt = (98 * one / 100) // qUpper = 98%
    val qLowerThreshold: BigInt = one / 2          // qLower = 50%
    val PhiFission: BigInt = (one / 1000).toBigInt // fission fee = 0.1%
    val PhiFusion: BigInt  = (one / 200).toBigInt  // fusion fee  = 0.5%
    val Phi0 = one / 200 // BetaDecay Fee y-intercept: Phi0 = 0.5%
    val Phi1 = one       // BetaDecay Fee slope:       Phi1 = 1

    // # Internal State Variables
    val SNeutrons: BigInt = (NEUTRONS_TOTAL_SUPPLY - IN_GLUON_NEUTRONS_TOKEN._2).toBigInt // Variable in Paper: S_neutrons
    val SProtons: BigInt  = (PROTONS_TOTAL_SUPPLY - IN_GLUON_PROTONS_TOKEN._2).toBigInt   // Variable in Paper: S_protons
    val RErg: BigInt      = (IN_GLUON_BOX.value - _MinFee).toBigInt // Variable in Paper: R
    
    // # External State Variables
    val Pt: BigInt        = ORACLE_BOX.R4[Long].get.toBigInt / 1000  // Oracle price

    // # State Dependent Variables
    val normalizedPt: BigInt     = Pt * inPegFactor.toBigInt / one // Adjusted oracle price: P_adjusted = Pt * pegFactor / one
    val q: BigInt = SNeutrons * normalizedPt / RErg // Current fusion ratio
    val fusionRatio: BigInt = min(one * q / (q + one - qStar), q)

    val isHealthyRange: Boolean    = (q >= qLowerThreshold) && (q <= qUpperThreshold) // Fusion, fission and beta decays only permitted when 0.50 <= q <= 0.98

    // ====== Tx Definitions ===== //

    // Note: AmountInCirculation = TotalSupply - AmountInReactorBox
    // Therefore an increase/decrease in circulation means AmountInReactorBox decreases/increases

    // # Fission: Splits ERG into protons and neutrons (mints protons and neutrons)
    val isFissionTx: Boolean = allOf(Coll(
        IN_GLUON_NEUTRONS_TOKEN._2 > OUT_GLUON_NEUTRONS_TOKEN._2, // Check Neutrons decrease
        IN_GLUON_PROTONS_TOKEN._2 > OUT_GLUON_PROTONS_TOKEN._2,   // Check Protons increase
        IN_GLUON_BOX.value < OUT_GLUON_BOX.value                  // Check ERG value increases
    ))

    // # Fission: Merges protons and neutrons into ERG (redeems protons and neutrons)
    val isFusionTx: Boolean = allOf(Coll(
        IN_GLUON_NEUTRONS_TOKEN._2 < OUT_GLUON_NEUTRONS_TOKEN._2, // Check Neutrons increase
        IN_GLUON_PROTONS_TOKEN._2 < OUT_GLUON_PROTONS_TOKEN._2,   // Check Protons increase
        IN_GLUON_BOX.value > OUT_GLUON_BOX.value                  // Check ERG value decreases
    ))

    // # BetaDecayPlus: Transmutes Protons to Neutrons
    // Decreases protons in circulation and increases neutrons in circulation
    val isBetaDecayPlusTx: Boolean = allOf(Coll(
        IN_GLUON_NEUTRONS_TOKEN._2 > OUT_GLUON_NEUTRONS_TOKEN._2, // Check Neutrons decrease
        IN_GLUON_PROTONS_TOKEN._2 < OUT_GLUON_PROTONS_TOKEN._2,   // Check Protons increase
        IN_GLUON_BOX.value == OUT_GLUON_BOX.value // Check ERG value is preserved
    ))

    // # BetaDecayPlus: Transmutes Neutrons to Protons
    // Decreases neutrons in circulation and increases protons in circulation
    val isBetaDecayMinusTx: Boolean = allOf(Coll(
        IN_GLUON_NEUTRONS_TOKEN._2 < OUT_GLUON_NEUTRONS_TOKEN._2, // Check Neutrons increase
        IN_GLUON_PROTONS_TOKEN._2 > OUT_GLUON_PROTONS_TOKEN._2,   // Check Protons decrease
        IN_GLUON_BOX.value == OUT_GLUON_BOX.value                 // Check ERG value is preserved
    ))

    // # AdjustPeg: Changes the peg factor that determines the peg
    val isAdjustPegTx: Boolean = (inPegFactor != outPegFactor)

    // # UpdateTreasury: Changes the address that receives dev fees
    val isUpdateTreasury: Boolean = (INPUTS(1).propositionBytes == TREASURY_MULTISIG.propBytes)

    // ===== (END) Tx Definition ===== //

    // Preservation Checks: When a transaction does not change something, we must explicitly check that it remained unchanged
    val cSameContract: Boolean = IN_GLUON_BOX.propositionBytes == OUT_GLUON_BOX.propositionBytes
    val cSameTokens: Boolean   = IN_GLUON_BOX.tokens == OUT_GLUON_BOX.tokens
    val cSameTokenIdentifiers: Boolean = IN_GLUON_BOX.tokens(0)._1 == OUT_GLUON_BOX.tokens(0)._1 && // For fission, fusion and beta decays,
                                         IN_GLUON_BOX.tokens(1)._1 == OUT_GLUON_BOX.tokens(1)._1 && // token amounts may change,
                                         IN_GLUON_BOX.tokens(2)._1 == OUT_GLUON_BOX.tokens(2)._1    // but the token identifiers must be preserved
    val cSameValue: Boolean    = IN_GLUON_BOX.value  == OUT_GLUON_BOX.value
    val cSameR4: Boolean       = IN_GLUON_BOX.R4[(Long,Long)].get == OUT_GLUON_BOX.R4[(Long,Long)].get  // Total Neutron and Proton Suplies preserved
    val cSameR5: Boolean       = IN_GLUON_BOX.R5[SigmaProp].get  == OUT_GLUON_BOX.R5[SigmaProp].get     // Treasury Multisig preserved    
    val cSameR6: Boolean       = IN_GLUON_BOX.R6[(Long,Long)].get == OUT_GLUON_BOX.R6[(Long,Long)].get  // Dev fees accounting preserved
    val cSameR7: Boolean       = IN_GLUON_BOX.R7[Coll[Long]].get == OUT_GLUON_BOX.R7[Coll[Long]].get    // BetaDecayPlus volume preserved
    val cSameR8: Boolean       = IN_GLUON_BOX.R8[Coll[Long]].get == OUT_GLUON_BOX.R8[Coll[Long]].get    // BetaDecayMinus volume preserved
    val cSameR9: Boolean       = IN_GLUON_BOX.R9[(Long, Long)].get == OUT_GLUON_BOX.R9[(Long, Long)].get // LastBucketBlock and PegFactor preserved
    val cSameR9LastBucketBlock: Boolean = inLastBucketBlock == outLastBucketBlock // LastBucketBlock preserved
    val cSameR9PegFactor: Boolean = inPegFactor == outPegFactor                   // PegFactor preserved


    if (anyOf(Coll(isFissionTx, isFusionTx, isBetaDecayPlusTx, isBetaDecayMinusTx))) {
        def valueOfProtons(protonsAmount: Long): BigInt = {
            val protonsPrice: BigInt = (one - fusionRatio).toBigInt * RErg / SProtons
            val protonsInNanoergs: BigInt = protonsAmount.toBigInt * protonsPrice / one
            protonsInNanoergs
        }

        def valueOfNeutrons(neutronsAmount: Long): BigInt = {
            val neutronPrice: BigInt = (fusionRatio * RErg) / SNeutrons
            val neutronsInNanoergs: BigInt = neutronsAmount.toBigInt * neutronPrice / one
            neutronsInNanoergs
        }

        def sum(collLong: Coll[Long]): BigInt = {
            collLong.fold(0L, {(acc: Long, indexedValue: Long) => acc + indexedValue}).toBigInt
        }

        

        // ===== (START) Fee Declarations ===== //
        // Reference from https://github.com/K-Singh/Sigma-Finance/blob/master/contracts/ex/ExOrderERG.ergo
        val _optUIFeeAddress = getVar[SigmaProp](0)
        val fees: Coll[(Coll[Byte], BigInt)] = {
            val feeDenom: BigInt = 1000L.toBigInt
            val devFee: BigInt = 5L.toBigInt
            val oracleFee: BigInt = 1L.toBigInt
            val uiFee: BigInt = 4L.toBigInt
            val emptyFees: (Coll[Byte], Long) = (Coll(1.toByte), 0L.toBigInt)

            // principal is the amount that is requested
            val principal: BigInt = if (isFissionTx) {
                    (OUT_GLUON_BOX.value - IN_GLUON_BOX.value).toBigInt
                } else if (isFusionTx) {
                    (IN_GLUON_BOX.value - OUT_GLUON_BOX.value).toBigInt
                } else if (isBetaDecayPlusTx) {
                    // Calculate the value based on protons
                    // Check Protons reduction in OutBox
                    val protonsAmount: Long = OUT_GLUON_PROTONS_TOKEN._2 - IN_GLUON_PROTONS_TOKEN._2
                    val protonsInNanoergs: BigInt = valueOfProtons(protonsAmount)
                    protonsInNanoergs
                } else {
                    // Calculate the value based on neutrons
                    val neutronsAmount: Long = OUT_GLUON_NEUTRONS_TOKEN._2 - IN_GLUON_NEUTRONS_TOKEN._2
                    val neutronsInNanoergs: BigInt = valueOfNeutrons(neutronsAmount)
                    neutronsInNanoergs
                }

            val devFeePayout: BigInt = if (DEV_FEE_REPAID < MAX_DEV_FEE_THRESHOLD) {
                val initialFee: BigInt = (devFee * principal) / feeDenom
                val decayedFee: BigInt = initialFee * (MAX_DEV_FEE_THRESHOLD - DEV_FEE_REPAID) / MAX_DEV_FEE_THRESHOLD
                decayedFee
            } else {
                0L.toBigInt
            }
            val uiFeePayout: BigInt = (uiFee * principal) / feeDenom
            val oracleFeePayout: BigInt = (oracleFee * principal) / feeDenom

            val devFeeAddressAndPayout: (Coll[Byte], BigInt) =
                (TREASURY_MULTISIG.propBytes, devFeePayout)
            val oracleFeeAddressAndPayout: (Coll[Byte], BigInt) =
                (_OracleFeePk, oracleFeePayout)

            // TODO: lots of refactoring possibilities here:
            if (isBetaDecayMinusTx || isBetaDecayPlusTx) { // Fission and Fusion does not need Oracle
                if (_optUIFeeAddress.isDefined) { // If Ui fee is defined, then we add an additional 0.4% fee
                    Coll(
                        devFeeAddressAndPayout,
                        oracleFeeAddressAndPayout,
                        (_optUIFeeAddress.get.propBytes, uiFeePayout),
                    )
                }
                else {
                    Coll(
                        devFeeAddressAndPayout,
                        oracleFeeAddressAndPayout,
                        emptyFees,
                    )
                }
            }
            else {
                // If Ui fee is defined, then we add an additional 0.4% fee
                if (_optUIFeeAddress.isDefined) {
                    Coll(
                        devFeeAddressAndPayout,
                        (_optUIFeeAddress.get.propBytes, uiFeePayout),
                        emptyFees,
                    )
                }
                else {
                    Coll(
                        devFeeAddressAndPayout,
                        emptyFees,
                        emptyFees
                    )
                }
            }
        }

        val feesPaid: Boolean = {
            val uiFees: (Coll[Byte], BigInt) = if (isBetaDecayPlusTx || isBetaDecayMinusTx) fees(2) else fees(1)
            val uiFeesToBePaid: Boolean = uiFees._2 > 0
            val oracleFeesToBePaid: Boolean = fees(1)._2 > 0

            val oracleOutput: Box = OUTPUTS(2)

             val oracleFeesPaid: Boolean = {
                if (isBetaDecayPlusTx || isBetaDecayMinusTx) {
                    if (oracleFeesToBePaid) { // Oracle fee is greater than 0
                        // The oracle buy back input box is always the last input
                        val oracleBuybackInputBox: Box = INPUTS(INPUTS.size - 1)
                        allOf(
                            Coll(
                                oracleOutput.propositionBytes       == fees(1)._1,
                                oracleOutput.propositionBytes       == oracleBuybackInputBox.propositionBytes,
                                oracleOutput.tokens(0)._1           == _OracleBuybackNFT,
                                oracleOutput.value.toBigInt         == oracleBuybackInputBox.value.toBigInt + fees(1)._2 + _MinFee
                            )
                        )
                    } else {
                        true // do nothing if dev fee doesn't add up greater than 0, prevents errors on low value fee
                    }
                } else {
                    true // if oracle fee is not defined, then default to true.
                }
            }

            val devFeesPaid: Boolean = {
                if (fees(0)._2 > 0)
                {
                    // Dev fee is greater than 0
                    // If there is a need to pay oracle fees, we check OUTPUTS(3)
                    val devOutput: Box = if (!oracleFeesToBePaid) { OUTPUTS(2) } else { OUTPUTS(3) }
                    allOf(
                        Coll(
                            devOutput.propositionBytes      == fees(0)._1,
                            devOutput.value.toBigInt        == fees(0)._2 + _MinFee
                        )
                    )
                }
                else
                {
                    true // do nothing if dev fee doesn't add up greater than 0, prevents errors on low value fees
                }
            }

            val uiFeesPaid: Boolean = {
                if (_optUIFeeAddress.isDefined)
                {
                    if(fees(1)._2 > 0) {
                        // UI fee is greater than 0
                        val uiOutput: Box = if (!oracleFeesToBePaid) { OUTPUTS(3) } else { OUTPUTS(4) }
                        allOf(
                            Coll(
                                uiOutput.propositionBytes       == fees(1)._1,
                                uiOutput.value.toBigInt         == fees(1)._2 + _MinFee
                            )
                        )
                    }
                    else
                    {
                        true // do nothing if ui fee doesn't end up greater than 0, prevents errors on low value fee
                    }
                } else {
                    true // if ui fee isn't defined, then default to true.
                }
            }

            devFeesPaid && uiFeesPaid && oracleFeesPaid
        }

        val devFeeRepaidValueAdded: Boolean = (OUT_DEV_FEE_REPAID - DEV_FEE_REPAID) == fees(0)._2
        val maxDevFeeThresholdSame: Boolean = OUT_MAX_DEV_FEE_THRESHOLD == MAX_DEV_FEE_THRESHOLD

        val __feesCheck: Boolean = allOf(Coll(
            feesPaid,
            devFeeRepaidValueAdded,
            maxDevFeeThresholdSame
        ))
        // ===== (END) Fee Declarations ===== //

        // In the case of fission and fusion transactions, the variables related to volume handling should remain unchanged
        val volumePlusPreserved = inVolumePlus == outVolumePlus
        val volumeMinusPreserved = inVolumeMinus == outVolumeMinus
        val lastBucketBlockPreserved = inLastBucketBlock == outLastBucketBlock
        val __validVolumeHandling = allOf(Coll(volumePlusPreserved, volumeMinusPreserved, lastBucketBlockPreserved))

        // NOTE:
        // In all of these transactions, the Input value varies, however, the output does not. The output is exactly how much
        // the user wants. Therefore we can use the outbox to calculate the value of M by using OutBox.value - InBox.value
        if (isFissionTx) {
            // Equation: M [Ergs] ==> (M (1 - PhiFission) (S Protons / R)) [Protons] + (M (1 - PhiFission) (S Neutrons / R)) [Neutrons]
            val M: BigInt = (OUT_GLUON_BOX.value - IN_GLUON_BOX.value).toBigInt 

            val NeutronsActualValue: BigInt = (IN_GLUON_NEUTRONS_TOKEN._2 - OUT_GLUON_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (IN_GLUON_PROTONS_TOKEN._2 - OUT_GLUON_PROTONS_TOKEN._2).toBigInt

            val NeutronsExpectedValue: BigInt = (M * SNeutrons * (one - PhiFission) / RErg) / one
            val ProtonsExpectedValue: BigInt = (M * SProtons * (one - PhiFission) / RErg) / one

            // ### The 2 conditions to ensure that the values out are right ### //
            val __outNeutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __outProtonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue

            sigmaProp(allOf(Coll(
                isHealthyRange,
                cSameContract, cSameTokenIdentifiers,
                __outNeutronsValueValid, __outProtonsValueValid,
                cSameR4, cSameR5, cSameR6, 
                __validVolumeHandling, // TODO: Replace by cSameR7 and cSameR8?
                cSameR9PegFactor,
                __feesCheck
            )))
        }
        else if (isFusionTx) {
            // Equation: (M (S neutrons / R)) [Protons] + (M (S protons / R)) [Neutrons] ==> M (1 - PhiFission) [Ergs]

            // The protons and neutrons are more in outbox than inputbox
            val NeutronsActualValue: BigInt = (OUT_GLUON_NEUTRONS_TOKEN._2 - IN_GLUON_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (OUT_GLUON_PROTONS_TOKEN._2 - IN_GLUON_PROTONS_TOKEN._2).toBigInt

            // M = Ergs
            val M: BigInt = (IN_GLUON_BOX.value - OUT_GLUON_BOX.value).toBigInt

            val inProtonsNumerator: BigInt = M * SProtons * one
            val inNeutronsNumerator: BigInt = M * SNeutrons * one
            val denominator: BigInt = RErg * (one - PhiFusion)

            val NeutronsExpectedValue: BigInt = inNeutronsNumerator / denominator
            val ProtonsExpectedValue: BigInt =  inProtonsNumerator / denominator

            // ### The 2 conditions to ensure that the values out is right ### //
            val __inNeutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __inProtonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue

            sigmaProp(allOf(Coll(
                isHealthyRange,
                cSameContract, cSameTokenIdentifiers,  
                __inNeutronsValueValid, __inProtonsValueValid,
                cSameR4, cSameR5, cSameR6, 
                 __validVolumeHandling, // TODO: Replace by cSameR7 and cSameR8?
                cSameR9PegFactor,
                __feesCheck
            )))
        }
        else if (isBetaDecayPlusTx) {
            // Equation: M [Protons] ==> M * (1 - PhiBeta(T)) * ((1 - q(R, S neutron)) / q(R, S neutron)) * (S neutrons / S protons) [Neutrons]

            // Equations for determining the proton price, Pp, and proton volume, Vp, given N protons.
            // q  = min(q*, Sn*Pt/R)
            // Pp = (1-q) * R / Sp
            // Vp = N*Pp

            // Proton value
            val M: Long = (OUT_GLUON_PROTONS_TOKEN._2 - IN_GLUON_PROTONS_TOKEN._2)

            // The protons increase in output, neutrons decrease in outputs
            val NeutronsActualValue: BigInt = (IN_GLUON_NEUTRONS_TOKEN._2 - OUT_GLUON_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (OUT_GLUON_PROTONS_TOKEN._2 - IN_GLUON_PROTONS_TOKEN._2).toBigInt
            val ErgsActualValue: BigInt = (OUT_GLUON_BOX.value).toBigInt

            // === VarPhiBeta Calculation === //
            val currentBlockNumber: Long = CONTEXT.HEIGHT

            // Check Protons reduction in OutBox
            val worthOfMInErgs: BigInt = valueOfProtons(M) // This actually represents the volume of protons in units of Erg, M being the amount of protons.

            // Calculate the amount of days that has been since the last betaDecayTx
            // 1000 - 200 = 800 | 800 / 720 = 1
            val nDays: Int = ((currentBlockNumber - inLastBucketBlock) / BLOCKS_PER_VOLUME_BUCKET).toInt

            // We don't need to shift it, we just need to check if outVolumePlus is correct.
            // Therefore, if there is a requirement to shift, we just need to check if the
            // value after n is the same for the next 14.
            //
            // Here's an example:
            // assuming our initial block is this
            // [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
            //
            // If nDays = 4, and worthOfMInErgs = x
            // We should expect:
            // [x, 0, 0 ,0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
            //
            // If nDays = 0, and worthOfMInErgs = x
            // [1 + x, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
            //
            // The conditions are:
            // 1. The value in outVolumePlus(0) should be worthOfMInErgs + if (n == 0) inVolumePlus(0) else 0
            // 2. For the block after 0, if n > 1, then n - 1 of the blocks after should be 0.
            // 3. The rest of the value, outVolumePlus(x) [where x = n up to 14 - n] should be
            //      equal to inVolumePlus(y) [where y = 0 up to n]
            // 4. The volume should be 14.
            // The same conditions go for outVolumeMinus, other than #1 whereby, it is replaced
            // by outVolumeMinus(0) == if (nDays == 0) {inVolumeMinus(0)} else {0L}

            // #1
            val outVolumePlusExpectedValue = (if (nDays == 0) {inVolumePlus(0)} else {0L}) + worthOfMInErgs
            val _volumePlusAccounted = outVolumePlus(0) == outVolumePlusExpectedValue

            // #2
            // We sliced n - 1 of the value in between, and we check if all of it are 0s.
            val slicedNVolumePlus: Coll[Long] = outVolumePlus.slice(1, nDays)
            val _nVolumePlusAllZeros: Boolean = slicedNVolumePlus.forall{(indexedValue: Long) => indexedValue == 0L}

            // #3
            // If we slice the correct pieces from in and out, we should get the same
            // exact value
            val slicedOutVolumePlus: Coll[Long] = outVolumePlus.slice(nDays, BUCKETS)
            val slicedInVolumePlus: Coll[Long] = inVolumePlus.slice(0, BUCKETS - nDays)
            val _isSlicedValuedVolumePlusEqual: Boolean = if (nDays > 0) {
                // When there are multiple days involved, we have to compare the days
                // that are pushed towards the right in outVolumeMinus, this starts at
                // nDays and end at the last index.
                // For inVolumeMinus, it would be the first till BUCKETS - nDays
                slicedOutVolumePlus == slicedInVolumePlus
            } else {
                // When the days are the same, we compare 1 - BUCKETS because only the
                // first index changed.
                outVolumePlus.slice(1, BUCKETS) == inVolumePlus.slice(1, BUCKETS)
            }

            val __outVolumePlusValidated: Boolean = allOf(Coll(
                outVolumePlus.size == BUCKETS,
                _volumePlusAccounted,
                _isSlicedValuedVolumePlusEqual,
                _nVolumePlusAllZeros
            ))

            // #1
            val outVolumeMinusExpectedValue = if (nDays == 0) {inVolumeMinus(0)} else {0L}
            val _outVolumeMinusFirstIndexedPreserved = outVolumeMinusExpectedValue == outVolumeMinus(0)

            // #2
            // We sliced n - 1 of the value in between, and we check if all of it are 0s.
            val slicedNVolumeMinus: Coll[Long] = outVolumeMinus.slice(1, nDays)
            val _nVolumeMinusAllZeros: Boolean = slicedNVolumeMinus.forall{(indexedValue: Long) => indexedValue == 0L}

            // #3
            val slicedOutVolumeMinus: Coll[Long] = outVolumeMinus.slice(nDays, BUCKETS)
            val slicedInVolumeMinus: Coll[Long] = inVolumeMinus.slice(0, BUCKETS - nDays)
            val _isSlicedValuedVolumeMinusEqual: Boolean = if (nDays > 0) {
                // When there are multiple days involved, we have to compare the days
                // that are pushed towards the right in outVolumeMinus, this starts at
                // nDays and end at the last index.
                // For inVolumeMinus, it would be the first till BUCKETS - nDays.
                slicedOutVolumeMinus == slicedInVolumeMinus
            } else {
                // When the days are the same, we compare 1 - BUCKETS
                // because only the first index changed.
                outVolumeMinus.slice(1, BUCKETS) == inVolumeMinus.slice(1, BUCKETS)
            }

            val __outVolumeMinusValidated: Boolean = allOf(Coll(
                outVolumeMinus.size == BUCKETS,
                _outVolumeMinusFirstIndexedPreserved,
                _isSlicedValuedVolumeMinusEqual,
                _nVolumeMinusAllZeros
            ))

            val volumePlus: BigInt = sum(outVolumePlus) // adds all elements of the collection, computing the total volume
            val volumeMinus: BigInt = sum(outVolumeMinus)

            val volume: BigInt = if (volumeMinus > volumePlus) {0L.toBigInt} else {volumePlus - volumeMinus} // integer subtraction

            // === Tx FEE for pool === //
            // This is the fee that gets collected to add into the pool during decay.

            val VarPhiBeta: BigInt = Phi0 + ((Phi1 * volume) / RErg)

            // Due to some issues with moving towards the next block. We should give it a margin of error of +/- 3 blocks.
            // There is a tricky situation where if the lastblock is within a day, and if it is always updated,
            // then we will always be at day 0 as long as there is a decay that happened within a day before
            // the lastBlockPreserved.
            //
            // To counteract this situation, we want to only get the currentBlockNumber that is closest to the previous Blocks_Per_volume_bucket.
            val closestPreviousBlockValueViaBuckets: Int = (currentBlockNumber / BLOCKS_PER_VOLUME_BUCKET) * BLOCKS_PER_VOLUME_BUCKET
            val __lastBlockPreserved: Boolean = outLastBucketBlock == closestPreviousBlockValueViaBuckets

            // === VarPhiBeta Calculation End === //

            // === Fusion Ratio === //

            // The steps of multiplication and division done below are to avoid overflow errors.
            val oneMinusPhiBeta: BigInt = (one - VarPhiBeta)
            val oneMinusFusionRatio: BigInt = (one - fusionRatio)
            val ratio1: BigInt = (M.toBigInt * oneMinusPhiBeta) / SProtons
            val ratio2: BigInt = (oneMinusFusionRatio * SNeutrons) / one
            val outNeutronsAmount: BigInt = (ratio1 * ratio2) / fusionRatio

            val NeutronsExpectedValue: BigInt = outNeutronsAmount
            val ProtonsExpectedValue: BigInt = M.toBigInt
            val ErgsExpectedValue: BigInt = (IN_GLUON_BOX.value).toBigInt

            // ### The 2 conditions to ensure that the values out is right ### //
            val __neutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __protonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
            val __ergsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

            sigmaProp(allOf(Coll(
                isHealthyRange,
                cSameContract, cSameTokenIdentifiers,  
                __neutronsValueValid, __protonsValueValid, __ergsValueValid,
                cSameR4, cSameR5, cSameR6,
                __outVolumeMinusValidated, __outVolumePlusValidated, 
                __lastBlockPreserved, // TODO: Replace this and the next line by cSameR9?
                cSameR9PegFactor,
                __feesCheck,
                __oracleCheck
            )))
        } else if (isBetaDecayMinusTx) {
            //Equation: M [Neutrons] = M * (1 - PhiBeta(T)) * ((q(R, S neutron)) / 1 - q(R, S neutron)) * (S protons / S neutrons) [Protons]
            
            val M: Long = (OUT_GLUON_NEUTRONS_TOKEN._2 - IN_GLUON_NEUTRONS_TOKEN._2)

            // Equations for determining the neutron price, Pn, and neutron volume, Vn, given N neutrons.
            // Note that the target price, Pt, i.e. oracle price, is not the same as the neutron price.
            // q = min(q*, Sn*Pt/R)
            // Pn = q * R / Sn
            // Vn = N*Pn

            // === VarPhiBeta Calculation === //
            val currentBlockNumber: Long = CONTEXT.HEIGHT

            // Check Neutrons reduction in OutBox
            val worthOfMInErgs: BigInt = valueOfNeutrons(M) // This actually represents the volume of neutrons in units of Erg, M being the amount of neutrons.

            // Calculate the amount of days that has been since the last betaDecayTx
            // 1000 - 200 = 800 | 800 / 720 = 1
            val getNDaysPreFilteredValue: Int = ((currentBlockNumber - inLastBucketBlock) / BLOCKS_PER_VOLUME_BUCKET).toInt
            val nDays: Int = if (getNDaysPreFilteredValue >= BUCKETS) {BUCKETS} else getNDaysPreFilteredValue

            // SAME AS BetaDecayPlus, but reversed between plus and minus
            // #1
            val outVolumeMinusExpectedValue = (if (nDays == 0) {inVolumeMinus(0)} else {0L}) + worthOfMInErgs
            val _volumeMinusAccounted = outVolumeMinus(0) == outVolumeMinusExpectedValue

            // #2
            // We sliced n - 1 of the value in between, and we check if all of it are 0s.
            val slicedNVolumeMinus: Coll[Long] = outVolumeMinus.slice(1, nDays)
            val _nVolumeMinusAllZeros: Boolean = slicedNVolumeMinus.forall{(indexedValue: Long) => indexedValue == 0L}

            // #3
            // If we slice the correct pieces from in and out, we should get the same
            // exact value.
            val slicedOutVolumeMinus: Coll[Long] = outVolumeMinus.slice(nDays, BUCKETS)
            val slicedInVolumeMinus: Coll[Long] = inVolumeMinus.slice(0, BUCKETS - nDays)
            val _isSlicedValuedVolumeMinusEqual: Boolean = if (nDays > 0) {
                // When there are multiple days involved, we have to compare the days
                // that are pushed towards the right in outVolumeMinus, this starts at
                // nDays and end at the last index.
                // For inVolumeMinus, it would be the first till BUCKETS - nDays
                slicedOutVolumeMinus == slicedInVolumeMinus
            } else {
                // When the days are the same, we compare 1 - BUCKETS because only the
                // first index changed.
                outVolumeMinus.slice(1, BUCKETS) == inVolumeMinus.slice(1, BUCKETS)
            }

            val __outVolumeMinusValidated: Boolean = allOf(Coll(
                outVolumeMinus.size == BUCKETS,
                _volumeMinusAccounted,
                _isSlicedValuedVolumeMinusEqual,
                _nVolumeMinusAllZeros
            ))

            // #1
            val outVolumePlusExpectedValue = if (nDays == 0) {inVolumePlus(0)} else {0L}
            val _outVolumePlusFirstIndexedPreserved = outVolumePlusExpectedValue == outVolumePlus(0)

            // #2
            // We sliced n - 1 of the value in between, and we check if all of it are 0s.
            val slicedNVolumePlus: Coll[Long] = outVolumePlus.slice(1, nDays)
            val _nVolumePlusAllZeros: Boolean = slicedNVolumePlus.forall{(indexedValue: Long) => indexedValue == 0L}

            // #3
            val slicedOutVolumePlus: Coll[Long] = outVolumePlus.slice(nDays, BUCKETS)
            val slicedInVolumePlus: Coll[Long] = inVolumePlus.slice(0, BUCKETS - nDays)
            val _isSlicedValuedVolumePlusEqual: Boolean = if (nDays > 0) {
                // When there are multiple days involved, we have to compare the days
                // that are pushed towards the right in outVolumeMinus, this starts at
                // nDays and end at the last index
                // for inVolumeMinus, it would be the first till BUCKETS - nDays
                slicedOutVolumePlus == slicedInVolumePlus
            } else {
                // When the days are the same, we compare 1 - BUCKETS because only the
                // first index changed
                outVolumePlus.slice(1, BUCKETS) == inVolumePlus.slice(1, BUCKETS)
            }

            val __outVolumePlusValidated: Boolean = allOf(Coll(
                outVolumePlus.size == BUCKETS,
                _outVolumePlusFirstIndexedPreserved,
                _isSlicedValuedVolumePlusEqual,
                _nVolumePlusAllZeros
            ))

            val volumePlus: BigInt = sum(outVolumePlus) // adds all elements of the collection, computing the total volume
            val volumeMinus: BigInt = sum(outVolumeMinus)

            val volume: BigInt = if (volumePlus > volumeMinus) {0L.toBigInt} else {volumeMinus - volumePlus} // integer subtraction

            // === Tx FEE for pool === //
            // This is the fee that gets collected to add into the pool during decay.



            val VarPhiBeta: BigInt = Phi0 + ((Phi1 * volume) / RErg)

            // Due to some issues with moving towards the next block. We should give it a margin of error of +/- 3 blocks.
            // There is a tricky situation where if the lastblock is within a day, and if it is always updated,
            // then we will always be at day 0 as long as there is a decay that happened within a day before
            // the lastBlockPreserved.
            //
            // To counteract this situation, we want to only get the currentBlockNumber that is closest to the previous Blocks_Per_volume_bucket
            val closestPreviousBlockValueViaBuckets: Int = (currentBlockNumber / BLOCKS_PER_VOLUME_BUCKET) * BLOCKS_PER_VOLUME_BUCKET
            val __lastBlockPreserved: Boolean = outLastBucketBlock == closestPreviousBlockValueViaBuckets

            // === VarPhiBeta Calculation End === //

            // Neutrons increase in output, protons decrease in output.
            val NeutronsActualValue: BigInt = (OUT_GLUON_NEUTRONS_TOKEN._2 - IN_GLUON_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (IN_GLUON_PROTONS_TOKEN._2 - OUT_GLUON_PROTONS_TOKEN._2).toBigInt
            val ErgsActualValue: BigInt = (OUT_GLUON_BOX.value).toBigInt

            // === Fusion Ratio === //

            // The steps of multiplication and division done below are to avoid overflow errors.
            val oneMinusPhiBeta: BigInt = one - VarPhiBeta
            val oneMinusFusionRatio: BigInt = one - fusionRatio
            val ratio1: BigInt = (M.toBigInt * oneMinusPhiBeta) / SNeutrons
            val ratio2: BigInt = (fusionRatio * SProtons) / one
            val outProtonsAmount: BigInt = (ratio1 * ratio2) / oneMinusFusionRatio

            val NeutronsExpectedValue: BigInt = M.toBigInt
            val ProtonsExpectedValue: BigInt = outProtonsAmount
            val ErgsExpectedValue: BigInt = (IN_GLUON_BOX.value).toBigInt

            // ### The 2 conditions to ensure that the values out are right ### //
            val __neutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __protonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
            val __ergsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

            sigmaProp(allOf(Coll(
                isHealthyRange,
                cSameContract, cSameTokenIdentifiers, 
                __neutronsValueValid, __protonsValueValid, __ergsValueValid,
                cSameR4, cSameR5, cSameR6,
                __outVolumePlusValidated, __outVolumeMinusValidated,
                __lastBlockPreserved, // TODO: Replace this and the next line by cSameR9?
                cSameR9PegFactor,
                __feesCheck,
                __oracleCheck
            )))
        } else sigmaProp(false)
    } else if (isAdjustPegTx) {
        val pegFactorCorrect: Boolean = // PegFactor update direction and magnitude must be correct
            if (q > qUpperThreshold) outPegFactor.toBigInt == inPegFactor.toBigInt * 99 / 100       // Peg Factor must have decreased by 1%
            else if (q < qLowerThreshold) outPegFactor.toBigInt == inPegFactor.toBigInt * 101 / 100 // Peg Factor must have increased by 1%
            else false

        sigmaProp(allOf(Coll(
            !isHealthyRange, // Only when outside the healthy range.
            __oracleCheck,
            cSameContract, cSameTokens, cSameValue, cSameR4, cSameR5, cSameR6, cSameR7, cSameR8, cSameR9LastBucketBlock,
            pegFactorCorrect // Peg Factor is the only register variable that changes
        ))) // Anyone may do this transaction when outside the healthy range
    } else if (isUpdateTreasury) {
        val newMultisig: SigmaProp = OUT_GLUON_BOX.R5[SigmaProp].get
        sigmaProp(allOf(Coll(
            cSameContract, cSameValue, cSameTokens, cSameR4,
            newMultisig != TREASURY_MULTISIG // R5 is the only register that changes and it must change
            cSameR6, cSameR7, cSameR8, cSameR9
        ))) && TREASURY_MULTISIG // The transaction must be signed by the current TREASURY_MULTISIG
    } else { 
        sigmaProp(false)
    }
}
