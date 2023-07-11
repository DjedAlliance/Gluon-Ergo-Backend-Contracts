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
    // val _NeutronsTokenId:            Coll[Byte]
    // val _ProtonsTokenId:             Coll[Byte]

    // ===== Box Contents ===== //
    // Tokens
    // 1. (GluonWNFT, 1)
    // 2. (Neutrons, IntMax)
    // 3. (Protons, IntMax)
    //
    // Registers
    // R4 - (Total Protons Supply, Total Protons Supply): (Long, Long)

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
    val GOLD_ORACLE_BOX = CONTEXT.dataInputs(0)

    val IN_GLUONW_NEUTRONS_TOKEN: (Coll[Byte], Long) = IN_GLUONW_BOX.tokens
        .filter{(token: (Coll[Byte], Long)) => token._1 == _NeutronsTokenId}
    val IN_GLUONW_PROTONS_TOKEN: (Coll[Byte], Long) = IN_GLUONW_BOX.tokens
        .filter{(token: (Coll[Byte], Long)) => token._1 == _ProtonsTokenId}

    val OUT_GLUONW_NEUTRONS_TOKEN: (Coll[Byte], Long) = OUT_GLUONW_BOX.tokens
        .filter{(token: (Coll[Byte], Long)) => token._1 == _NeutronsTokenId}
    val OUT_GLUONW_PROTONS_TOKEN: (Coll[Byte], Long) = OUT_GLUONW_BOX.tokens
        .filter{(token: (Coll[Byte], Long)) => token._1 == _ProtonsTokenId}

    val __checkGluonWBoxNFT: Boolean = allOf(Coll(
        IN_GLUONW_BOX.tokens(0)._1 == OUT_GLUONW_BOX.tokens(0)._1,
        IN_GLUONW_BOX.propositionBytes == OUT_GLUONW_BOX.propositionBytes
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

    val isBetaDecayPlusTx: Boolean = allOf(Coll(
        __checkGluonWBoxNFT,
        // Check Neutrons increment in OutBox
        IN_GLUONW_NEUTRONS_TOKEN._2 < OUT_GLUONW_NEUTRONS_TOKEN._2,

        // Check Protons reduction in OutBox
        IN_GLUONW_PROTONS_TOKEN._2 > OUT_GLUONW_PROTONS_TOKEN._2,

        // Check Erg value increment in OutBox
        IN_GLUONW_BOX.value < OUT_GLUONW_BOX.value
    ))

    val isBetaDecayMinusTx: Boolean = allOf(Coll(
        __checkGluonWBoxNFT,
        // Check Neutrons decrement in OutBox
        IN_GLUONW_NEUTRONS_TOKEN._2 > OUT_GLUONW_NEUTRONS_TOKEN._2,

        // Check Protons increment in OutBox
        IN_GLUONW_PROTONS_TOKEN._2 < OUT_GLUONW_PROTONS_TOKEN._2,

        // Check Erg value increment in OutBox
        IN_GLUONW_BOX.value < OUT_GLUONW_BOX.value
    ))

    // ===== (END) Tx Definition ===== //

    // ===== Variable Declarations ===== //
    // @todo kii: Change this amount to something more legit
    val _totalMintedNeutronsAmount: Long = 1000000
    val _totalMintedProtonsAmount: Long = 1000000

    // Variable in Paper: S neutrons
    val _neutronsInCirculation: Long = _totalMintedNeutronsAmount - IN_GLUONW_NEUTRONS_TOKEN._2
    // Variable in Paper: S protons
    val _protonsInCirculation: Long = _totalMintedProtonsAmount - IN_GLUONW_PROTONS_TOKEN._2
    val SNeutrons: Long = _neutronsInCirculation
    val SProtons: Long = _protonsInCirculation

    // Variable in Paper: R
    // As the box that come into existence would have a minimum fee, we have to reduce the
    // value of fissionedErg by minimum value of erg for a box
    val _fissionedErg: Long = IN_GLUONW_BOX.value - _MinFee
    val RErg: Long = _fissionedErg
    // Price of Gold
    val Pt: Long = CONTEXT.dataInputs(0).R4[Long]

    // We're using 10,000 because there are constants that goes up to 0.66
    val precision: Long = 10000

    // q* = 0.66
    // @todo kii, reason about replacing 1 with precision at all parts using 1.
    val qStar = 66 / precision
    val rightHandMin = SNeutrons * Pt / R
    val fusionRatio: Long = min(qStar, rightHandMin)

    // ===== (END) Variable Declarations ===== //

    // NOTE:
    // In all of these transactions, the Inputs value varies, however, the output does not. The output is exactly how much
    // the user wants. Therefore we can use the outbox to calculate the value of M by using Outbox.value - InputBox
    if (isFissionTx)
    {
        // ===== FISSION Tx ===== //
        // Equation: M [Ergs] = (M (1 - PhiT) (S Protons / R)) [Protons] + (M (1 - PhiT) (S Neutrons / R)) [Neutrons]

        val M: Long = OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value

        // ** Tx FEE for pool ** @todo v2: Fix with real Equation
        // This is the fee that gets collected to add into the pool during fission. There is an equation for this fee
        // but for v1, we're just going to use a constant of 1%.
        val PhiT: Long = 10 / precision

        // The protons and neutrons are lesser in outbox than inputbox
        val NeutronsActualValue: Long = IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2
        val ProtonsActualValue: Long = IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2
        val ErgsActualValue: Long = OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value

        val NeutronsExpectedValue: Long = M * (1 - PhiT) * (SNeutrons / RErg)
        val ProtonsExpectedValue: Long = M * (1 - PhiT) * (SProtons / RErg)
        val ErgsExpectedValue: Long = M

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
        val PhiFusion: Long = 10 / precision

        // The protons and neutrons are lesser in outbox than inputbox
        val NeutronsActualValue: Long = IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2
        val ProtonsActualValue: Long = IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2
        val ErgsActualValue: Long = OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value

        // M(1 - phiFusion) = Ergs
        // therefore, M = Ergs / (1 - phiFusion)
        val M: Long = ErgsActualValue * (precision / (precision - PhiFusion))


        val NeutronsExpectedValue: Long = M * (SNeutrons / RErg)
        val ProtonsExpectedValue: Long = M * (SProtons / RErg)
        // This is technically always true. We can take this off, but I don't want to.
        val ErgsExpectedValue: Long = ErgsActualValue

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
        // ===== BetaDecayPlus Tx ===== //
        // Equation: M [Neutrons] = M * (1 - PhiBeta(T)) * ((1 - q(R, S proton)) / q(R, S proton)) * (S protons / S neutrons) [Protons]

        val M: Long = OUT_GLUONW_NEUTRONS_TOKEN._2 - IN_GLUONW_NEUTRONS_TOKEN._2

        // ** Tx FEE for pool ** @todo v2: Fix with real Equation
        // This is the fee that gets collected to add into the pool during decay. There is an equation for this fee
        // but for v1, we're just going to use a constant of 1%.
        val VarPhiBeta: Long = 20 / precision

        // The protons and neutrons are lesser in outbox than inputbox
        val NeutronsActualValue: Long = IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2
        val ProtonsActualValue: Long = IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2
        val ErgsActualValue: Long = OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value

        // ** Fusion Ratio **
        // min(q*, (SNeutrons * Pt / R))
        // where q* is a constant, Pt is the price of gold in Ergs.
        val NeutronsExpectedValue: Long = M * (1 - VarPhiBeta) *
            ((1 - fusionRatio) / fusionRatio) *
            (SNeutrons / SProtons)
        val ProtonsExpectedValue: Long = M
        val ErgsExpectedValue: Long = IN_GLUONW_BOX.value + VarPhiBeta

        // ### The 2 conditions to ensure that the values out is right ### //
        val __inNeutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
        val __outProtonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
        val __ergsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

        sigmaProp(allOf(Coll(
            __checkGluonWBoxNFT,
            __inNeutronsValueValid,
            __outProtonsValueValid,
            __ergsValueValid
        )))
    }
    else if (isBetaDecayMinusTx)
    {
        // ===== BetaDecayMinus Tx ===== //
        // Equation: M [Protons] = M * (1 - PhiBeta(T)) * ((1 - q(R, S neutron)) / q(R, S neutron)) * (S neutrons / S protons) [Neutrons]

        val M: Long = OUT_GLUONW_PROTONS_TOKEN._2 - IN_GLUONW_PROTONS_TOKEN._2

        // ** Tx FEE for pool ** @todo v2: Fix with real Equation
        // This is the fee that gets collected to add into the pool during decay. There is an equation for this fee
        // but for v1, we're just going to use a constant of 1%.
        val VarPhiBeta: Long = 20 / precision

        // The protons and neutrons are lesser in outbox than inputbox
        val NeutronsActualValue: Long = IN_GLUONW_NEUTRONS_TOKEN._2 - OUT_GLUONW_NEUTRONS_TOKEN._2
        val ProtonsActualValue: Long = IN_GLUONW_PROTONS_TOKEN._2 - OUT_GLUONW_PROTONS_TOKEN._2
        val ErgsActualValue: Long = OUT_GLUONW_BOX.value - IN_GLUONW_BOX.value

        // ** Fusion Ratio **
        // min(q*, (SNeutrons * Pt / R))
        // where q* is a constant, Pt is the price of gold in Ergs.
        val NeutronsExpectedValue: Long = M
        val ProtonsExpectedValue: Long = M * (1 - VarPhiBeta) *
            (fusionRatio / (1 - fusionRatio)) *
            (SProtons / SNeutrons)
        val ErgsExpectedValue: Long = IN_GLUONW_BOX.value + VarPhiBeta

        // ### The 2 conditions to ensure that the values out is right ### //
        val __inNeutronsValueValid: Boolean = NeutronsActualValue == NeutronsExpectedValue
        val __outProtonsValueValid: Boolean = ProtonsActualValue == ProtonsExpectedValue
        val __ergsValueValid: Boolean = ErgsActualValue == ErgsExpectedValue

        sigmaProp(allOf(Coll(
            __checkGluonWBoxNFT,
            __inNeutronsValueValid,
            __outProtonsValueValid,
            __ergsValueValid
        )))
    }

    // Fails if not a valid tx
    sigmaProp(false)
}