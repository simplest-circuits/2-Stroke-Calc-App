package com.simplestsoft.twostrokecalc.domain.tools.vibration

import kotlin.math.max
import kotlin.math.sqrt

/**
 * 1D constant-state Kalman filter for a slowly drifting baseline (noise floor).
 * Vibration appears as the residual: measurement − baseline.
 */
class BaselineKalmanFilter(
    initialEstimate: Double = 0.0,
    initialVariance: Double = 1.0,
    /** How fast the baseline may drift (keep small → floor stays sticky). */
    private val processNoise: Double = 1e-5,
    /** Sensor / ambient measurement noise. */
    private val measurementNoise: Double = 0.04,
) {
    var estimate: Double = initialEstimate
        private set
    var variance: Double = initialVariance
        private set

    fun reset(initialEstimate: Double = 0.0, initialVariance: Double = 1.0) {
        estimate = initialEstimate
        variance = initialVariance
    }

    /**
     * @return residual (measurement − baseline). Near zero when only ground noise is present.
     */
    fun update(measurement: Double): Double {
        // Predict
        val predictedVariance = variance + processNoise
        // Kalman gain
        val gain = predictedVariance / (predictedVariance + measurementNoise)
        // Update
        val innovation = measurement - estimate
        estimate += gain * innovation
        variance = (1.0 - gain) * predictedVariance
        return measurement - estimate
    }

    /** Approximate 1σ of the residual under the current filter state. */
    fun residualSigma(): Double = sqrt(max(variance + measurementNoise, 1e-12))
}

/**
 * Per-axis baseline Kalman + magnitude gating so quiet ground noise collapses to ~0.
 */
class VibrationKalmanDenoiser(
    processNoise: Double = 1e-5,
    measurementNoise: Double = 0.04,
    /** Residuals below this multiple of σ are treated as silence. */
    private val gateSigma: Double = 2.5,
) {
    private val filterX = BaselineKalmanFilter(processNoise = processNoise, measurementNoise = measurementNoise)
    private val filterY = BaselineKalmanFilter(processNoise = processNoise, measurementNoise = measurementNoise)
    private val filterZ = BaselineKalmanFilter(processNoise = processNoise, measurementNoise = measurementNoise)
    private val filterMag = BaselineKalmanFilter(
        processNoise = processNoise * 0.5,
        measurementNoise = measurementNoise,
    )

    var sampleCount: Int = 0
        private set

    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
        filterMag.reset()
        sampleCount = 0
    }

    /**
     * Warm-up: only update baselines, return zero vibration so calibration does not
     * pollute RMS / spectrum with initial transients.
     */
    fun isWarmingUp(warmUpSamples: Int = DEFAULT_WARMUP_SAMPLES): Boolean =
        sampleCount < warmUpSamples

    fun push(sample: VibrationSample, warmUpSamples: Int = DEFAULT_WARMUP_SAMPLES): DenoisedVibrationSample {
        sampleCount++
        val rx = filterX.update(sample.x.toDouble())
        val ry = filterY.update(sample.y.toDouble())
        val rz = filterZ.update(sample.z.toDouble())

        if (isWarmingUp(warmUpSamples)) {
            // Teach magnitude floor with raw magnitude while axes settle.
            val rawMag = sqrt(sample.x * sample.x + sample.y * sample.y + sample.z * sample.z).toDouble()
            filterMag.update(rawMag)
            return DenoisedVibrationSample(
                timestampMs = sample.timestampMs,
                x = 0f,
                y = 0f,
                z = 0f,
                magnitude = 0.0,
                noiseFloor = filterMag.estimate,
                hasSignificantVibration = false,
            )
        }

        val sigma = (filterX.residualSigma() + filterY.residualSigma() + filterZ.residualSigma()) / 3.0
        val gate = gateSigma * sigma
        val gx = if (kotlin.math.abs(rx) < gate) 0.0 else rx
        val gy = if (kotlin.math.abs(ry) < gate) 0.0 else ry
        val gz = if (kotlin.math.abs(rz) < gate) 0.0 else rz
        val residualMag = sqrt(rx * rx + ry * ry + rz * rz)
        val gatedMag = sqrt(gx * gx + gy * gy + gz * gz)
        // Only adapt the magnitude floor while quiet — never pull vibration into the baseline.
        if (gatedMag == 0.0) {
            filterMag.update(residualMag)
        }

        return DenoisedVibrationSample(
            timestampMs = sample.timestampMs,
            x = gx.toFloat(),
            y = gy.toFloat(),
            z = gz.toFloat(),
            magnitude = gatedMag,
            noiseFloor = filterMag.estimate,
            hasSignificantVibration = gatedMag > 0.0,
        )
    }

    companion object {
        const val DEFAULT_WARMUP_SAMPLES = 40 // ~0.4 s at 100 Hz
    }
}

data class DenoisedVibrationSample(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Double,
    val noiseFloor: Double,
    val hasSignificantVibration: Boolean,
)
