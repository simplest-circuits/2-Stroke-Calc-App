package com.simplestsoft.twostrokecalc.domain.tools.rpm

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RpmAnalysisConfig(
    val sampleRateHz: Int = 44100,
    /** 93 ms at 44.1 kHz: short enough for a responsive live readout. */
    val fftSize: Int = 4096,
    /** ~1320 rpm @ 1 pulse/rev; covers normal two-stroke idle. */
    val bandpassMinHz: Double = 22.0,
    val bandpassMaxHz: Double = 400.0,
    val cylinderCount: Int = 1,
    val pulsesPerRevolution: Double = 1.0,
    val medianWindow: Int = 3,
    val minRms: Double = 0.006,
    /** Optional user-provided target used to reject octave errors. */
    val hintRpm: Double? = null,
    val hintTolerance: Double = 0.45,
)

data class RpmAnalysisResult(
    val rpm: Double,
    val peakFrequencyHz: Double,
    val confidence: Double,
    val timestampMs: Long = 0L,
    val signalRms: Double = 0.0,
    /** True when this is the previous lock, held while a jump is still unconfirmed. */
    val held: Boolean = false,
)

/** Estimates engine RPM with YIN periodicity detection on raw and envelope signals. */
object RpmEstimator {
    private val recentRpms = ArrayDeque<Double>()
    private var pendingChangeRpm: Double? = null
    private var pendingChangeFrames: Int = 0
    private var lastPublished: RpmAnalysisResult? = null

    fun reset() {
        recentRpms.clear()
        pendingChangeRpm = null
        pendingChangeFrames = 0
        lastPublished = null
    }

    fun analyze(samples: FloatArray, config: RpmAnalysisConfig): RpmAnalysisResult? {
        if (samples.size < config.fftSize || config.sampleRateHz <= 0) return null

        var sumSq = 0.0
        for (i in 0 until config.fftSize) {
            val s = samples[i].toDouble()
            sumSq += s * s
        }
        val rms = sqrt(sumSq / config.fftSize)
        if (rms < config.minRms) return null

        val rawCandidate = RpmPitchDetector.detectYin(
            samples = samples,
            sampleRateHz = config.sampleRateHz,
            minHz = config.bandpassMinHz,
            maxHz = config.bandpassMaxHz,
        )
        // Rectification + low-pass turns harsh exhaust bursts into a pulse envelope.
        val envelopeCandidate = if (rawCandidate == null || rawCandidate.clarity < 0.78) {
            RpmPitchDetector.detectYin(
                samples = RpmPitchDetector.envelope(samples, config.sampleRateHz),
                sampleRateHz = config.sampleRateHz,
                minHz = config.bandpassMinHz,
                maxHz = config.bandpassMaxHz,
            )
        } else {
            null
        }
        val candidate = selectCandidate(rawCandidate, envelopeCandidate, config) ?: return null
        val peakHz = candidate.frequencyHz

        val rawRpm = peakHz * 60.0 / pulsesPerRevolution(config)
        if (!isConfirmedChange(rawRpm)) return holdLast()
        val rmsScore = ((rms - config.minRms) / 0.05).coerceIn(0.0, 1.0)
        val confidence = (
            0.75 * candidate.clarity +
                0.25 * rmsScore
            ).coerceIn(0.0, 1.0)

        if (confidence < MIN_ACCEPT_CONFIDENCE) return holdLast()
        val smoothed = medianFilter(rawRpm, config.medianWindow)

        val result = RpmAnalysisResult(
            rpm = smoothed,
            peakFrequencyHz = peakHz,
            confidence = confidence,
            signalRms = rms,
            held = false,
        )
        lastPublished = result
        return result
    }

    private fun holdLast(): RpmAnalysisResult? =
        lastPublished?.copy(held = true)

    fun analyzeWindow(samples: FloatArray, config: RpmAnalysisConfig, timestampMs: Long): RpmAnalysisResult? =
        analyze(samples, config)?.copy(timestampMs = timestampMs)

    const val MIN_ACCEPT_CONFIDENCE = 0.35

    private fun pulsesPerRevolution(config: RpmAnalysisConfig): Double =
        if (config.pulsesPerRevolution > 0.0) {
            config.pulsesPerRevolution
        } else {
            config.cylinderCount.coerceAtLeast(1).toDouble()
        }

    private fun selectCandidate(
        raw: RpmPitchCandidate?,
        envelope: RpmPitchCandidate?,
        config: RpmAnalysisConfig,
    ): RpmPitchCandidate? {
        val candidates = listOfNotNull(raw, envelope)
        if (candidates.isEmpty()) return null
        return candidates.maxByOrNull { candidate ->
            val rpm = candidate.frequencyHz * 60.0 / pulsesPerRevolution(config)
            val hintScore = config.hintRpm?.let { hint ->
                (1.0 - abs(rpm - hint) / (hint * config.hintTolerance).coerceAtLeast(1.0))
                    .coerceIn(0.0, 1.0)
            } ?: 0.0
            candidate.clarity + hintScore * 0.35
        }
    }

    /**
     * A false pitch lock commonly appears as a single ½×, 2×, or unrelated RPM
     * frame. Require an out-of-band value to repeat before it changes the live
     * value. A real throttle change is normally present in the next overlapping
     * frame, so this adds only one update of delay.
     */
    private fun isConfirmedChange(candidateRpm: Double): Boolean {
        // Do not publish the very first lock immediately. A false initial octave lock
        // would otherwise poison Min/Peak and make the whole graph misleading.
        if (recentRpms.isEmpty()) {
            return confirmPendingCandidate(
                candidateRpm = candidateRpm,
                requiredFrames = 2,
            )
        }

        val baseline = recentRpms.sorted()[recentRpms.size / 2]
        val difference = abs(candidateRpm - baseline)
        // In-band changes remain immediate. Larger changes must prove they persist.
        val allowedDifference = max(400.0, baseline * 0.12)
        if (difference <= allowedDifference) {
            pendingChangeRpm = null
            pendingChangeFrames = 0
            return true
        }

        val ratio = candidateRpm / baseline.coerceAtLeast(1.0)
        val octaveLike = abs(ratio - 2.0) < 0.22 || abs(ratio - 0.5) < 0.12
        // A fall is much more likely to be a missed pulse / wrong lock than a real
        // instantaneous engine deceleration, so it gets stronger hysteresis.
        val requiredFrames = when {
            octaveLike -> 8
            candidateRpm < baseline -> 5
            else -> 3
        }
        return confirmPendingCandidate(candidateRpm, requiredFrames)
    }

    private fun confirmPendingCandidate(candidateRpm: Double, requiredFrames: Int): Boolean {
        val pending = pendingChangeRpm
        val agreesWithPending = pending != null &&
            abs(candidateRpm - pending) <= max(300.0, pending * 0.10)
        if (agreesWithPending) {
            pendingChangeFrames++
        } else {
            pendingChangeRpm = candidateRpm
            pendingChangeFrames = 1
        }
        if (pendingChangeFrames < requiredFrames) return false
        pendingChangeRpm = null
        pendingChangeFrames = 0
        return true
    }

    private fun medianFilter(value: Double, window: Int): Double {
        recentRpms.addLast(value)
        while (recentRpms.size > window.coerceAtLeast(1)) {
            recentRpms.removeFirst()
        }
        val sorted = recentRpms.sorted()
        return sorted[sorted.size / 2]
    }


    fun synthesiseTone(frequencyHz: Double, sampleRate: Int, samples: Int, amplitude: Float = 0.5f): FloatArray {
        return FloatArray(samples) { i ->
            (amplitude * sin(2.0 * PI * frequencyHz * i / sampleRate)).toFloat()
        }
    }

    fun synthesisePulseTrain(
        frequencyHz: Double,
        sampleRate: Int,
        samples: Int,
        amplitude: Float = 0.45f,
        pulseWidthSamples: Int = 8,
    ): FloatArray {
        val period = (sampleRate / frequencyHz).roundToInt().coerceAtLeast(pulseWidthSamples * 2)
        return FloatArray(samples) { i ->
            val phase = i % period
            if (phase < pulseWidthSamples) {
                val env = 1f - phase.toFloat() / pulseWidthSamples
                amplitude * env
            } else {
                0f
            }
        }
    }

    fun synthesiseNoise(samples: Int, amplitude: Float = 0.002f, seed: Int = 42): FloatArray {
        var state = seed
        return FloatArray(samples) {
            state = state * 1103515245 + 12345
            val unit = ((state ushr 16) and 0x7fff) / 32768.0
            ((unit * 2.0 - 1.0) * amplitude).toFloat()
        }
    }

    fun confidenceLabel(confidence: Double): String = when {
        confidence >= 0.7 -> "good"
        confidence >= 0.45 -> "fair"
        else -> "poor"
    }

    fun dbFromMagnitude(magnitude: Double): Double =
        if (magnitude <= 0.0) -120.0 else 20.0 * ln(magnitude) / ln(10.0)
}

data class RpmLiveStats(
    val currentRpm: Double = 0.0,
    val minRpm: Double = Double.POSITIVE_INFINITY,
    val maxRpm: Double = 0.0,
    val peakHoldRpm: Double = 0.0,
    val confidence: Double = 0.0,
    val hasSignal: Boolean = false,
) {
    fun update(result: RpmAnalysisResult): RpmLiveStats {
        val rpm = result.rpm
        return copy(
            currentRpm = rpm,
            minRpm = if (minRpm.isInfinite()) rpm else min(minRpm, rpm),
            maxRpm = max(maxRpm, rpm),
            peakHoldRpm = max(peakHoldRpm, rpm),
            confidence = result.confidence,
            hasSignal = true,
        )
    }

    fun withoutSignal(): RpmLiveStats = copy(
        confidence = 0.0,
        hasSignal = false,
    )
}
