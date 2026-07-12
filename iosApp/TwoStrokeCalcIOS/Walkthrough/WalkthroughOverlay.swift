import SwiftUI

struct WalkthroughOverlay: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @Binding var stepIndex: Int
    let onFinish: () -> Void

    @State private var targetRect: CGRect?

    private var steps: [WalkthroughStepModel] { WalkthroughBuilder.steps(for: appState) }
    private var step: WalkthroughStepModel { steps[min(stepIndex, steps.count - 1)] }

    var body: some View {
        GeometryReader { overlayProxy in
            ZStack {
                WalkthroughCoachmarkOverlay(targetRect: targetRect, cornerRadius: 12)
                VStack {
                    Spacer()
                    card
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                }
            }
            .onPreferenceChange(WalkthroughAnchorKey.self) { anchors in
                updateTarget(anchors: anchors, in: overlayProxy)
            }
        }
        .coordinateSpace(name: WalkthroughCoordinateSpace.name)
        .onAppear { navigateForStep(step) }
        .onChange(of: stepIndex) { new in navigateForStep(steps[new]) }
    }

    private var card: some View {
        VStack(spacing: 16) {
            Text(step.title)
                .font(.title3.bold())
                .multilineTextAlignment(.center)
            Text(step.body)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(colors.onSurfaceVariant)
            Text(String(format: S.walkthroughStepCounter, stepIndex + 1, steps.count))
                .font(.caption)
                .foregroundStyle(colors.onSurfaceVariant)
            HStack {
                Button(S.walkthroughSkip, action: onFinish)
                Spacer()
                if stepIndex > 0 {
                    Button(S.walkthroughBack) { stepIndex -= 1 }
                }
                Button(stepIndex >= steps.count - 1 ? S.walkthroughFinish : S.walkthroughNext) {
                    if stepIndex >= steps.count - 1 {
                        onFinish()
                    } else {
                        stepIndex += 1
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(colors.primary)
            }
        }
        .padding(24)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
    }

    private func navigateForStep(_ step: WalkthroughStepModel) {
        if let tab = step.targetTab {
            appState.selectedTab = tab
        }
    }

    private func updateTarget(anchors: [String: Anchor<CGRect>], in proxy: GeometryProxy) {
        guard let key = step.targetKey, let anchor = anchors[key] else {
            targetRect = nil
            return
        }
        targetRect = proxy[anchor]
    }
}

enum WalkthroughCoordinateSpace {
    static let name = "walkthrough"
}

private extension WalkthroughStepModel {
    var targetKey: String? {
        switch id {
        case "calc_nav", "calc_free", "calc_locked", "calc_list": return WalkthroughTargetIds.calculatorTab
        case "vehicles", "vehicles_add": return WalkthroughTargetIds.vehiclesTab
        case "settings", "pro", "appearance": return WalkthroughTargetIds.settingsTab
        default: return nil
        }
    }
}
