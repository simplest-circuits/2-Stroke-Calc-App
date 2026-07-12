import SwiftUI

struct WalkthroughAnchorKey: PreferenceKey {
    static var defaultValue: [String: Anchor<CGRect>] = [:]
    static func reduce(value: inout [String: Anchor<CGRect>], nextValue: () -> [String: Anchor<CGRect>]) {
        value.merge(nextValue(), uniquingKeysWith: { $1 })
    }
}

extension View {
    func walkthroughAnchor(_ id: String) -> some View {
        anchorPreference(key: WalkthroughAnchorKey.self, value: .bounds) { anchor in
            [id: anchor]
        }
    }

    func walkthroughTargetSpace() -> some View {
        coordinateSpace(name: WalkthroughCoordinateSpace.name)
    }
}

struct WalkthroughCoachmarkOverlay: View {
    let targetRect: CGRect?
    let cornerRadius: CGFloat

    var body: some View {
        GeometryReader { geo in
            Canvas { context, size in
                var path = Path(CGRect(origin: .zero, size: size))
                if let targetRect {
                    let local = CGRect(
                        x: targetRect.minX,
                        y: targetRect.minY,
                        width: targetRect.width,
                        height: targetRect.height
                    ).insetBy(dx: -6, dy: -6)
                    path.addRoundedRect(in: local, cornerSize: CGSize(width: cornerRadius, height: cornerRadius))
                }
                path.fill(style: FillStyle(eoFill: true))
                context.fill(path, with: .color(.black.opacity(0.55)))
            }
            .ignoresSafeArea()
        }
    }
}

enum WalkthroughTargetIds {
    static let calculatorTab = "calc_tab"
    static let vehiclesTab = "vehicles_tab"
    static let settingsTab = "settings_tab"
    static let addVehicleFab = "add_vehicle_fab"
    static let proSection = "pro_section"
}
