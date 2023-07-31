{
    // ===== Contract Info ===== //
    // Name             : GluonW Box Guard Script
    // Description      :
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : June 25th 2023
    // Version          : v 1.0
    // Status           : In Progress

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _MutatePk:                   Coll[Byte]

    // ===== Box Contents ===== //
    // Tokens
    // 1. (GluonWNFT, 1)
    // 2. (Neutrons, IntMax)
    // 3. (Protons, IntMax)
    //
    // Registers
    // R4 - (Total Neutrons Supply, Total Protons Supply): (Long, Long)
    // R5 - (NeutronsTokenId, ProtonsTokenId): (Coll[Byte], Coll[Byte])

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
    val GOLD_ORACLE_BOX: Box = CONTEXT.dataInputs(0)
    val ASSET_TOKENID_REGISTER: (Coll[Byte], Coll[Byte]) = IN_GLUONW_BOX.R5[(Coll[Byte], Coll[Byte])].get
    val ASSET_TOTAL_SUPPLY_REGISTER: (Long, Long) = IN_GLUONW_BOX.R4[(Long, Long)].get
    val NEUTRONS_TOKEN_ID: Coll[Byte] = ASSET_TOKENID_REGISTER._1
    val PROTONS_TOKEN_ID: Coll[Byte] = ASSET_TOKENID_REGISTER._2
    val NEUTRONS_TOTAL_SUPPLY: Long = ASSET_TOTAL_SUPPLY_REGISTER._1
    val PROTONS_TOTAL_SUPPLY: Long = ASSET_TOTAL_SUPPLY_REGISTER._2

    val IN_GLUONW_NEUTRONS_TOKEN: (Coll[Byte], Long) = IN_GLUONW_BOX.tokens(1)
    val IN_GLUONW_PROTONS_TOKEN: (Coll[Byte], Long) = IN_GLUONW_BOX.tokens(2)

    val OUT_GLUONW_NEUTRONS_TOKEN: (Coll[Byte], Long) = OUT_GLUONW_BOX.tokens(1)
    val OUT_GLUONW_PROTONS_TOKEN: (Coll[Byte], Long) = OUT_GLUONW_BOX.tokens(2)

    val __checkGluonWBoxNFT: Boolean = allOf(Coll(
        IN_GLUONW_BOX.tokens(0)._1 == OUT_GLUONW_BOX.tokens(0)._1,
        IN_GLUONW_BOX.tokens(1)._1 == OUT_GLUONW_BOX.tokens(1)._1,
        IN_GLUONW_BOX.tokens(2)._1 == OUT_GLUONW_BOX.tokens(2)._1,
        IN_GLUONW_BOX.propositionBytes == OUT_GLUONW_BOX.propositionBytes,
        IN_GLUONW_BOX.R4[(Long, Long)].get == OUT_GLUONW_BOX.R4[(Long, Long)].get,
        IN_GLUONW_BOX.R5[(Coll[Byte], Coll[Byte])].get == OUT_GLUONW_BOX.R5[(Coll[Byte], Coll[Byte])].get
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
    val Pt: BigInt = CONTEXT.dataInputs(0).R6[Long].get.toBigInt / 1000

    // We're using 1,000,000,000 because the precision is based on nanoErgs
    val precision: BigInt = (1000 * 1000 * 1000).toBigInt

    // q* = 0.66
    // @todo kii, reason about replacing 1 with precision at all parts using 1.
    val qStar: BigInt = (66 * precision / 100)
    val rightHandMin: BigInt = SNeutrons * Pt / RErg
    val fusionRatio: BigInt = min(qStar, rightHandMin)

    // ===== (END) Variable Declarations ===== //

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
            __inErgsValueValid
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
            __outErgsValueValid
        )))
    }
    else if (isBetaDecayPlusTx)
    {
        //===== BetaDecayPlus Tx ===== //
        //Equation: M [Neutrons] = M * (1 - PhiBeta(T)) * ((1 - q(R, S proton)) / q(R, S proton)) * (S protons / S neutrons) [Protons]

        val M: BigInt = (OUT_GLUONW_PROTONS_TOKEN._2 - IN_GLUONW_PROTONS_TOKEN._2).toBigInt

        // ** Tx FEE for pool ** @todo v2: Fix with real Equation
        // This is the fee that gets collected to add into the pool during decay. There is an equation for this fee
        // but for v1, we're just going to use a constant of 1%.
        val VarPhiBeta: BigInt = (2 * precision / 100).toBigInt

        // The protons and neutrons are lesser in outbox than inputbox
        val NeutronsActualValue: BigInt = (IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2).toBigInt
        val ProtonsActualValue: BigInt = (OUT_GLUONW_PROTONS_TOKEN._2 - IN_GLUONW_PROTONS_TOKEN._2).toBigInt
        val ErgsActualValue: BigInt = (OUT_GLUONW_BOX.value).toBigInt

        // ** Fusion Ratio **
        // min(q*, (SNeutrons * Pt / R))
        // where q* is a constant, Pt is the price of gold in Ergs.
        val oneMinusPhiBeta: BigInt = precision - VarPhiBeta
        val oneMinusFusionRatio: BigInt = precision - fusionRatio
        val minusesMultiplied: BigInt =
            oneMinusPhiBeta * oneMinusFusionRatio / precision
        val outNeutronsAmount: BigInt =
            (((M * minusesMultiplied) / fusionRatio) * SNeutrons) / SProtons

        val outProtonsAmount: BigInt = M

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
            __ergsValueValid
        )))
    }
    else if (isBetaDecayMinusTx)
    {
        // ===== BetaDecayMinus Tx ===== //
        // Equation: M [Protons] = M * (1 - PhiBeta(T)) * ((1 - q(R, S neutron)) / q(R, S neutron)) * (S neutrons / S protons) [Neutrons]
        val M: BigInt = (OUT_GLUONW_NEUTRONS_TOKEN._2 - IN_GLUONW_NEUTRONS_TOKEN._2).toBigInt

        // ** Tx FEE for pool ** @todo v2: Fix with real Equation
        // This is the fee that gets collected to add into the pool during decay. There is an equation for this fee
        // but for v1, we're just going to use a constant of 1%.
        val VarPhiBeta: BigInt = (2 * precision / 100).toBigInt

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
            M * oneMinusPhiBeta / precision
        val outProtonsAmount: BigInt =
            ((neutronsToDecayMultiplyOneMinusPhiBeta * SProtons / SNeutrons) * fusionRatio) / oneMinusFusionRatio
        val outNeutronsAmount: BigInt = M

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
            __ergsValueValid
        )))
    } else {
        val isMutate: Boolean = allOf(Coll(
            __checkGluonWBoxNFT,
            // Check Neutrons reduction in OutBox
            IN_GLUONW_NEUTRONS_TOKEN._2 == OUT_GLUONW_NEUTRONS_TOKEN._2,

            // Check Protons reduction in OutBox
            IN_GLUONW_PROTONS_TOKEN._2 == OUT_GLUONW_PROTONS_TOKEN._2,

            // Check Erg value increment in OutBox
            IN_GLUONW_BOX.value == OUT_GLUONW_BOX.value
        ))

        if (isMutate) {
            _MutatePk
        } else {
            // Fails if not a valid tx
            sigmaProp(false)
        }
    }
}