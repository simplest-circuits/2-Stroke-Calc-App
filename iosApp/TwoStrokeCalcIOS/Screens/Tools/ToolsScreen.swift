import SwiftUI
import sharedKit

struct ToolsScreen: View {
    @Environment(\.themeColors) private var colors
    @State private var selectedTool: ToolId?

    private let tools: [ToolId] = [
        .rpmTachometer,
        .portTimingAssist,
        .vibrationAnalyzer,
        .gpsDyno,
    ]

    var body: some View {
        Group {
            if let selectedTool {
                toolDetail(selectedTool)
            } else {
                overview
            }
        }
    }

    private var overview: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(L.t("tools_overview_title"))
                    .font(.title2.bold())
                    .foregroundStyle(colors.onSurface)
                Text(L.t("tools_overview_subtitle"))
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)

                ForEach(tools, id: \.name) { tool in
                    Button {
                        selectedTool = tool
                    } label: {
                        ZStack(alignment: .topTrailing) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(title(for: tool))
                                    .font(.headline)
                                    .foregroundStyle(colors.onSurface)
                                Text(description(for: tool))
                                    .font(.caption)
                                    .foregroundStyle(colors.onSurfaceVariant)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.leading, 16)
                            .padding(.trailing, 28)
                            .padding(.vertical, 16)

                            BetaCornerRibbon()
                        }
                        .background(colors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(20)
        }
        .background(colors.background)
    }

    @ViewBuilder
    private func toolDetail(_ tool: ToolId) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Button {
                    selectedTool = nil
                } label: {
                    Image(systemName: "chevron.left")
                }
                .buttonStyle(.plain)
                Text(title(for: tool))
                    .font(.headline)
                Spacer()
            }
            .padding(.horizontal, 16)

            switch tool {
            case .rpmTachometer:
                RpmTachometerView()
            case .portTimingAssist:
                PortTimingAssistView()
            case .vibrationAnalyzer:
                VibrationAnalyzerView()
            case .gpsDyno:
                GpsDynoView()
            default:
                Text(L.t("tools_coming_soon"))
                    .padding()
            }
            Spacer()
        }
        .background(colors.background)
    }

    private func title(for tool: ToolId) -> String {
        switch tool {
        case .rpmTachometer: return L.t("tool_tab_rpm")
        case .portTimingAssist: return L.t("tool_tab_port_timing")
        case .vibrationAnalyzer: return L.t("tool_tab_vibration")
        case .gpsDyno: return L.t("tool_tab_gps_dyno")
        default: return tool.name
        }
    }

    private func description(for tool: ToolId) -> String {
        switch tool {
        case .rpmTachometer: return L.t("tool_overview_rpm_desc")
        case .portTimingAssist: return L.t("tool_overview_port_timing_desc")
        case .vibrationAnalyzer: return L.t("tool_overview_vibration_desc")
        case .gpsDyno: return L.t("tool_overview_gps_dyno_desc")
        default: return ""
        }
    }
}

private struct BetaCornerRibbon: View {
    @Environment(\.themeColors) private var colors

    var body: some View {
        Text(L.t("tools_beta_badge").uppercased())
            .font(.system(size: 8, weight: .bold))
            .tracking(0.8)
            .foregroundStyle(colors.onPrimary)
            .lineLimit(1)
            .padding(.horizontal, 28)
            .padding(.vertical, 2)
            .background(colors.primary)
            .fixedSize()
            .rotationEffect(.degrees(45))
            .offset(x: 20, y: 4)
            .allowsHitTesting(false)
    }
}

struct RpmTachometerView: View {
    @Environment(\.themeColors) private var colors
    @State private var rpmText = "—"
    @State private var message = L.t("tool_rpm_hint")

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(colors.onSurfaceVariant)
            Text(rpmText)
                .font(.system(size: 56, weight: .bold))
                .foregroundStyle(colors.onSurface)
            Text("RPM")
                .foregroundStyle(colors.onSurfaceVariant)
            Text(L.t("tools_coming_soon"))
                .font(.caption)
                .foregroundStyle(colors.onSurfaceVariant)
            Spacer()
        }
        .padding(20)
    }
}

struct PortTimingAssistView: View {
    @Environment(\.themeColors) private var colors
    var body: some View {
        Text(L.t("tool_overview_port_timing_desc"))
            .padding(20)
            .foregroundStyle(colors.onSurface)
    }
}

struct VibrationAnalyzerView: View {
    @Environment(\.themeColors) private var colors
    var body: some View {
        Text(L.t("tool_overview_vibration_desc"))
            .padding(20)
            .foregroundStyle(colors.onSurface)
    }
}

struct GpsDynoView: View {
    @Environment(\.themeColors) private var colors
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(L.t("tool_dyno_disclaimer"))
                .foregroundStyle(colors.onSurfaceVariant)
            Text(L.t("tool_overview_gps_dyno_desc"))
                .foregroundStyle(colors.onSurface)
        }
        .padding(20)
    }
}
