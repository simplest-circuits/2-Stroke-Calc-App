package com.simplestsoft.twostrokecalc.domain.portarea

/**
 * Radii for one exhaust window (UT-Ecken, OT-Ecken, gebogene Oberkante).
 *
 * [utCornerRadiusMm] – fillets at the UT (BDC / piston) corners.
 * [otCornerRadiusMm] – fillets at the OT (head) corners where slants meet the top.
 * [topEdgeRadiusMm] – sagitta of the convex curved OT edge (bulges toward head).
 */
data class PortWindowRadii(
    val utCornerRadiusMm: Double = 0.0,
    val otCornerRadiusMm: Double = 0.0,
    val topEdgeRadiusMm: Double = 0.0,
) {
    /** @deprecated Use [utCornerRadiusMm] */
    val cornerRadiusMm: Double get() = utCornerRadiusMm

    fun isEmpty(): Boolean =
        utCornerRadiusMm <= 0.0 && otCornerRadiusMm <= 0.0 && topEdgeRadiusMm <= 0.0
}

fun PortWindowRadii.coerced(bottomWidthMm: Double, topWidthMm: Double, heightMm: Double): PortWindowRadii {
    val maxUt = if (bottomWidthMm <= 0.0) {
        // Triangular / apex-bottom windows: UT fillet rounds the bottom apex.
        minOf(topWidthMm, heightMm) / 2.0
    } else {
        minOf(bottomWidthMm, topWidthMm, heightMm) / 2.0
    }
    val maxTopEdge = minOf(topWidthMm / 2.0, heightMm / 2.0)
    val topEdge = topEdgeRadiusMm.coerceIn(0.0, maxTopEdge)

    val maxOt = if (topWidthMm > 0.0 && heightMm > 0.0) {
        val chordHalf = topWidthMm / 2.0
        val bulgeRadius = if (topEdge > 0.0) {
            (topWidthMm * topWidthMm + 4.0 * topEdge * topEdge) / (8.0 * topEdge)
        } else {
            Double.MAX_VALUE
        }
        val arcLimit = if (topEdge > 0.0) {
            minOf(bulgeRadius * 0.45, chordHalf * 0.4, heightMm / 3.0)
        } else {
            minOf(topWidthMm / 2.0, heightMm / 3.0)
        }
        minOf(topWidthMm / 2.0, heightMm / 3.0, arcLimit)
    } else {
        0.0
    }

    return PortWindowRadii(
        utCornerRadiusMm = utCornerRadiusMm.coerceIn(0.0, maxUt),
        otCornerRadiusMm = otCornerRadiusMm.coerceIn(0.0, maxOt),
        topEdgeRadiusMm = topEdge,
    )
}
