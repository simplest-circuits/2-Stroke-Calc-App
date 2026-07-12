package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Manual validation against literature / chart reference values.
 * Run with: ./gradlew :androidApp:testDebugUnitTest --tests CarbJetCorrectionValidationTest
 */
class CarbJetCorrectionValidationTest {

    private data class Case(
        val name: String,
        val baseJet: Int,
        val refAltM: Double,
        val refTempC: Double,
        val tgtAltM: Double,
        val tgtTempC: Double,
        val expectedFactor: Double? = null,
        val expectedJet: Int? = null,
        val factorTolerance: Double = 0.02,
        val jetTolerance: Int = 1,
        val expectLargerJet: Boolean? = null,
    )

    @Test
    fun literatureReferenceCases() {
        val cases = listOf(
            Case(
                name = "Identische Bedingungen",
                baseJet = 165,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 0.0,
                tgtTempC = 15.0,
                expectedFactor = 1.0,
                expectedJet = 165,
                factorTolerance = 0.001,
                jetTolerance = 0,
            ),
            Case(
                name = "Rotax 447 Meadow Lake (6800 ft, 20 C)",
                baseJet = 165,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 2073.0,
                tgtTempC = 20.0,
                expectedFactor = 0.94,
                expectedJet = 155,
            ),
            Case(
                name = "Blasterforum 2000 m / 15 C",
                baseJet = 260,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 2000.0,
                tgtTempC = 15.0,
                expectedFactor = 0.94,
                expectedJet = 245,
            ),
            Case(
                name = "Poiseuille 8000 ft (~2438 m)",
                baseJet = 165,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 2438.0,
                tgtTempC = 15.0,
                expectedJet = 154,
                jetTolerance = 1,
            ),
            Case(
                name = "Kaltwetter Meereshöhe -10 C",
                baseJet = 165,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 0.0,
                tgtTempC = -10.0,
                expectLargerJet = true,
            ),
            Case(
                name = "Heisser Tag 35 C Meereshöhe",
                baseJet = 165,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 0.0,
                tgtTempC = 35.0,
                expectLargerJet = false,
            ),
            Case(
                name = "Denver ~1600 m / 21 C",
                baseJet = 175,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 1600.0,
                tgtTempC = 21.0,
                expectedJet = 165,
                jetTolerance = 2,
            ),
            Case(
                name = "Alpen 2500 m / 10 C",
                baseJet = 180,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 2500.0,
                tgtTempC = 10.0,
                expectedJet = 168,
                jetTolerance = 1,
            ),
            Case(
                name = "Nur Temperatur 15 C -> 30 C",
                baseJet = 160,
                refAltM = 0.0,
                refTempC = 15.0,
                tgtAltM = 0.0,
                tgtTempC = 30.0,
                expectedFactor = 0.987,
                expectedJet = 158,
                factorTolerance = 0.005,
            ),
            Case(
                name = "Nightrider 1500 ft -> 4500 ft bei 100 F",
                baseJet = 150,
                refAltM = 457.0,
                refTempC = 15.0,
                tgtAltM = 1372.0,
                tgtTempC = 37.8,
                expectedFactor = 0.94,
                expectedJet = 141,
                jetTolerance = 2,
            ),
            Case(
                name = "Rueckrechnung Denver -> Meeresspiegel",
                baseJet = 168,
                refAltM = 1600.0,
                refTempC = 21.0,
                tgtAltM = 0.0,
                tgtTempC = 15.0,
                expectLargerJet = true,
            ),
        )

        val report = buildString {
            appendLine("CarbJetCorrection Validation Report")
            appendLine("=".repeat(80))
            cases.forEach { case ->
                val result = CarbJetCorrectionCalculator.calculate(
                    baseMainJet = case.baseJet,
                    referenceAltitudeM = case.refAltM,
                    referenceTemperatureC = case.refTempC,
                    targetAltitudeM = case.tgtAltM,
                    targetTemperatureC = case.tgtTempC,
                )
                assertNotNull("${case.name}: null result", result)
                val r = result!!

                appendLine(case.name)
                appendLine("  Input:  jet=${case.baseJet}, ref=${case.refAltM}m/${case.refTempC}C -> ${case.tgtAltM}m/${case.tgtTempC}C")
                appendLine("  Output: factor=${"%.4f".format(r.correctionFactor)}, exact=${"%.2f".format(r.correctedMainJetExact)}, jet=${r.correctedMainJet}")
                appendLine("  Press:  ref=${"%.0f".format(r.referencePressureMbar)} mbar, tgt=${"%.0f".format(r.targetPressureMbar)} mbar")

                case.expectedFactor?.let { expected ->
                    assertTrue(
                        "${case.name}: factor ${r.correctionFactor} not within ${case.factorTolerance} of $expected",
                        abs(r.correctionFactor - expected) <= case.factorTolerance,
                    )
                    appendLine("  Factor check: OK (expected ~$expected)")
                }
                case.expectedJet?.let { expected ->
                    assertTrue(
                        "${case.name}: jet ${r.correctedMainJet} not within ${case.jetTolerance} of $expected",
                        abs(r.correctedMainJet - expected) <= case.jetTolerance,
                    )
                    appendLine("  Jet check: OK (expected ~$expected)")
                }
                case.expectLargerJet?.let { larger ->
                    if (larger) {
                        assertTrue("${case.name}: expected larger jet", r.correctedMainJet > case.baseJet)
                        appendLine("  Direction check: OK (larger jet)")
                    } else {
                        assertTrue("${case.name}: expected smaller jet", r.correctedMainJet < case.baseJet)
                        appendLine("  Direction check: OK (smaller jet)")
                    }
                }
                appendLine()
            }
        }
        println(report)
    }

    @Test
    fun pressureAt2000m_matchesIsa() {
        val pressure = CarbJetCorrectionCalculator.pressureAtAltitudeMeters(2000.0)
        assertNotNull(pressure)
        assertEquals(794.7, pressure!!, 2.0)
    }
}
