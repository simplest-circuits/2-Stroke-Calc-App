package com.simplestsoft.twostrokecalc.domain.tools.vibration

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class VibrationSample(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

data class SpectrumBin(
    val frequencyHz: Double,
    val magnitude: Double,
)

data class VibrationAnalysisResult(
    val spectrum: List<SpectrumBin>,
    val rms: Double,
    val dominantFrequencyHz: Double,
    val dominantMagnitude: Double,
    val durationMs: Long,
    val noiseFloor: Double = 0.0,
    val hasSignificantVibration: Boolean = false,
)

object VibrationAnalyzer {
    fun analyze(
        samples: List<VibrationSample>,
        sampleRateHz: Int = 100,
        fftSize: Int = 256,
        highpassHz: Double = 5.0,
    ): VibrationAnalysisResult? {
        if (samples.size < fftSize || sampleRateHz <= 0) return null
        val denoiser = VibrationKalmanDenoiser()
        val denoised = samples.takeLast(fftSize).map { denoiser.push(it) }
        return analyzeMagnitudes(
            magnitudes = denoised.map { it.magnitude },
            timestampsMs = denoised.map { it.timestampMs },
            sampleRateHz = sampleRateHz,
            fftSize = fftSize,
            highpassHz = highpassHz,
            noiseFloor = denoised.last().noiseFloor,
        )
    }

    /**
     * Analyze a pre-denoised magnitude series (live path where
     * [VibrationKalmanDenoiser] already runs per sample).
     */
    fun analyzeMagnitudes(
        magnitudes: List<Double>,
        timestampsMs: List<Long>,
        sampleRateHz: Int = 100,
        fftSize: Int = 256,
        highpassHz: Double = 5.0,
        noiseFloor: Double = 0.0,
    ): VibrationAnalysisResult? {
        if (magnitudes.size < fftSize || timestampsMs.size < fftSize || sampleRateHz <= 0) return null
        val window = magnitudes.takeLast(fftSize)
        val ts = timestampsMs.takeLast(fftSize)
        val hasVibration = window.any { it > 0.0 }
        val mean = window.average()
        val centered = FloatArray(fftSize) { i -> (window[i] - mean).toFloat() }
        val filtered = highpass(centered, sampleRateHz, highpassHz)
        val spectrumMags = fftMagnitude(filtered)
        val binHz = sampleRateHz.toDouble() / fftSize
        val spectrum = spectrumMags.mapIndexed { index, mag ->
            SpectrumBin(frequencyHz = index * binHz, magnitude = mag)
        }.drop(1)
        val duration = ts.last() - ts.first()
        if (!hasVibration) {
            return VibrationAnalysisResult(
                spectrum = spectrum.map { it.copy(magnitude = 0.0) },
                rms = 0.0,
                dominantFrequencyHz = 0.0,
                dominantMagnitude = 0.0,
                durationMs = duration.coerceAtLeast(0L),
                noiseFloor = noiseFloor,
                hasSignificantVibration = false,
            )
        }
        val dominant = spectrum.maxByOrNull { it.magnitude } ?: return null
        val rms = sqrt(filtered.map { it * it.toDouble() }.average())
        return VibrationAnalysisResult(
            spectrum = spectrum,
            rms = rms,
            dominantFrequencyHz = dominant.frequencyHz,
            dominantMagnitude = dominant.magnitude,
            durationMs = duration.coerceAtLeast(0L),
            noiseFloor = noiseFloor,
            hasSignificantVibration = true,
        )
    }

    private fun highpass(input: FloatArray, sampleRateHz: Int, cutoffHz: Double): FloatArray {
        if (cutoffHz <= 0.0) return input
        val rc = 1.0 / (2.0 * PI * cutoffHz)
        val dt = 1.0 / sampleRateHz
        val alpha = rc / (rc + dt)
        val out = FloatArray(input.size)
        out[0] = input[0]
        for (i in 1 until input.size) {
            out[i] = (alpha * (out[i - 1] + input[i] - input[i - 1])).toFloat()
        }
        return out
    }

    private fun fftMagnitude(input: FloatArray): DoubleArray {
        val n = input.size
        require(n > 0 && n and (n - 1) == 0)
        val real = DoubleArray(n) { input[it].toDouble() }
        val imag = DoubleArray(n)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wlenRe = cos(angle)
            val wlenIm = sin(angle)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = real[i + k]
                    val uIm = imag[i + k]
                    val vRe = real[i + k + len / 2] * wRe - imag[i + k + len / 2] * wIm
                    val vIm = real[i + k + len / 2] * wIm + imag[i + k + len / 2] * wRe
                    real[i + k] = uRe + vRe
                    imag[i + k] = uIm + vIm
                    real[i + k + len / 2] = uRe - vRe
                    imag[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    wIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
        return DoubleArray(n / 2) { idx ->
            sqrt(real[idx] * real[idx] + imag[idx] * imag[idx])
        }
    }
}
