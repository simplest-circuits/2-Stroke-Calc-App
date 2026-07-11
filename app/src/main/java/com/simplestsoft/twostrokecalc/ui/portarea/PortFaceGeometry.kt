package com.simplestsoft.twostrokecalc.ui.portarea

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Port face geometry for the Time-Area diagram.
 * OT at [topY] (smaller Y), UT at [topY] + [height].
 */
internal object PortFaceGeometry {

    private const val EPS = 1e-4f

    fun bottomLeftX(topLeftX: Float, topWidth: Float, bottomWidth: Float): Float =
        topLeftX + (topWidth - bottomWidth) / 2f

    fun trapezoidPath(
        topLeftX: Float,
        topWidth: Float,
        bottomWidth: Float,
        topY: Float,
        height: Float,
        utCornerRadius: Float = 0f,
        otCornerRadius: Float = 0f,
        topEdgeSagitta: Float = 0f,
    ): Path {
        if (bottomWidth <= EPS) {
            return trianglePath(
                topLeftX = topLeftX,
                topWidth = topWidth,
                topY = topY,
                height = height,
                utCornerRadius = utCornerRadius,
                otCornerRadius = otCornerRadius,
                topEdgeSagitta = topEdgeSagitta,
            )
        }

        val bottomY = topY + height
        val bottomLeft = bottomLeftX(topLeftX, topWidth, bottomWidth)
        val tl = Offset(topLeftX, topY)
        val tr = Offset(topLeftX + topWidth, topY)
        val bl = Offset(bottomLeft, bottomY)
        val br = Offset(bottomLeft + bottomWidth, bottomY)

        val rightSlantLen = hypot(tr.x - br.x, tr.y - br.y)
        val leftSlantLen = hypot(tl.x - bl.x, tl.y - bl.y)
        val utR = utCornerRadius.coerceIn(0f, min(bottomWidth, height) / 2f)
        val sagitta = topEdgeSagitta.coerceIn(0f, min(topWidth / 2f, height / 2f))
        val topArc = if (sagitta > 0f) topBulgeArc(tl, tr, sagitta) else null
        val maxOtR = maxOtRadius(topWidth, height, rightSlantLen, leftSlantLen, topArc)
        val otR = otCornerRadius.coerceIn(0f, maxOtR)

        val blFillet = if (utR > 0f) cornerFillet(prev = tl, corner = bl, next = br, radius = utR) else null
        val brFillet = if (utR > 0f) cornerFillet(prev = bl, corner = br, next = tr, radius = utR) else null
        val trLineFillet = if (otR > 0f && topArc == null) {
            cornerFillet(prev = br, corner = tr, next = tl, radius = otR)
        } else {
            null
        }
        val tlLineFillet = if (otR > 0f && topArc == null) {
            cornerFillet(prev = tr, corner = tl, next = bl, radius = otR)
        } else {
            null
        }

        return Path().apply {
            if (blFillet != null) moveTo(blFillet.exit.x, blFillet.exit.y) else moveTo(bl.x, bl.y)

            if (brFillet != null) {
                lineTo(brFillet.entry.x, brFillet.entry.y)
                appendFilletArc(brFillet)
            } else {
                lineTo(br.x, br.y)
            }

            val rightFoot = brFillet?.exit ?: br
            val leftFoot = blFillet?.entry ?: bl
            appendTopRegion(
                rightFoot = rightFoot,
                leftFoot = leftFoot,
                tr = tr,
                tl = tl,
                topArc = topArc,
                otR = otR,
                trLineFillet = trLineFillet,
                tlLineFillet = tlLineFillet,
            )

            if (blFillet != null) appendFilletArc(blFillet)
            close()
        }
    }

    fun vShapePath(
        topLeftX: Float,
        topWidth: Float,
        topY: Float,
        height: Float,
        apexAtEnd: Boolean,
        otCornerRadius: Float = 0f,
        topEdgeSagitta: Float = 0f,
    ): Path {
        val tl = Offset(topLeftX, topY)
        val tr = Offset(topLeftX + topWidth, topY)
        val apex = if (apexAtEnd) {
            Offset(topLeftX + topWidth, topY + height)
        } else {
            Offset(topLeftX, topY + height)
        }
        val outerLen = hypot(
            (if (apexAtEnd) tl.x else tr.x) - apex.x,
            (if (apexAtEnd) tl.y else tr.y) - apex.y,
        )
        val sagitta = topEdgeSagitta.coerceIn(0f, min(topWidth / 2f, height / 2f))
        val topArc = if (sagitta > 0f) topBulgeArc(tl, tr, sagitta) else null
        val maxOtR = maxOtRadius(topWidth, height, max(outerLen, height), min(outerLen, height), topArc)
        val otR = otCornerRadius.coerceIn(0f, maxOtR)

        val (trLineFillet, tlLineFillet) = if (otR > 0f && topArc == null) {
            if (apexAtEnd) {
                cornerFillet(prev = apex, corner = tr, next = tl, radius = otR) to
                    cornerFillet(prev = tr, corner = tl, next = apex, radius = otR)
            } else {
                cornerFillet(prev = tl, corner = tr, next = apex, radius = otR) to
                    cornerFillet(prev = apex, corner = tl, next = tr, radius = otR)
            }
        } else {
            null to null
        }

        return Path().apply {
            moveTo(apex.x, apex.y)
            if (topArc != null && otR > 0f) {
                appendTopRegion(
                    rightFoot = apex,
                    leftFoot = apex,
                    tr = tr,
                    tl = tl,
                    topArc = topArc,
                    otR = otR,
                    trLineFillet = trLineFillet,
                    tlLineFillet = tlLineFillet,
                )
            } else if (topArc != null) {
                appendTopArcOnly(apex, apex, tr, tl, topArc)
            } else if (apexAtEnd) {
                appendOtCorner(trLineFillet, tr)
                appendOtCorner(tlLineFillet, tl)
            } else {
                appendOtCorner(tlLineFillet, tl)
                appendOtCorner(trLineFillet, tr)
            }
            close()
        }
    }

    fun trianglePath(
        topLeftX: Float,
        topWidth: Float,
        topY: Float,
        height: Float,
        utCornerRadius: Float = 0f,
        otCornerRadius: Float = 0f,
        topEdgeSagitta: Float = 0f,
    ): Path {
        val tl = Offset(topLeftX, topY)
        val tr = Offset(topLeftX + topWidth, topY)
        val apex = Offset(topLeftX + topWidth / 2f, topY + height)
        val sideLen = hypot(tr.x - apex.x, tr.y - apex.y)
        val utR = utCornerRadius.coerceIn(0f, min(min(topWidth / 2f, height / 2f), sideLen * 0.4f))
        val apexFillet = if (utR > 0f) {
            // CCW: right slant (tr → apex) then left slant (apex → tl).
            cornerFillet(prev = tr, corner = apex, next = tl, radius = utR)
        } else {
            null
        }
        val sagitta = topEdgeSagitta.coerceIn(0f, min(topWidth / 2f, height / 2f))
        val topArc = if (sagitta > 0f) topBulgeArc(tl, tr, sagitta) else null
        val maxOtR = maxOtRadius(topWidth, height, sideLen, sideLen, topArc)
        val otR = otCornerRadius.coerceIn(0f, maxOtR)

        // CCW outline: bottomStart → tl → tr → bottomEnd (OT fillets use path direction prev → corner → next).
        val trLineFillet = if (otR > 0f && topArc == null) {
            cornerFillet(prev = tl, corner = tr, next = apex, radius = otR)
        } else {
            null
        }
        val tlLineFillet = if (otR > 0f && topArc == null) {
            cornerFillet(prev = apex, corner = tl, next = tr, radius = otR)
        } else {
            null
        }

        val bottomStart = apexFillet?.exit ?: apex
        val bottomEnd = apexFillet?.entry ?: apex

        return Path().apply {
            moveTo(bottomStart.x, bottomStart.y)
            appendOtCorner(tlLineFillet, tl)

            when {
                topArc != null && otR > 0f -> appendTopRegion(
                    rightFoot = bottomEnd,
                    leftFoot = bottomStart,
                    tr = tr,
                    tl = tl,
                    topArc = topArc,
                    otR = otR,
                    trLineFillet = trLineFillet,
                    tlLineFillet = tlLineFillet,
                )

                topArc != null -> appendTopArcOnly(bottomEnd, bottomStart, tr, tl, topArc)

                else -> appendOtCorner(trLineFillet, tr)
            }

            lineTo(bottomEnd.x, bottomEnd.y)
            if (apexFillet != null) appendFilletArc(apexFillet)
            close()
        }
    }

    private fun Path.appendOtCorner(fillet: CornerFillet?, corner: Offset) {
        if (fillet != null) {
            lineTo(fillet.entry.x, fillet.entry.y)
            appendFilletArc(fillet)
        } else {
            lineTo(corner.x, corner.y)
        }
    }

    private fun Path.appendTopRegion(
        rightFoot: Offset,
        leftFoot: Offset,
        tr: Offset,
        tl: Offset,
        topArc: CircleArc?,
        otR: Float,
        trLineFillet: CornerFillet?,
        tlLineFillet: CornerFillet?,
    ) {
        when {
            topArc != null && otR > 0f -> {
                val interiorRef = Offset(
                    (tl.x + tr.x + rightFoot.x + leftFoot.x) / 4f,
                    (tl.y + tr.y + rightFoot.y + leftFoot.y) / 4f,
                )
                val trJoin = resolveLineCircleFillet(
                    lineStart = rightFoot,
                    lineEnd = tr,
                    circle = topArc,
                    radius = otR,
                    corner = tr,
                    interiorRef = interiorRef,
                )
                val tlJoin = resolveLineCircleFillet(
                    lineStart = leftFoot,
                    lineEnd = tl,
                    circle = topArc,
                    radius = otR,
                    corner = tl,
                    interiorRef = interiorRef,
                )
                if (trJoin != null && tlJoin != null) {
                    lineTo(trJoin.lineTangent.x, trJoin.lineTangent.y)
                    appendArcAroundCenter(
                        from = trJoin.lineTangent,
                        to = trJoin.circleTangent,
                        center = trJoin.center,
                        radius = trJoin.radius,
                        near = trJoin.interiorPoint,
                    )
                    appendCircleArc(trJoin.circleTangent, tlJoin.circleTangent, topArc)
                    appendArcAroundCenter(
                        from = tlJoin.circleTangent,
                        to = tlJoin.lineTangent,
                        center = tlJoin.center,
                        radius = tlJoin.radius,
                        near = tlJoin.interiorPoint,
                    )
                    lineTo(leftFoot.x, leftFoot.y)
                } else {
                    appendTopArcOnly(rightFoot, leftFoot, tr, tl, topArc)
                }
            }

            topArc != null -> appendTopArcOnly(rightFoot, leftFoot, tr, tl, topArc)

            trLineFillet != null || tlLineFillet != null -> {
                val rightTop = trLineFillet?.entry ?: tr
                lineTo(rightTop.x, rightTop.y)
                if (trLineFillet != null) {
                    appendFilletArc(trLineFillet)
                    if (tlLineFillet != null) {
                        lineTo(tlLineFillet.entry.x, tlLineFillet.entry.y)
                        appendFilletArc(tlLineFillet)
                    } else {
                        lineTo(tl.x, tl.y)
                    }
                } else {
                    lineTo(tr.x, tr.y)
                    if (tlLineFillet != null) {
                        lineTo(tlLineFillet.entry.x, tlLineFillet.entry.y)
                        appendFilletArc(tlLineFillet)
                    } else {
                        lineTo(tl.x, tl.y)
                    }
                }
                lineTo(leftFoot.x, leftFoot.y)
            }

            else -> {
                lineTo(tr.x, tr.y)
                lineTo(tl.x, tl.y)
                lineTo(leftFoot.x, leftFoot.y)
            }
        }
    }

    private fun Path.appendTopArcOnly(
        rightFoot: Offset,
        leftFoot: Offset,
        tr: Offset,
        tl: Offset,
        topArc: CircleArc,
    ) {
        val rightJoin = slantCircleJoin(rightFoot, tr, topArc)
        lineTo(rightJoin.x, rightJoin.y)
        val leftJoin = slantCircleJoin(leftFoot, tl, topArc)
        appendCircleArc(rightJoin, leftJoin, topArc)
        lineTo(leftFoot.x, leftFoot.y)
    }

    private fun maxOtRadius(
        topWidth: Float,
        height: Float,
        rightSlantLen: Float,
        leftSlantLen: Float,
        topArc: CircleArc?,
    ): Float {
        val slantLimit = min(rightSlantLen, leftSlantLen) * 0.35f
        val base = min(min(topWidth / 2f, height / 3f), slantLimit)
        if (topArc == null) return base
        val arcLimit = min(min(topArc.radius * 0.45f, topWidth * 0.4f), height / 3f)
        return min(base, arcLimit)
    }

    private data class CornerFillet(
        val entry: Offset,
        val exit: Offset,
        val corner: Offset,
        val center: Offset,
        val radius: Float,
        val interiorAngle: Float,
    )

    private data class CircleArc(
        val center: Offset,
        val radius: Float,
        val bulge: Offset,
        val sagitta: Float,
    )

    private data class LineCircleFillet(
        val lineTangent: Offset,
        val circleTangent: Offset,
        val center: Offset,
        val radius: Float,
        val interiorPoint: Offset,
    )

    private fun resolveLineCircleFillet(
        lineStart: Offset,
        lineEnd: Offset,
        circle: CircleArc,
        radius: Float,
        corner: Offset,
        interiorRef: Offset,
    ): LineCircleFillet? {
        var attempt = radius
        repeat(10) {
            lineCircleFillet(lineStart, lineEnd, circle, attempt, corner, interiorRef)?.let { return it }
            attempt *= 0.85f
            if (attempt < EPS) return null
        }
        return null
    }

    /**
     * Fillet of [radius] between slant segment ([lineStart]→[lineEnd]) and OT bulge [circle].
     * [interiorRef] is a point inside the port cavity (used to pick the inward offset side).
     */
    private fun lineCircleFillet(
        lineStart: Offset,
        lineEnd: Offset,
        circle: CircleArc,
        radius: Float,
        corner: Offset,
        interiorRef: Offset,
    ): LineCircleFillet? {
        if (radius <= EPS) return null
        val bigR = circle.radius
        if (radius >= bigR - EPS) return null

        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-8f) return null
        val len = sqrt(lenSq)
        val ux = dx / len
        val uy = dy / len
        val leftNx = -uy
        val leftNy = ux
        val rightNx = uy
        val rightNy = -ux
        val toInteriorX = interiorRef.x - corner.x
        val toInteriorY = interiorRef.y - corner.y
        val useLeft = leftNx * toInteriorX + leftNy * toInteriorY >= rightNx * toInteriorX + rightNy * toInteriorY
        val nx = if (useLeft) leftNx else rightNx
        val ny = if (useLeft) leftNy else rightNy
        val innerDist = bigR - radius

        val ox = lineStart.x + nx * radius
        val oy = lineStart.y + ny * radius
        val fx = ox - circle.center.x
        val fy = oy - circle.center.y
        val b = 2f * (fx * ux + fy * uy)
        val c = fx * fx + fy * fy - innerDist * innerDist
        val disc = b * b - 4f * lenSq * c
        if (disc < 0f) return null

        val sqrtDisc = sqrt(disc)
        val candidates = mutableListOf<LineCircleFillet>()
        for (sign in floatArrayOf(-1f, 1f)) {
            val t = (-b + sign * sqrtDisc) / (2f * lenSq)
            val lineTan = Offset(lineStart.x + t * ux, lineStart.y + t * uy)
            val cx = lineTan.x + nx * radius
            val cy = lineTan.y + ny * radius
            if (!isInteriorFilletCenter(Offset(cx, cy), corner)) continue
            if (!onSegment(lineTan, lineStart, lineEnd, slack = radius * 0.25f)) continue

            val toBig = hypot(cx - circle.center.x, cy - circle.center.y)
            if (abs(toBig - innerDist) > radius * 0.05f) continue

            val circleTan = Offset(
                circle.center.x + (cx - circle.center.x) / toBig * bigR,
                circle.center.y + (cy - circle.center.y) / toBig * bigR,
            )
            if (!isOnTopArc(circleTan, circle, corner)) continue

            candidates += LineCircleFillet(
                lineTangent = lineTan,
                circleTangent = circleTan,
                center = Offset(cx, cy),
                radius = radius,
                interiorPoint = Offset(
                    (lineTan.x + circleTan.x) / 2f + nx * radius * 0.25f,
                    (lineTan.y + circleTan.y) / 2f + ny * radius * 0.25f,
                ),
            )
        }
        return candidates.minByOrNull { hypot(it.lineTangent.x - corner.x, it.lineTangent.y - corner.y) }
    }

    /** Fillet center must sit inside the port cavity below the OT corner. */
    private fun isInteriorFilletCenter(center: Offset, corner: Offset): Boolean =
        center.y >= corner.y - EPS

    /** Tangent on the bulge arc must lie on the OT edge near [corner]. */
    private fun isOnTopArc(point: Offset, circle: CircleArc, corner: Offset): Boolean {
        if (point.y > corner.y + circle.sagitta * 0.6f) return false
        val distToCorner = hypot(point.x - corner.x, point.y - corner.y)
        if (distToCorner > circle.radius * 0.6f) return false
        val distToBulge = hypot(point.x - circle.bulge.x, point.y - circle.bulge.y)
        return distToBulge <= circle.radius * 1.05f
    }

    private fun onSegment(point: Offset, a: Offset, b: Offset, slack: Float): Boolean {
        val minX = min(a.x, b.x) - slack
        val maxX = max(a.x, b.x) + slack
        val minY = min(a.y, b.y) - slack
        val maxY = max(a.y, b.y) + slack
        return point.x in minX..maxX && point.y in minY..maxY
    }

    private fun cornerFillet(
        prev: Offset,
        corner: Offset,
        next: Offset,
        radius: Float,
    ): CornerFillet? {
        if (radius <= 0f) return null

        val inDx = corner.x - prev.x
        val inDy = corner.y - prev.y
        val inLen = hypot(inDx, inDy)
        val outDx = next.x - corner.x
        val outDy = next.y - corner.y
        val outLen = hypot(outDx, outDy)
        if (inLen < EPS || outLen < EPS) return null

        val uInX = inDx / inLen
        val uInY = inDy / inLen
        val uOutX = outDx / outLen
        val uOutY = outDy / outLen

        val interiorAngle = acos((-uInX * uOutX - uInY * uOutY).coerceIn(-1f, 1f))
        if (interiorAngle < 1e-3f) return null

        val tanHalf = tan(interiorAngle / 2f)
        if (abs(tanHalf) < 1e-6f) return null

        val maxRadius = min(inLen, outLen) * tanHalf * 0.98f
        val effectiveRadius = min(radius, maxRadius)
        if (effectiveRadius <= 0f) return null

        val trim = effectiveRadius / tanHalf
        val bisX = -uInX + uOutX
        val bisY = -uInY + uOutY
        val bisLen = hypot(bisX, bisY)
        if (bisLen < EPS) return null

        val centerDist = effectiveRadius / sin(interiorAngle / 2f)
        return CornerFillet(
            entry = Offset(corner.x - uInX * trim, corner.y - uInY * trim),
            exit = Offset(corner.x + uOutX * trim, corner.y + uOutY * trim),
            corner = corner,
            center = Offset(
                corner.x + bisX / bisLen * centerDist,
                corner.y + bisY / bisLen * centerDist,
            ),
            radius = effectiveRadius,
            interiorAngle = interiorAngle,
        )
    }

    private fun topBulgeArc(start: Offset, end: Offset, sagitta: Float): CircleArc {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val chord = hypot(dx, dy)
        val radius = (chord * chord + 4f * sagitta * sagitta) / (8f * sagitta)
        val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        val tangentX = dx / chord
        val tangentY = dy / chord
        var normalX = -tangentY
        var normalY = tangentX
        if (normalY > 0f) {
            normalX = -normalX
            normalY = -normalY
        }
        val bulge = Offset(mid.x + normalX * sagitta, mid.y + normalY * sagitta)
        val center = Offset(
            mid.x - normalX * (radius - sagitta),
            mid.y - normalY * (radius - sagitta),
        )
        return CircleArc(center, radius, bulge, sagitta)
    }

    private fun slantCircleJoin(slantFoot: Offset, slantTop: Offset, circle: CircleArc): Offset {
        val dirX = slantTop.x - slantFoot.x
        val dirY = slantTop.y - slantFoot.y
        val dirLenSq = dirX * dirX + dirY * dirY
        if (dirLenSq < 1e-8f) return slantTop

        val hits = lineCircleIntersections(slantFoot, Offset(dirX, dirY), circle.center, circle.radius)
        if (hits.isEmpty()) return slantTop

        val onRay = hits.filter { hit ->
            val t = ((hit.x - slantFoot.x) * dirX + (hit.y - slantFoot.y) * dirY) / dirLenSq
            t >= -0.02f
        }
        return (onRay.ifEmpty { hits }).minBy { hypot(it.x - slantTop.x, it.y - slantTop.y) }
    }

    private fun lineCircleIntersections(
        origin: Offset,
        direction: Offset,
        center: Offset,
        radius: Float,
    ): List<Offset> {
        val dx = direction.x
        val dy = direction.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-8f) return emptyList()

        val fx = origin.x - center.x
        val fy = origin.y - center.y
        val b = 2f * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - radius * radius
        val disc = b * b - 4f * lenSq * c
        if (disc < 0f) return emptyList()

        val sqrtDisc = sqrt(disc)
        return floatArrayOf(-1f, 1f).map { sign ->
            val t = (-b + sign * sqrtDisc) / (2f * lenSq)
            Offset(origin.x + t * dx, origin.y + t * dy)
        }
    }

    private fun Path.appendCircleArc(from: Offset, to: Offset, circle: CircleArc) {
        val cx = circle.center.x
        val cy = circle.center.y
        val r = circle.radius
        val startDeg = atan2(from.y - cy, from.x - cx) * 180f / PI.toFloat()
        val endDeg = atan2(to.y - cy, to.x - cx) * 180f / PI.toFloat()
        val bulgeDeg = atan2(circle.bulge.y - cy, circle.bulge.x - cx) * 180f / PI.toFloat()
        arcTo(Rect(cx - r, cy - r, cx + r, cy + r), startDeg, sweepThroughPoint(startDeg, endDeg, bulgeDeg), false)
    }

    private fun Path.appendFilletArc(fillet: CornerFillet) {
        appendArcAroundCenter(fillet.entry, fillet.exit, fillet.center, fillet.radius, fillet.corner)
    }

    private fun Path.appendArcAroundCenter(
        from: Offset,
        to: Offset,
        center: Offset,
        radius: Float,
        near: Offset,
    ) {
        val cx = center.x
        val cy = center.y
        val r = radius
        val startRad = atan2(from.y - cy, from.x - cx)
        val endRad = atan2(to.y - cy, to.x - cx)

        var shortSweep = endRad - startRad
        while (shortSweep > PI.toFloat()) shortSweep -= (2f * PI.toFloat())
        while (shortSweep <= -PI.toFloat()) shortSweep += (2f * PI.toFloat())

        val longSweep = if (shortSweep >= 0f) shortSweep - 2f * PI.toFloat() else shortSweep + 2f * PI.toFloat()

        fun cornerDistance(sweep: Float): Float {
            val midRad = startRad + sweep / 2f
            return hypot(
                cx + r * cos(midRad) - near.x,
                cy + r * sin(midRad) - near.y,
            )
        }

        val sweep = if (cornerDistance(shortSweep) <= cornerDistance(longSweep)) shortSweep else longSweep
        val steps = max(4, (abs(sweep) / (PI.toFloat() / 12f)).toInt())
        for (i in 1..steps) {
            val angle = startRad + sweep * (i.toFloat() / steps)
            lineTo(cx + r * cos(angle), cy + r * sin(angle))
        }
    }

    private fun sweepThroughPoint(startDeg: Float, endDeg: Float, throughDeg: Float): Float {
        var sweep = endDeg - startDeg
        if (sweep > 180f) sweep -= 360f
        if (sweep < -180f) sweep += 360f
        if (isAngleBetween(startDeg, throughDeg, sweep)) return sweep
        return if (sweep > 0f) sweep - 360f else sweep + 360f
    }

    private fun isAngleBetween(startDeg: Float, probeDeg: Float, sweepDeg: Float): Boolean {
        fun norm(a: Float): Float {
            var x = a % 360f
            if (x < 0f) x += 360f
            return x
        }
        val start = norm(startDeg)
        val probe = norm(probeDeg)
        val end = norm(startDeg + sweepDeg)
        return if (sweepDeg >= 0f) {
            if (end >= start) probe in start..end else probe >= start || probe <= end
        } else {
            if (end <= start) probe in end..start else probe <= start || probe >= end
        }
    }
}
