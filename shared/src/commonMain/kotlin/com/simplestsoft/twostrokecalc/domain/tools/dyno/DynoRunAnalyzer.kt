package com.simplestsoft.twostrokecalc.domain.tools.dyno

import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsEngineMode
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsGearInput
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsInput
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class DynoRunSample(
    val timestampMs: Long,
    val speedKmh: Double,
    val accelMs2: Double? = null,
    val gpsAccuracyM: Double? = null,
)

data class DynoPowerPoint(
    val speedKmh: Double,
    val powerPs: Double,
    val accelMs2: Double,
)

data class DynoSpeedChartPoint(
    val elapsedMs: Long,
    val speedKmh: Double,
)

data class DynoRunResult(
    val powerCurve: List<DynoPowerPoint>,
    val peakPowerPs: Double,
    val time0to60Ms: Long?,
    val time0to100Ms: Long?,
    val quarterMileSeconds: Double?,
    val confidence: Double,
    val maxSpeedKmh: Double,
)

data class DynoVehicleContext(
    val massKg: Double,
    val driverMassKg: Double = 75.0,
    val dragCoefficient: Double = 0.65,
    val frontalAreaM2: Double = 0.50,
    val rollingResistance: Double = 0.015,
    val massFactor: Double = 1.10,
    val drivetrainEfficiency: Double = 0.85,
    val airDensityKgM3: Double = VehicleDynamicsCalculator.DEFAULT_AIR_DENSITY_KG_M3,
    val gradientPercent: Double = 0.0,
)

object DynoRunAnalyzer {
    fun analyze(
        samples: List<DynoRunSample>,
        context: DynoVehicleContext,
    ): DynoRunResult? {
        if (samples.size < 5) return null
        val sorted = samples.sortedBy { it.timestampMs }
        val smoothedSpeeds = savitzkyGolaySmooth(sorted.map { it.speedKmh }, window = 3)
        val powerPoints = mutableListOf<DynoPowerPoint>()
        var accuracySum = 0.0
        var accuracyCount = 0

        for (i in 1 until smoothedSpeeds.size) {
            val dtS = (sorted[i].timestampMs - sorted[i - 1].timestampMs) / 1000.0
            if (dtS <= 0.0) continue
            val v0 = smoothedSpeeds[i - 1] / 3.6
            val v1 = smoothedSpeeds[i] / 3.6
            val accel = sorted[i].accelMs2 ?: ((v1 - v0) / dtS)
            val speedKmh = smoothedSpeeds[i]
            val powerPs = powerFromAccel(
                accelMs2 = accel,
                speedKmh = speedKmh,
                context = context,
            )
            powerPoints += DynoPowerPoint(
                speedKmh = speedKmh,
                powerPs = powerPs,
                accelMs2 = accel,
            )
            sorted[i].gpsAccuracyM?.let {
                accuracySum += it
                accuracyCount++
            }
        }
        if (powerPoints.isEmpty()) return null

        val peak = powerPoints.maxOf { it.powerPs }
        val t60 = sprintTimeMs(sorted, smoothedSpeeds, 60.0)
        val t100 = sprintTimeMs(sorted, smoothedSpeeds, 100.0)
        val quarter = estimateQuarterMile(sorted, smoothedSpeeds)
        val avgAccuracy = if (accuracyCount > 0) accuracySum / accuracyCount else 15.0
        val confidence = (1.0 - ((avgAccuracy - 3.0) / 25.0)).coerceIn(0.15, 0.95)

        return DynoRunResult(
            powerCurve = powerPoints,
            peakPowerPs = peak,
            time0to60Ms = t60,
            time0to100Ms = t100,
            quarterMileSeconds = quarter,
            confidence = confidence,
            maxSpeedKmh = smoothedSpeeds.maxOrNull() ?: 0.0,
        )
    }

    fun speedChartPoints(samples: List<DynoRunSample>): List<DynoSpeedChartPoint> {
        if (samples.isEmpty()) return emptyList()
        val sorted = samples.sortedBy { it.timestampMs }
        val smoothed = savitzkyGolaySmooth(sorted.map { it.speedKmh }, window = 3)
        val startTime = sorted.first().timestampMs
        return sorted.indices.map { index ->
            DynoSpeedChartPoint(
                elapsedMs = sorted[index].timestampMs - startTime,
                speedKmh = smoothed[index],
            )
        }
    }

    fun instantPowerPs(
        previous: DynoRunSample,
        current: DynoRunSample,
        context: DynoVehicleContext,
    ): Double? {
        val dtS = (current.timestampMs - previous.timestampMs) / 1000.0
        if (dtS <= 0.0) return null
        val v0 = previous.speedKmh / 3.6
        val v1 = current.speedKmh / 3.6
        val accel = current.accelMs2 ?: ((v1 - v0) / dtS)
        return powerFromAccel(accelMs2 = accel, speedKmh = current.speedKmh, context = context)
    }

    private fun powerFromAccel(
        accelMs2: Double,
        speedKmh: Double,
        context: DynoVehicleContext,
    ): Double {
        val totalMass = context.massKg + context.driverMassKg
        val speedMs = speedKmh / 3.6
        val drag = VehicleDynamicsCalculator.dragForceN(
            speedMs = speedMs,
            dragCoefficient = context.dragCoefficient,
            frontalAreaM2 = context.frontalAreaM2,
            airDensityKgM3 = context.airDensityKgM3,
        )
        val rolling = VehicleDynamicsCalculator.rollingForceN(
            massKg = totalMass,
            rollingResistance = context.rollingResistance,
            gradientPercent = context.gradientPercent,
        )
        val gradient = VehicleDynamicsCalculator.gradientForceN(
            massKg = totalMass,
            gradientPercent = context.gradientPercent,
        )
        val net = context.massFactor * totalMass * accelMs2
        val drive = net + drag + rolling + gradient
        val wheelPowerW = max(0.0, drive * speedMs)
        val crankPowerW = wheelPowerW / context.drivetrainEfficiency.coerceIn(0.1, 1.0)
        return crankPowerW / VehicleDynamicsCalculator.PS_TO_WATT
    }

    fun dummyDynamicsInputForReference(context: DynoVehicleContext, powerPs: Double): VehicleDynamicsInput =
        VehicleDynamicsInput(
            massKg = context.massKg + context.driverMassKg,
            engineMode = VehicleDynamicsEngineMode.POWER,
            powerPs = powerPs,
            torqueNm = null,
            engineRpm = 7000.0,
            maxEngineRpm = 10000.0,
            primaryPinion = 15,
            primaryGear = 54,
            stages = listOf(VehicleDynamicsGearInput(15, 42)),
            wheelCircumferenceMm = 1307.0,
            shiftRpm = 9000.0,
            drivetrainEfficiency = context.drivetrainEfficiency,
            dragCoefficient = context.dragCoefficient,
            frontalAreaM2 = context.frontalAreaM2,
            rollingResistance = context.rollingResistance,
            airDensityKgM3 = context.airDensityKgM3,
            massFactor = context.massFactor,
            gradientPercent = context.gradientPercent,
            tractionLimitG = null,
            analysisSpeedKmh = 50.0,
            targetSpeedKmh = 80.0,
            selectedGearIndex = 0,
        )

    private fun sprintTimeMs(
        samples: List<DynoRunSample>,
        speeds: List<Double>,
        targetKmh: Double,
    ): Long? {
        val startIdx = speeds.indexOfFirst { it >= 1.0 }
        val endIdx = speeds.indexOfFirst { it >= targetKmh }
        if (startIdx < 0 || endIdx < 0 || endIdx <= startIdx) return null
        return samples[endIdx].timestampMs - samples[startIdx].timestampMs
    }

    private fun estimateQuarterMile(
        samples: List<DynoRunSample>,
        speeds: List<Double>,
    ): Double? {
        if (samples.size < 2) return null
        var distanceM = 0.0
        for (i in 1 until speeds.size) {
            val dt = (samples[i].timestampMs - samples[i - 1].timestampMs) / 1000.0
            val vAvg = ((speeds[i] + speeds[i - 1]) / 2.0) / 3.6
            distanceM += vAvg * dt
            if (distanceM >= 402.336) {
                return (samples[i].timestampMs - samples.first().timestampMs) / 1000.0
            }
        }
        return null
    }

    /** Simple odd-window moving average as Savitzky–Golay stand-in for KMP. */
    fun savitzkyGolaySmooth(values: List<Double>, window: Int = 5): List<Double> {
        if (values.isEmpty()) return emptyList()
        val w = if (window % 2 == 0) window + 1 else window
        val half = w / 2
        return values.indices.map { i ->
            val from = max(0, i - half)
            val to = min(values.lastIndex, i + half)
            values.subList(from, to + 1).average()
        }
    }

    fun rmsAccelError(a: List<Double>, b: List<Double>): Double {
        val n = min(a.size, b.size)
        if (n == 0) return Double.POSITIVE_INFINITY
        var sum = 0.0
        for (i in 0 until n) {
            sum += (a[i] - b[i]).pow(2)
        }
        return sqrt(sum / n)
    }
}
