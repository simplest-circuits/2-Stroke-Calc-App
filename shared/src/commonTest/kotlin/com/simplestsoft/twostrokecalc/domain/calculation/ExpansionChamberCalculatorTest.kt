package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpansionChamberCalculatorTest {

    private fun sampleInput(
        diffuserStages: ExpansionChamberDiffuserStages = ExpansionChamberDiffuserStages.THREE,
    ) = ExpansionChamberInput(
        exhaustPortWidthMm = 40.0,
        exhaustPortHeightMm = 20.0,
        exhaustPortDurationDeg = 180.0,
        transferPortDurationDeg = 130.0,
        rpm = 9000.0,
        powerPs = 30.0,
        displacementCc = 125.0,
        diffuserStages = diffuserStages,
        coefficients = ExpansionChamberCoefficients.forPreset(ExpansionChamberPreset.ENDURO),
    )

    @Test
    fun calculate_threeStage_enduroExample() {
        val result = ExpansionChamberCalculator.calculate(sampleInput())!!

        assertEquals(800.0, result.portAreaMm2, 0.01)
        assertEquals(31.91, result.equivalentPortDiameterMm, 0.01)
        assertEquals(11.76, result.bmepBar, 0.01)
        assertEquals(650.0, result.exhaustGasTempCelsius, 0.01)
        assertEquals(3, result.diffuserStageCount)
        assertEquals(7, result.segments.size)
        assertEquals(result.tunedLengthMm * 1.24, result.totalLengthMm, 0.01)
        assertEquals(result.diametersMm["D1"]!!, result.segments.first().startDiameterMm, 0.01)
        assertEquals(result.diametersMm["D5"]!!, result.segments.last().startDiameterMm, 0.01)
    }

    @Test
    fun calculate_twoStage_hasSixSegments() {
        val result = ExpansionChamberCalculator.calculate(
            sampleInput(diffuserStages = ExpansionChamberDiffuserStages.TWO),
        )!!

        assertEquals(2, result.diffuserStageCount)
        assertEquals(6, result.segments.size)
        assertEquals(result.tunedLengthMm * 1.24, result.totalLengthMm, 0.01)
        assertEquals(4, result.diametersMm.size)
    }

    @Test
    fun calculate_oneStage_hasFiveSegments() {
        val result = ExpansionChamberCalculator.calculate(
            sampleInput(diffuserStages = ExpansionChamberDiffuserStages.ONE),
        )!!

        assertEquals(1, result.diffuserStageCount)
        assertEquals(5, result.segments.size)
        assertEquals(result.tunedLengthMm * 1.24, result.totalLengthMm, 0.01)
        assertEquals(3, result.diametersMm.size)
    }

    @Test
    fun calculate_hasExactlyNDiffuserSegments() {
        ExpansionChamberDiffuserStages.entries.forEach { stages ->
            val result = ExpansionChamberCalculator.calculate(
                sampleInput(diffuserStages = stages),
            )!!

            assertEquals(stages.stageCount, result.diffuserStageCount)
            assertEquals(stages.stageCount, result.diffuserSegments.size)
        }
    }

    @Test
    fun calculate_diffuserStepsBecomeSmallerWithMoreSegments() {
        val oneStage = ExpansionChamberCalculator.calculate(
            sampleInput(diffuserStages = ExpansionChamberDiffuserStages.ONE),
        )!!
        val threeStage = ExpansionChamberCalculator.calculate(
            sampleInput(diffuserStages = ExpansionChamberDiffuserStages.THREE),
        )!!

        val oneStageStep = oneStage.diffuserSegments.single().let {
            it.endDiameterMm - it.startDiameterMm
        }
        val threeStageMaxStep = threeStage.diffuserSegments.maxOf {
            it.endDiameterMm - it.startDiameterMm
        }

        assert(oneStageStep > threeStageMaxStep)
    }

    @Test
    fun calculate_diametersIncreaseThroughDiffuser() {
        val result = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                exhaustPortWidthMm = 35.0,
                exhaustPortHeightMm = 18.0,
                exhaustPortDurationDeg = 175.0,
                transferPortDurationDeg = 128.0,
                rpm = 11000.0,
                powerPs = 45.0,
            ),
        )!!

        val d1 = result.diametersMm["D1"]!!
        val d2 = result.diametersMm["D2"]!!
        val d3 = result.diametersMm["D3"]!!
        val d4 = result.diametersMm["D4"]!!
        val d5 = result.diametersMm["D5"]!!

        assert(d1 < d2)
        assert(d2 < d3)
        assert(d3 < d4)
        assert(d5 < d4)
        assert(d5 < d1)
    }

    @Test
    fun calculate_manualExhaustTemp_changesTunedLength() {
        val auto = ExpansionChamberCalculator.calculate(sampleInput())!!
        val manual = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                optionalInputs = ExpansionChamberOptionalInputs(
                    exhaustGasTempCelsius = 350.0,
                ),
            ),
        )!!

        assert(manual.tunedLengthMm < auto.tunedLengthMm)
        assertEquals(350.0, manual.exhaustGasTempCelsius, 0.01)
        assert(manual.usedManualExhaustTemp)
    }

    @Test
    fun calculate_manualBmep_withoutPower() {
        val result = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                powerPs = null,
                displacementCc = null,
                optionalInputs = ExpansionChamberOptionalInputs(bmepBar = 9.5),
            ),
        )!!

        assertEquals(9.5, result.bmepBar, 0.01)
        assertEquals(600.0, result.exhaustGasTempCelsius, 0.01)
        assert(result.usedManualBmep)
    }

    @Test
    fun calculate_manualPortArea() {
        val result = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                exhaustPortWidthMm = null,
                exhaustPortHeightMm = null,
                optionalInputs = ExpansionChamberOptionalInputs(
                    portAreaMm2 = 900.0,
                ),
            ),
        )!!

        assertEquals(900.0, result.portAreaMm2, 0.01)
        assert(result.usedManualPortArea)
    }

    @Test
    fun calculate_invalidInput() {
        assertNull(
            ExpansionChamberCalculator.calculate(
                sampleInput().copy(exhaustPortWidthMm = 0.0),
            ),
        )
    }

    @Test
    fun calculate_hornCoefficient_changesThreeStageIntermediateDiameters() {
        val base = sampleInput()
        val narrow = ExpansionChamberCalculator.calculate(
            base.copy(
                coefficients = base.coefficients.copy(hornCoefficient = 1.0),
            ),
        )!!
        val wide = ExpansionChamberCalculator.calculate(
            base.copy(
                coefficients = base.coefficients.copy(hornCoefficient = 2.0),
            ),
        )!!

        assert(narrow.diametersMm["D2"]!! != wide.diametersMm["D2"]!!)
        assert(narrow.diametersMm["D3"]!! != wide.diametersMm["D3"]!!)
        assertEquals(narrow.tunedLengthMm, wide.tunedLengthMm, 0.01)
        assertEquals(narrow.diametersMm["D1"]!!, wide.diametersMm["D1"]!!, 0.01)
        assertEquals(narrow.diametersMm["D4"]!!, wide.diametersMm["D4"]!!, 0.01)
    }

    @Test
    fun calculate_hornCoefficient_changesTwoStageIntermediateDiameter() {
        val base = sampleInput(diffuserStages = ExpansionChamberDiffuserStages.TWO)
        val narrow = ExpansionChamberCalculator.calculate(
            base.copy(
                coefficients = base.coefficients.copy(hornCoefficient = 1.0),
            ),
        )!!
        val wide = ExpansionChamberCalculator.calculate(
            base.copy(
                coefficients = base.coefficients.copy(hornCoefficient = 2.0),
            ),
        )!!

        assert(narrow.diametersMm["D2"]!! != wide.diametersMm["D2"]!!)
        assertEquals(narrow.tunedLengthMm, wide.tunedLengthMm, 0.01)
    }

    @Test
    fun calculate_manualEquivalentPortDiameter_withoutPortDimensions() {
        val result = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                exhaustPortWidthMm = null,
                exhaustPortHeightMm = null,
                optionalInputs = ExpansionChamberOptionalInputs(
                    equivalentPortDiameterMm = 35.0,
                ),
            ),
        )!!

        assertEquals(35.0, result.equivalentPortDiameterMm, 0.01)
        assertEquals(962.1, result.portAreaMm2, 0.1)
        assert(result.usedManualEquivalentPortDiameter)
    }

    @Test
    fun calculate_manualSpeedOfSound_changesTunedLength() {
        val auto = ExpansionChamberCalculator.calculate(sampleInput())!!
        val manual = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                optionalInputs = ExpansionChamberOptionalInputs(
                    speedOfSoundMs = 400.0,
                ),
            ),
        )!!

        assert(manual.tunedLengthMm < auto.tunedLengthMm)
        assertEquals(400.0, manual.speedOfSoundMs, 0.01)
        assert(manual.usedManualSpeedOfSound)
    }

    @Test
    fun calculate_manualGamma_changesTunedLengthWhenExhaustTempManual() {
        val lowGamma = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                optionalInputs = ExpansionChamberOptionalInputs(
                    exhaustGasTempCelsius = 600.0,
                    specificHeatRatio = 1.2,
                ),
            ),
        )!!
        val highGamma = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                optionalInputs = ExpansionChamberOptionalInputs(
                    exhaustGasTempCelsius = 600.0,
                    specificHeatRatio = 1.6,
                ),
            ),
        )!!

        assert(highGamma.tunedLengthMm > lowGamma.tunedLengthMm)
        assertEquals(1.2, lowGamma.specificHeatRatio, 0.01)
        assertEquals(1.6, highGamma.specificHeatRatio, 0.01)
    }

    @Test
    fun calculate_manualGasConstant_changesTunedLengthWhenExhaustTempManual() {
        val lowR = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                optionalInputs = ExpansionChamberOptionalInputs(
                    exhaustGasTempCelsius = 600.0,
                    gasConstant = 250.0,
                ),
            ),
        )!!
        val highR = ExpansionChamberCalculator.calculate(
            sampleInput().copy(
                optionalInputs = ExpansionChamberOptionalInputs(
                    exhaustGasTempCelsius = 600.0,
                    gasConstant = 320.0,
                ),
            ),
        )!!

        assert(highR.tunedLengthMm > lowR.tunedLengthMm)
    }

    @Test
    fun calculate_transferPortDuration_doesNotChangeTunedLength() {
        val shortTransfer = ExpansionChamberCalculator.calculate(
            sampleInput().copy(transferPortDurationDeg = 100.0),
        )!!
        val longTransfer = ExpansionChamberCalculator.calculate(
            sampleInput().copy(transferPortDurationDeg = 160.0),
        )!!

        assertEquals(shortTransfer.tunedLengthMm, longTransfer.tunedLengthMm, 0.01)
    }

    @Test
    fun exhaustTemperatureFromBmep() {
        assertEquals(650.0, ExpansionChamberCalculator.exhaustTemperatureFromBmep(12.0), 0.01)
        assertEquals(600.0, ExpansionChamberCalculator.exhaustTemperatureFromBmep(10.0), 0.01)
        assertEquals(500.0, ExpansionChamberCalculator.exhaustTemperatureFromBmep(8.5), 0.01)
        assertEquals(350.0, ExpansionChamberCalculator.exhaustTemperatureFromBmep(6.0), 0.01)
    }
}
