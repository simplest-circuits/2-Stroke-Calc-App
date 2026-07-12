package com.simplestsoft.twostrokecalc.domain.portarea

enum class ExhaustPortType(val portCount: Int) {
    SINGLE(1),
    TWIN(2),
    TRIPLE(3),
}

enum class ExhaustPortShape {
    RECTANGULAR,
    ROUNDED_RECT,
    OVAL,
    TRAPEZOID,
    /** Booster / single window: base at OT, apex centred at UT. */
    TRIANGULAR,
    /** Twin ports: V-form with apex at the bridge-side bottom corner. */
    V_SHAPE,
}

enum class PortAreaAssessment {
    CRITICAL_LOW,
    SERIES,
    SPORTY,
    AGGRESSIVE,
}

data class TransferPortInput(
    val boreMm: Double,
    val strokeMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val channelCount: Int,
    val sideAngleDeg: Double?,
    val correctionFactor: Double,
    val durationDeg: Double,
)

/**
 * Exhaust port input. Orientation: **top** = toward cylinder head (OT side of window),
 * **bottom** = toward piston (UT side). Trapezoids and triangles are wider at the top.
 *
 * For [ExhaustPortType.TRIPLE], the TorqSoft-style layout is used automatically:
 * trapezoidal main port in the centre, flanking booster ports (triangular or trapezoidal).
 */
data class ExhaustPortInput(
    val boreMm: Double,
    val strokeMm: Double,
    val type: ExhaustPortType,
    val shape: ExhaustPortShape = ExhaustPortShape.RECTANGULAR,
    val portHeightMm: Double,
    /** Total bottom span (uniform layout) or unused when triple compound fields are set. */
    val totalSpanMm: Double,
    /** Total top span (uniform layout). Must be greater than [totalSpanMm] for trapezoids. */
    val topSpanMm: Double? = null,
    val bridgeWidthMm: Double,
    val cornerRadiusMm: Double? = null,
    /** OT corner fillets (head side); independent of [cornerRadiusMm] (UT). */
    val otCornerRadiusMm: Double? = null,
    val topEdgeRadiusMm: Double? = null,
    /** Centre main port bottom width (3-part compound). */
    val mainBottomSpanMm: Double? = null,
    /** Centre main port top width (3-part compound). */
    val mainTopSpanMm: Double? = null,
    /** Top width of each booster port (3-part compound, base at top). */
    val boosterTopSpanMm: Double? = null,
    /** Bottom width of each booster (3-part trapezoidal boosters, e.g. KTM 50/65). */
    val boosterBottomSpanMm: Double? = null,
    /** Per-side booster bottom width when shapes differ (3-part). Falls back to [boosterBottomSpanMm]. */
    val leftBoosterBottomSpanMm: Double? = null,
    val rightBoosterBottomSpanMm: Double? = null,
    /** Height of each booster window (3-part). Must be less than [portHeightMm]; OT edges align. */
    val boosterHeightMm: Double? = null,
    /** Booster shape for 3-part layout (both sides). Legacy fallback for per-side shapes. */
    val boosterShape: ExhaustPortShape? = null,
    val leftBoosterShape: ExhaustPortShape? = null,
    val rightBoosterShape: ExhaustPortShape? = null,
    /** Main port shape for 3-part layout (trapezoid or rounded). */
    val mainShape: ExhaustPortShape? = null,
    /** Booster corner radius (3-teilig). */
    val boosterCornerRadiusMm: Double? = null,
    val boosterOtCornerRadiusMm: Double? = null,
    /** Booster curved OT edge (3-teilig). */
    val boosterTopEdgeRadiusMm: Double? = null,
    /** Optional second bridge width (3-teilig, main↔right booster); defaults to [bridgeWidthMm]. */
    val bridgeWidth2Mm: Double? = null,
    val durationDeg: Double,
)

/** Single exhaust window in a multi-port layout (viewed from the port face). */
data class PortWindowGeometry(
    val shape: ExhaustPortShape,
    val bottomWidthMm: Double,
    val topWidthMm: Double,
    val heightMm: Double,
    val areaMm2: Double,
    val roleLabelKey: PortWindowRole = PortWindowRole.UNIFORM,
    /** Boosters share the OT edge with the main port and are shorter toward UT. */
    val topAlignedWithMain: Boolean = false,
    val radii: PortWindowRadii = PortWindowRadii(),
    /** For [ExhaustPortShape.V_SHAPE]: apex at bottom-right (toward next bridge) when true. */
    val vShapeApexAtEnd: Boolean = true,
)

/** Bridge width after window [index]; empty for the last window. */
fun PortGeometryLayout.bridgeAfterWindow(index: Int): Double =
    bridgeWidthsMm.getOrElse(index) { 0.0 }

enum class PortWindowRole {
    UNIFORM,
    MAIN,
    BOOSTER,
}

data class PortGeometryLayout(
    val type: ExhaustPortType,
    val windows: List<PortWindowGeometry>,
    /** Bridge after each window except the last; length = windows.size - 1. */
    val bridgeWidthsMm: List<Double> = emptyList(),
    /** First bridge width for legacy callers. */
    val bridgeWidthMm: Double = bridgeWidthsMm.firstOrNull() ?: 0.0,
    /** Full height of the main / reference port (OT to UT). */
    val portHeightMm: Double,
    val totalBottomSpanMm: Double,
    val totalTopSpanMm: Double,
    val totalAreaMm2: Double,
    val boosterHeightMm: Double? = null,
) {
    val shape: ExhaustPortShape?
        get() = windows.singleOrNull()?.shape

    val singlePortWidthMm: Double
        get() = windows.firstOrNull()?.bottomWidthMm ?: 0.0

    val singlePortTopWidthMm: Double?
        get() = windows.firstOrNull()?.topWidthMm

    val totalSpanMm: Double
        get() = totalBottomSpanMm

    val totalTopSpanMmLegacy: Double?
        get() = totalTopSpanMm.takeIf { it != totalBottomSpanMm }

    val singlePortAreaMm2: Double
        get() = windows.firstOrNull()?.areaMm2 ?: 0.0
}

data class PortAreaResult(
    val areaMm2: Double,
    val timeAreaMm2Deg: Double,
    val boreAreaMm2: Double,
    val areaToBoreRatio: Double,
    val timeAreaPerCc: Double,
    val displacementCc: Double,
    val assessment: PortAreaAssessment,
)

/** Width metrics from the port face (Breite bei UT, Sehnenmaß oben). */
data class ExhaustChordMetrics(
    val bottomWidthUtMm: Double,
    val topChordMm: Double,
    val chordPercentOfBore: Double,
)

enum class ExhaustChordRating {
    CONSERVATIVE,
    EVERYDAY,
    SERIES_SPORT,
    TUNER,
}
