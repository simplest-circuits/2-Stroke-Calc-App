import SwiftUI
import sharedKit

struct CalculatorScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors

    var body: some View {
        Group {
            if let selected = appState.calculatorStack {
                calculatorDetail(selected)
            } else {
                CalculatorOverviewScreen(
                    visibleCalculators: appState.visibleCalculators,
                    isReadOnly: { appState.isCalculatorReadOnly($0) },
                    onSelect: { appState.calculatorStack = $0 }
                )
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(colors.background)
    }

    @ViewBuilder
    private func calculatorDetail(_ id: CalculatorId) -> some View {
        let editingEnabled = !appState.isCalculatorReadOnly(id)

        VStack(spacing: 0) {
            calculatorDetailTopBar(id)
            if !editingEnabled {
                ProReadOnlyBanner {
                    appState.proUpsellModule = ProModuleMapping.fromCalculator(id)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
            }
            CalculatorDetailView(calculatorId: id)
                .environment(\.calculatorEditingEnabled, editingEnabled)
        }
    }

    private func calculatorDetailTopBar(_ id: CalculatorId) -> some View {
        HStack(spacing: 4) {
            Button {
                appState.calculatorStack = nil
            } label: {
                Image(systemName: "chevron.left")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(colors.onBackground)
                    .padding(12)
            }
            .accessibilityLabel(S.calculatorBack)

            Text(S.calculatorTab(id))
                .font(.title3.weight(.semibold))
                .foregroundStyle(colors.onBackground)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 4)
    }
}
