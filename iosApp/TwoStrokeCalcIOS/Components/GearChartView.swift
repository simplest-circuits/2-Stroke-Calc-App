import SwiftUI

/// Geschwindigkeits-/Drehzahl-Linien pro Gang (vereinfacht).
struct GearChartView: View {
    @Environment(\.themeColors) private var colors
    let speedLines: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Canvas { context, size in
                let step = size.height / CGFloat(max(speedLines.count, 1) + 1)
                for (index, line) in speedLines.prefix(6).enumerated() {
                    let y = step * CGFloat(index + 1)
                    var path = Path()
                    path.move(to: CGPoint(x: 12, y: y))
                    path.addLine(to: CGPoint(x: size.width - 12, y: y - 8))
                    context.stroke(path, with: .color(colors.primary.opacity(0.7 - Double(index) * 0.08)), lineWidth: 2)
                    context.draw(Text(line).font(.caption2), at: CGPoint(x: 16, y: y - 12))
                }
            }
            .frame(height: max(120, CGFloat(speedLines.count) * 22))
        }
        .padding(8)
        .background(colors.surfaceVariant.opacity(0.35))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
