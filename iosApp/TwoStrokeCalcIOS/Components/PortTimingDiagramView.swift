import SwiftUI
import sharedKit

/// Steuerzeit-Diagramm (vereinfachte SwiftUI-Portierung von PortTimingDiagram.kt).
struct PortTimingDiagramView: View {
    @Environment(\.themeColors) private var colors
    let result: PortTimingResult
    let showIntake: Bool

    private let tdcAngle: Double = 270
    private let bdcAngle: Double = 90

    var body: some View {
        VStack(spacing: 12) {
            Canvas { context, size in
                let cx = size.width / 2
                let cy = size.height / 2
                let radius = min(cx, cy) * 0.92
                let rect = CGRect(x: cx - radius, y: cy - radius, width: radius * 2, height: radius * 2)

                context.fill(Path(ellipseIn: rect), with: .color(colors.surfaceVariant.opacity(0.35)))
                context.stroke(Path(ellipseIn: rect), with: .color(colors.outline), lineWidth: 1)

                drawCrosshair(context: &context, cx: cx, cy: cy, radius: radius)
                drawMarker(context: &context, cx: cx, cy: cy, radius: radius, angle: tdcAngle, label: "OT")
                drawMarker(context: &context, cx: cx, cy: cy, radius: radius, angle: bdcAngle, label: "UT")

                if let exhaust = result.exhaust {
                    drawChannelArc(
                        context: &context, cx: cx, cy: cy, radius: radius,
                        inner: 0.58, outer: 0.74,
                        openBeforeBdc: exhaust.openBeforeBdc, duration: exhaust.duration,
                        color: .red.opacity(0.55)
                    )
                }
                if let transfer = result.transfer {
                    drawChannelArc(
                        context: &context, cx: cx, cy: cy, radius: radius,
                        inner: 0.36, outer: 0.52,
                        openBeforeBdc: transfer.openBeforeBdc, duration: transfer.duration,
                        color: .blue.opacity(0.55)
                    )
                }
                if showIntake, let intake = result.intake {
                    let start = tdcAngle - intake.openBeforeTdc
                    let end = tdcAngle + intake.closeAfterTdc
                    drawArc(context: &context, cx: cx, cy: cy, radius: radius, inner: 0.78, outer: 0.98,
                            startDeg: start, endDeg: end, color: .green.opacity(0.55))
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .frame(maxHeight: 280)

            HStack(spacing: 16) {
                legendDot(color: .red, label: "Auslass")
                legendDot(color: .blue, label: "Transfer")
                if showIntake { legendDot(color: .green, label: "Einlass") }
            }
            .font(.caption)
        }
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 10, height: 10)
            Text(label)
        }
    }

    private func drawCrosshair(context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat) {
        var path = Path()
        path.move(to: CGPoint(x: cx, y: cy - radius))
        path.addLine(to: CGPoint(x: cx, y: cy + radius))
        path.move(to: CGPoint(x: cx - radius, y: cy))
        path.addLine(to: CGPoint(x: cx + radius, y: cy))
        context.stroke(path, with: .color(colors.outline.opacity(0.5)), lineWidth: 1)
    }

    private func drawMarker(context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat, angle: Double, label: String) {
        let rad = angle * .pi / 180
        let x = cx + CGFloat(cos(rad)) * radius * 1.08
        let y = cy + CGFloat(sin(rad)) * radius * 1.08
        context.draw(Text(label).font(.caption2.bold()), at: CGPoint(x: x, y: y))
    }

    private func drawChannelArc(
        context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat,
        inner: CGFloat, outer: CGFloat,
        openBeforeBdc: Double, duration: Double,
        color: Color
    ) {
        let openAngle = bdcAngle - openBeforeBdc
        let closeAngle = openAngle + duration
        drawArc(context: &context, cx: cx, cy: cy, radius: radius, inner: inner, outer: outer,
                startDeg: openAngle, endDeg: closeAngle, color: color)
    }

    private func drawArc(
        context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, radius: CGFloat,
        inner: CGFloat, outer: CGFloat,
        startDeg: Double, endDeg: Double, color: Color
    ) {
        var path = Path()
        path.addArc(
            center: CGPoint(x: cx, y: cy),
            radius: radius * outer,
            startAngle: .degrees(startDeg),
            endAngle: .degrees(endDeg),
            clockwise: false
        )
        path.addArc(
            center: CGPoint(x: cx, y: cy),
            radius: radius * inner,
            startAngle: .degrees(endDeg),
            endAngle: .degrees(startDeg),
            clockwise: true
        )
        path.closeSubpath()
        context.fill(path, with: .color(color))
    }
}
