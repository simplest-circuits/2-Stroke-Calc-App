import SwiftUI
import sharedKit

private struct SegmentColors {
    let fill: Color
    let stroke: Color
}

private struct SegmentLayout {
    let index: Int
    let left: CGFloat
    let right: CGFloat
    let centerY: CGFloat
    let startRadius: CGFloat
    let endRadius: CGFloat
}

private struct PipeDrawMetrics {
    let startX: CGFloat
    let centerY: CGFloat
    let scale: CGFloat
    let contentWidth: CGFloat
}

private let paddingHFraction: CGFloat = 0.04
private let paddingVFraction: CGFloat = 0.12
private let minPipeAspectRatio: CGFloat = 3
private let maxPipeAspectRatio: CGFloat = 12

private let segmentColorMap: [ExpansionChamberSegmentId: SegmentColors] = [
    .header: SegmentColors(fill: Color(red: 0.47, green: 0.56, blue: 0.61), stroke: Color(red: 0.33, green: 0.43, blue: 0.48)),
    .diffuser1: SegmentColors(fill: Color(red: 0.26, green: 0.65, blue: 0.96), stroke: Color(red: 0.08, green: 0.40, blue: 0.75)),
    .diffuser2: SegmentColors(fill: Color(red: 0.16, green: 0.71, blue: 0.96), stroke: Color(red: 0.01, green: 0.47, blue: 0.74)),
    .diffuser3: SegmentColors(fill: Color(red: 0.15, green: 0.78, blue: 0.85), stroke: Color(red: 0.00, green: 0.51, blue: 0.56)),
    .belly: SegmentColors(fill: Color(red: 0.40, green: 0.73, blue: 0.42), stroke: Color(red: 0.18, green: 0.49, blue: 0.20)),
    .baffle: SegmentColors(fill: Color(red: 1.00, green: 0.65, blue: 0.15), stroke: Color(red: 0.94, green: 0.42, blue: 0.00)),
    .stinger: SegmentColors(fill: Color(red: 0.94, green: 0.33, blue: 0.31), stroke: Color(red: 0.78, green: 0.16, blue: 0.16)),
]

private func pipeAspectRatio(_ result: ExpansionChamberResult) -> CGFloat {
    let maxDiameter = result.segments
        .map { max($0.startDiameterMm, $0.endDiameterMm) }
        .max() ?? 1
    return CGFloat(min(max(result.totalLengthMm / maxDiameter, Double(minPipeAspectRatio)), Double(maxPipeAspectRatio)))
}

private func computeDrawMetrics(
    width: CGFloat,
    height: CGFloat,
    result: ExpansionChamberResult,
    horizontalPaddingFraction: CGFloat = paddingHFraction,
    verticalPaddingFraction: CGFloat = paddingVFraction,
    contentInsetPx: CGFloat = 0
) -> PipeDrawMetrics {
    let paddingH = width * horizontalPaddingFraction
    let paddingV = height * verticalPaddingFraction
    let drawWidth = max(width - paddingH * 2 - contentInsetPx * 2, 1)
    let drawHeight = max(height - paddingV * 2 - contentInsetPx * 2, 1)

    let totalLength = CGFloat(result.totalLengthMm)
    let maxDiameter = CGFloat(result.segments.map { max($0.startDiameterMm, $0.endDiameterMm) }.max() ?? 1)
    let scale = min(drawWidth / totalLength, drawHeight / maxDiameter)

    let contentWidth = totalLength * scale
    let contentHeight = maxDiameter * scale
    let startX = paddingH + (drawWidth - contentWidth) / 2
    let centerY = paddingV + (drawHeight - contentHeight) / 2 + contentHeight / 2

    return PipeDrawMetrics(startX: startX, centerY: centerY, scale: scale, contentWidth: contentWidth)
}

private func computeSegmentLayouts(
    width: CGFloat,
    height: CGFloat,
    result: ExpansionChamberResult,
    horizontalPaddingFraction: CGFloat = paddingHFraction,
    verticalPaddingFraction: CGFloat = paddingVFraction,
    contentInsetPx: CGFloat = 0
) -> [SegmentLayout] {
    let metrics = computeDrawMetrics(
        width: width,
        height: height,
        result: result,
        horizontalPaddingFraction: horizontalPaddingFraction,
        verticalPaddingFraction: verticalPaddingFraction,
        contentInsetPx: contentInsetPx
    )

    var x = metrics.startX
    return result.segments.enumerated().map { index, segment in
        let segmentWidth = CGFloat(segment.lengthMm) * metrics.scale
        let layout = SegmentLayout(
            index: index,
            left: x,
            right: x + segmentWidth,
            centerY: metrics.centerY,
            startRadius: CGFloat(segment.startDiameterMm / 2) * metrics.scale,
            endRadius: CGFloat(segment.endDiameterMm / 2) * metrics.scale
        )
        x += segmentWidth
        return layout
    }
}

func exhaustSegmentLabel(_ id: ExpansionChamberSegmentId) -> String {
    switch id {
    case .header: return L.t("exhaust_legend_header")
    case .diffuser1: return L.t("exhaust_segment_diffuser_1")
    case .diffuser2: return L.t("exhaust_segment_diffuser_2")
    case .diffuser3: return L.t("exhaust_segment_diffuser_3")
    case .belly: return L.t("exhaust_legend_belly")
    case .baffle: return L.t("exhaust_legend_baffle")
    case .stinger: return L.t("exhaust_legend_stinger")
    default: return id.name
    }
}

private func legendLabel(for id: ExpansionChamberSegmentId) -> String? {
    switch id {
    case .header: return L.t("exhaust_legend_header")
    case .diffuser1, .diffuser2, .diffuser3: return L.t("exhaust_legend_diffuser")
    case .belly: return L.t("exhaust_legend_belly")
    case .baffle: return L.t("exhaust_legend_baffle")
    case .stinger: return L.t("exhaust_legend_stinger")
    default: return nil
    }
}

/// Resorohr-Skizze aus berechneten Segmenten (wie Android ExhaustPipeDiagram).
struct ExhaustPipeDiagramView: View {
    @Environment(\.themeColors) private var colors
    let result: ExpansionChamberResult
    var selectedSegmentIndex: Int? = nil
    var horizontalPaddingFraction: CGFloat = paddingHFraction
    var verticalPaddingFraction: CGFloat = paddingVFraction
    var showLegend: Bool = true

    var body: some View {
        let aspect = pipeAspectRatio(result)
        VStack(alignment: .leading, spacing: 8) {
            GeometryReader { geometry in
                ExhaustPipeCanvas(
                    result: result,
                    selectedSegmentIndex: selectedSegmentIndex,
                    horizontalPaddingFraction: horizontalPaddingFraction,
                    verticalPaddingFraction: verticalPaddingFraction
                )
            }
            .aspectRatio(aspect, contentMode: .fit)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))

            if showLegend {
                ExhaustPipeLegend(segments: result.segments)
            }
        }
    }
}

private struct ExhaustPipeCanvas: View {
    let result: ExpansionChamberResult
    var selectedSegmentIndex: Int?
    var horizontalPaddingFraction: CGFloat
    var verticalPaddingFraction: CGFloat

    var body: some View {
        Canvas { context, size in
            let metrics = computeDrawMetrics(
                width: size.width,
                height: size.height,
                result: result,
                horizontalPaddingFraction: horizontalPaddingFraction,
                verticalPaddingFraction: verticalPaddingFraction
            )

            var x = metrics.startX
            for (index, segment) in result.segments.enumerated() {
                let segmentWidth = CGFloat(segment.lengthMm) * metrics.scale
                let startRadius = CGFloat(segment.startDiameterMm / 2) * metrics.scale
                let endRadius = CGFloat(segment.endDiameterMm / 2) * metrics.scale
                let segmentColors = segmentColorMap[segment.id] ?? SegmentColors(fill: .gray, stroke: .secondary)
                let selected = index == selectedSegmentIndex

                var path = Path()
                path.move(to: CGPoint(x: x, y: metrics.centerY - startRadius))
                path.addLine(to: CGPoint(x: x + segmentWidth, y: metrics.centerY - endRadius))
                path.addLine(to: CGPoint(x: x + segmentWidth, y: metrics.centerY + endRadius))
                path.addLine(to: CGPoint(x: x, y: metrics.centerY + startRadius))
                path.closeSubpath()

                context.fill(path, with: .color(segmentColors.fill))
                context.stroke(
                    path,
                    with: .color(selected ? .white : segmentColors.stroke),
                    lineWidth: selected ? 3 : 1.5
                )
                x += segmentWidth
            }

            var axis = Path()
            axis.move(to: CGPoint(x: metrics.startX, y: metrics.centerY))
            axis.addLine(to: CGPoint(x: metrics.startX + metrics.contentWidth, y: metrics.centerY))
            context.stroke(
                axis,
                with: .color(.gray.opacity(0.35)),
                style: StrokeStyle(lineWidth: 1, dash: [8, 6])
            )
        }
    }
}

private struct ExhaustPipeLegend: View {
    @Environment(\.themeColors) private var colors
    let segments: [ExpansionChamberSegment]

    private var legendItems: [(ExpansionChamberSegmentId, String)] {
        var seen = Set<String>()
        var items: [(ExpansionChamberSegmentId, String)] = []
        for segment in segments {
            guard let label = legendLabel(for: segment.id), !seen.contains(label) else { continue }
            seen.insert(label)
            items.append((segment.id, label))
        }
        return items
    }

    var body: some View {
        FlowLayout(spacing: 12, rowSpacing: 8) {
            ForEach(legendItems, id: \.1) { id, label in
                HStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(segmentColorMap[id]?.fill ?? .gray)
                        .frame(width: 14, height: 8)
                    Text(label)
                        .font(.caption2)
                        .foregroundStyle(colors.onSurfaceVariant)
                }
            }
        }
    }
}

/// Einfaches Flow-Layout für die Legende.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    var rowSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > maxWidth {
                x = 0
                y += rowHeight + rowSpacing
                rowHeight = 0
            }
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            totalWidth = max(totalWidth, x)
        }
        return CGSize(width: min(totalWidth, maxWidth), height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + rowSpacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
    }
}

/// Interaktive Vollbild-Vorschau im Querformat.
struct ExhaustPipeLandscapeView: View {
    @Environment(\.themeColors) private var colors
    let result: ExpansionChamberResult
    @State private var selectedSegmentIndex: Int? = nil

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(L.t("calculator_tab_exhaust"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(colors.onBackground)
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(L.t("exhaust_diagram_segment_hint"))
                        .font(.caption2)
                        .foregroundStyle(colors.onSurfaceVariant)
                    Text(L.t("exhaust_fullscreen_hint"))
                        .font(.caption2)
                        .foregroundStyle(colors.onSurfaceVariant)
                }
            }
            .padding(.horizontal, 8)

            HStack(alignment: .center, spacing: 6) {
                landscapeInfoPanel
                    .frame(width: 152)

                InteractiveExhaustPipeCanvas(
                    result: result,
                    selectedSegmentIndex: selectedSegmentIndex,
                    onSegmentSelected: { index in
                        if let index, selectedSegmentIndex == index {
                            selectedSegmentIndex = nil
                        } else {
                            selectedSegmentIndex = index
                        }
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
            }
            .padding(.horizontal, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(colors.background)
    }

    @ViewBuilder
    private var landscapeInfoPanel: some View {
        if let index = selectedSegmentIndex,
           index < result.segments.count {
            let segment = result.segments[index]
            landscapePanel {
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(segmentColorMap[segment.id]?.fill ?? colors.primary)
                        .frame(width: 10, height: 6)
                    Text(exhaustSegmentLabel(segment.id))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(colors.onSurface)
                }
                landscapeMetric(label: L.t("exhaust_segment_detail_length"), value: "\(fmt(segment.lengthMm, decimals: 0)) mm")
                landscapeMetric(label: "Ø Anfang", value: "\(fmt(segment.startDiameterMm, decimals: 1)) mm")
                landscapeMetric(label: "Ø Ende", value: "\(fmt(segment.endDiameterMm, decimals: 1)) mm")
            }
        } else {
            landscapePanel {
                Text(L.t("exhaust_fullscreen_summary_title"))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(colors.onSurface)
                landscapeMetric(label: L.t("exhaust_result_total_length"), value: "\(fmt(result.totalLengthMm, decimals: 0)) mm")
                landscapeMetric(label: L.t("exhaust_result_tuned_length"), value: "\(fmt(result.tunedLengthMm, decimals: 0)) mm")
                if result.diffuserStageCount > 1 {
                    landscapeMetric(label: L.t("exhaust_horn_coeff_label"), value: fmt(result.hornCoefficient, decimals: 2))
                }
                LandscapeDiameterMetrics(result: result)
            }
        }
    }

    private func landscapePanel<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            content()
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(colors.outline.opacity(0.35), lineWidth: 1)
        )
    }

    private func landscapeMetric(label: String, value: String) -> some View {
        HStack(spacing: 4) {
            Text("\(label):")
                .font(.caption2)
                .foregroundStyle(colors.onSurfaceVariant)
                .lineLimit(1)
            Text(value)
                .font(.caption2.weight(.medium))
                .foregroundStyle(colors.onSurface)
                .lineLimit(1)
        }
    }
}

private struct LandscapeDiameterMetrics: View {
    @Environment(\.themeColors) private var colors
    let result: ExpansionChamberResult

    var body: some View {
        let diameterEntries = result.diametersMm.asDoubleDict
        ForEach(diameterEntries.keys.sorted(), id: \.self) { key in
            if let value = diameterEntries[key] {
                HStack(spacing: 4) {
                    Text("\(key):")
                        .font(.caption2)
                        .foregroundStyle(colors.onSurfaceVariant)
                        .lineLimit(1)
                    Text("\(fmt(value, decimals: 1)) mm")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(colors.onSurface)
                        .lineLimit(1)
                }
            }
        }
    }
}

private struct InteractiveExhaustPipeCanvas: View {
    let result: ExpansionChamberResult
    var selectedSegmentIndex: Int?
    let onSegmentSelected: (Int?) -> Void

    var body: some View {
        GeometryReader { geometry in
            Canvas { context, size in
                let metrics = computeDrawMetrics(
                    width: size.width,
                    height: size.height,
                    result: result,
                    horizontalPaddingFraction: 0.08,
                    verticalPaddingFraction: 0.08,
                    contentInsetPx: 3
                )

                var x = metrics.startX
                for (index, segment) in result.segments.enumerated() {
                    let segmentWidth = CGFloat(segment.lengthMm) * metrics.scale
                    let startRadius = CGFloat(segment.startDiameterMm / 2) * metrics.scale
                    let endRadius = CGFloat(segment.endDiameterMm / 2) * metrics.scale
                    let segmentColors = segmentColorMap[segment.id] ?? SegmentColors(fill: .gray, stroke: .secondary)
                    let selected = index == selectedSegmentIndex

                    var path = Path()
                    path.move(to: CGPoint(x: x, y: metrics.centerY - startRadius))
                    path.addLine(to: CGPoint(x: x + segmentWidth, y: metrics.centerY - endRadius))
                    path.addLine(to: CGPoint(x: x + segmentWidth, y: metrics.centerY + endRadius))
                    path.addLine(to: CGPoint(x: x, y: metrics.centerY + startRadius))
                    path.closeSubpath()

                    context.fill(path, with: .color(segmentColors.fill))
                    context.stroke(
                        path,
                        with: .color(selected ? .white : segmentColors.stroke),
                        lineWidth: selected ? 3 : 1.5
                    )
                    x += segmentWidth
                }

                var axis = Path()
                axis.move(to: CGPoint(x: metrics.startX, y: metrics.centerY))
                axis.addLine(to: CGPoint(x: metrics.startX + metrics.contentWidth, y: metrics.centerY))
                context.stroke(
                    axis,
                    with: .color(.gray.opacity(0.35)),
                    style: StrokeStyle(lineWidth: 1, dash: [8, 6])
                )
            }
            .contentShape(Rectangle())
            .onTapGesture { location in
                let layouts = computeSegmentLayouts(
                    width: geometry.size.width,
                    height: geometry.size.height,
                    result: result,
                    horizontalPaddingFraction: 0.08,
                    verticalPaddingFraction: 0.08,
                    contentInsetPx: 3
                )
                let hit = hitTestSegment(at: location, layouts: layouts)
                if let hit, selectedSegmentIndex == hit {
                    onSegmentSelected(nil)
                } else {
                    onSegmentSelected(hit)
                }
            }
        }
    }
}

private func hitTestSegment(at tap: CGPoint, layouts: [SegmentLayout]) -> Int? {
    for layout in layouts {
        guard tap.x >= layout.left, tap.x <= layout.right else { continue }
        let segmentWidth = layout.right - layout.left
        guard segmentWidth > 0 else { continue }
        let t = (tap.x - layout.left) / segmentWidth
        let radiusAtTap = layout.startRadius + (layout.endRadius - layout.startRadius) * t
        if abs(tap.y - layout.centerY) <= radiusAtTap {
            return layout.index
        }
    }
    return nil
}
