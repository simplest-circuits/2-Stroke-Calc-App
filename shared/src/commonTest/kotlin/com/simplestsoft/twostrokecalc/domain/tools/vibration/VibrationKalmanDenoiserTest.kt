package com.simplestsoft.twostrokecalc.domain.tools.vibration

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class VibrationKalmanDenoiserTest {

    @Test
    fun quietNoiseCollapsesToNearZeroAfterWarmup() {
        val denoiser = VibrationKalmanDenoiser(
            processNoise = 1e-5,
            measurementNoise = 0.04,
            gateSigma = 2.5,
        )
        // Constant bias + tiny noise ≈ phone at rest
        var seed = 1
        fun noise(): Float {
            seed = seed * 1103515245 + 12345
            return (((seed ushr 16) and 0x7fff) / 32768f - 0.5f) * 0.01f
        }
        val residuals = (0 until 120).map { i ->
            denoiser.push(
                VibrationSample(
                    timestampMs = i * 10L,
                    x = 0.02f + noise(),
                    y = -0.01f + noise(),
                    z = 0.015f + noise(),
                ),
            ).magnitude
        }
        val afterWarmup = residuals.drop(VibrationKalmanDenoiser.DEFAULT_WARMUP_SAMPLES)
        val meanMag = afterWarmup.average()
        assertTrue(meanMag < 0.01, "meanMag=$meanMag")
        assertTrue(afterWarmup.count { it > 0.0 } < afterWarmup.size / 5)
    }

    @Test
    fun realOscillationPassesGate() {
        val denoiser = VibrationKalmanDenoiser()
        // Warm up on quiet
        repeat(50) { i ->
            denoiser.push(VibrationSample(i * 10L, 0.01f, 0f, 0f))
        }
        val vibrating = (0 until 80).map { i ->
            val t = i / 100.0
            val a = (1.2 * sin(2.0 * PI * 25.0 * t)).toFloat()
            denoiser.push(VibrationSample((50 + i) * 10L, a, 0f, 0f)).magnitude
        }
        assertTrue(vibrating.count { it > 0.2 } > 20, "expected vibration peaks")
    }
}
