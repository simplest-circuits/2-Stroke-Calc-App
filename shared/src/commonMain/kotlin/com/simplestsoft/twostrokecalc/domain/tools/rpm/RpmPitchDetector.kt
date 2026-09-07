package com.simplestsoft.twostrokecalc.domain.tools.rpm

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class RpmPitchCandidate(
    val frequencyHz: Double,
    /** 0…1 periodicity confidence from YIN's CMNDF minimum. */
    val clarity: Double,
)

/**
 * YIN pitch detection adapted for engine acoustics.
 *
 * The input is first decimated to keep the O(N × lag) difference calculation
 * inexpensive. [envelope] is used as a second analysis path for sharp exhaust
 * bursts whose fundamental is weak in the raw microphone waveform.
 */
object RpmPitchDetector {
    private const val DECIMATION = 8
    private const val YIN_THRESHOLD = 0.18

    fun envelope(samples: FloatArray, sampleRateHz: Int): FloatArray {
        val output = FloatArray(samples.size)
        // 350 Hz leaves the firing envelope intact but removes harsh high-frequency content.
        val alpha = exp(-2.0 * PI * 350.0 / sampleRateHz.coerceAtLeast(1))
        var state = 0.0
        for (i in samples.indices) {
            state = alpha * state + (1.0 - alpha) * abs(samples[i])
            output[i] = state.toFloat()
        }
        return output
    }

    fun detectYin(
        samples: FloatArray,
        sampleRateHz: Int,
        minHz: Double,
        maxHz: Double,
    ): RpmPitchCandidate? {
        if (sampleRateHz <= 0 || minHz <= 0.0 || maxHz <= minHz) return null

        val count = samples.size / DECIMATION
        if (count < 64) return null
        val signal = DoubleArray(count)
        var mean = 0.0
        for (i in 0 until count) {
            // Box averaging is a cheap anti-alias filter before decimation.
            var sum = 0.0
            val offset = i * DECIMATION
            for (j in 0 until DECIMATION) sum += samples[offset + j]
            val value = sum / DECIMATION
            signal[i] = value
            mean += value
        }
        mean /= count
        var energy = 0.0
        for (i in signal.indices) {
            signal[i] -= mean
            energy += signal[i] * signal[i]
        }
        if (energy < 1e-10) return null

        val rate = sampleRateHz.toDouble() / DECIMATION
        val minLag = max(2, (rate / maxHz).toInt())
        val maxLag = min(count / 2, (rate / minHz).toInt())
        if (minLag >= maxLag) return null

        val difference = DoubleArray(maxLag + 1)
        for (lag in minLag..maxLag) {
            var sum = 0.0
            for (i in 0 until count - lag) {
                val delta = signal[i] - signal[i + lag]
                sum += delta * delta
            }
            difference[lag] = sum
        }

        val cmndf = DoubleArray(maxLag + 1) { 1.0 }
        var cumulative = 0.0
        for (lag in 1..maxLag) {
            cumulative += difference[lag]
            if (lag >= minLag && cumulative > 1e-12) {
                cmndf[lag] = difference[lag] * lag / cumulative
            }
        }

        var bestLag = -1
        var bestValue = Double.POSITIVE_INFINITY
        // YIN chooses the first strong local minimum rather than the global one,
        // which prevents selecting an integer multiple of the real engine period.
        var lag = minLag + 1
        while (lag < maxLag) {
            if (cmndf[lag] < YIN_THRESHOLD && cmndf[lag] <= cmndf[lag - 1]) {
                while (lag + 1 <= maxLag && cmndf[lag + 1] < cmndf[lag]) lag++
                bestLag = lag
                bestValue = cmndf[lag]
                break
            }
            lag++
        }
        if (bestLag < 0) {
            for (candidateLag in minLag..maxLag) {
                if (cmndf[candidateLag] < bestValue) {
                    bestValue = cmndf[candidateLag]
                    bestLag = candidateLag
                }
            }
        }
        if (bestLag < 0 || bestValue > 0.45) return null

        val prev = if (bestLag > minLag) cmndf[bestLag - 1] else cmndf[bestLag]
        val centre = cmndf[bestLag]
        val next = if (bestLag < maxLag) cmndf[bestLag + 1] else cmndf[bestLag]
        val denominator = prev - 2.0 * centre + next
        val offset = if (abs(denominator) < 1e-12) 0.0 else {
            (0.5 * (prev - next) / denominator).coerceIn(-0.5, 0.5)
        }
        val refinedLag = bestLag + offset
        val frequency = rate / refinedLag
        if (frequency !in minHz..maxHz) return null
        return RpmPitchCandidate(
            frequencyHz = frequency,
            clarity = (1.0 - bestValue).coerceIn(0.0, 1.0),
        )
    }
}
