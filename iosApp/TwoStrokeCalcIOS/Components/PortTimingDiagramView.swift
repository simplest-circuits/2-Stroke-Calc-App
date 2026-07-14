import SwiftUI
import sharedKit

/// Steuerzeit-Diagramm – SwiftUI-Portierung von PortTimingDiagram.kt.
struct PortTimingDiagramView: View {
    @Environment(\.themeColors) private var colors
    let result: PortTimingResult
    let showIntake: Bool

    private let tdcAngle: Double = 270
    private let bdcAngle: Double = 90

    private let transferInner: CGFloat = 0.36
    private let transferOuter: CGFloat = 0.52
    private let exhaustInner: CGFloat = 0.58
    private let exhaustOuter: CGFloat = 0.74
    private let intakeInner: CGFloat = 0.78
    private let intakeOuter: CGFloat = 0.98

    var body: some View {
        let palette = diagramPalette
        let overlapSpans = computeOverlapSpans(result: result, showIntake: showIntake)
        let blowdownSpans = computeBlowdownSpans(result: result)

        VStack(spacing: 10) {
            Text(L.t("port_area_exhaust_diagram_ot"))
                .font(.caption.bold())
                .foregroundStyle(colors.onSurfaceVariant)

            Canvas { context, size in
                let layout = computeDiagramLayout(size: size)
                let cx = layout.cx
                let cy = layout.cy
                let maxRadius = layout.maxRadius

                drawRingTracks(context: &context, cx: cx, cy: cy, maxRadius: maxRadius, palette: palette, showIntake: showIntake)
                drawCircleGuides(context: &context, cx: cx, cy: cy, maxRadius: maxRadius, palette: palette, showIntake: showIntake)
                drawCrosshairs(context: &context, cx: cx, cy: cy, radius: maxRadius, color: palette.crosshair)
                drawAngleTicks(context: &context, cx: cx, cy: cy, radius: maxRadius, color: palette.crosshair)
                drawTdcBdcMarkers(context: &context, cx: cx, cy: cy, radius: maxRadius, color: palette.marker)

                if let transfer = result.transfer {
                    drawRingSector(
                        context: &context, cx: cx, cy: cy,
                        innerRadius: maxRadius * transferInner,
                        outerRadius: maxRadius * transferOuter,
                        startAngle: bdcAngle - transfer.openBeforeBdc,
                        sweepAngle: transfer.duration,
                        fill: palette.transferFill,
                        stroke: palette.transferStroke
                    )
                }

                if let exhaust = result.exhaust {
                    drawRingSector(
                        context: &context, cx: cx, cy: cy,
                        innerRadius: maxRadius * exhaustInner,
                        outerRadius: maxRadius * exhaustOuter,
                        startAngle: bdcAngle - exhaust.openBeforeBdc,
                        sweepAngle: exhaust.duration,
                        fill: palette.exhaustFill,
                        stroke: palette.exhaustStroke
                    )
                }

                if showIntake, let intake = result.intake {
                    drawRingSector(
                        context: &context, cx: cx, cy: cy,
                        innerRadius: maxRadius * intakeInner,
                        outerRadius: maxRadius * intakeOuter,
                        startAngle: tdcAngle - intake.openBeforeTdc,
                        sweepAngle: intake.duration,
                        fill: palette.intakeFill,
                        stroke: palette.intakeStroke
                    )
                }

                for blowdown in blowdownSpans {
                    drawRingSector(
                        context: &context, cx: cx, cy: cy,
                        innerRadius: maxRadius * blowdown.innerFactor,
                        outerRadius: maxRadius * blowdown.outerFactor,
                        startAngle: blowdown.startAngle,
                        sweepAngle: blowdown.sweepAngle,
                        fill: palette.blowdownFill,
                        stroke: palette.blowdownStroke,
                        dashed: true
                    )
                }

                for overlap in overlapSpans {
                    drawRingSector(
                        context: &context, cx: cx, cy: cy,
                        innerRadius: maxRadius * overlap.innerFactor,
                        outerRadius: maxRadius * overlap.outerFactor,
                        startAngle: overlap.startAngle,
                        sweepAngle: overlap.sweepAngle,
                        fill: palette.overlapFill,
                        stroke: palette.overlapStroke,
                        strokeWidth: 3
                    )
                    drawOverlapBoundaryMarkers(
                        context: &context, cx: cx, cy: cy,
                        innerRadius: maxRadius * overlap.markerInner,
                        outerRadius: maxRadius * overlap.markerOuter,
                        startAngle: overlap.startAngle,
                        endAngle: overlap.startAngle + overlap.sweepAngle,
                        color: palette.overlapStroke
                    )
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .frame(maxHeight: 280)
            .padding(.horizontal, 4)

            Text(L.t("port_area_exhaust_diagram_ut"))
                .font(.caption.bold())
                .foregroundStyle(colors.onSurfaceVariant)

            diagramLegend(
                palette: palette,
                showIntake: showIntake,
                showOverlap: !overlapSpans.isEmpty,
                showBlowdown: !blowdownSpans.isEmpty
            )
        }
    }

    // MARK: - Palette

    private struct DiagramPalette {
        let crosshair: Color
        let marker: Color
        let ring: Color
        let ringInner: Color
        let trackBackground: Color
        let exhaustFill: Color
        let exhaustStroke: Color
        let transferFill: Color
        let transferStroke: Color
        let intakeFill: Color
        let intakeStroke: Color
        let overlapFill: Color
        let overlapStroke: Color
        let blowdownFill: Color
        let blowdownStroke: Color
    }

    private var diagramPalette: DiagramPalette {
        let overlapStroke = colors.isDark ? Color(red: 1, green: 0.44, blue: 0.26) : Color(red: 0.9, green: 0.29, blue: 0.1)
        return DiagramPalette(
            crosshair: colors.outline.opacity(colors.isDark ? 0.55 : 0.4),
            marker: colors.onSurfaceVariant.opacity(0.85),
            ring: colors.outline.opacity(colors.isDark ? 0.7 : 0.55),
            ringInner: colors.outline.opacity(colors.isDark ? 0.35 : 0.25),
            trackBackground: colors.onSurface.opacity(colors.isDark ? 0.06 : 0.04),
            exhaustFill: colors.onSurfaceVariant.opacity(colors.isDark ? 0.55 : 0.42),
            exhaustStroke: colors.onSurfaceVariant,
            transferFill: colors.primary.opacity(colors.isDark ? 0.42 : 0.32),
            transferStroke: colors.primary,
            intakeFill: colors.primaryContainer.opacity(colors.isDark ? 0.38 : 0.28),
            intakeStroke: colors.primaryContainer,
            overlapFill: overlapStroke.opacity(colors.isDark ? 0.9 : 0.82),
            overlapStroke: overlapStroke,
            blowdownFill: colors.onSurfaceVariant.opacity(colors.isDark ? 0.72 : 0.58),
            blowdownStroke: colors.onSurface.opacity(colors.isDark ? 0.85 : 0.7)
        )
    }

    // MARK: - Layout

    private struct DiagramLayout {
        let cx: CGFloat
        let cy: CGFloat
        let maxRadius: CGFloat
    }

    private func computeDiagramLayout(size: CGSize) -> DiagramLayout {
        let inset = min(size.width, size.height) * 0.035
        let maxRadius = min(size.width - inset * 2, size.height - inset * 2) / 2
        return DiagramLayout(cx: size.width / 2, cy: size.height / 2, maxRadius: maxRadius)
    }

    // MARK: - Legend

    @ViewBuilder
    private func diagramLegend(
        palette: DiagramPalette,
        showIntake: Bool,
        showOverlap: Bool,
        showBlowdown: Bool
    ) -> some View {
        FlowLayout(spacing: 12) {
            legendDot(color: palette.exhaustStroke, label: L.t("pt_legend_exhaust"))
            legendDot(color: palette.transferStroke, label: L.t("pt_legend_transfer"))
            if showIntake {
                legendDot(color: palette.intakeStroke, label: L.t("pt_legend_intake"))
            }
            if showBlowdown {
                legendDot(color: palette.blowdownStroke, label: L.t("pt_legend_blowdown"))
            }
            if showOverlap {
                overlapLegendItem(color: palette.overlapStroke)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 6) {
            Circle().fill(color).frame(width: 10, height: 10)
            Text(label).font(.caption).foregroundStyle(colors.onSurfaceVariant)
        }
    }

    private func overlapLegendItem(color: Color) -> some View {
        HStack(spacing: 6) {
            Canvas { context, size in
                var path = Path()
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
                context.stroke(path, with: .color(color), lineWidth: 2.5)
                let r = size.minDimension * 0.22
                let y = size.height / 2
                context.stroke(Path(ellipseIn: CGRect(x: size.width * 0.28 - r, y: y - r, width: r * 2, height: r * 2)), with: .color(color), lineWidth: 2.5)
                context.stroke(Path(ellipseIn: CGRect(x: size.width * 0.72 - r, y: y - r, width: r * 2, height: r * 2)), with: .color(color), lineWidth: 2.5)
            }
            .frame(width: 18, height: 10)
            Text(L.t("pt_legend_overlap")).font(.caption).foregroundStyle(colors.onSurfaceVariant)
        }
    }

    // MARK: - Spans

    private struct SectorSpan {
        let startAngle: Double
        let sweepAngle: Double
        let innerFactor: CGFloat
        let outerFactor: CGFloat
        var markerInner: CGFloat = 0
        var markerOuter: CGFloat = 0
    }

    private struct AngleSpan {
        let start: Double
        let end: Double
    }

    private func computeBlowdownSpans(result: PortTimingResult) -> [SectorSpan] {
        guard let exhaust = result.exhaust,
              let transfer = result.transfer,
              let blowdownRaw = result.blowdown else { return [] }
        let blowdown = blowdownRaw.asDouble
        guard blowdown > 0.05 else { return [] }

        let exhaustHalf = exhaust.openBeforeBdc
        let transferHalf = transfer.openBeforeBdc
        return [
            angleInterval(start: bdcAngle - exhaustHalf, end: bdcAngle - transferHalf),
            angleInterval(start: bdcAngle + transferHalf, end: bdcAngle + exhaustHalf),
        ].map { start, sweep in
            SectorSpan(startAngle: start, sweepAngle: sweep, innerFactor: exhaustInner, outerFactor: exhaustOuter)
        }
    }

    private func computeOverlapSpans(result: PortTimingResult, showIntake: Bool) -> [SectorSpan] {
        var overlaps: [SectorSpan] = []
        let exhaust = result.exhaust
        let transfer = result.transfer

        if let exhaust, let transfer {
            let exhaustSpan = symmetricBdcSpan(openBeforeBdc: exhaust.openBeforeBdc)
            let transferSpan = symmetricBdcSpan(openBeforeBdc: transfer.openBeforeBdc)
            for interval in intersectSpans(exhaustSpan, transferSpan) {
                overlaps.append(overlapBridgeSpan(
                    interval: interval,
                    bridgeInner: transferOuter,
                    bridgeOuter: exhaustInner,
                    markerInner: transferInner,
                    markerOuter: exhaustOuter
                ))
            }
        }

        if showIntake, let exhaust, let intake = result.intake {
            let exhaustSpan = symmetricBdcSpan(openBeforeBdc: exhaust.openBeforeBdc)
            let intakeAngleSpan = makeIntakeSpan(openBeforeTdc: intake.openBeforeTdc, closeAfterTdc: intake.closeAfterTdc)
            for interval in intersectSpans(exhaustSpan, intakeAngleSpan) {
                overlaps.append(overlapBridgeSpan(
                    interval: interval,
                    bridgeInner: exhaustOuter,
                    bridgeOuter: intakeInner,
                    markerInner: exhaustInner,
                    markerOuter: intakeOuter
                ))
            }
        }

        return overlaps
    }

    private func overlapBridgeSpan(
        interval: (start: Double, end: Double),
        bridgeInner: CGFloat,
        bridgeOuter: CGFloat,
        markerInner: CGFloat,
        markerOuter: CGFloat
    ) -> SectorSpan {
        SectorSpan(
            startAngle: interval.start,
            sweepAngle: interval.end - interval.start,
            innerFactor: bridgeInner,
            outerFactor: bridgeOuter,
            markerInner: markerInner,
            markerOuter: markerOuter
        )
    }

    private func angleInterval(start: Double, end: Double) -> (start: Double, sweep: Double) {
        let normalizedStart = normalizeAngle(start)
        let normalizedEnd = normalizeAngle(end)
        if normalizedStart <= normalizedEnd {
            return (normalizedStart, normalizedEnd - normalizedStart)
        }
        return (normalizedStart, 360 - normalizedStart + normalizedEnd)
    }

    private func symmetricBdcSpan(openBeforeBdc: Double) -> AngleSpan {
        AngleSpan(
            start: normalizeAngle(bdcAngle - openBeforeBdc),
            end: normalizeAngle(bdcAngle + openBeforeBdc)
        )
    }

    private func makeIntakeSpan(openBeforeTdc: Double, closeAfterTdc: Double) -> AngleSpan {
        AngleSpan(
            start: normalizeAngle(tdcAngle - openBeforeTdc),
            end: normalizeAngle(tdcAngle + closeAfterTdc)
        )
    }

    private func normalizeAngle(_ angle: Double) -> Double {
        var normalized = angle.truncatingRemainder(dividingBy: 360)
        if normalized < 0 { normalized += 360 }
        return normalized
    }

    private func spanToIntervals(_ span: AngleSpan) -> [(start: Double, end: Double)] {
        let start = normalizeAngle(span.start)
        let end = normalizeAngle(span.end)
        if start <= end {
            return [(start, end)]
        }
        return [(start, 360), (0, end)]
    }

    private func intersectSpans(_ first: AngleSpan, _ second: AngleSpan) -> [(start: Double, end: Double)] {
        var intervals: [(Double, Double)] = []
        for a in spanToIntervals(first) {
            for b in spanToIntervals(second) {
                let overlapStart = max(a.start, b.start)
                let overlapEnd = min(a.end, b.end)
                if overlapEnd - overlapStart > 0.05 {
                    intervals.append((overlapStart, overlapEnd))
                }
            }
        }
        return intervals
    }

    // MARK: - Drawing

    private func drawRingTracks(
        context: inout GraphicsContext,
        cx: CGFloat, cy: CGFloat, maxRadius: CGFloat,
        palette: DiagramPalette,
        showIntake: Bool
    ) {
        var tracks = [(transferInner, transferOuter), (exhaustInner, exhaustOuter)]
        if showIntake { tracks.append((intakeInner, intakeOuter)) }
        for (inner, outer) in tracks {
            drawRingSector(
                context: &context, cx: cx, cy: cy,
                innerRadius: maxRadius * inner,
                outerRadius: maxRadius * outer,
                startAngle: 0, sweepAngle: 360,
                fill: palette.trackBackground,
                stroke: .clear,
                strokeWidth: 0
            )
        }
    }

    private func drawCircleGuides(
        context: inout GraphicsContext,
        cx: CGFloat, cy: CGFloat, maxRadius: CGFloat,
        palette: DiagramPalette,
        showIntake: Bool
    ) {
        var factors: [CGFloat] = [transferInner, transferOuter, exhaustInner, exhaustOuter]
        if showIntake { factors.append(contentsOf: [intakeInner, intakeOuter]) }
        for factor in factors {
            let rect = CGRect(x: cx - maxRadius * factor, y: cy - maxRadius * factor, width: maxRadius * factor * 2, height: maxRadius * factor * 2)
            context.stroke(Path(ellipseIn: rect), with: .color(palette.ringInner), lineWidth: 1)
        }
        let outerRect = CGRect(x: cx - maxRadius, y: cy - maxRadius, width: maxRadius * 2, height: maxRadius * 2)
        context.stroke(Path(ellipseIn: outerRect), with: .color(palette.ring), lineWidth: 1.5)
    }

    private func drawCrosshairs(context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat, color: Color) {
        var path = Path()
        path.move(to: CGPoint(x: cx, y: cy - radius))
        path.addLine(to: CGPoint(x: cx, y: cy + radius))
        path.move(to: CGPoint(x: cx - radius, y: cy))
        path.addLine(to: CGPoint(x: cx + radius, y: cy))
        context.stroke(path, with: .color(color), lineWidth: 1)
    }

    private func drawAngleTicks(context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat, color: Color) {
        for angle in stride(from: 0, through: 330, by: 30) {
            let isMajor = angle % 90 == 0
            let radians = Double(angle) * .pi / 180
            let cosValue = cos(radians)
            let sinValue = sin(radians)
            let innerFactor: CGFloat = isMajor ? 0.965 : 0.985
            let outerFactor: CGFloat = isMajor ? 1.035 : 1.012
            var path = Path()
            path.move(to: CGPoint(x: cx + radius * innerFactor * cosValue, y: cy + radius * innerFactor * sinValue))
            path.addLine(to: CGPoint(x: cx + radius * outerFactor * cosValue, y: cy + radius * outerFactor * sinValue))
            context.stroke(path, with: .color(color), lineWidth: isMajor ? 1.75 : 1)
        }
    }

    private func drawTdcBdcMarkers(context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat, color: Color) {
        let markerLength = radius * 0.08
        var top = Path()
        top.move(to: CGPoint(x: cx, y: cy - radius - markerLength))
        top.addLine(to: CGPoint(x: cx, y: cy - radius + markerLength * 0.35))
        var bottom = Path()
        bottom.move(to: CGPoint(x: cx, y: cy + radius - markerLength * 0.35))
        bottom.addLine(to: CGPoint(x: cx, y: cy + radius + markerLength))
        context.stroke(top, with: .color(color), lineWidth: 2.5)
        context.stroke(bottom, with: .color(color), lineWidth: 2.5)
    }

    private func drawRingSector(
        context: inout GraphicsContext,
        cx: CGFloat, cy: CGFloat,
        innerRadius: CGFloat, outerRadius: CGFloat,
        startAngle: Double, sweepAngle: Double,
        fill: Color,
        stroke: Color,
        strokeWidth: CGFloat = 2.5,
        dashed: Bool = false
    ) {
        guard sweepAngle > 0 else { return }

        var remainingStart = normalizeAngle(startAngle)
        var remainingSweep = sweepAngle
        while remainingSweep > 0.001 {
            let segmentSweep = min(remainingSweep, 360 - remainingStart)
            let path = ringSectorPath(
                cx: cx, cy: cy,
                innerRadius: innerRadius,
                outerRadius: outerRadius,
                startAngle: remainingStart,
                sweepAngle: segmentSweep
            )
            context.fill(path, with: .color(fill))
            if strokeWidth > 0, stroke != .clear {
                if dashed {
                    context.stroke(
                        path,
                        with: .color(stroke),
                        style: StrokeStyle(lineWidth: strokeWidth, dash: [8, 6])
                    )
                } else {
                    context.stroke(path, with: .color(stroke), lineWidth: strokeWidth)
                }
            }
            remainingSweep -= segmentSweep
            remainingStart = 0
        }
    }

    private func ringSectorPath(
        cx: CGFloat, cy: CGFloat,
        innerRadius: CGFloat, outerRadius: CGFloat,
        startAngle: Double, sweepAngle: Double
    ) -> Path {
        let startRadians = startAngle * .pi / 180
        let endAngle = startAngle + sweepAngle
        let endRadians = endAngle * .pi / 180

        var path = Path()
        path.move(to: CGPoint(
            x: cx + outerRadius * cos(startRadians),
            y: cy + outerRadius * sin(startRadians)
        ))
        path.addArc(
            center: CGPoint(x: cx, y: cy),
            radius: outerRadius,
            startAngle: .radians(startRadians),
            endAngle: .radians(endRadians),
            clockwise: true
        )
        path.addLine(to: CGPoint(
            x: cx + innerRadius * cos(endRadians),
            y: cy + innerRadius * sin(endRadians)
        ))
        path.addArc(
            center: CGPoint(x: cx, y: cy),
            radius: innerRadius,
            startAngle: .radians(endRadians),
            endAngle: .radians(startRadians),
            clockwise: false
        )
        path.closeSubpath()
        return path
    }

    private func drawOverlapBoundaryMarkers(
        context: inout GraphicsContext,
        cx: CGFloat, cy: CGFloat,
        innerRadius: CGFloat, outerRadius: CGFloat,
        startAngle: Double, endAngle: Double,
        color: Color
    ) {
        for angle in [startAngle, endAngle] {
            let radians = angle * .pi / 180
            var path = Path()
            path.move(to: CGPoint(x: cx + innerRadius * cos(radians), y: cy + innerRadius * sin(radians)))
            path.addLine(to: CGPoint(x: cx + outerRadius * cos(radians), y: cy + outerRadius * sin(radians)))
            context.stroke(path, with: .color(color), lineWidth: 2.5)
        }
    }
}

// Simple wrapping legend layout for diagram chips.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        return CGSize(width: maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
    }
}
