{
    // ===== Contract Info ===== //
    // Name             : GluonW Box Guard Script
    // Description      :
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : June 25th 2023
    // Version          : v 1.1
    // Status           : V1 in Test

    // ===== Version Logs ===== //
    // - 1.0: GluonWBox implemented without dev fees
    // - 1.1: Dev fees and UI fees implemented

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _DevPk:                      SigmaProp
    // val _OracleFeePk:                Coll[Byte]
    // val _OraclePoolNFT:              Coll[Byte]

    // ===== Box Contents ===== //
    // Tokens
    // 1. (GluonWNFT, 1)
    // 2. (Neutrons, IntMax)
    // 3. (Protons, IntMax)
    //
    // Registers
    // R4 - (Total Neutrons Supply, Total Protons Supply): (Long, Long)
    // R5 - (NeutronsTokenId, ProtonsTokenId): (Coll[Byte], Coll[Byte])
    // R6 - (MaxAmountDevFeesToBePaid, TotalDevFeesPaid): (Coll[Long], Coll[Long])
    // R7 - BetaPlusVolume: Coll[Long]
    // R8 - BetaMinusVolume: Coll[Long]
    // R9 - LastBucketBlock: Long

    // ===== Context Vars ===== //
    // val _optUIFee                    SigmaProp

    // ===== Relevant Transactions ===== //
    // 1. Fission           - The user sends Ergs to the reactor (bank) and receives Neutrons and Protons
    // 2. Fusion            - The user sends Neutrons and Protons to the reactor and receives Ergs
    // 3. Beta Decay +      - The user sends Neutrons to the reactor and receives Protons
    // 4. Beta Decay -      - The user sends Protons to the reactor and receives Neutrons
    //
    // For all 4 tx:
    // Inputs: GluonWBox, UserPk
    // DataInputs: GoldOracle
    // Outputs: GluonWBox, UserPk

    // ====== Tx Definitions ===== //
    // The main way to figure out whether the reactor is going through a specific
    // tx is by comparing the value of the tokens in the reactor itself.
    // For each Tx:
    // 1. Fission       - the reactor has a reduction in both protons and neutrons but
    //                      has an increment in Ergs
    // Out.Neutrons.val < In.Neutrons.val, Out.Protons.val < In.Protons.val, Out.val > In.val
    //
    // 2. Fission       - the reactor has a increment in both protons and neutrons but
    //                      has an reduction in Ergs
    // Out.Neutrons.val > In.Neutrons.val, Out.Protons.val > In.Protons.val, Out.val < In.val
    //
    // 3. BetaDecay +    - the reactor has an increment of neutrons (SigGold) and a decrement in protons (Protons)
    //                      has an increment in Ergs due to fees
    // Out.Neutrons.val > In.Neutrons.val, Out.Protons.val < In.Protons.val, Out.val > In.val
    //
    // 4. BetaDecay -    - the reactor has an decrement of neutrons (SigGold) and a increment in protons (Protons)
    //                      has an increment in Ergs due to fees
    // Out.Neutrons.val < In.Neutrons.val, Out.Protons.val > In.Protons.val, Out.val > In.val

    val IN_GLUONW_BOX: Box = SELF
    val OUT_GLUONW_BOX: Box = OUTPUTS(0)
    val ORACLE_BOX: Box = CONTEXT.dataInputs(0)
    val ASSET_TOKENID_REGISTER: (Coll[Byte], Coll[Byte]) = IN_GLUONW_BOX.R5[(Coll[Byte], Coll[Byte])].get
    val ASSET_TOTAL_SUPPLY_REGISTER: (Long, Long) = IN_GLUONW_BOX.R4[(Long, Long)].get
    val ASSET_MAX_DEV_FEE_THRESHOLD: (Long, Long) = IN_GLUONW_BOX.R6[(Long, Long)].get
    val OUT_ASSET_MAX_DEV_FEE_THRESHOLD: (Long, Long) = OUT_GLUONW_BOX.R6[(Long, Long)].get
    val NEUTRONS_TOKEN_ID: Coll[Byte] = ASSET_TOKENID_REGISTER._1
    val PROTONS_TOKEN_ID: Coll[Byte] = ASSET_TOKENID_REGISTER._2
    val NEUTRONS_TOTAL_SUPPLY: Long = ASSET_TOTAL_SUPPLY_REGISTER._1
    val PROTONS_TOTAL_SUPPLY: Long = ASSET_TOTAL_SUPPLY_REGISTER._2
    val DEV_FEE_REPAID: Long = ASSET_MAX_DEV_FEE_THRESHOLD._1
    val MAX_DEV_FEE_THRESHOLD: Long = ASSET_MAX_DEV_FEE_THRESHOLD._2
    val OUT_DEV_FEE_REPAID: Long = OUT_ASSET_MAX_DEV_FEE_THRESHOLD._1
    val OUT_MAX_DEV_FEE_THRESHOLD: Long = OUT_ASSET_MAX_DEV_FEE_THRESHOLD._2

    val IN_GLUONW_NEUTRONS_TOKEN: (Coll[Byte], Long) = IN_GLUONW_BOX.tokens(1)
    val IN_GLUONW_PROTONS_TOKEN: (Coll[Byte], Long) = IN_GLUONW_BOX.tokens(2)

    val OUT_GLUONW_NEUTRONS_TOKEN: (Coll[Byte], Long) = OUT_GLUONW_BOX.tokens(1)
    val OUT_GLUONW_PROTONS_TOKEN: (Coll[Byte], Long) = OUT_GLUONW_BOX.tokens(2)

    val inVolumePlus: Coll[Long] = IN_GLUONW_BOX.R7[Coll[Long]].get
    val inVolumeMinus: Coll[Long] = IN_GLUONW_BOX.R8[Coll[Long]].get
    val outVolumePlus: Coll[Long] = OUT_GLUONW_BOX.R7[Coll[Long]].get
    val outVolumeMinus: Coll[Long] = OUT_GLUONW_BOX.R8[Coll[Long]].get

    val inLastBucketBlock: Long = IN_GLUONW_BOX.R9[Long].get
    val outLastBucketBlock: Long = OUT_GLUONW_BOX.R9[Long].get 

    val BLOCKS_PER_VOLUME_BUCKET: Int = 720 // Approximately 1 day per volume bucket
    val BUCKETS: Int = 14 // Tracking volume of approximately 14 days

    val __checkGluonWBoxNFT: Boolean = allOf(Coll(
        IN_GLUONW_BOX.tokens(0)._1 == OUT_GLUONW_BOX.tokens(0)._1,
        IN_GLUONW_BOX.tokens(1)._1 == OUT_GLUONW_BOX.tokens(1)._1,
        IN_GLUONW_BOX.tokens(2)._1 == OUT_GLUONW_BOX.tokens(2)._1,
        IN_GLUONW_BOX.propositionBytes == OUT_GLUONW_BOX.propositionBytes,
        IN_GLUONW_BOX.R4[(Long, Long)].get == OUT_GLUONW_BOX.R4[(Long, Long)].get,
        IN_GLUONW_BOX.R5[(Coll[Byte], Coll[Byte])].get == OUT_GLUONW_BOX.R5[(Coll[Byte], Coll[Byte])].get,
        IN_GLUONW_BOX.R6[(Long, Long)].get._2 == OUT_GLUONW_BOX.R6[(Long, Long)].get._2,
    ))

    val isFissionTx: Boolean = allOf(Coll(
        __checkGluonWBoxNFT,
        // Check Neutrons reduction in OutBox
        IN_GLUONW_NEUTRONS_TOKEN._2 > OUT_GLUONW_NEUTRONS_TOKEN._2,

        // Check Protons reduction in OutBox
        IN_GLUONW_PROTONS_TOKEN._2 > OUT_GLUONW_PROTONS_TOKEN._2,

        // Check Erg value increment in OutBox
        IN_GLUONW_BOX.value < OUT_GLUONW_BOX.value
    ))

    val isFusionTx: Boolean = allOf(Coll(
        __checkGluonWBoxNFT,
        // Check Neutrons increment in OutBox
        IN_GLUONW_NEUTRONS_TOKEN._2 < OUT_GLUONW_NEUTRONS_TOKEN._2,

        // Check Protons increment in OutBox
        IN_GLUONW_PROTONS_TOKEN._2 < OUT_GLUONW_PROTONS_TOKEN._2,

        // Check Erg value reduction in OutBox
        IN_GLUONW_BOX.value > OUT_GLUONW_BOX.value
    ))

    // Transmute Protons to Neutrons
    // Remove Protons from Circulation
    // Increase Neutrons in Circulation
    //
    // TotalInCirculation = TotalSupply - TokensAmountInBox
    // Therefore an increase in circulation means TokensAmountInBox is lesser
    val isBetaDecayPlusTx: Boolean = allOf(Coll(
        __checkGluonWBoxNFT,
        // Check Neutrons increment in OutBox
        IN_GLUONW_NEUTRONS_TOKEN._2 > OUT_GLUONW_NEUTRONS_TOKEN._2,

        // Check Protons reduction in OutBox
        IN_GLUONW_PROTONS_TOKEN._2 < OUT_GLUONW_PROTONS_TOKEN._2,

        // Check Erg value increment in OutBox
        IN_GLUONW_BOX.value == OUT_GLUONW_BOX.value
    ))

    // Transmute Neutrons to Protons
    // Remove Neutrons from Circulation
    // Increase Protons in Circulation
    //
    // TotalInCirculation = TotalSupply - TokensAmountInBox
    // Therefore an increase in circulation means TokensAmountInBox is lesser
    val isBetaDecayMinusTx: Boolean = allOf(Coll(
        __checkGluonWBoxNFT,
        // Check Neutrons decrement in OutBox
        IN_GLUONW_NEUTRONS_TOKEN._2 < OUT_GLUONW_NEUTRONS_TOKEN._2,

        // Check Protons increment in OutBox
        IN_GLUONW_PROTONS_TOKEN._2 > OUT_GLUONW_PROTONS_TOKEN._2,

        // Check Erg value increment in OutBox
        IN_GLUONW_BOX.value == OUT_GLUONW_BOX.value
    ))
    // ===== (END) Tx Definition ===== //

    if (anyOf(Coll(isFissionTx, isFusionTx, isBetaDecayPlusTx, isBetaDecayMinusTx)))
    {
        // ===== Variable Declarations ===== //
        // Variable in Paper: S neutrons
        val _neutronsInCirculation: Long = NEUTRONS_TOTAL_SUPPLY - IN_GLUONW_NEUTRONS_TOKEN._2
        // Variable in Paper: S protons
        val _protonsInCirculation: Long = PROTONS_TOTAL_SUPPLY - IN_GLUONW_PROTONS_TOKEN._2
        val SNeutrons: BigInt = _neutronsInCirculation.toBigInt
        val SProtons: BigInt = _protonsInCirculation.toBigInt

        // Variable in Paper: R
        // As the box that come into existence would have a minimum fee, we have to reduce the
        // value of fissionedErg by minimum value of erg for a box
        val _fissionedErg: Long = IN_GLUONW_BOX.value - _MinFee
        val RErg: BigInt = _fissionedErg.toBigInt
        // Price of Gold
        val Pt: BigInt = CONTEXT.dataInputs(0).R4[Long].get.toBigInt / 1000

        // We're using 1,000,000,000 because the precision is based on nanoErgs
        val precision: BigInt = (1000 * 1000 * 1000).toBigInt

        // q* = 0.66
        // @todo kii, reason about replacing 1 with precision at all parts using 1.
        val qStar: BigInt = (66 * precision / 100)
        val rightHandMin: BigInt = SNeutrons * Pt / RErg
        val fusionRatio: BigInt = min(qStar, rightHandMin)

        // Calculate the value based on protons
        // Check Protons reduction in OutBox
        def getProtonsPrice(protonsValue: Long): BigInt = {
            val oneMinusFusionRatio: BigInt = (precision - fusionRatio).toBigInt
            val protonsPrice: BigInt = oneMinusFusionRatio * RErg.toBigInt / SProtons.toBigInt
            val protonsInNanoergs: BigInt = protonsValue.toBigInt * protonsPrice / precision.toBigInt

            protonsInNanoergs
        }

        def getNeutronsPrice(neutronsValue: Long): BigInt = {
            val neutronsInNanoergs: BigInt = neutronsValue.toBigInt * Pt.toBigInt / precision.toBigInt

            neutronsInNanoergs
        }

        def sum(collLong: Coll[Long]): BigInt = {
            collLong.fold(0L, {(acc: Long, indexedValue: Long) => acc + indexedValue}).toBigInt
        }

        // ===== (END) Variable Declarations ===== //

        // ===== (START) Oracle Checks ===== //
        // The two checks for the oracle is:
        // 1. It has the right NFT on it
        // 2. It's height is within 70 min, (35 blocks)
        val oracleBoxCreationHeightDifferenceFromNow: Int = CONTEXT.HEIGHT - ORACLE_BOX.creationInfo._1
        val oracleBoxPoolNFT: (Coll[Byte], Long) = ORACLE_BOX.tokens(0)

        val __oracleCheck: Boolean = allOf(Coll(
            oracleBoxCreationHeightDifferenceFromNow < 35 && oracleBoxCreationHeightDifferenceFromNow > 0,
            oracleBoxPoolNFT._1 == _OraclePoolNFT
        ))
        // ===== (END) Oracle Checks ===== //

        // ===== (START) Fee Declarations ===== //
        // reference from https://github.com/K-Singh/Sigma-Finance/blob/master/contracts/ex/ExOrderERG.ergo
        val _optUIFee: Coll[Byte] = getVar[SigmaProp](0)
        val fees: Coll[(Coll[Byte], BigInt)] = {
            val feeDenom: Long = 1000L.toBigInt
            val devFee: Long = 5L.toBigInt
            val oracleFee: Long = 1L.toBigInt
            val uiFee: Long = 4L.toBigInt
            val emptyFees: (Coll[Byte], Long)          = (Coll(1.toByte), 0L.toBigInt)

            // principal is the amount that is requested
            val principal: BigInt = if (isFissionTx) {
                    (OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value).toBigInt
                } else if (isFusionTx) {
                    (IN_GLUONW_BOX.value - OUT_GLUONW_BOX.value).toBigInt
                } else if (isBetaDecayPlusTx) {
                    // Calculate the value based on protons
                    // Check Protons reduction in OutBox
                    val protonsValue: Long = OUT_GLUONW_PROTONS_TOKEN._2 - IN_GLUONW_PROTONS_TOKEN._2
                    val protonsInNanoergs: BigInt = getProtonsPrice(protonsValue)

                    protonsInNanoergs
                } else {
                    // Calculate the value based on neutrons
                    val neutronsValue: Long = OUT_GLUONW_NEUTRONS_TOKEN._2 - IN_GLUONW_NEUTRONS_TOKEN._2
                    val neutronsInNanoergs: BigInt = getNeutronsPrice(neutronsValue)

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
                (_DevPk.propBytes, devFeePayout)
            val oracleFeeAddressAndPayout: (Coll[Byte], BigInt) =
                (_OracleFeePk, oracleFeePayout)

            if (isBetaDecayMinusTx || isBetaDecayPlusTx)
            {
                // If Ui fee is defined, then we add an additional 0.4% fee
                if (_optUIFee.isDefined) {
                    Coll(
                        devFeeAddressAndPayout,
                        (_optUIFee.get.propBytes, uiFeePayout),
                        oracleFeeAddressAndPayout
                    )
                }
                else {
                    Coll(
                        devFeeAddressAndPayout,
                        emptyFees,
                        oracleFeeAddressAndPayout
                    )
                }
            }
            else
            {
                // If Ui fee is defined, then we add an additional 0.4% fee
                if (_optUIFee.isDefined) {
                    Coll(
                        devFeeAddressAndPayout,
                        (_optUIFee.get.propBytes, uiFeePayout),
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
            val uiFeesToBePaid: Boolean = fees(1)._2 > 0
            val oracleFeesToBePaid: Boolean = fees(2)._2 > 0
            val devFeesPaid: Boolean = {
                if (fees(0)._2 > 0)
                {
                    // Dev fee is greater than 0
                    val devOutput: Box   = OUTPUTS(2)
                    allOf(
                        Coll(
                            devOutput.propositionBytes      == fees(0)._1,
                            devOutput.value.toBigInt        == fees(0)._2 + _MinFee
                        )
                    )
                }
                else
                {
                    true // do nothing if dev fee doesn't add up greater than 0, prevents errors on low value bonds
                }
            }

            val uiFeesPaid: Boolean = {
                if (_optUIFee.isDefined)
                {
                    if(fees(1)._2 > 0) {
                        // UI fee is greater than 0
                        val uiOutput: Box    = OUTPUTS(3)
                        allOf(
                            Coll(
                                uiOutput.propositionBytes       == fees(1)._1,
                                uiOutput.value.toBigInt         == fees(1)._2 + _MinFee
                            )
                        )
                    }
                    else
                    {
                        true // do nothing if ui fee doesn't end up greater than 0, prevents errors on low value bonds
                    }
                } else {
                    true // if ui fee isn't defined, then default to true.
                }
            }

            val oracleOutput: Box = if (uiFeesToBePaid)
            {
                OUTPUTS(4)
            }
            else
            {
                OUTPUTS(3)
            }

             val oracleFeesPaid: Boolean = {
                if (isBetaDecayPlusTx || isBetaDecayMinusTx)
                {
                    if (fees(2)._2 > 0)
                    { // Dev fee is greater than 0
                        allOf(
                            Coll(
                                oracleOutput.propositionBytes      == fees(2)._1,
                                oracleOutput.value.toBigInt         == fees(2)._2 + _MinFee
                            )
                        )
                    }
                    else
                    {
                        true // do nothing if dev fee doesn't add up greater than 0, prevents errors on low value bonds
                    }
                } else {
                    true // if oracle fee is not defined, then default to true.
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
        val volumePlusPreserved = inVolumePlus == outVolumePlus // TODO: fix this: we need to check that all respective elements of the two collections are the same.
        val volumeMinusPreserved = inVolumeMinus == outVolumeMinus // TODO: fix this: we need to check that all the respective elements of the two collections are the same.
        val lastBucketBlockPreserved = inLastBucketBlock == outLastBucketBlock
        val __validVolumeHandling = allOf(Coll(
            volumePlusPreserved,
            volumeMinusPreserved,
            lastBucketBlockPreserved
        ))

        // NOTE:
        // In all of these transactions, the Inputs value varies, however, the output does not. The output is exactly how much
        // the user wants. Therefore we can use the outbox to calculate the value of M by using Outbox.value - InputBox
        if (isFissionTx)
        {
            // ===== FISSION Tx ===== //
            // Equation: M [Ergs] = (M (1 - PhiT) (S Protons / R)) [Protons] + (M (1 - PhiT) (S Neutrons / R)) [Neutrons]

            val M: BigInt = (OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value).toBigInt

            // ** Tx FEE for pool ** @todo v2: Fix with real Equation
            // This is the fee that gets collected to add into the pool during fission. There is an equation for this fee
            // but for v1, we're just going to use a constant of 1%.
            val PhiT: BigInt = (precision / 100).toBigInt

            // The protons and neutrons are lesser in outbox than inputbox
            val NeutronsActualValue: BigInt = (IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2).toBigInt
            val ErgsActualValue: BigInt = (OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value).toBigInt

            val NeutronsExpectedValue: BigInt = (M * SNeutrons * (precision - PhiT) / RErg) / precision
            val ProtonsExpectedValue: BigInt = (M * SProtons * (precision - PhiT) / RErg) / precision
            val ErgsExpectedValue: BigInt = M

            // ### The 2 conditions to ensure that the values out is right ### //
            val __outNeutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __outProtonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
            val __inErgsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

            sigmaProp(allOf(Coll(
                __checkGluonWBoxNFT,
                __outNeutronsValueValid,
                __outProtonsValueValid,
                __inErgsValueValid,
                __feesCheck,
                __validVolumeHandling
            )))
        }
        else if (isFusionTx)
        {
            // ===== FISSION Tx ===== //
            // Equation: (M (S neutrons / R)) [Protons] + (M (S protons / R)) [Neutrons] = M (1 - PhiT) [Ergs]

            // ** Tx FEE for pool ** @todo v2: Fix with real Equation
            // This is the fee that gets collected to add into the pool during fission. There is an equation for this fee
            // but for v1, we're just going to use a constant of 1%.
            val PhiFusion: BigInt = (precision / 100).toBigInt

            // The protons and neutrons are lesser in outbox than inputbox
            val NeutronsActualValue: BigInt = (IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2).toBigInt
            val ErgsActualValue: BigInt = (OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value).toBigInt

            // M(1 - phiFusion) = Ergs
            // therefore, M = Ergs / (1 - phiFusion)
            val M: BigInt = ErgsActualValue * (precision / (precision - PhiFusion))


            val inProtonsNumerator: BigInt = M * SProtons * precision
            val inNeutronsNumerator: BigInt = M * SNeutrons * precision
            val denominator: BigInt = RErg * (precision - PhiFusion)

            val NeutronsExpectedValue: BigInt = inNeutronsNumerator / denominator
            val ProtonsExpectedValue: BigInt =  inProtonsNumerator / denominator
            // This is technically always true. We can take this off, but I don't want to.
            val ErgsExpectedValue: BigInt = ErgsActualValue

            // ### The 2 conditions to ensure that the values out is right ### //
            val __inNeutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __inProtonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
            val __outErgsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

            sigmaProp(allOf(Coll(
                __checkGluonWBoxNFT,
                __inNeutronsValueValid,
                __inProtonsValueValid,
                __outErgsValueValid,
                __feesCheck,
                __validVolumeHandling
            )))
        }
        else if (isBetaDecayPlusTx)
        {
            //===== BetaDecayPlus Tx ===== //
            //Equation: M [Neutrons] = M * (1 - PhiBeta(T)) * ((1 - q(R, S proton)) / q(R, S proton)) * (S protons / S neutrons) [Protons]

            // proton value
            val M: Long = (OUT_GLUONW_PROTONS_TOKEN._2 - IN_GLUONW_PROTONS_TOKEN._2)

            // The protons and neutrons are lesser in outbox than inputbox
            val NeutronsActualValue: BigInt = (IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (OUT_GLUONW_PROTONS_TOKEN._2 - IN_GLUONW_PROTONS_TOKEN._2).toBigInt
            val ErgsActualValue: BigInt = (OUT_GLUONW_BOX.value).toBigInt

            // ** VarPhiBeta Calculation **
            val currentBlockNumber: Long = CONTEXT.HEIGHT

            // Check Protons reduction in OutBox
            val worthOfMInErgs: BigInt = getProtonsPrice(M)

            // calculate the amount of days that has been since the last betaDecayTx
            // 1000 - 200 = 800 | 800 / 720 = 1
            val nDays: Int = ((currentBlockNumber - inLastBucketBlock) / BLOCKS_PER_VOLUME_BUCKET).toInt

            // we don't need to shift it, we just need to check if outVolumePlus is correct.
            // Therefore, if there is a requirement to shift, we just need to check if the
            // value after n is the same for the next 14.
            //
            // Here's an example:
            // assuming our initial block is this
            // [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
            //
            // if nDays = 4, and worthOfMInErgs = x
            // We should expect:
            // [x, 0, 0 ,0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
            //
            // if nDays = 0, and worthOfMInErgs = x
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
                // nDays and end at the last index
                // for inVolumeMinus, it would be the first till BUCKETS - nDays
                slicedOutVolumeMinus == slicedInVolumeMinus
            } else {
                // When the days are the same, we compare 1 - BUCKETS because only the
                // first index changed
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

            val volume: BigInt = if (volumeMinus > volumePlus) {0L.toBigInt}
                else {volumePlus - volumeMinus} // ATTENTION: this should be integer subtraction and should be 0 when volumeMinus > volumePlus

            // ** Tx FEE for pool ** @todo v2: Fix with real Equation
            // This is the fee that gets collected to add into the pool during decay. There is an equation for this fee
            // but for v1, we're just going to use a constant of 1%.

            val Phi0 = precision / 100
            val Phi1 = precision / 2

            val VarPhiBeta: BigInt = Phi0 + Phi1 * volume / RErg // TODO: handle precision properly

            // Due to some issues with moving towards the next block. We should give it a margin of error of +-3 blocks
            // There is a tricky situation where if the lastblock is within a day, and if it is always updated,
            // then we will always be at day 0 as long as there is a decay that happened within a day before
            // the lastBlockPreserved.
            //
            // To counteract this situation, we want to only get the currentBlockNumber that is closest to the previous Blocks_Per_volume_bucket
            val closestPreviousBlockValueViaBuckets: Int = (currentBlockNumber / BLOCKS_PER_VOLUME_BUCKET) * BLOCKS_PER_VOLUME_BUCKET
            val __lastBlockPreserved: Boolean = outLastBucketBlock == closestPreviousBlockValueViaBuckets

            // ** VarPhiBeta Calculation End **

            // ** Fusion Ratio **
            // min(q*, (SNeutrons * Pt / R))
            // where q* is a constant, Pt is the price of gold in Ergs.
            val oneMinusPhiBeta: BigInt = precision - VarPhiBeta
            val oneMinusFusionRatio: BigInt = precision - fusionRatio
            val minusesMultiplied: BigInt =
                oneMinusPhiBeta * oneMinusFusionRatio / precision
            val outNeutronsAmount: BigInt =
                (((M * minusesMultiplied) / fusionRatio) * SNeutrons) / SProtons

            val outProtonsAmount: BigInt = M.toBigInt

            val NeutronsExpectedValue: BigInt = outNeutronsAmount
            val ProtonsExpectedValue: BigInt = outProtonsAmount
            val ErgsExpectedValue: BigInt = (IN_GLUONW_BOX.value).toBigInt

            // ### The 2 conditions to ensure that the values out is right ### //
            val __neutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __protonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
            val __ergsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

            sigmaProp(allOf(Coll(
                __checkGluonWBoxNFT,
                __neutronsValueValid,
                __protonsValueValid,
                __ergsValueValid,
                __feesCheck,
                __outVolumeMinusValidated,
                __outVolumePlusValidated,
                __lastBlockPreserved
            )))
        }
        else if (isBetaDecayMinusTx)
        {
            // ===== BetaDecayMinus Tx ===== //
            // Equation: M [Protons] = M * (1 - PhiBeta(T)) * ((1 - q(R, S neutron)) / q(R, S neutron)) * (S neutrons / S protons) [Neutrons]
            val M: Long = (OUT_GLUONW_NEUTRONS_TOKEN._2 - IN_GLUONW_NEUTRONS_TOKEN._2)

            // ** VarPhiBeta Calculation **
            val currentBlockNumber: Long = CONTEXT.HEIGHT

            // Check Neutrons reduction in OutBox
            val worthOfMInErgs: BigInt = getNeutronsPrice(M)

            // calculate the amount of days that has been since the last betaDecayTx
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
            // exact value
            val slicedOutVolumeMinus: Coll[Long] = outVolumeMinus.slice(nDays, BUCKETS)
            val slicedInVolumeMinus: Coll[Long] = inVolumeMinus.slice(0, BUCKETS - nDays)
            val _isSlicedValuedVolumeMinusEqual: Boolean = if (nDays > 0) {
                // When there are multiple days involved, we have to compare the days
                // that are pushed towards the right in outVolumeMinus, this starts at
                // nDays and end at the last index
                // for inVolumeMinus, it would be the first till BUCKETS - nDays
                slicedOutVolumeMinus == slicedInVolumeMinus
            } else {
                // When the days are the same, we compare 1 - BUCKETS because only the
                // first index changed
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

            val volume: BigInt = if (volumePlus > volumeMinus) {0L.toBigInt}
                else {volumeMinus - volumePlus} // ATTENTION: this should be integer subtraction and should be 0 when volumeMinus > volumePlus

            // ** Tx FEE for pool ** @todo v2: Fix with real Equation
            // This is the fee that gets collected to add into the pool during decay. There is an equation for this fee
            // but for v1, we're just going to use a constant of 1%.

            val Phi0 = precision / 100
            val Phi1 = precision / 2

            val VarPhiBeta: BigInt = Phi0 + ((Phi1 * volume) / RErg) // TODO: handle precision properly

            // Due to some issues with moving towards the next block. We should give it a margin of error of +-3 blocks
            // There is a tricky situation where if the lastblock is within a day, and if it is always updated,
            // then we will always be at day 0 as long as there is a decay that happened within a day before
            // the lastBlockPreserved.
            //
            // To counteract this situation, we want to only get the currentBlockNumber that is closest to the previous Blocks_Per_volume_bucket
            val closestPreviousBlockValueViaBuckets: Int = (currentBlockNumber / BLOCKS_PER_VOLUME_BUCKET) * BLOCKS_PER_VOLUME_BUCKET
            val __lastBlockPreserved: Boolean = outLastBucketBlock == closestPreviousBlockValueViaBuckets

            // ** VarPhiBeta Calculation End **

            // The protons and neutrons are lesser in outbox than inputbox
            val NeutronsActualValue: BigInt = (OUT_GLUONW_NEUTRONS_TOKEN._2 - IN_GLUONW_NEUTRONS_TOKEN._2).toBigInt
            val ProtonsActualValue: BigInt = (IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2).toBigInt
            val ErgsActualValue: BigInt = (OUT_GLUONW_BOX.value).toBigInt

            // ** Fusion Ratio **
            // min(q*, (SNeutrons * Pt / R))
            // where q* is a constant, Pt is the price of gold in Ergs.
            val oneMinusPhiBeta: BigInt = precision - VarPhiBeta
            val oneMinusFusionRatio: BigInt = precision - fusionRatio
            val neutronsToDecayMultiplyOneMinusPhiBeta: BigInt =
                M.toBigInt * oneMinusPhiBeta / precision
            val outProtonsAmount: BigInt =
                ((neutronsToDecayMultiplyOneMinusPhiBeta * SProtons / SNeutrons) * fusionRatio) / oneMinusFusionRatio
            val outNeutronsAmount: BigInt = M.toBigInt

            val NeutronsExpectedValue: BigInt = outNeutronsAmount
            val ProtonsExpectedValue: BigInt = outProtonsAmount
            val ErgsExpectedValue: BigInt = (IN_GLUONW_BOX.value).toBigInt

            // ### The 2 conditions to ensure that the values out is right ### //
            val __neutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
            val __protonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
            val __ergsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

            sigmaProp(allOf(Coll(
                __checkGluonWBoxNFT,
                __neutronsValueValid,
                __protonsValueValid,
                __ergsValueValid,
                __feesCheck,
                __outVolumePlusValidated,
                __outVolumeMinusValidated,
                __lastBlockPreserved
            )))
        } else sigmaProp(false)
    } else {
//        val isMutate: Boolean = allOf(Coll(
//            IN_GLUONW_BOX.tokens(0)._1 == OUT_GLUONW_BOX.tokens(0)._1,
//            IN_GLUONW_BOX.tokens(1)._1 == OUT_GLUONW_BOX.tokens(1)._1,
//            IN_GLUONW_BOX.tokens(2)._1 == OUT_GLUONW_BOX.tokens(2)._1,
//            // Check Neutrons reduction in OutBox
//            IN_GLUONW_NEUTRONS_TOKEN._2 == OUT_GLUONW_NEUTRONS_TOKEN._2,
//
//            // Check Protons reduction in OutBox
//            IN_GLUONW_PROTONS_TOKEN._2 == OUT_GLUONW_PROTONS_TOKEN._2,
//
//            // Check Erg value increment in OutBox
//            IN_GLUONW_BOX.value == OUT_GLUONW_BOX.value
//        ))

//        if (isMutate) {
            _DevPk
//        } else {
//             Fails if not a valid tx
//            sigmaProp(false)
//        }
    }
}