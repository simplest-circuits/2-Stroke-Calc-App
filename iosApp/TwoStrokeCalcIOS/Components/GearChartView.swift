import SwiftUI
import sharedKit

/// Bar chart of shift speeds per gear (aligned with Android bar chart mode).
struct GearChartView: View {
    @Environment(\.themeColors) private var colors
    let stages: [GearStageResult]
    let outputType: GearOutputType

    private var chartValues: [(label: String, value: Double)] {
        stages.map { stage in
            (
                label: L.tf("gear_chart_axis_gear_tick", String(stage.stageNumber)),
                value: stage.shiftSpeedKmh
            )
        }
    }

    private var maxValue: Double {
        max(chartValues.map(\.value).max() ?? 1, 1)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            GeometryReader { geometry in
                let barWidth = max(
                    (geometry.size.width - CGFloat(chartValues.count + 1) * 8) / CGFloat(max(chartValues.count, 1)),
                    12
                )
                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(Array(chartValues.enumerated()), id: \.offset) { index, item in
                        VStack(spacing: 6) {
                            Text(formatValue(item.value))
                                .font(.caption2.monospacedDigit())
                                .foregroundStyle(colors.onSurfaceVariant)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                            RoundedRectangle(cornerRadius: 6)
                                .fill(barColor(index))
                                .frame(
                                    width: barWidth,
                                    height: max(12, CGFloat(item.value / maxValue) * (geometry.size.height - 36))
                                )
                            Text(item.label)
                                .font(.caption2)
                                .foregroundStyle(colors.onSurfaceVariant)
                        }
                        .frame(maxHeight: .infinity, alignment: .bottom)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
            }
            .frame(height: max(140, CGFloat(chartValues.count) * 28))

            Text(
                outputType == .outputRpm
                    ? L.t("gear_chart_axis_output_rpm")
                    : L.t("gear_chart_axis_speed")
            )
            .font(.caption)
            .foregroundStyle(colors.onSurfaceVariant)
        }
        .padding(12)
        .background(colors.surfaceVariant.opacity(0.35))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func formatValue(_ value: Double) -> String {
        fmt(value, decimals: outputType == .outputRpm ? 0 : 1)
    }

    private func barColor(_ index: Int) -> Color {
        let palette: [Color] = [
            colors.primary,
            Color(red: 0.26, green: 0.65, blue: 0.96),
            Color(red: 0.40, green: 0.73, blue: 0.42),
            Color(red: 1.00, green: 0.65, blue: 0.15),
            Color(red: 0.94, green: 0.33, blue: 0.31),
            Color(red: 0.61, green: 0.35, blue: 0.71),
        ]
        return palette[index % palette.count].opacity(0.85)
    }
}
