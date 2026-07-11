package com.simplestsoft.twostrokecalc.ui.portarea

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class PortFaceGeometryTopEdgeOtTest {

    @Test
    fun trapezoidPath_topEdgeAndOtRadius_differsFromTopEdgeOnly() {
        val base = PortFaceGeometry.trapezoidPath(
            topLeftX = 10f,
            topWidth = 40f,
            bottomWidth = 30f,
            topY = 20f,
            height = 15f,
            topEdgeSagitta = 3f,
        )
        val withOt = PortFaceGeometry.trapezoidPath(
            topLeftX = 10f,
            topWidth = 40f,
            bottomWidth = 30f,
            topY = 20f,
            height = 15f,
            otCornerRadius = 3f,
            topEdgeSagitta = 3f,
        )
        val tr = Offset(50f, 20f)
        assertTrue(
            "OT fillet should keep the path away from the sharp top-right corner",
            minDistanceToCorner(withOt, tr) < minDistanceToCorner(base, tr) - 0.5f ||
                pathLength(withOt) > pathLength(base) + 1f,
        )
    }

    private fun minDistanceToCorner(path: Path, corner: Offset): Float {
        val points = samplePath(path, steps = 200)
        return points.minOf { hypot(it.x - corner.x, it.y - corner.y) }
    }

    private fun pathLength(path: Path): Float {
        val points = samplePath(path, steps = 200)
        return points.zipWithNext().sumOf { (a, b) ->
            hypot(b.x - a.x, b.y - a.y).toDouble()
        }.toFloat()
    }

    private fun samplePath(path: Path, steps: Int): List<Offset> {
        val measure = androidx.compose.ui.graphics.PathMeasure()
        measure.setPath(path, false)
        val length = measure.length
        return (0..steps).map { i ->
            measure.getPosition(length * i / steps)
        }
    }
}
