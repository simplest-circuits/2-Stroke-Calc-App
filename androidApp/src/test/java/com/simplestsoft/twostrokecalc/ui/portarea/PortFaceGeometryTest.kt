package com.simplestsoft.twostrokecalc.ui.portarea

import org.junit.Assert.assertTrue
import org.junit.Test

class PortFaceGeometryTest {

    @Test
    fun trianglePath_utRadiusOnly_staysWithinBounds() {
        val path = PortFaceGeometry.trianglePath(
            topLeftX = 0f,
            topWidth = 10f,
            topY = 0f,
            height = 10f,
            utCornerRadius = 2f,
        )
        val bounds = path.getBounds()
        assertTrue(bounds.top >= -0.01f)
        assertTrue(bounds.bottom <= 10.01f)
        assertTrue(bounds.left >= -0.01f)
        assertTrue(bounds.right <= 10.01f)
    }

    @Test
    fun trianglePath_otAndUtRadii_staysWithinBounds() {
        val path = PortFaceGeometry.trianglePath(
            topLeftX = 0f,
            topWidth = 10f,
            topY = 0f,
            height = 10f,
            utCornerRadius = 2f,
            otCornerRadius = 2f,
        )
        val bounds = path.getBounds()
        assertTrue(bounds.top >= -0.01f)
        assertTrue(bounds.bottom <= 10.01f)
        assertTrue(bounds.left >= -0.01f)
        assertTrue(bounds.right <= 10.01f)
    }

    @Test
    fun trianglePath_otRadiusOnly_staysWithinBounds() {
        val path = PortFaceGeometry.trianglePath(
            topLeftX = 0f,
            topWidth = 10f,
            topY = 0f,
            height = 10f,
            otCornerRadius = 2f,
        )
        val bounds = path.getBounds()
        assertTrue(bounds.top >= -0.01f)
        assertTrue(bounds.bottom <= 10.01f)
        assertTrue(bounds.left >= -0.01f)
        assertTrue(bounds.right <= 10.01f)
    }
}
