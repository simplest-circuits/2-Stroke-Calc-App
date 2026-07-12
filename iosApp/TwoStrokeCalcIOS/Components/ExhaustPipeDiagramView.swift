import SwiftUI

/// Vereinfachte Resorohr-Skizze (Diffusor-Segmente).
struct ExhaustPipeDiagramView: View {
    @Environment(\.themeColors) private var colors
    let headerLength: Double
    let diffuserLength: Double
    let tailLength: Double

    var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            let baseline = h * 0.55
            var path = Path()
            path.move(to: CGPoint(x: 16, y: baseline))
            path.addLine(to: CGPoint(x: 16 + w * 0.15, y: baseline))
            path.addLine(to: CGPoint(x: 16 + w * 0.55, y: baseline - h * 0.18))
            path.addLine(to: CGPoint(x: w - 16, y: baseline - h * 0.22))
            context.stroke(path, with: .color(colors.primary), lineWidth: 3)
            context.draw(Text("Header").font(.caption2), at: CGPoint(x: w * 0.12, y: baseline + 14))
            context.draw(Text("Diffusor").font(.caption2), at: CGPoint(x: w * 0.45, y: baseline - h * 0.28))
            context.draw(Text("Tail").font(.caption2), at: CGPoint(x: w * 0.82, y: baseline - h * 0.32))
        }
        .frame(height: 120)
        .background(colors.surfaceVariant.opacity(0.35))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(alignment: .bottomLeading) {
            Text(String(format: "L: %.0f · D: %.0f · T: %.0f mm", headerLength, diffuserLength, tailLength))
                .font(.caption2)
                .foregroundStyle(colors.onSurfaceVariant)
                .padding(8)
        }
    }
}
