package com.simplestsoft.twostrokecalc.domain.portarea

/**
 * Suggests exhaust geometry that maximizes time-area within Sehnenmaß limits.
 */
object ExhaustPortOptimizer {

    enum class RingProfile(val maxChordPercent: Double) {
        CAST(65.0),
        CHROME(70.0),
        SERIES_SPORT(89.0),
        TUNER(95.0),
    }

    data class Request(
        val boreMm: Double,
        val strokeMm: Double,
        val durationDeg: Double,
        val portHeightMm: Double,
        val type: ExhaustPortType,
        val ringProfile: RingProfile = RingProfile.SERIES_SPORT,
        val bridgeWidthMm: Double = 8.0,
        val bridgeWidth2Mm: Double? = null,
        /** Curved OT edge for ring durability (mm sagitta). 0 = straight (max pressure wave). */
        val topEdgeRadiusMm: Double = 3.0,
        val cornerRadiusMm: Double = 0.0,
    )

    data class Result(
        val input: ExhaustPortInput,
        val layout: PortGeometryLayout,
        val portResult: PortAreaResult,
        val chordMetrics: ExhaustChordMetrics,
        val ringProfile: RingProfile,
    )

    fun optimize(request: Request): Result? {
        if (request.boreMm <= 0.0 || request.strokeMm <= 0.0) return null
        if (request.portHeightMm <= 0.0 || request.durationDeg <= 0.0) return null
        if (request.bridgeWidthMm < 0.0) return null

        val input = when (request.type) {
            ExhaustPortType.SINGLE -> buildSingle(request)
            ExhaustPortType.TWIN -> buildTwin(request)
            ExhaustPortType.TRIPLE -> buildTriple(request)
        } ?: return null

        val layout = PortAreaCalculator.calculateExhaustLayout(input) ?: return null
        val portResult = PortAreaCalculator.calculateExhaust(input) ?: return null
        val chord = PortAreaCalculator.exhaustChordMetrics(request.boreMm, layout) ?: return null

        return Result(
            input = input,
            layout = layout,
            portResult = portResult,
            chordMetrics = chord,
            ringProfile = request.ringProfile,
        )
    }

    private fun buildSingle(request: Request): ExhaustPortInput? {
        val chord = request.boreMm * request.ringProfile.maxChordPercent / 100.0
        val bottomUt = wikiBottomPerPortUt(request.boreMm)
        return ExhaustPortInput(
            boreMm = request.boreMm,
            strokeMm = request.strokeMm,
            type = ExhaustPortType.SINGLE,
            shape = ExhaustPortShape.TRAPEZOID,
            portHeightMm = request.portHeightMm,
            totalSpanMm = bottomUt,
            topSpanMm = chord,
            bridgeWidthMm = 0.0,
            cornerRadiusMm = request.cornerRadiusMm.takeIf { it > 0.0 },
            topEdgeRadiusMm = request.topEdgeRadiusMm.takeIf { it > 0.0 },
            durationDeg = request.durationDeg,
        ).takeIf { PortAreaCalculator.calculateExhaustLayout(it) != null }
    }

    private fun buildTwin(request: Request): ExhaustPortInput? {
        val chord = request.boreMm * request.ringProfile.maxChordPercent / 100.0
        val bottomPerPort = wikiBottomPerPortUt(request.boreMm)
        val totalBottom = bottomPerPort * 2.0 + request.bridgeWidthMm
        return ExhaustPortInput(
            boreMm = request.boreMm,
            strokeMm = request.strokeMm,
            type = ExhaustPortType.TWIN,
            shape = ExhaustPortShape.TRAPEZOID,
            portHeightMm = request.portHeightMm,
            totalSpanMm = totalBottom,
            topSpanMm = chord,
            bridgeWidthMm = request.bridgeWidthMm,
            cornerRadiusMm = request.cornerRadiusMm.takeIf { it > 0.0 },
            topEdgeRadiusMm = request.topEdgeRadiusMm.takeIf { it > 0.0 },
            durationDeg = request.durationDeg,
        ).takeIf { PortAreaCalculator.calculateExhaustLayout(it) != null }
    }

    private fun buildTriple(request: Request): ExhaustPortInput? {
        val chord = request.boreMm * request.ringProfile.maxChordPercent / 100.0
        val bridge = request.bridgeWidthMm
        val bridge2 = request.bridgeWidth2Mm ?: bridge

        val boosterTopRatio = when (request.ringProfile) {
            RingProfile.CAST -> 0.18
            RingProfile.CHROME -> 0.18
            RingProfile.SERIES_SPORT -> 0.21
            RingProfile.TUNER -> 0.24
        }
        val boosterTop = ((chord - bridge - bridge2) * boosterTopRatio)
            .coerceAtLeast(wikiBottomPerPortUt(request.boreMm))
        val mainTop = chord - bridge - bridge2 - boosterTop * 2.0
        if (mainTop <= 0.0) return null

        val mainBottom = mainTop * 0.55
        val boosterBottom = boosterTop * 0.35
        val boosterHeight = (request.portHeightMm * 0.5)
            .coerceAtLeast(6.0)
            .coerceAtMost(request.portHeightMm - 2.0)

        return ExhaustPortInput(
            boreMm = request.boreMm,
            strokeMm = request.strokeMm,
            type = ExhaustPortType.TRIPLE,
            portHeightMm = request.portHeightMm,
            totalSpanMm = 0.0,
            bridgeWidthMm = bridge,
            bridgeWidth2Mm = bridge2.takeIf { it != bridge },
            mainBottomSpanMm = mainBottom,
            mainTopSpanMm = mainTop,
            boosterTopSpanMm = boosterTop,
            boosterBottomSpanMm = boosterBottom,
            leftBoosterBottomSpanMm = boosterBottom,
            rightBoosterBottomSpanMm = boosterBottom,
            boosterHeightMm = boosterHeight,
            leftBoosterShape = ExhaustPortShape.TRAPEZOID,
            rightBoosterShape = ExhaustPortShape.TRAPEZOID,
            mainShape = ExhaustPortShape.TRAPEZOID,
            cornerRadiusMm = request.cornerRadiusMm.takeIf { it > 0.0 },
            topEdgeRadiusMm = request.topEdgeRadiusMm.takeIf { it > 0.0 },
            boosterTopEdgeRadiusMm = (request.topEdgeRadiusMm * 0.6).takeIf { it > 0.0 },
            durationDeg = request.durationDeg,
        ).takeIf { PortAreaCalculator.calculateExhaustLayout(it) != null }
    }

    /** 125 cc reference: 10 mm per window at UT for 54 mm bore. */
    fun wikiBottomPerPortUt(boreMm: Double): Double =
        if (boreMm > 0.0) 10.0 * (boreMm / 54.0) else 10.0
}
