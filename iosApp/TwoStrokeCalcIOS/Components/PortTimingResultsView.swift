import SwiftUI
import sharedKit

// MARK: - Port timing results

struct PortTimingResultsCard: View {
    @Environment(\.themeColors) private var colors
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    let result: PortTimingResult?
    let input: PortTimingInput?
    let roundResults: Bool
    let onRoundResultsChange: (Bool) -> Void

    var body: some View {
        CalculatorResultCard(L.t("pt_result_title")) {
            VStack(alignment: .leading, spacing: 10) {
                PortTimingRoundTile(
                    checked: roundResults,
                    onCheckedChange: onRoundResultsChange,
                    accent: colors.outline,
                    enabled: editingEnabled
                )

                if result == nil || input == nil {
                    CalculatorEmptyResultText(text: L.t("pt_not_calculated"))
                } else if let result, let input {
                    PortTimingResultsList(
                        rows: buildPortTimingResultRows(result: result, input: input),
                        colors: portTimingChannelColors(colors: colors)
                    )
                }
            }
        }
    }
}

// MARK: - Row models

private enum PortTimingChannel {
    case round
    case exhaust
    case blowdown
    case transfer
    case intake
}

private struct PortTimingChannelColors {
    let round: Color
    let exhaust: Color
    let blowdown: Color
    let transfer: Color
    let intake: Color

    func color(for channel: PortTimingChannel) -> Color {
        switch channel {
        case .round: return round
        case .exhaust: return exhaust
        case .blowdown: return blowdown
        case .transfer: return transfer
        case .intake: return intake
        }
    }
}

private struct PortTimingValueItem {
    let label: String
    let value: String
    let channel: PortTimingChannel
}

private struct PortTimingIntakeRow {
    let label: String
    let value: String
    let emphasized: Bool
}

private enum PortTimingResultRowItem {
    case value(PortTimingValueItem)
    case intakeGroup([PortTimingIntakeRow])
}

// MARK: - Builders

private func portTimingChannelColors(colors: ThemeColors) -> PortTimingChannelColors {
    PortTimingChannelColors(
        round: colors.outline,
        exhaust: colors.onSurfaceVariant,
        blowdown: colors.onSurface.opacity(colors.isDark ? 0.85 : 0.7),
        transfer: colors.primary,
        intake: colors.primaryContainer
    )
}

private func formatPortTimingDegrees(_ value: Double, round: Bool) -> String {
    formatIgnitionDegrees(value, round: round)
}

private func buildPortTimingResultRows(
    result: PortTimingResult,
    input: PortTimingInput
) -> [PortTimingResultRowItem] {
    var rows: [PortTimingResultRowItem] = []

    if let exhaust = result.exhaust {
        rows.append(
            .value(
                PortTimingValueItem(
                    label: L.t("pt_exhaust_duration"),
                    value: formatPortTimingDegrees(exhaust.duration, round: input.roundResults),
                    channel: .exhaust
                )
            )
        )
    }

    if let blowdown = result.blowdown, blowdown > 0.05 {
        rows.append(
            .value(
                PortTimingValueItem(
                    label: L.t("pt_blowdown"),
                    value: formatPortTimingDegrees(blowdown, round: input.roundResults),
                    channel: .blowdown
                )
            )
        )
    }

    if let transfer = result.transfer {
        rows.append(
            .value(
                PortTimingValueItem(
                    label: L.t("pt_transfer_duration"),
                    value: formatPortTimingDegrees(transfer.duration, round: input.roundResults),
                    channel: .transfer
                )
            )
        )
    }

    if input.intakeSystem == .rotaryValve, let intake = result.intake {
        rows.append(
            .intakeGroup([
                PortTimingIntakeRow(
                    label: L.t("pt_intake_before_tdc"),
                    value: formatPortTimingDegrees(intake.openBeforeTdc, round: input.roundResults),
                    emphasized: false
                ),
                PortTimingIntakeRow(
                    label: L.t("pt_intake_after_tdc"),
                    value: formatPortTimingDegrees(intake.closeAfterTdc, round: input.roundResults),
                    emphasized: false
                ),
                PortTimingIntakeRow(
                    label: L.t("pt_intake_duration"),
                    value: formatPortTimingDegrees(intake.duration, round: input.roundResults),
                    emphasized: true
                ),
            ])
        )
    }

    return rows
}

// MARK: - List

private struct PortTimingResultsList: View {
    let rows: [PortTimingResultRowItem]
    let colors: PortTimingChannelColors

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                switch row {
                case .value(let item):
                    PortTimingResultTile(item: item, channelColors: colors)
                case .intakeGroup(let intakeRows):
                    PortTimingIntakeGroupTile(rows: intakeRows, colors: colors)
                }
            }
        }
    }
}

// MARK: - Tiles

private struct PortTimingRoundTile: View {
    @Environment(\.themeColors) private var colors

    let checked: Bool
    let onCheckedChange: (Bool) -> Void
    let accent: Color
    let enabled: Bool

    var body: some View {
        PortTimingTileContainer(accent: accent) {
            Text(L.t("pt_round"))
                .font(.body)
                .foregroundStyle(colors.onSurfaceVariant)
                .frame(maxWidth: .infinity, alignment: .leading)
            Toggle("", isOn: Binding(
                get: { checked },
                set: { onCheckedChange($0) }
            ))
            .labelsHidden()
            .disabled(!enabled)
        }
        .padding(.vertical, 8)
        .padding(.trailing, 8)
    }
}

private struct PortTimingResultTile: View {
    @Environment(\.themeColors) private var themeColors

    let item: PortTimingValueItem
    let channelColors: PortTimingChannelColors

    var body: some View {
        PortTimingTileContainer(accent: channelColors.color(for: item.channel)) {
            VStack(alignment: .leading, spacing: 2) {
                Text(item.label)
                    .font(.body)
                    .foregroundStyle(themeColors.onSurfaceVariant)
                Text(item.value)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(themeColors.primary)
                    .monospacedDigit()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct PortTimingIntakeGroupTile: View {
    @Environment(\.themeColors) private var themeColors

    let rows: [PortTimingIntakeRow]
    let colors: PortTimingChannelColors

    private var accent: Color { colors.intake }

    var body: some View {
        PortTimingTileContainer(accent: accent, verticalAlignment: .top) {
            VStack(alignment: .leading, spacing: 10) {
                Text(L.t("pt_legend_intake"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(accent)

                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                    if index > 0 {
                        Divider()
                            .overlay(accent.opacity(0.22))
                    }
                    PortTimingIntakeGroupRow(row: row)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct PortTimingIntakeGroupRow: View {
    @Environment(\.themeColors) private var colors

    let row: PortTimingIntakeRow

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(row.label)
                .font(.body)
                .foregroundStyle(colors.onSurfaceVariant)
            Text(row.value)
                .font(row.emphasized ? .title2.weight(.bold) : .title3.weight(.semibold))
                .foregroundStyle(row.emphasized ? colors.primary : colors.onSurface)
                .monospacedDigit()
        }
    }
}

private struct PortTimingTileContainer<Content: View>: View {
    @Environment(\.themeColors) private var themeColors

    let accent: Color
    var verticalAlignment: VerticalAlignment = .center
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(alignment: verticalAlignment, spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(accent)
                .frame(width: 4)
            content()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accent.opacity(themeColors.isDark ? 0.16 : 0.10))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
