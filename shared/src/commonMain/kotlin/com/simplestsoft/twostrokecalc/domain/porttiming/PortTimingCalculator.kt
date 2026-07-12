package com.simplestsoft.twostrokecalc.domain.porttiming

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PortTimingCalculator {

    fun calculate(input: PortTimingInput): PortTimingResult {
        val crankRadius = input.strokeMm / 2.0
        val rodLength = input.connectingRodMm

        val exhaust = calcSymmetricChannel(
            distanceMm = input.exhaustPortMm,
            pistonDeckMm = input.pistonDeckMm,
            strokeMm = input.strokeMm,
            crankRadius = crankRadius,
            rodLength = rodLength,
        )
        val transfer = calcSymmetricChannel(
            distanceMm = input.transferPortMm,
            pistonDeckMm = input.pistonDeckMm,
            strokeMm = input.strokeMm,
            crankRadius = crankRadius,
            rodLength = rodLength,
        )

        val blowdown = if (exhaust != null && transfer != null) {
            exhaust.openBeforeBdc - transfer.openBeforeBdc
        } else {
            null
        }

        val intake = if (input.intakeSystem == IntakeSystem.ROTARY_VALVE) {
            calcIntake(
                openDistanceMm = input.intakeOpenMm,
                closeDistanceMm = input.intakeCloseMm,
                pistonDeckMm = input.pistonDeckMm,
                strokeMm = input.strokeMm,
                crankRadius = crankRadius,
                rodLength = rodLength,
            )
        } else {
            null
        }

        return PortTimingResult(
            exhaust = exhaust,
            transfer = transfer,
            blowdown = blowdown,
            intake = intake,
        )
    }

    fun pistonPosition(angleDeg: Double, crankRadius: Double, rodLength: Double): Double {
        val angleRad = angleDeg * PI / 180.0
        val sinRatio = crankRadius * sin(angleRad) / rodLength
        return crankRadius * (1.0 - cos(angleRad)) +
            rodLength * (1.0 - sqrt(1.0 - sinRatio * sinRatio))
    }

    fun angleForPosition(position: Double, crankRadius: Double, rodLength: Double): Double {
        var low = 0.0
        var high = 180.0
        repeat(60) {
            val mid = (low + high) / 2.0
            if (pistonPosition(mid, crankRadius, rodLength) < position) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2.0
    }

    private fun calcSymmetricChannel(
        distanceMm: Double,
        pistonDeckMm: Double,
        strokeMm: Double,
        crankRadius: Double,
        rodLength: Double,
    ): ChannelTiming? {
        val position = distanceMm + pistonDeckMm
        if (position <= 0.0 || position >= strokeMm) return null

        val angle = angleForPosition(position, crankRadius, rodLength)
        val openBeforeBdc = 180.0 - angle
        return ChannelTiming(
            openBeforeBdc = openBeforeBdc,
            duration = 2.0 * openBeforeBdc,
        )
    }

    private fun calcIntake(
        openDistanceMm: Double,
        closeDistanceMm: Double,
        pistonDeckMm: Double,
        strokeMm: Double,
        crankRadius: Double,
        rodLength: Double,
    ): IntakeTiming? {
        val openPosition = (openDistanceMm + pistonDeckMm).coerceIn(0.001, strokeMm - 0.001)
        val closePosition = (closeDistanceMm + pistonDeckMm).coerceIn(0.001, strokeMm - 0.001)

        val openBeforeTdc = angleForPosition(openPosition, crankRadius, rodLength)
        val closeAfterTdc = angleForPosition(closePosition, crankRadius, rodLength)

        return IntakeTiming(
            openBeforeTdc = openBeforeTdc,
            closeAfterTdc = closeAfterTdc,
            duration = openBeforeTdc + closeAfterTdc,
        )
    }
}
