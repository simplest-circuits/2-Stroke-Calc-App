import SwiftUI
import sharedKit

struct ExhaustPortGeometryDiagramView: View {
    @Environment(\.themeColors) private var colors
    let layout: PortGeometryLayout

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Port-Geometrie")
                .font(.headline)
            Canvas { context, size in
                let padding = size.width * 0.06
                let topLabel = size.height * 0.14
                let bottomLabel = size.height * 0.12
                let drawWidth = size.width - padding * 2
                let drawHeight = size.height - padding * 2 - topLabel - bottomLabel
                let scale = min(
                    drawWidth / CGFloat(layout.totalTopSpanMm),
                    drawHeight / CGFloat(layout.portHeightMm)
                )
                let contentWidth = CGFloat(layout.totalTopSpanMm) * scale
                let contentHeight = CGFloat(layout.portHeightMm) * scale
                let originX = padding + (drawWidth - contentWidth) / 2
                let originY = padding + topLabel

                var wall = Path(CGRect(x: originX, y: originY, width: contentWidth, height: contentHeight))
                context.stroke(wall, with: .color(colors.onSurfaceVariant.opacity(0.35)), lineWidth: 2)

                var xTop = originX
                for (index, window) in layout.windows.enumerated() {
                    let topWidth = CGFloat(window.topWidthMm) * scale
                    let bottomWidth = CGFloat(window.bottomWidthMm) * scale
                    let height = CGFloat(window.heightMm) * scale
                    let path = trapezoidPath(
                        topLeftX: xTop,
                        topWidth: topWidth,
                        bottomWidth: bottomWidth,
                        topY: originY,
                        height: height
                    )
                    let fill = window.roleLabelKey == PortWindowRole.booster
                        ? colors.primary.opacity(0.35)
                        : colors.primary.opacity(0.55)
                    context.fill(path, with: .color(fill))
                    context.stroke(path, with: .color(colors.primary), lineWidth: 1.5)
                    xTop += topWidth
                    if index < layout.windows.count - 1 {
                        let bridge: CGFloat = 4
                        let bridgeRect = CGRect(x: xTop, y: originY, width: bridge, height: contentHeight)
                        context.fill(Path(bridgeRect), with: .color(colors.onSurfaceVariant.opacity(0.25)))
                        xTop += bridge
                    }
                }

                let otText = Text("OT").font(.caption2.bold())
                context.draw(otText, at: CGPoint(x: originX + contentWidth / 2, y: originY - topLabel / 2))
                let utText = Text("UT").font(.caption2.bold())
                context.draw(utText, at: CGPoint(x: originX + contentWidth / 2, y: originY + contentHeight + bottomLabel / 2))
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(2.2, contentMode: .fit)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
        }
    }

    private func trapezoidPath(topLeftX: CGFloat, topWidth: CGFloat, bottomWidth: CGFloat, topY: CGFloat, height: CGFloat) -> Path {
        let inset = (topWidth - bottomWidth) / 2
        var path = Path()
        path.move(to: CGPoint(x: topLeftX, y: topY))
        path.addLine(to: CGPoint(x: topLeftX + topWidth, y: topY))
        path.addLine(to: CGPoint(x: topLeftX + topWidth - inset, y: topY + height))
        path.addLine(to: CGPoint(x: topLeftX + inset, y: topY + height))
        path.closeSubpath()
        return path
    }
}
