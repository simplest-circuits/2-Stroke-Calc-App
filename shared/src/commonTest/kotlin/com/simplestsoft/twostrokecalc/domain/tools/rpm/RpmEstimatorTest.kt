package com.simplestsoft.twostrokecalc.domain.tools.rpm

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RpmEstimatorTest {

    @Test
    fun detectsKnownToneAsRpm() {
        RpmEstimator.reset()
        // 100 Hz → 6000 rpm for 1 pulse/rev 2-stroke
        val samples = RpmEstimator.synthesiseTone(
            frequencyHz = 100.0,
            sampleRate = 44100,
            samples = 8192,
            amplitude = 0.4f,
        )
        val result = detectAfterInitialConfirmation(
            samples = samples,
            config = RpmAnalysisConfig(fftSize = 8192, pulsesPerRevolution = 1.0),
        )
        assertNotNull(result, "expected tone detection")
        assertTrue(
            result!!.rpm in 5500.0..6500.0,
            "rpm=${result.rpm} conf=${result.confidence} f=${result.peakFrequencyHz}",
        )
        assertTrue(
            result.confidence >= RpmEstimator.MIN_ACCEPT_CONFIDENCE,
            "confidence=${result.confidence}",
        )
    }

    @Test
    fun detectsIdlePulseTrain() {
        RpmEstimator.reset()
        // 30 Hz → 1800 rpm (typical scooter idle)
        val samples = RpmEstimator.synthesisePulseTrain(
            frequencyHz = 30.0,
            sampleRate = 44100,
            samples = 8192,
            amplitude = 0.5f,
        )
        val result = detectAfterInitialConfirmation(samples, RpmAnalysisConfig())
        assertNotNull(result, "expected idle detection")
        assertTrue(
            result!!.rpm in 1500.0..2100.0,
            "idle rpm=${result.rpm} conf=${result.confidence} f=${result.peakFrequencyHz}",
        )
    }

    @Test
    fun prefersFundamentalWhenHarmonicDominates() {
        RpmEstimator.reset()
        val sampleRate = 44100
        val n = 8192
        val samples = FloatArray(n) { i ->
            val fund = 0.22f * kotlin.math.sin(2.0 * kotlin.math.PI * 30.0 * i / sampleRate).toFloat()
            val harm = 0.45f * kotlin.math.sin(2.0 * kotlin.math.PI * 60.0 * i / sampleRate).toFloat()
            fund + harm
        }
        val result = detectAfterInitialConfirmation(samples, RpmAnalysisConfig())
        assertNotNull(result, "expected harmonic-aware detection")
        assertTrue(
            result!!.rpm in 1500.0..2200.0,
            "harmonic rpm=${result.rpm} f=${result.peakFrequencyHz}",
        )
    }

    @Test
    fun tracksMidRangeTone() {
        RpmEstimator.reset()
        // 150 Hz → 9000 rpm
        val samples = RpmEstimator.synthesiseTone(
            frequencyHz = 150.0,
            sampleRate = 44100,
            samples = 8192,
            amplitude = 0.35f,
        )
        val result = detectAfterInitialConfirmation(samples, RpmAnalysisConfig())
        assertNotNull(result)
        assertTrue(
            result!!.rpm in 8500.0..9500.0,
            "mid rpm=${result.rpm} f=${result.peakFrequencyHz}",
        )
    }

    @Test
    fun holdsLastRpmForSingleOutlierAfterStableLock() {
        RpmEstimator.reset()
        val config = RpmAnalysisConfig()
        val idle = RpmEstimator.synthesisePulseTrain(
            frequencyHz = 30.0,
            sampleRate = 44100,
            samples = 8192,
            amplitude = 0.5f,
        )
        repeat(3) { RpmEstimator.analyze(idle, config) }
        assertNotNull(RpmEstimator.analyze(idle, config))

        // A one-frame lock on 120 Hz would otherwise turn 1800 RPM into 7200 RPM.
        val outlier = RpmEstimator.synthesiseTone(
            frequencyHz = 120.0,
            sampleRate = 44100,
            samples = 8192,
            amplitude = 0.5f,
        )
        val held = RpmEstimator.analyze(outlier, config)
        assertNotNull(held, "expected previous lock to be held")
        assertTrue(held!!.held, "outlier must not replace the live value")
        assertTrue(
            held.rpm in 1500.0..2100.0,
            "held rpm=${held.rpm}",
        )
        assertNotNull(RpmEstimator.analyze(idle, config))
    }

    @Test
    fun silenceAfterLockDoesNotInventAValue() {
        RpmEstimator.reset()
        val config = RpmAnalysisConfig()
        val idle = RpmEstimator.synthesisePulseTrain(
            frequencyHz = 30.0,
            sampleRate = 44100,
            samples = 8192,
            amplitude = 0.5f,
        )
        detectAfterInitialConfirmation(idle, config)
        val silence = FloatArray(8192) { 0f }
        assertNull(RpmEstimator.analyze(silence, config))
    }

    @Test
    fun returnsNullForSilence() {
        RpmEstimator.reset()
        val silence = FloatArray(8192) { 0f }
        assertNull(RpmEstimator.analyze(silence, RpmAnalysisConfig()))
    }

    @Test
    fun returnsNullForQuietNoise() {
        RpmEstimator.reset()
        val noise = RpmEstimator.synthesiseNoise(samples = 8192, amplitude = 0.002f)
        assertNull(RpmEstimator.analyze(noise, RpmAnalysisConfig()))
    }

    private fun detectAfterInitialConfirmation(
        samples: FloatArray,
        config: RpmAnalysisConfig,
    ): RpmAnalysisResult? {
        RpmEstimator.analyze(samples, config)
        return RpmEstimator.analyze(samples, config)
    }
}
