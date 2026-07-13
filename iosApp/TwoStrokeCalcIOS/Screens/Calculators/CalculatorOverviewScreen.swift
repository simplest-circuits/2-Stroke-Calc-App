import SwiftUI
import sharedKit

private enum CalculatorOverviewViewMode: String {
    case list
    case grid
}

struct CalculatorOverviewScreen: View {
    @Environment(\.themeColors) private var colors

    let visibleCalculators: [CalculatorId]
    let isReadOnly: (CalculatorId) -> Bool
    let onSelect: (CalculatorId) -> Void

    @State private var viewMode: CalculatorOverviewViewMode = .list

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            overviewHeader

            ScrollView {
                switch viewMode {
                case .list:
                    LazyVStack(spacing: 12) {
                        ForEach(visibleCalculators, id: \.name) { id in
                            CalculatorOverviewListCard(
                                calculatorId: id,
                                isReadOnly: isReadOnly(id),
                                onSelect: { onSelect(id) }
                            )
                        }
                    }
                case .grid:
                    LazyVGrid(
                        columns: [GridItem(.flexible()), GridItem(.flexible())],
                        spacing: 12
                    ) {
                        ForEach(visibleCalculators, id: \.name) { id in
                            CalculatorOverviewGridCard(
                                calculatorId: id,
                                isReadOnly: isReadOnly(id),
                                onSelect: { onSelect(id) }
                            )
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
    }

    private var overviewHeader: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(S.navCalculator)
                    .font(.title2.bold())
                    .foregroundStyle(colors.onBackground)
                Spacer()
                Button {
                    viewMode = viewMode == .list ? .grid : .list
                } label: {
                    Image(systemName: viewMode == .list ? "square.grid.2x2" : "list.bullet")
                        .foregroundStyle(colors.onSurfaceVariant)
                        .padding(8)
                }
                .accessibilityLabel(viewMode == .list ? L.t("cd_calculator_overview_grid") : L.t("cd_calculator_overview_list"))
            }
            Text(S.calculatorOverviewSubtitle)
                .font(.subheadline)
                .foregroundStyle(colors.onSurfaceVariant)
        }
    }
}

private struct CalculatorOverviewListCard: View {
    @Environment(\.themeColors) private var colors
    let calculatorId: CalculatorId
    let isReadOnly: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 16) {
                CalculatorOverviewIcon(calculatorId: calculatorId)
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(S.calculatorTab(calculatorId))
                            .font(.headline)
                            .foregroundStyle(colors.onBackground)
                        Spacer(minLength: 0)
                        if isReadOnly {
                            ProBadge()
                        }
                    }
                    Text(S.calculatorDescription(calculatorId))
                        .font(.caption)
                        .foregroundStyle(colors.onSurfaceVariant)
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius)
                    .stroke(colors.outline.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct CalculatorOverviewGridCard: View {
    @Environment(\.themeColors) private var colors
    let calculatorId: CalculatorId
    let isReadOnly: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            VStack(spacing: 8) {
                CalculatorOverviewIcon(calculatorId: calculatorId)
                if isReadOnly {
                    ProBadge()
                }
                Text(S.calculatorTab(calculatorId))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(colors.onBackground)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius)
                    .stroke(colors.outline.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct CalculatorOverviewIcon: View {
    @Environment(\.themeColors) private var colors
    let calculatorId: CalculatorId

    var body: some View {
        Image(systemName: systemImage)
            .font(.title2)
            .foregroundStyle(colors.primary)
            .frame(width: 40, height: 40)
    }

    private var systemImage: String {
        switch calculatorId {
        case .timing: return "timer"
        case .portArea: return "square.grid.3x3"
        case .ignition: return "bolt.fill"
        case .dcCable: return "cable.connector"
        case .fluid: return "drop.fill"
        case .compression: return "arrow.up.and.down.circle"
        case .squishBand: return "circle.circle"
        case .meanPressure: return "gauge.with.needle"
        case .gear: return "gearshape.2"
        case .exhaust: return "wind"
        case .counterweight: return "scalemass"
        case .variatorWeight: return "circle.hexagongrid"
        case .fuelMix: return "fuelpump"
        case .carbJet: return "aqi.medium"
        case .dynoInertia: return "circle.dashed"
        case .vehicleDynamics: return "car.side"
        default: return "function"
        }
    }
}

private struct ProBadge: View {
    @Environment(\.themeColors) private var colors

    var body: some View {
        Text("Pro")
            .font(.caption2.bold())
            .foregroundStyle(colors.primary)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(colors.primary.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}
