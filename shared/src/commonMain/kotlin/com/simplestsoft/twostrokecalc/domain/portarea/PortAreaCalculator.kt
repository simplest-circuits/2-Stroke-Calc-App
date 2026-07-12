package com.simplestsoft.twostrokecalc.domain.portarea

import kotlin.math.PI
import kotlin.math.cos

object PortAreaCalculator {

    fun displacementCc(boreMm: Double, strokeMm: Double): Double =
        PI * boreMm * boreMm / 4.0 * strokeMm / 1000.0

    fun boreAreaMm2(boreMm: Double): Double =
        PI * boreMm * boreMm / 4.0

    /**
     * [topChordMm] is the Sehnenmaß (widest chord at the top),
     * [bottomWidthUtMm] is the total width at UT (BDC side).
     */
    fun exhaustChordMetrics(boreMm: Double, layout: PortGeometryLayout): ExhaustChordMetrics? {
        if (boreMm <= 0.0) return null
        return ExhaustChordMetrics(
            bottomWidthUtMm = layout.totalBottomSpanMm,
            topChordMm = layout.totalTopSpanMm,
            chordPercentOfBore = layout.totalTopSpanMm / boreMm * 100.0,
        )
    }

    /** Guideline: ~65% Gussringe, ~70% Verchromte, Serien-125er ~89%, Tuner bis ~95%. */
    fun rateChordPercent(chordPercent: Double): ExhaustChordRating = when {
        chordPercent <= 65.0 -> ExhaustChordRating.CONSERVATIVE
        chordPercent <= 70.0 -> ExhaustChordRating.EVERYDAY
        chordPercent <= 90.0 -> ExhaustChordRating.SERIES_SPORT
        else -> ExhaustChordRating.TUNER
    }

    /** Wiki example for 54 mm bore: 10 mm per window at UT, 48 mm Sehnenmaß total (89 %). */
    fun wikiTwinDefaultsForBore(boreMm: Double, bridgeWidthMm: Double = 8.0): Pair<Double, Double> {
        val scale = if (boreMm > 0.0) boreMm / 54.0 else 1.0
        val bottomPerPortUt = 10.0 * scale
        val chord = 48.0 * scale
        val totalBottomUt = bottomPerPortUt * 2.0 + bridgeWidthMm
        return totalBottomUt to chord
    }

    /** Ensures layout totals match the sum of per-window widths plus bridges (diagram source of truth). */
    fun layoutHorizontalSpansConsistent(layout: PortGeometryLayout): Boolean {
        if (layout.windows.isEmpty()) return false
        val bridges = layout.bridgeWidthsMm.sum()
        val sumTop = layout.windows.sumOf { it.topWidthMm } + bridges
        val sumBottom = layout.windows.sumOf { it.bottomWidthMm } + bridges
        return kotlin.math.abs(sumTop - layout.totalTopSpanMm) < 0.001 &&
            kotlin.math.abs(sumBottom - layout.totalBottomSpanMm) < 0.001
    }

    private fun bridgeList(count: Int, first: Double, second: Double? = null): List<Double> {
        if (count <= 0) return emptyList()
        return List(count) { index ->
            if (index == 1 && second != null) second else first
        }
    }

    fun effectiveAreaFactor(sideAngleDeg: Double?, correctionFactor: Double): Double {
        if (sideAngleDeg != null) {
            if (sideAngleDeg < 0.0 || sideAngleDeg >= 90.0) return 0.0
            return cos(Math.toRadians(sideAngleDeg))
        }
        return correctionFactor.coerceAtLeast(0.0)
    }

    fun singleChannelAreaMm2(widthMm: Double, heightMm: Double, effectiveFactor: Double): Double =
        widthMm * heightMm * effectiveFactor

    fun calculateTransfer(input: TransferPortInput): PortAreaResult? {
        if (input.boreMm <= 0.0 || input.strokeMm <= 0.0) return null
        if (input.widthMm <= 0.0 || input.heightMm <= 0.0 || input.channelCount <= 0) return null
        if (input.durationDeg <= 0.0) return null

        val factor = effectiveAreaFactor(input.sideAngleDeg, input.correctionFactor)
        if (factor <= 0.0) return null

        val area = singleChannelAreaMm2(input.widthMm, input.heightMm, factor) * input.channelCount
        return buildResult(
            areaMm2 = area,
            durationDeg = input.durationDeg,
            boreMm = input.boreMm,
            strokeMm = input.strokeMm,
            isTransfer = true,
        )
    }

    fun calculateExhaustLayout(input: ExhaustPortInput): PortGeometryLayout? {
        if (input.portHeightMm <= 0.0 || input.bridgeWidthMm < 0.0) return null

        return when (input.type) {
            ExhaustPortType.TRIPLE -> buildMainBoostersLayout(input)
            else -> buildUniformLayout(input)
        }
    }

    /**
     * TorqSoft-style 3-part layout: shorter boosters flanking a taller trapezoidal main port.
     * Booster OT edges align with the main port top; boosters open later and close earlier.
     */
    private fun buildMainBoostersLayout(input: ExhaustPortInput): PortGeometryLayout? {
        val mainBottom = input.mainBottomSpanMm ?: return null
        val mainTop = input.mainTopSpanMm ?: return null
        val boosterTop = input.boosterTopSpanMm ?: return null
        val mainHeight = input.portHeightMm
        val boosterHeight = input.boosterHeightMm ?: return null
        if (mainBottom <= 0.0 || mainTop <= 0.0 || boosterTop <= 0.0) return null
        if (mainTop <= mainBottom) return null
        if (boosterHeight <= 0.0 || boosterHeight >= mainHeight) return null

        val leftBooster = buildSideBoosterWindow(
            input = input,
            side = BoosterSide.LEFT,
            boosterTop = boosterTop,
            boosterHeight = boosterHeight,
        ) ?: return null
        val rightBooster = buildSideBoosterWindow(
            input = input,
            side = BoosterSide.RIGHT,
            boosterTop = boosterTop,
            boosterHeight = boosterHeight,
        ) ?: return null

        val bridge = input.bridgeWidthMm
        val bridge2 = input.bridgeWidth2Mm?.takeIf { it >= 0.0 } ?: bridge
        val bridges = bridgeList(2, bridge, bridge2)

        val mainShape = input.mainShape?.takeIf {
            it == ExhaustPortShape.TRAPEZOID || it == ExhaustPortShape.ROUNDED_RECT
        } ?: ExhaustPortShape.TRAPEZOID
        val mainRadii = PortWindowRadii(
            utCornerRadiusMm = input.cornerRadiusMm ?: 0.0,
            otCornerRadiusMm = input.otCornerRadiusMm ?: 0.0,
            topEdgeRadiusMm = input.topEdgeRadiusMm ?: 0.0,
        ).coerced(mainBottom, mainTop, mainHeight)

        val mainArea = portWindowAreaMm2(
            shape = mainShape,
            bottomWidthMm = mainBottom,
            topWidthMm = mainTop,
            heightMm = mainHeight,
            radii = mainRadii,
        ) ?: return null

        val mainWindow = PortWindowGeometry(
            shape = mainShape,
            bottomWidthMm = mainBottom,
            topWidthMm = mainTop,
            heightMm = mainHeight,
            areaMm2 = mainArea,
            roleLabelKey = PortWindowRole.MAIN,
            radii = mainRadii,
        )

        val windows = listOf(leftBooster, mainWindow, rightBooster)
        val totalBottom = leftBooster.bottomWidthMm + bridge + mainBottom + bridge2 + rightBooster.bottomWidthMm
        val totalTop = boosterTop + bridge + mainTop + bridge2 + boosterTop
        val totalArea = leftBooster.areaMm2 + mainArea + rightBooster.areaMm2

        return PortGeometryLayout(
            type = ExhaustPortType.TRIPLE,
            windows = windows,
            bridgeWidthsMm = bridges,
            portHeightMm = mainHeight,
            totalBottomSpanMm = totalBottom,
            totalTopSpanMm = totalTop,
            totalAreaMm2 = totalArea,
            boosterHeightMm = boosterHeight,
        )
    }

    private enum class BoosterSide { LEFT, RIGHT }

    private fun buildSideBoosterWindow(
        input: ExhaustPortInput,
        side: BoosterSide,
        boosterTop: Double,
        boosterHeight: Double,
    ): PortWindowGeometry? {
        val shape = when (side) {
            BoosterSide.LEFT -> input.leftBoosterShape ?: input.boosterShape ?: ExhaustPortShape.TRIANGULAR
            BoosterSide.RIGHT -> input.rightBoosterShape ?: input.boosterShape ?: ExhaustPortShape.TRIANGULAR
        }
        if (shape != ExhaustPortShape.TRIANGULAR && shape != ExhaustPortShape.TRAPEZOID) {
            return null
        }

        val boosterBottom = when (shape) {
            ExhaustPortShape.TRIANGULAR -> 0.0
            ExhaustPortShape.TRAPEZOID -> {
                val bottom = when (side) {
                    BoosterSide.LEFT -> input.leftBoosterBottomSpanMm ?: input.boosterBottomSpanMm
                    BoosterSide.RIGHT -> input.rightBoosterBottomSpanMm ?: input.boosterBottomSpanMm
                } ?: return null
                if (bottom <= 0.0 || boosterTop <= bottom) return null
                bottom
            }
            else -> return null
        }

        val boosterRadii = PortWindowRadii(
            utCornerRadiusMm = input.boosterCornerRadiusMm ?: 0.0,
            otCornerRadiusMm = input.boosterOtCornerRadiusMm ?: 0.0,
            topEdgeRadiusMm = input.boosterTopEdgeRadiusMm ?: 0.0,
        ).coerced(boosterBottom, boosterTop, boosterHeight)

        val effectiveShape = when (shape) {
            ExhaustPortShape.ROUNDED_RECT -> ExhaustPortShape.TRAPEZOID
            else -> shape
        }

        val boosterArea = portWindowAreaMm2(
            shape = effectiveShape,
            bottomWidthMm = boosterBottom,
            topWidthMm = boosterTop,
            heightMm = boosterHeight,
            radii = boosterRadii,
        ) ?: return null

        return PortWindowGeometry(
            shape = effectiveShape,
            bottomWidthMm = boosterBottom,
            topWidthMm = boosterTop,
            heightMm = boosterHeight,
            areaMm2 = boosterArea,
            roleLabelKey = PortWindowRole.BOOSTER,
            topAlignedWithMain = true,
            radii = boosterRadii,
        )
    }

    private fun buildUniformLayout(input: ExhaustPortInput): PortGeometryLayout? {
        val usesTopSpanOnly = input.shape == ExhaustPortShape.TRIANGULAR ||
            input.shape == ExhaustPortShape.V_SHAPE
        if (!usesTopSpanOnly && input.totalSpanMm <= 0.0) return null
        if (usesTopSpanOnly && input.topSpanMm == null) return null

        val bridgeCount = input.type.portCount - 1
        val bridgesTotal = bridgeCount * input.bridgeWidthMm

        val bottomPerPort: Double
        val topPerPort: Double
        val totalBottomSpan: Double
        val totalTopSpan: Double

        when (input.shape) {
            ExhaustPortShape.TRIANGULAR -> {
                val topSpan = input.topSpanMm!!
                if (topSpan <= 0.0 || bridgesTotal >= topSpan) return null
                topPerPort = (topSpan - bridgesTotal) / input.type.portCount
                if (topPerPort <= 0.0) return null
                bottomPerPort = 0.0
                totalBottomSpan = topSpan * 0.1
                totalTopSpan = topSpan
            }
            ExhaustPortShape.V_SHAPE -> {
                if (input.type != ExhaustPortType.TWIN) return null
                val topSpan = input.topSpanMm!!
                if (topSpan <= 0.0 || bridgesTotal >= topSpan) return null
                topPerPort = (topSpan - bridgesTotal) / input.type.portCount
                if (topPerPort <= 0.0) return null
                bottomPerPort = 0.0
                totalBottomSpan = bridgesTotal
                totalTopSpan = topSpan
            }
            ExhaustPortShape.TRAPEZOID -> {
                if (input.totalSpanMm <= 0.0) return null
                if (bridgesTotal >= input.totalSpanMm) return null
                val topSpan = input.topSpanMm ?: return null
                if (topSpan <= input.totalSpanMm || bridgesTotal >= topSpan) return null
                bottomPerPort = (input.totalSpanMm - bridgesTotal) / input.type.portCount
                topPerPort = (topSpan - bridgesTotal) / input.type.portCount
                if (bottomPerPort <= 0.0 || topPerPort <= bottomPerPort) return null
                totalBottomSpan = input.totalSpanMm
                totalTopSpan = topSpan
            }
            else -> {
                if (input.totalSpanMm <= 0.0) return null
                if (bridgesTotal >= input.totalSpanMm) return null
                bottomPerPort = (input.totalSpanMm - bridgesTotal) / input.type.portCount
                if (bottomPerPort <= 0.0) return null
                topPerPort = bottomPerPort
                totalBottomSpan = input.totalSpanMm
                totalTopSpan = input.totalSpanMm
            }
        }

        val uniformRadii = PortWindowRadii(
            utCornerRadiusMm = input.cornerRadiusMm ?: 0.0,
            otCornerRadiusMm = input.otCornerRadiusMm ?: 0.0,
            topEdgeRadiusMm = input.topEdgeRadiusMm ?: 0.0,
        ).coerced(bottomPerPort, topPerPort, input.portHeightMm)

        val effectiveShape = when (input.shape) {
            ExhaustPortShape.ROUNDED_RECT -> ExhaustPortShape.ROUNDED_RECT
            else -> input.shape
        }

        val windowArea = portWindowAreaMm2(
            shape = effectiveShape,
            bottomWidthMm = bottomPerPort,
            topWidthMm = topPerPort,
            heightMm = input.portHeightMm,
            radii = uniformRadii,
        ) ?: return null

        val window = PortWindowGeometry(
            shape = effectiveShape,
            bottomWidthMm = bottomPerPort,
            topWidthMm = topPerPort,
            heightMm = input.portHeightMm,
            areaMm2 = windowArea,
            radii = uniformRadii,
        )
        val windows = if (effectiveShape == ExhaustPortShape.V_SHAPE) {
            List(input.type.portCount) { index ->
                window.copy(vShapeApexAtEnd = index < input.type.portCount - 1)
            }
        } else {
            List(input.type.portCount) { window }
        }
        val bridges = bridgeList(bridgeCount, input.bridgeWidthMm)

        return PortGeometryLayout(
            type = input.type,
            windows = windows,
            bridgeWidthsMm = bridges,
            portHeightMm = input.portHeightMm,
            totalBottomSpanMm = totalBottomSpan,
            totalTopSpanMm = totalTopSpan,
            totalAreaMm2 = windowArea * input.type.portCount,
        )
    }

    /**
     * Area of one port window. [bottomWidthMm] is the width at the UT edge, [topWidthMm] at the OT edge.
     * Triangles use a top base and a bottom apex (top wider than bottom).
     */
    fun portWindowAreaMm2(
        shape: ExhaustPortShape,
        bottomWidthMm: Double,
        topWidthMm: Double,
        heightMm: Double,
        radii: PortWindowRadii = PortWindowRadii(),
        cornerRadiusMm: Double? = null,
    ): Double? {
        if (topWidthMm <= 0.0 || heightMm <= 0.0) return null
        if (bottomWidthMm < 0.0) return null

        val merged = radii.copy(
            utCornerRadiusMm = radii.utCornerRadiusMm.takeIf { it > 0.0 }
                ?: cornerRadiusMm ?: 0.0,
        ).coerced(bottomWidthMm, topWidthMm, heightMm)

        val base = when (shape) {
            ExhaustPortShape.RECTANGULAR -> {
                if (bottomWidthMm <= 0.0) return null
                bottomWidthMm * heightMm
            }

            ExhaustPortShape.OVAL -> {
                if (bottomWidthMm <= 0.0) return null
                PI * bottomWidthMm * heightMm / 4.0
            }

            ExhaustPortShape.ROUNDED_RECT -> {
                if (bottomWidthMm <= 0.0) return null
                val radius = merged.utCornerRadiusMm.takeIf { it > 0.0 } ?: return null
                bottomWidthMm * heightMm - (4.0 - PI) * radius * radius
            }

            ExhaustPortShape.TRAPEZOID -> {
                if (bottomWidthMm <= 0.0 || topWidthMm <= bottomWidthMm) return null
                (topWidthMm + bottomWidthMm) / 2.0 * heightMm
            }

            ExhaustPortShape.TRIANGULAR,
            ExhaustPortShape.V_SHAPE,
            -> {
                topWidthMm * heightMm / 2.0
            }
        }

        return base + topEdgeAreaCorrection(topWidthMm, merged.topEdgeRadiusMm)
    }

    /** Approximate area added by a convex OT edge (bulge toward the head). */
    fun topEdgeAreaCorrection(topWidthMm: Double, topEdgeRadiusMm: Double): Double {
        if (topEdgeRadiusMm <= 0.0) return 0.0
        return (2.0 / 3.0) * topEdgeRadiusMm * topWidthMm
    }

    /** @deprecated Use [portWindowAreaMm2] with explicit top/bottom widths. */
    fun singlePortShapeAreaMm2(
        shape: ExhaustPortShape,
        widthMm: Double,
        heightMm: Double,
        topWidthMm: Double? = null,
        cornerRadiusMm: Double? = null,
    ): Double? = portWindowAreaMm2(
        shape = shape,
        bottomWidthMm = widthMm,
        topWidthMm = topWidthMm ?: widthMm,
        heightMm = heightMm,
        cornerRadiusMm = cornerRadiusMm,
    )

    fun calculateExhaust(input: ExhaustPortInput): PortAreaResult? {
        if (input.boreMm <= 0.0 || input.strokeMm <= 0.0) return null
        if (input.durationDeg <= 0.0) return null

        val layout = calculateExhaustLayout(input) ?: return null
        return buildResult(
            areaMm2 = layout.totalAreaMm2,
            durationDeg = input.durationDeg,
            boreMm = input.boreMm,
            strokeMm = input.strokeMm,
            isTransfer = false,
        )
    }

    fun assessTransfer(areaToBoreRatio: Double, timeAreaPerCc: Double): PortAreaAssessment {
        val ratioScore = when {
            areaToBoreRatio < 0.50 -> 0
            areaToBoreRatio < 0.70 -> 1
            areaToBoreRatio < 0.95 -> 2
            else -> 3
        }
        val taScore = when {
            timeAreaPerCc < 450.0 -> 0
            timeAreaPerCc < 650.0 -> 1
            timeAreaPerCc < 900.0 -> 2
            else -> 3
        }
        return scoreToAssessment(minOf(ratioScore, taScore))
    }

    fun assessExhaust(areaToBoreRatio: Double, timeAreaPerCc: Double): PortAreaAssessment {
        val ratioScore = when {
            areaToBoreRatio < 0.35 -> 0
            areaToBoreRatio < 0.50 -> 1
            areaToBoreRatio < 0.70 -> 2
            else -> 3
        }
        val taScore = when {
            timeAreaPerCc < 350.0 -> 0
            timeAreaPerCc < 500.0 -> 1
            timeAreaPerCc < 700.0 -> 2
            else -> 3
        }
        return scoreToAssessment(minOf(ratioScore, taScore))
    }

    private fun buildResult(
        areaMm2: Double,
        durationDeg: Double,
        boreMm: Double,
        strokeMm: Double,
        isTransfer: Boolean,
    ): PortAreaResult {
        val boreArea = boreAreaMm2(boreMm)
        val displacement = displacementCc(boreMm, strokeMm)
        val timeArea = areaMm2 * durationDeg
        val ratio = areaMm2 / boreArea
        val taPerCc = if (displacement > 0.0) timeArea / displacement else 0.0
        val assessment = if (isTransfer) {
            assessTransfer(ratio, taPerCc)
        } else {
            assessExhaust(ratio, taPerCc)
        }
        return PortAreaResult(
            areaMm2 = areaMm2,
            timeAreaMm2Deg = timeArea,
            boreAreaMm2 = boreArea,
            areaToBoreRatio = ratio,
            timeAreaPerCc = taPerCc,
            displacementCc = displacement,
            assessment = assessment,
        )
    }

    private fun scoreToAssessment(score: Int): PortAreaAssessment = when (score) {
        0 -> PortAreaAssessment.CRITICAL_LOW
        1 -> PortAreaAssessment.SERIES
        2 -> PortAreaAssessment.SPORTY
        else -> PortAreaAssessment.AGGRESSIVE
    }
}
