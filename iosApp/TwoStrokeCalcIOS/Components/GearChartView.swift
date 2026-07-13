import SwiftUI
import sharedKit

private enum GearChartStyle: String {
    case line = "LINE"
    case bar = "BAR"
}

enum GearChartDisplayMode {
    case embedded
    case fullscreen
}

private struct GearShiftPoint: Identifiable {
    let stageNumber: Int
    let rpm: Double
    let speedKmh: Double
    let speedJumpKmh: Double?
    let rpmJump: Double?
    let color: Color

    var id: Int { stageNumber }
}

private struct ChartAxisTick {
    let fraction: CGFloat
    let label: String
}

private let gearChartInsetFraction: CGFloat = 0.02
private let gearRpmConstant = 16_000.0

// MARK: - Public chart

struct GearSpeedChartView: View {
    @Environment(\.themeColors) private var colors

    let result: GearCalculatorResult
    let shiftRpm: Double?
    let wheelCircumferenceMm: Double?
    let resonanceEntryRpm: Double?
    let resonancePeakRpm: Double?
    let displayMode: GearChartDisplayMode
    @Binding var chartStyleName: String
    @Binding var selectedShiftPointId: Int?

    private var isMultiSpeed: Bool { result.driveMode == .multiSpeed }

    private var chartStyle: GearChartStyle {
        let requested = GearChartStyle(rawValue: chartStyleName) ?? .line
        return isMultiSpeed ? requested : .line
    }

    private var gearColors: [Color] { gearStageColors(theme: colors) }

    private var chartModel: GearChartModel? {
        GearChartModel.build(
            result: result,
            shiftRpm: shiftRpm,
            wheelCircumferenceMm: wheelCircumferenceMm,
            resonanceEntryRpm: resonanceEntryRpm,
            resonancePeakRpm: resonancePeakRpm,
            chartStyle: chartStyle,
            gearColors: gearColors
        )
    }

    var body: some View {
        if let chartModel {
            switch displayMode {
            case .embedded:
                embeddedChart(model: chartModel)
            case .fullscreen:
                fullscreenChart(model: chartModel)
            }
        }
    }

    @ViewBuilder
    private func embeddedChart(model: GearChartModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if isMultiSpeed {
                HStack(spacing: 8) {
                    CalculatorFilterChip(
                        label: L.t("gear_chart_style_line"),
                        selected: chartStyle == .line
                    ) { chartStyleName = GearChartStyle.line.rawValue }
                    CalculatorFilterChip(
                        label: L.t("gear_chart_style_bar"),
                        selected: chartStyle == .bar
                    ) { chartStyleName = GearChartStyle.bar.rawValue }
                }
            }

            Text(chartSubtitle)
                .font(.caption)
                .foregroundStyle(colors.onSurfaceVariant)

            if isMultiSpeed {
                Text(L.t("gear_chart_shift_point_hint"))
                    .font(.caption)
                    .foregroundStyle(colors.onSurfaceVariant)
            }

            GearChartAxesView(
                model: model,
                expandToFill: false,
                selectedShiftPointId: $selectedShiftPointId
            )

            if let selectedId = selectedShiftPointId,
               let point = model.shiftPoints.first(where: { $0.stageNumber == selectedId }) {
                GearShiftPointDetailCard(point: point)
            }

            GearChartLegendView(
                model: model,
                horizontal: false
            )
        }
    }

    @ViewBuilder
    private func fullscreenChart(model: GearChartModel) -> some View {
        ZStack(alignment: .top) {
            GearChartAxesView(
                model: model,
                expandToFill: true,
                selectedShiftPointId: $selectedShiftPointId
            )

            if let selectedId = selectedShiftPointId,
               let point = model.shiftPoints.first(where: { $0.stageNumber == selectedId }) {
                GearShiftPointCompactBar(point: point)
                    .padding(.top, 4)
            }
        }
    }

    private var chartSubtitle: String {
        switch (isMultiSpeed, chartStyle) {
        case (true, .bar): return L.t("gear_chart_subtitle_bar")
        case (true, .line): return L.t("gear_chart_subtitle_line")
        case (false, _): return L.t("gear_chart_subtitle_line_fixed")
        }
    }
}

// MARK: - Landscape overlay

struct GearChartLandscapeView: View {
    @Environment(\.themeColors) private var colors

    let chartDataReady: Bool
    let result: GearCalculatorResult?
    let shiftRpm: Double?
    let wheelCircumferenceMm: Double?
    let resonanceEntryRpm: Double?
    let resonancePeakRpm: Double?
    @Binding var chartStyleName: String
    @Binding var selectedShiftPointId: Int?

    private var isMultiSpeed: Bool { result?.driveMode == GearDriveMode.multiSpeed }

    private var chartStyle: GearChartStyle {
        let requested = GearChartStyle(rawValue: chartStyleName) ?? .line
        return isMultiSpeed ? requested : .line
    }

    var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 4) {
                HStack {
                    Text(L.t("gear_chart_fullscreen_title"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(colors.onSurface)
                    Spacer()
                    CalculatorFilterChip(
                        label: L.t("gear_chart_style_line"),
                        selected: chartStyle == .line
                    ) { chartStyleName = GearChartStyle.line.rawValue }
                    if isMultiSpeed {
                        CalculatorFilterChip(
                            label: L.t("gear_chart_style_bar"),
                            selected: chartStyle == .bar
                        ) { chartStyleName = GearChartStyle.bar.rawValue }
                    }
                }
                .padding(.horizontal, 8)

                Text(L.t("gear_chart_fullscreen_hint"))
                    .font(.caption2)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.horizontal, 8)

                if chartDataReady, let result {
                    let gearColors = gearStageColors(theme: colors)
                    if let model = GearChartModel.build(
                        result: result,
                        shiftRpm: shiftRpm,
                        wheelCircumferenceMm: wheelCircumferenceMm,
                        resonanceEntryRpm: resonanceEntryRpm,
                        resonancePeakRpm: resonancePeakRpm,
                        chartStyle: chartStyle,
                        gearColors: gearColors
                    ) {
                        VStack(spacing: 8) {
                            GearSpeedChartView(
                                result: result,
                                shiftRpm: shiftRpm,
                                wheelCircumferenceMm: wheelCircumferenceMm,
                                resonanceEntryRpm: resonanceEntryRpm,
                                resonancePeakRpm: resonancePeakRpm,
                                displayMode: .fullscreen,
                                chartStyleName: $chartStyleName,
                                selectedShiftPointId: $selectedShiftPointId
                            )
                            .frame(maxWidth: .infinity, maxHeight: .infinity)

                            ScrollView(.horizontal, showsIndicators: false) {
                                GearChartLegendView(model: model, horizontal: true)
                            }
                            .padding(.horizontal, 8)
                        }
                    }
                } else {
                    Text(L.t("gear_chart_fullscreen_empty"))
                        .font(.body)
                        .foregroundStyle(colors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(24)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .padding(.vertical, 4)
        }
    }
}

// MARK: - Chart model

private struct GearChartModel {
    let result: GearCalculatorResult
    let chartStyle: GearChartStyle
    let gearColors: [Color]
    let chartMaxRpm: Double
    let chartMaxOutput: Double
    let shiftPoints: [GearShiftPoint]
    let lineSegments: [GearLineSegment]
    let dashedTransitions: [GearDashedTransition]
    let referenceRpmMarkers: [Double]
    let resonanceRange: (start: Double, end: Double)?
    let xTicks: [ChartAxisTick]
    let yTicks: [ChartAxisTick]
    let yAxisTitle: String
    let xAxisTitle: String
    let legendLabels: [String]
    let showResoBand: Bool
    let isMultiSpeed: Bool

    struct GearLineSegment {
        let xValues: [Double]
        let yValues: [Double]
        let color: Color
    }

    struct GearDashedTransition {
        let xValues: [Double]
        let yValues: [Double]
        let color: Color
    }

    static func build(
        result: GearCalculatorResult,
        shiftRpm: Double?,
        wheelCircumferenceMm: Double?,
        resonanceEntryRpm: Double?,
        resonancePeakRpm: Double?,
        chartStyle: GearChartStyle,
        gearColors: [Color]
    ) -> GearChartModel? {
        let isMultiSpeed = result.driveMode == .multiSpeed
        let wheel = wheelCircumferenceMm ?? 0

        func chartOutputValue(rpm: Double, ratio: Double) -> Double {
            sharedDouble(
                GearCalculator.shared.outputValue(
                    rpm: rpm,
                    gearRatio: ratio,
                    outputType: result.outputType,
                    wheelCircumferenceMm: wheel,
                    rpmConstant: gearRpmConstant
                )
            )
        }

        let referenceRpms: [Double] = if isMultiSpeed {
            result.stages.map(\.referenceRpm)
        } else {
            result.fixedReferenceRpms.map(\.asDouble)
        }

        let outputBuffer = sharedDouble(
            GearCalculator.shared.chartSegmentOutputBuffer(outputType: result.outputType)
        )

        let baseChartMaxRpm = niceChartMaximum(
            values: referenceRpms + [shiftRpm].compactMap { $0 },
            step: 500
        )

        let multiSpeedSegments: [GearCalculator.GearChartSegment]? =
            if isMultiSpeed, let shiftRpm {
                GearCalculator.shared.buildMultiSpeedChartSegments(
                    stages: result.stages,
                    shiftRpm: shiftRpm,
                    chartMaxRpm: baseChartMaxRpm,
                    outputType: result.outputType,
                    wheelCircumferenceMm: wheel,
                    rpmConstant: gearRpmConstant,
                    outputBuffer: outputBuffer
                )
            } else {
                nil
            }

        let chartMaxRpm: Double = if let multiSpeedSegments {
            niceChartMaximum(
                values: [baseChartMaxRpm] + multiSpeedSegments.flatMap { segment in
                    [segment.startRpm, segment.endRpm, segment.coreEndRpm]
                },
                step: 500
            )
        } else {
            baseChartMaxRpm
        }

        let segmentOutputs = multiSpeedSegments?.flatMap { segment in
            [segment.startOutput, segment.endOutput]
        } ?? []

        let chartMaxOutput = niceChartMaximum(
            values: result.stages.map { chartOutputValue(rpm: chartMaxRpm, ratio: $0.gearRatio) }
                + result.stages.flatMap { stage -> [Double] in
                    let refs = stage.speedsAtReferenceRpmKmh.map(\.asDouble)
                    if isMultiSpeed {
                        return refs + [stage.shiftSpeedKmh]
                    }
                    return refs
                }
                + segmentOutputs,
            step: result.outputType == .outputRpm ? 500 : 20
        )

        let shiftPoints: [GearShiftPoint] = if let shiftRpm, isMultiSpeed {
            result.stages.enumerated().map { index, stage in
                GearShiftPoint(
                    stageNumber: Int(stage.stageNumber),
                    rpm: shiftRpm,
                    speedKmh: stage.shiftSpeedKmh,
                    speedJumpKmh: stage.speedJumpKmh?.asDouble,
                    rpmJump: stage.rpmJump?.asDouble,
                    color: gearColors[index % gearColors.count]
                )
            }
        } else {
            []
        }

        var lineSegments: [GearLineSegment] = []
        var dashedTransitions: [GearDashedTransition] = []

        if chartStyle == .line {
            let gearSegments: [GearCalculator.GearChartSegment] = if let multiSpeedSegments {
                multiSpeedSegments
            } else {
                result.stages.map { stage in
                    let coreEndOutput = chartOutputValue(rpm: chartMaxRpm, ratio: stage.gearRatio)
                    let bufferedEndOutput = coreEndOutput + outputBuffer
                    let endRpm = sharedDouble(
                        GearCalculator.shared.engineRpmAtOutputValue(
                            outputValue: bufferedEndOutput,
                            gearRatio: stage.gearRatio,
                            outputType: result.outputType,
                            wheelCircumferenceMm: wheel,
                            rpmConstant: gearRpmConstant
                        )
                    )
                    return GearCalculator.GearChartSegment(
                        startRpm: 0,
                        startOutput: 0,
                        endRpm: endRpm,
                        endOutput: bufferedEndOutput,
                        coreStartRpm: 0,
                        coreStartOutput: 0,
                        coreEndRpm: chartMaxRpm,
                        coreEndOutput: coreEndOutput
                    )
                }
            }

            lineSegments = gearSegments.enumerated().map { index, segment in
                GearLineSegment(
                    xValues: [segment.startRpm, segment.endRpm],
                    yValues: [segment.startOutput, segment.endOutput],
                    color: gearColors[index % gearColors.count]
                )
            }

            if isMultiSpeed, let shiftRpm {
                dashedTransitions = gearSegments.dropFirst().enumerated().map { index, segment in
                    let previousStage = result.stages[index]
                    return GearDashedTransition(
                        xValues: [shiftRpm, segment.coreStartRpm],
                        yValues: [previousStage.shiftSpeedKmh, segment.coreStartOutput],
                        color: gearColors[(index + 1) % gearColors.count].opacity(0.45)
                    )
                }
            }
        }

        let referenceRpmMarkers = (shiftRpm.map { [$0] } ?? []) + referenceRpms
        let distinctMarkers = Array(Set(referenceRpmMarkers.filter { $0 >= 1 && $0 <= chartMaxRpm }))

        let resonanceRange: (Double, Double)? = {
            guard let pair = GearCalculator.shared.resolveResonanceRpmRange(
                result: result,
                fallbackEntryRpm: resonanceEntryRpm.map { KotlinDouble(value: $0) },
                fallbackPeakRpm: resonancePeakRpm.map { KotlinDouble(value: $0) }
            ) else { return nil }
            return (sharedDouble(pair.first), sharedDouble(pair.second))
        }()

        let showResoBand = chartStyle == .line && resonanceRange != nil

        let legendLabels: [String] = if chartStyle == .line {
            if isMultiSpeed {
                result.stages.map { L.tf("gear_chart_legend_gear", String($0.stageNumber)) }
            } else {
                [L.t("gear_chart_legend_fixed_ratio")]
            }
        } else {
            result.stages.map { gearChartReferenceLabel($0.referenceLabel) }
                + [L.t("gear_chart_legend_shift")]
        }

        return GearChartModel(
            result: result,
            chartStyle: chartStyle,
            gearColors: gearColors,
            chartMaxRpm: chartMaxRpm,
            chartMaxOutput: chartMaxOutput,
            shiftPoints: shiftPoints,
            lineSegments: lineSegments,
            dashedTransitions: dashedTransitions,
            referenceRpmMarkers: distinctMarkers.sorted(),
            resonanceRange: resonanceRange,
            xTicks: chartStyle == .line
                ? buildUniformRpmAxisTicks(chartMaxRpm: chartMaxRpm)
                : result.stages.map { stage in
                    ChartAxisTick(
                        fraction: CGFloat(stage.stageNumber) / CGFloat(result.stages.count + 1),
                        label: L.tf("gear_chart_axis_gear_tick", String(stage.stageNumber))
                    )
                },
            yTicks: buildOutputAxisTicks(maxValue: chartMaxOutput, outputType: result.outputType),
            yAxisTitle: result.outputType == .outputRpm
                ? L.t("gear_chart_axis_output_rpm")
                : L.t("gear_chart_axis_speed"),
            xAxisTitle: chartStyle == .line
                ? L.t("gear_chart_axis_rpm")
                : L.t("gear_chart_axis_gear"),
            legendLabels: legendLabels,
            showResoBand: showResoBand,
            isMultiSpeed: isMultiSpeed
        )
    }
}

// MARK: - Axes + canvas

private struct GearChartAxesView: View {
    @Environment(\.themeColors) private var colors

    let model: GearChartModel
    let expandToFill: Bool
    @Binding var selectedShiftPointId: Int?

    private let yAxisWidth: CGFloat = 48
    private let xAxisHeight: CGFloat = 56
    private let chartTopInset: CGFloat = 18
    private let hitRadius: CGFloat = 28

    var body: some View {
        GeometryReader { geometry in
            let chartWidth = geometry.size.width - yAxisWidth
            let chartHeight = geometry.size.height - xAxisHeight

            ZStack(alignment: .topLeading) {
                Text(model.yAxisTitle)
                    .font(.caption2)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .frame(width: yAxisWidth, alignment: .center)
                    .padding(.top, 2)

                ZStack(alignment: .topLeading) {
                    ForEach(Array(model.yTicks.enumerated()), id: \.offset) { _, tick in
                        Text(tick.label)
                            .font(.caption2.monospacedDigit())
                            .foregroundStyle(colors.onSurfaceVariant)
                            .frame(width: yAxisWidth - 4, alignment: .trailing)
                            .offset(
                                y: chartTopInset + plotAxisFraction(
                                    inset: gearChartInsetFraction,
                                    dataFraction: 1 - tick.fraction
                                ) * chartHeight - 7
                            )
                    }
                }
                .frame(width: yAxisWidth, height: chartHeight + chartTopInset, alignment: .topLeading)

                VStack(spacing: 0) {
                    ZStack {
                        GearChartCanvasView(
                            model: model,
                            selectedShiftPointId: $selectedShiftPointId
                        )
                        .frame(width: chartWidth, height: chartHeight)

                        ForEach(model.shiftPoints) { point in
                            let center = shiftPointCenter(
                                point: point,
                                chartSize: CGSize(width: chartWidth, height: chartHeight)
                            )
                            Circle()
                                .fill(Color.clear)
                                .frame(width: hitRadius * 2, height: hitRadius * 2)
                                .position(center)
                                .contentShape(Circle())
                                .onTapGesture {
                                    if selectedShiftPointId == point.stageNumber {
                                        selectedShiftPointId = nil
                                    } else {
                                        selectedShiftPointId = point.stageNumber
                                    }
                                }
                        }
                    }
                    .padding(.top, chartTopInset)

                    ZStack(alignment: .topLeading) {
                        ForEach(Array(model.xTicks.enumerated()), id: \.offset) { _, tick in
                            Text(tick.label)
                                .font(.caption2.monospacedDigit())
                                .foregroundStyle(colors.onSurfaceVariant)
                                .frame(width: 44)
                                .offset(
                                    x: clampAxisTickOffset(
                                        tickAreaWidth: chartWidth,
                                        labelWidth: 44,
                                        dataFraction: tick.fraction
                                    ),
                                    y: 4
                                )
                        }
                    }
                    .frame(width: chartWidth, height: xAxisHeight - 20, alignment: .topLeading)

                    Text(model.xAxisTitle)
                        .font(.caption2)
                        .foregroundStyle(colors.onSurfaceVariant)
                        .frame(width: chartWidth, alignment: .center)
                }
                .offset(x: yAxisWidth)
            }
        }
        .aspectRatio(expandToFill ? nil : 1.55, contentMode: .fit)
        .frame(maxWidth: .infinity, maxHeight: expandToFill ? .infinity : nil)
        .padding(.top, expandToFill ? 0 : 8)
    }

    private func shiftPointCenter(point: GearShiftPoint, chartSize: CGSize) -> CGPoint {
        let chartLeft = chartSize.width * gearChartInsetFraction
        let chartRight = chartSize.width * (1 - gearChartInsetFraction)
        let chartTop = chartSize.height * gearChartInsetFraction
        let chartBottom = chartSize.height * (1 - gearChartInsetFraction)

        switch model.chartStyle {
        case .line:
            let x = chartLeft + CGFloat(point.rpm / model.chartMaxRpm) * (chartRight - chartLeft)
            let y = chartBottom - CGFloat(point.speedKmh / model.chartMaxOutput) * (chartBottom - chartTop)
            return CGPoint(x: x, y: y)
        case .bar:
            let barGroupWidth = (chartRight - chartLeft) / CGFloat(model.result.stages.count + 1)
            let groupCenterX = chartLeft + barGroupWidth * CGFloat(point.stageNumber)
            let chartHeight = chartBottom - chartTop
            let shiftHeight = CGFloat(point.speedKmh / model.chartMaxOutput) * chartHeight
            return CGPoint(x: groupCenterX, y: chartBottom - shiftHeight / 2)
        }
    }
}

private struct GearChartCanvasView: View {
    @Environment(\.themeColors) private var colors

    let model: GearChartModel
    @Binding var selectedShiftPointId: Int?

    var body: some View {
        Canvas { context, size in
            let chartLeft = size.width * gearChartInsetFraction
            let chartRight = size.width * (1 - gearChartInsetFraction)
            let chartTop = size.height * gearChartInsetFraction
            let chartBottom = size.height * (1 - gearChartInsetFraction)
            let gridColor = colors.outline.opacity(0.45)

            if model.showResoBand, let range = model.resonanceRange {
                drawResonanceBand(
                    context: &context,
                    startRpm: range.start,
                    endRpm: range.end,
                    chartMaxRpm: model.chartMaxRpm,
                    chartLeft: chartLeft,
                    chartRight: chartRight,
                    chartTop: chartTop,
                    chartBottom: chartBottom
                )
            }

            drawGrid(
                context: &context,
                chartLeft: chartLeft,
                chartRight: chartRight,
                chartTop: chartTop,
                chartBottom: chartBottom,
                gridColor: gridColor
            )

            switch model.chartStyle {
            case .line:
                for segment in model.lineSegments {
                    drawLine(
                        context: &context,
                        xValues: segment.xValues,
                        yValues: segment.yValues,
                        xMax: model.chartMaxRpm,
                        yMax: model.chartMaxOutput,
                        color: segment.color,
                        chartLeft: chartLeft,
                        chartRight: chartRight,
                        chartTop: chartTop,
                        chartBottom: chartBottom,
                        strokeWidth: 3,
                        pointRadius: 4,
                        dashed: false
                    )
                }
                for transition in model.dashedTransitions {
                    drawLine(
                        context: &context,
                        xValues: transition.xValues,
                        yValues: transition.yValues,
                        xMax: model.chartMaxRpm,
                        yMax: model.chartMaxOutput,
                        color: transition.color,
                        chartLeft: chartLeft,
                        chartRight: chartRight,
                        chartTop: chartTop,
                        chartBottom: chartBottom,
                        strokeWidth: 1.5,
                        pointRadius: 0,
                        dashed: true
                    )
                }
                for rpm in model.referenceRpmMarkers {
                    let x = chartLeft + CGFloat(rpm / model.chartMaxRpm) * (chartRight - chartLeft)
                    var path = Path()
                    path.move(to: CGPoint(x: x, y: chartTop))
                    path.addLine(to: CGPoint(x: x, y: chartBottom))
                    context.stroke(
                        path,
                        with: .color(gridColor.opacity(0.8)),
                        style: StrokeStyle(lineWidth: 1, dash: [6, 6])
                    )
                }
            case .bar:
                drawBars(
                    context: &context,
                    chartLeft: chartLeft,
                    chartRight: chartRight,
                    chartTop: chartTop,
                    chartBottom: chartBottom
                )
            }

            for point in model.shiftPoints {
                let center = shiftPointCanvasCenter(
                    point: point,
                    size: size,
                    chartLeft: chartLeft,
                    chartRight: chartRight,
                    chartTop: chartTop,
                    chartBottom: chartBottom
                )
                drawShiftPointMarker(
                    context: &context,
                    center: center,
                    color: point.color,
                    selected: point.stageNumber == selectedShiftPointId
                )
            }
        }
    }

    private func shiftPointCanvasCenter(
        point: GearShiftPoint,
        size: CGSize,
        chartLeft: CGFloat,
        chartRight: CGFloat,
        chartTop: CGFloat,
        chartBottom: CGFloat
    ) -> CGPoint {
        switch model.chartStyle {
        case .line:
            let x = chartLeft + CGFloat(point.rpm / model.chartMaxRpm) * (chartRight - chartLeft)
            let y = chartBottom - CGFloat(point.speedKmh / model.chartMaxOutput) * (chartBottom - chartTop)
            return CGPoint(x: x, y: y)
        case .bar:
            let barGroupWidth = (chartRight - chartLeft) / CGFloat(model.result.stages.count + 1)
            let groupCenterX = chartLeft + barGroupWidth * CGFloat(point.stageNumber)
            let chartHeight = chartBottom - chartTop
            let shiftHeight = CGFloat(point.speedKmh / model.chartMaxOutput) * chartHeight
            return CGPoint(x: groupCenterX, y: chartBottom - shiftHeight / 2)
        }
    }

    private func drawBars(
        context: inout GraphicsContext,
        chartLeft: CGFloat,
        chartRight: CGFloat,
        chartTop: CGFloat,
        chartBottom: CGFloat
    ) {
        let chartWidth = chartRight - chartLeft
        let barGroupWidth = chartWidth / CGFloat(model.result.stages.count + 1)
        let barWidth = barGroupWidth * 0.18
        let chartHeight = chartBottom - chartTop
        guard model.chartMaxOutput > 0 else { return }

        for (stageIndex, stage) in model.result.stages.enumerated() {
            let groupCenterX = chartLeft + barGroupWidth * CGFloat(stageIndex + 1)
            for (speedIndex, speed) in stage.speedsAtReferenceRpmKmh.enumerated() {
                let value = speed.asDouble
                let barHeight = CGFloat(value / model.chartMaxOutput) * chartHeight
                let x = groupCenterX + (CGFloat(speedIndex) - 1.5) * barWidth
                let rect = CGRect(
                    x: x,
                    y: chartBottom - barHeight,
                    width: barWidth * 0.9,
                    height: barHeight
                )
                context.fill(
                    Path(roundedRect: rect, cornerRadius: 4),
                    with: .color(model.gearColors[speedIndex % model.gearColors.count].opacity(0.85))
                )
            }
            let shiftHeight = CGFloat(stage.shiftSpeedKmh / model.chartMaxOutput) * chartHeight
            let shiftRect = CGRect(
                x: groupCenterX - barWidth * 0.45,
                y: chartBottom - shiftHeight,
                width: barWidth * 0.9,
                height: shiftHeight
            )
            context.fill(
                Path(roundedRect: shiftRect, cornerRadius: 4),
                with: .color(Color.white.opacity(0.85))
            )
        }
    }
}

// MARK: - Legend & shift point cards

private struct GearChartLegendView: View {
    @Environment(\.themeColors) private var colors

    let model: GearChartModel
    let horizontal: Bool

    private var resoBandColor: Color { Color.orange.opacity(0.16) }

    var body: some View {
        let items = legendContent
        Group {
            if horizontal {
                HStack(spacing: 12) { items }
            } else {
                VStack(alignment: .leading, spacing: 4) { items }
            }
        }
    }

    @ViewBuilder
    private var legendContent: some View {
        if model.isMultiSpeed {
            legendRow(color: nil, label: L.t("gear_chart_legend_shift_point"), lineStyle: false, filledBand: false)
        }
        if model.showResoBand {
            legendRow(
                color: resoBandColor,
                label: L.t("gear_chart_legend_reso_band"),
                lineStyle: false,
                filledBand: true
            )
        }
        ForEach(Array(model.legendLabels.enumerated()), id: \.offset) { index, label in
            let color = model.gearColors[index % model.gearColors.count]
            legendRow(
                color: color,
                label: label,
                lineStyle: model.chartStyle == .line,
                filledBand: false
            )
        }
    }

    private func legendRow(color: Color?, label: String, lineStyle: Bool, filledBand: Bool) -> some View {
        HStack(spacing: horizontal ? 4 : 8) {
            GearChartLegendMarker(
                color: color ?? colors.primary,
                lineStyle: lineStyle,
                filledBand: filledBand,
                shiftPoint: color == nil
            )
            Text(label)
                .font(horizontal ? .caption2 : .caption)
                .foregroundStyle(colors.onSurfaceVariant)
                .lineLimit(1)
        }
    }
}

private struct GearChartLegendMarker: View {
    let color: Color
    let lineStyle: Bool
    let filledBand: Bool
    let shiftPoint: Bool

    var body: some View {
        Canvas { context, size in
            if shiftPoint {
                drawShiftPointMarker(context: &context, center: CGPoint(x: size.width / 2, y: size.height / 2), color: color, selected: false)
            } else if filledBand {
                context.fill(
                    Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 2),
                    with: .color(color)
                )
            } else if lineStyle {
                var path = Path()
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
                context.stroke(path, with: .color(color), style: StrokeStyle(lineWidth: 3))
                context.fill(
                    Path(ellipseIn: CGRect(x: size.width / 2 - 4, y: size.height / 2 - 4, width: 8, height: 8)),
                    with: .color(color)
                )
            } else {
                context.fill(
                    Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 2),
                    with: .color(color.opacity(0.85))
                )
            }
        }
        .frame(width: shiftPoint ? 16 : 20, height: shiftPoint ? 16 : 12)
    }
}

private struct GearShiftPointDetailCard: View {
    let point: GearShiftPoint

    var body: some View {
        CalculatorResultCard(L.tf("gear_chart_shift_point_title", String(point.stageNumber))) {
            CalculatorResultRow(
                label: L.t("gear_chart_shift_point_rpm_label"),
                value: L.tf("gear_result_rpm_value", fmt(point.rpm, decimals: 0))
            )
            CalculatorResultRow(
                label: L.t("gear_chart_shift_point_speed_label"),
                value: L.tf("gear_result_speed_kmh", fmt(point.speedKmh, decimals: 1))
            )
            if let speedJump = point.speedJumpKmh, let rpmJump = point.rpmJump {
                CalculatorResultRow(
                    label: L.t("gear_chart_shift_point_speed_jump_label"),
                    value: L.tf("gear_result_speed_kmh", fmt(speedJump, decimals: 1))
                )
                CalculatorResultRow(
                    label: L.t("gear_chart_shift_point_rpm_jump_label"),
                    value: L.tf("gear_result_rpm_value", fmt(rpmJump, decimals: 0))
                )
            } else {
                Text(L.t("gear_chart_shift_point_final"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct GearShiftPointCompactBar: View {
    @Environment(\.themeColors) private var colors

    let point: GearShiftPoint

    var body: some View {
        Text(summary)
            .font(.caption2)
            .foregroundStyle(colors.onSurface)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(colors.surface.opacity(0.92))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .lineLimit(2)
    }

    private var summary: String {
        var parts = [
            L.tf("gear_chart_shift_point_title", String(point.stageNumber)),
            L.tf("gear_result_rpm_value", fmt(point.rpm, decimals: 0)),
            L.tf("gear_result_speed_kmh", fmt(point.speedKmh, decimals: 1)),
        ]
        if let speedJump = point.speedJumpKmh, point.rpmJump != nil {
            parts.append("↑ \(L.tf("gear_result_speed_kmh", fmt(speedJump, decimals: 1)))")
        }
        return parts.joined(separator: " · ")
    }
}

// MARK: - Drawing helpers

private func drawResonanceBand(
    context: inout GraphicsContext,
    startRpm: Double,
    endRpm: Double,
    chartMaxRpm: Double,
    chartLeft: CGFloat,
    chartRight: CGFloat,
    chartTop: CGFloat,
    chartBottom: CGFloat
) {
    guard chartMaxRpm > 0 else { return }
    let clampedStart = min(max(startRpm, 0), chartMaxRpm)
    let clampedEnd = min(max(endRpm, 0), chartMaxRpm)
    guard clampedEnd > clampedStart else { return }

    let chartWidth = chartRight - chartLeft
    let xStart = chartLeft + CGFloat(clampedStart / chartMaxRpm) * chartWidth
    let xEnd = chartLeft + CGFloat(clampedEnd / chartMaxRpm) * chartWidth
    let rect = CGRect(x: xStart, y: chartTop, width: xEnd - xStart, height: chartBottom - chartTop)
    context.fill(Path(rect), with: .color(Color.orange.opacity(0.16)))
}

private func drawGrid(
    context: inout GraphicsContext,
    chartLeft: CGFloat,
    chartRight: CGFloat,
    chartTop: CGFloat,
    chartBottom: CGFloat,
    gridColor: Color
) {
    for step in 0...4 {
        let y = chartBottom - CGFloat(step) / 4 * (chartBottom - chartTop)
        var path = Path()
        path.move(to: CGPoint(x: chartLeft, y: y))
        path.addLine(to: CGPoint(x: chartRight, y: y))
        context.stroke(path, with: .color(gridColor), style: StrokeStyle(lineWidth: 1))
    }
    for index in 0..<5 {
        let x = chartLeft + CGFloat(index) / 4 * (chartRight - chartLeft)
        var path = Path()
        path.move(to: CGPoint(x: x, y: chartTop))
        path.addLine(to: CGPoint(x: x, y: chartBottom))
        context.stroke(path, with: .color(gridColor), style: StrokeStyle(lineWidth: 1))
    }
}

private func drawLine(
    context: inout GraphicsContext,
    xValues: [Double],
    yValues: [Double],
    xMax: Double,
    yMax: Double,
    color: Color,
    chartLeft: CGFloat,
    chartRight: CGFloat,
    chartTop: CGFloat,
    chartBottom: CGFloat,
    strokeWidth: CGFloat,
    pointRadius: CGFloat,
    dashed: Bool
) {
    guard !xValues.isEmpty, !yValues.isEmpty, xMax > 0, yMax > 0 else { return }

    let points = zip(xValues, yValues).map { xValue, yValue in
        CGPoint(
            x: chartLeft + CGFloat(xValue / xMax) * (chartRight - chartLeft),
            y: chartBottom - CGFloat(yValue / yMax) * (chartBottom - chartTop)
        )
    }

    var path = Path()
    path.move(to: points[0])
    for point in points.dropFirst() {
        path.addLine(to: point)
    }
    context.stroke(
        path,
        with: .color(color),
        style: StrokeStyle(lineWidth: strokeWidth, dash: dashed ? [12, 8] : [])
    )
    if pointRadius > 0 {
        for point in points.dropFirst() {
            context.fill(
                Path(ellipseIn: CGRect(
                    x: point.x - pointRadius,
                    y: point.y - pointRadius,
                    width: pointRadius * 2,
                    height: pointRadius * 2
                )),
                with: .color(color)
            )
        }
    }
}

private func drawShiftPointMarker(
    context: inout GraphicsContext,
    center: CGPoint,
    color: Color,
    selected: Bool
) {
    let outerRadius: CGFloat = selected ? 16 : 12
    context.fill(
        Path(ellipseIn: CGRect(
            x: center.x - outerRadius,
            y: center.y - outerRadius,
            width: outerRadius * 2,
            height: outerRadius * 2
        )),
        with: .color(Color.white.opacity(0.95))
    )
    let innerRadius = outerRadius - 3.5
    context.fill(
        Path(ellipseIn: CGRect(
            x: center.x - innerRadius,
            y: center.y - innerRadius,
            width: innerRadius * 2,
            height: innerRadius * 2
        )),
        with: .color(color)
    )
    let coreRadius: CGFloat = selected ? 5 : 4
    context.fill(
        Path(ellipseIn: CGRect(
            x: center.x - coreRadius,
            y: center.y - coreRadius,
            width: coreRadius * 2,
            height: coreRadius * 2
        )),
        with: .color(.white)
    )
    if selected {
        context.stroke(
            Path(ellipseIn: CGRect(
                x: center.x - outerRadius - 6,
                y: center.y - outerRadius - 6,
                width: (outerRadius + 6) * 2,
                height: (outerRadius + 6) * 2
            )),
            with: .color(color.opacity(0.35)),
            style: StrokeStyle(lineWidth: 2)
        )
    }
}

// MARK: - Utilities

private func gearStageColors(theme: ThemeColors) -> [Color] {
    [
        theme.primary,
        Color(red: 0.30, green: 0.69, blue: 0.31),
        Color(red: 1.00, green: 0.60, blue: 0.00),
        Color(red: 0.91, green: 0.12, blue: 0.39),
        Color(red: 0.61, green: 0.15, blue: 0.69),
        Color(red: 0.00, green: 0.74, blue: 0.83),
    ]
}

private func gearChartReferenceLabel(_ label: GearReferenceLabel) -> String {
    switch label {
    case .low: return L.t("gear_chart_legend_low")
    case .high: return L.t("gear_chart_legend_high")
    case .resonanceEntry: return L.t("gear_chart_legend_reso_entry")
    case .resonancePeak: return L.t("gear_chart_legend_reso_peak")
    default: return L.t("gear_result_ref_additional_short")
    }
}

private func niceChartMaximum(values: [Double], step: Double) -> Double {
    let maxValue = max(values.max() ?? step, step)
    return ceil(maxValue / step) * step
}

private func buildOutputAxisTicks(maxValue: Double, outputType: GearOutputType) -> [ChartAxisTick] {
    let steps = 4
    return (0...steps).map { step in
        let value = maxValue / Double(steps) * Double(step)
        let label = fmt(value, decimals: 0)
        return ChartAxisTick(fraction: CGFloat(step) / CGFloat(steps), label: label)
    }
}

private func buildUniformRpmAxisTicks(chartMaxRpm: Double) -> [ChartAxisTick] {
    let steps = 4
    return (0...steps).map { step in
        let rpm = chartMaxRpm / Double(steps) * Double(step)
        return ChartAxisTick(
            fraction: CGFloat(step) / CGFloat(steps),
            label: fmt(rpm, decimals: 0)
        )
    }
}

private func plotAxisFraction(inset: CGFloat, dataFraction: CGFloat) -> CGFloat {
    inset + (1 - 2 * inset) * min(max(dataFraction, 0), 1)
}

private func clampAxisTickOffset(tickAreaWidth: CGFloat, labelWidth: CGFloat, dataFraction: CGFloat) -> CGFloat {
    let center = tickAreaWidth * plotAxisFraction(inset: gearChartInsetFraction, dataFraction: dataFraction)
    let halfLabel = labelWidth / 2
    return min(max(center - halfLabel, 0), max(tickAreaWidth - labelWidth, 0))
}

private func sharedDouble(_ value: Any) -> Double {
    if let kotlinValue = value as? KotlinDouble {
        return kotlinValue.asDouble
    }
    if let number = value as? NSNumber {
        return number.doubleValue
    }
    if let doubleValue = value as? Double {
        return doubleValue
    }
    return 0
}
