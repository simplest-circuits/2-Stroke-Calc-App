import SwiftUI
import sharedKit

private struct GearStageUiState: Equatable {
    var pinionText: String
    var gearText: String
    var referenceRpmText: String
}

private let gearMinStages = 2
private let gearMaxStages = 6
private let gearRpmConstant = 16_000.0

struct GearCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    @State private var driveModeName = GearDriveMode.multiSpeed.name
    @State private var outputTypeName = GearOutputType.vehicleSpeed.name
    @State private var presetName = GearPreset.vespaPx200.name
    @State private var chartStyleName = "LINE"
    @State private var selectedShiftPointId: Int?

    @State private var wheelCircumference = "1307"
    @State private var shiftRpm = "8000"
    @State private var referenceLowRpm = "3000"
    @State private var referenceHighRpm = "9500"
    @State private var resonanceEntryRpm = "6000"
    @State private var resonancePeakRpm = "8500"

    @State private var primaryPinion = "23"
    @State private var primaryGear = "64"
    @State private var fixedPinion = "12"
    @State private var fixedGear = "57"

    @State private var gearStages: [GearStageUiState] = [
        GearStageUiState(pinionText: "12", gearText: "57", referenceRpmText: "3000"),
        GearStageUiState(pinionText: "13", gearText: "42", referenceRpmText: "9500"),
        GearStageUiState(pinionText: "17", gearText: "38", referenceRpmText: "6000"),
        GearStageUiState(pinionText: "21", gearText: "35", referenceRpmText: "8500"),
    ]

    private let gearPresets: [GearPreset] = [.vespaPx200, .vespaPx125, .vespaPx80, .custom]
    private let driveModes: [GearDriveMode] = [.multiSpeed, .fixedTwoStage, .singleStage]

    private var driveMode: GearDriveMode {
        driveModes.first(where: { $0.name == driveModeName }) ?? .multiSpeed
    }

    private var outputType: GearOutputType {
        [GearOutputType.vehicleSpeed, .outputRpm].first(where: { $0.name == outputTypeName }) ?? .vehicleSpeed
    }

    private var preset: GearPreset {
        gearPresets.first(where: { $0.name == presetName }) ?? .vespaPx200
    }

    private var parsedStages: [GearStageConfig] {
        gearStages.compactMap { stage in
            guard let pinion = parsePositiveInt(stage.pinionText),
                  let gear = parsePositiveInt(stage.gearText),
                  let referenceRpm = parseDouble(stage.referenceRpmText),
                  referenceRpm > 0 else { return nil }
            return GearStageConfig(
                secondaryPinion: pinion,
                secondaryGear: gear,
                referenceRpm: referenceRpm
            )
        }
    }

    private var fixedReferenceRpms: [Double] {
        [referenceLowRpm, referenceHighRpm, resonanceEntryRpm, resonancePeakRpm].compactMap(parseDouble)
    }

    private var hasInput: Bool {
        switch driveMode {
        case .multiSpeed:
            return parseDouble(wheelCircumference) != nil
                && parseDouble(shiftRpm) != nil
                && parsePositiveInt(primaryPinion) != nil
                && parsePositiveInt(primaryGear) != nil
                && parsedStages.count == gearStages.count
                && parsedStages.count >= gearMinStages
                && parsedStages.count <= gearMaxStages
        case .fixedTwoStage:
            return parsePositiveInt(fixedPinion) != nil
                && parsePositiveInt(fixedGear) != nil
                && fixedReferenceRpms.count == 4
                && parsePositiveInt(primaryPinion) != nil
                && parsePositiveInt(primaryGear) != nil
                && (outputType == .outputRpm || parseDouble(wheelCircumference) != nil)
        case .singleStage:
            return parsePositiveInt(fixedPinion) != nil
                && parsePositiveInt(fixedGear) != nil
                && fixedReferenceRpms.count == 4
                && (outputType == .outputRpm || parseDouble(wheelCircumference) != nil)
        default:
            return false
        }
    }

    private var result: GearCalculatorResult? {
        guard hasInput else { return nil }
        switch driveMode {
        case .multiSpeed:
            guard let wheel = parseDouble(wheelCircumference),
                  let shift = parseDouble(shiftRpm),
                  let pPin = parsePositiveInt(primaryPinion),
                  let pGear = parsePositiveInt(primaryGear) else { return nil }
            return GearCalculator.shared.calculate(
                input: GearCalculatorInput(
                    driveMode: .multiSpeed,
                    outputType: .vehicleSpeed,
                    wheelCircumferenceMm: wheel,
                    rpmConstant: gearRpmConstant,
                    shiftRpm: shift,
                    primaryPinion: pPin,
                    primaryGear: pGear,
                    stages: parsedStages,
                    fixedReferenceRpms: []
                )
            )
        case .fixedTwoStage:
            guard let pPin = parsePositiveInt(primaryPinion),
                  let pGear = parsePositiveInt(primaryGear),
                  let fPin = parsePositiveInt(fixedPinion),
                  let fGear = parsePositiveInt(fixedGear) else { return nil }
            return GearCalculator.shared.calculate(
                input: GearCalculatorInput(
                    driveMode: .fixedTwoStage,
                    outputType: outputType,
                    wheelCircumferenceMm: parseDouble(wheelCircumference) ?? 0,
                    rpmConstant: gearRpmConstant,
                    shiftRpm: 0,
                    primaryPinion: pPin,
                    primaryGear: pGear,
                    stages: [
                        GearStageConfig(
                            secondaryPinion: fPin,
                            secondaryGear: fGear,
                            referenceRpm: fixedReferenceRpms[0]
                        ),
                    ],
                    fixedReferenceRpms: fixedReferenceRpms.map { KotlinDouble(value: $0) }
                )
            )
        case .singleStage:
            guard let fPin = parsePositiveInt(fixedPinion),
                  let fGear = parsePositiveInt(fixedGear) else { return nil }
            return GearCalculator.shared.calculate(
                input: GearCalculatorInput(
                    driveMode: .singleStage,
                    outputType: outputType,
                    wheelCircumferenceMm: parseDouble(wheelCircumference) ?? 0,
                    rpmConstant: gearRpmConstant,
                    shiftRpm: 0,
                    primaryPinion: 1,
                    primaryGear: 1,
                    stages: [
                        GearStageConfig(
                            secondaryPinion: fPin,
                            secondaryGear: fGear,
                            referenceRpm: fixedReferenceRpms[0]
                        ),
                    ],
                    fixedReferenceRpms: fixedReferenceRpms.map { KotlinDouble(value: $0) }
                )
            )
        default:
            return nil
        }
    }

    private var isLandscape: Bool {
        verticalSizeClass == .compact
    }

    private var chartDataReady: Bool {
        guard hasInput, result != nil else { return false }
        switch driveMode {
        case .multiSpeed:
            return parseDouble(shiftRpm) != nil && parseDouble(wheelCircumference) != nil
        default:
            return outputType == .outputRpm || parseDouble(wheelCircumference) != nil
        }
    }

    var body: some View {
        Group {
            if isLandscape {
                GearChartLandscapeView(
                    chartDataReady: chartDataReady,
                    result: result,
                    shiftRpm: driveMode == .multiSpeed ? parseDouble(shiftRpm) : nil,
                    wheelCircumferenceMm: parseDouble(wheelCircumference),
                    resonanceEntryRpm: parseDouble(resonanceEntryRpm),
                    resonancePeakRpm: parseDouble(resonancePeakRpm),
                    chartStyleName: $chartStyleName,
                    selectedShiftPointId: $selectedShiftPointId
                )
            } else if !isLandscape {
                calculatorContent
            }
        }
    }

    private var calculatorContent: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: L.t("gear_calculator_title"),
                subtitle: L.t("gear_calculator_subtitle")
            )

            CalculatorSection(L.t("gear_section_drive_mode")) {
                CalculatorChoiceField(
                    label: "",
                    options: driveModes.map { (key: $0.name, title: gearDriveModeLabel($0)) },
                    selectedKey: driveMode.name,
                    supportingText: gearDriveModeHint(driveMode),
                    enabled: editingEnabled,
                    onOptionSelected: { key in
                        driveModeName = key
                        if key == GearDriveMode.multiSpeed.name {
                            outputTypeName = GearOutputType.vehicleSpeed.name
                        } else if key == GearDriveMode.singleStage.name {
                            outputTypeName = GearOutputType.outputRpm.name
                        }
                        markCustomPreset()
                    }
                )
            }

            if driveMode == .multiSpeed {
                CalculatorSection(L.t("gear_presets_label")) {
                    CalculatorChoiceField(
                        label: "",
                        options: gearPresets.map { (key: $0.name, title: gearPresetLabel($0)) },
                        selectedKey: preset.name,
                        enabled: editingEnabled,
                        onOptionSelected: { key in
                            if let selected = gearPresets.first(where: { $0.name == key }) {
                                applyPreset(selected)
                            }
                        }
                    )
                }
            }

            if driveMode != .multiSpeed {
                CalculatorSection(L.t("gear_section_output")) {
                    CalculatorChoiceField(
                        label: "",
                        options: [
                            (GearOutputType.vehicleSpeed.name, L.t("gear_output_vehicle_speed")),
                            (GearOutputType.outputRpm.name, L.t("gear_output_shaft_rpm")),
                        ],
                        selectedKey: outputType.name,
                        supportingText: outputType == .vehicleSpeed
                            ? L.t("gear_output_vehicle_speed_hint")
                            : L.t("gear_output_shaft_rpm_hint"),
                        enabled: editingEnabled,
                        onOptionSelected: {
                            outputTypeName = $0
                            markCustomPreset()
                        }
                    )
                }
            }

            if driveMode == .multiSpeed || outputType == .vehicleSpeed {
                CalculatorSection(L.t("gear_section_global")) {
                    if driveMode == .multiSpeed || outputType == .vehicleSpeed {
                        DecimalField(
                            label: L.t("gear_wheel_circumference_label"),
                            text: $wheelCircumference,
                            suffix: L.t("pt_mm"),
                            enabled: editingEnabled
                        )
                    }
                    if driveMode == .multiSpeed {
                        DecimalField(
                            label: L.t("gear_shift_rpm_label"),
                            text: $shiftRpm,
                            suffix: L.t("gear_unit_rpm"),
                            enabled: editingEnabled
                        )
                    }
                }
            }

            if driveMode != .singleStage {
                CalculatorSection(L.t("gear_section_primary")) {
                    CalculatorFieldRow {
                        DecimalField(
                            label: L.t("gear_primary_pinion_label"),
                            text: $primaryPinion,
                            suffix: L.t("gear_unit_teeth"),
                            enabled: editingEnabled
                        )
                        DecimalField(
                            label: L.t("gear_primary_gear_label"),
                            text: $primaryGear,
                            suffix: L.t("gear_unit_teeth"),
                            enabled: editingEnabled
                        )
                    }
                }
            }

            switch driveMode {
            case .multiSpeed:
                CalculatorSection(L.t("gear_section_stages")) {
                    Text(
                        L.tf(
                            "gear_stage_count_hint",
                            String(gearMinStages),
                            String(gearMaxStages)
                        )
                    )
                    .font(.caption)
                    .foregroundStyle(.secondary)

                    ForEach(Array(gearStages.enumerated()), id: \.offset) { index, stage in
                        GearStageInputBlock(
                            stageNumber: index + 1,
                            stage: stage,
                            canRemove: gearStages.count > gearMinStages,
                            enabled: editingEnabled,
                            onChange: { updated in
                                gearStages[index] = updated
                                markCustomPreset()
                            },
                            onRemove: {
                                gearStages.remove(at: index)
                                markCustomPreset()
                            }
                        )
                    }

                    if gearStages.count < gearMaxStages {
                        Button {
                            let last = gearStages.last
                            let defaultRpm = GearCalculator.shared.defaultReferenceRpm(index: Int32(gearStages.count))
                            gearStages.append(
                                GearStageUiState(
                                    pinionText: last?.pinionText ?? "",
                                    gearText: last?.gearText ?? "",
                                    referenceRpmText: String(Int(defaultRpm))
                                )
                            )
                            markCustomPreset()
                        } label: {
                            Label(L.t("gear_add_stage"), systemImage: "plus")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderless)
                        .disabled(!editingEnabled)
                    }
                }
            case .fixedTwoStage:
                CalculatorSection(L.t("gear_section_fixed_secondary")) {
                    GearSimpleRatioRow(
                        pinionText: $fixedPinion,
                        gearText: $fixedGear,
                        enabled: editingEnabled
                    )
                }
                fixedReferenceSection
            case .singleStage:
                CalculatorSection(L.t("gear_section_single_gear")) {
                    GearSimpleRatioRow(
                        pinionText: $fixedPinion,
                        gearText: $fixedGear,
                        enabled: editingEnabled
                    )
                }
                fixedReferenceSection
            default:
                EmptyView()
            }

            GearResultsCard(result: result, hasInput: hasInput, outputType: outputType)

            if chartDataReady, let result {
                CalculatorSection(L.t("gear_chart_title")) {
                    GearSpeedChartView(
                        result: result,
                        shiftRpm: driveMode == .multiSpeed ? parseDouble(shiftRpm) : nil,
                        wheelCircumferenceMm: parseDouble(wheelCircumference),
                        resonanceEntryRpm: parseDouble(resonanceEntryRpm),
                        resonancePeakRpm: parseDouble(resonancePeakRpm),
                        displayMode: .embedded,
                        chartStyleName: $chartStyleName,
                        selectedShiftPointId: $selectedShiftPointId
                    )
                }
            }
        }
    }

    private var fixedReferenceSection: some View {
        CalculatorSection(L.t("gear_section_fixed_reference")) {
            CalculatorFieldRow {
                DecimalField(
                    label: L.t("gear_reference_low_rpm_label"),
                    text: $referenceLowRpm,
                    suffix: L.t("gear_unit_rpm"),
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("gear_reference_high_rpm_label"),
                    text: $referenceHighRpm,
                    suffix: L.t("gear_unit_rpm"),
                    enabled: editingEnabled
                )
            }
            CalculatorFieldRow {
                DecimalField(
                    label: L.t("gear_resonance_entry_rpm_label"),
                    text: $resonanceEntryRpm,
                    suffix: L.t("gear_unit_rpm"),
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("gear_resonance_peak_rpm_label"),
                    text: $resonancePeakRpm,
                    suffix: L.t("gear_unit_rpm"),
                    enabled: editingEnabled
                )
            }
        }
    }

    private func markCustomPreset() {
        presetName = GearPreset.custom.name
    }

    private func applyPreset(_ selected: GearPreset) {
        presetName = selected.name
        guard selected != .custom, let values = gearPresetValues(selected) else { return }
        driveModeName = GearDriveMode.multiSpeed.name
        outputTypeName = GearOutputType.vehicleSpeed.name
        primaryPinion = String(values.primaryPinion)
        primaryGear = String(values.primaryGear)
        gearStages = values.stages.enumerated().map { index, stage in
            GearStageUiState(
                pinionText: String(stage.secondaryPinion),
                gearText: String(stage.secondaryGear),
                referenceRpmText: String(Int(GearCalculator.shared.defaultReferenceRpm(index: Int32(index))))
            )
        }
    }

    private func gearPresetValues(_ preset: GearPreset) -> GearPresetValues? {
        switch preset {
        case .vespaPx200:
            return GearPresetValues(
                primaryPinion: 23,
                primaryGear: 64,
                stages: [
                    GearPresetStage(secondaryPinion: 12, secondaryGear: 57),
                    GearPresetStage(secondaryPinion: 13, secondaryGear: 42),
                    GearPresetStage(secondaryPinion: 17, secondaryGear: 38),
                    GearPresetStage(secondaryPinion: 21, secondaryGear: 35),
                ]
            )
        case .vespaPx125:
            return GearPresetValues(
                primaryPinion: 21,
                primaryGear: 68,
                stages: [
                    GearPresetStage(secondaryPinion: 12, secondaryGear: 58),
                    GearPresetStage(secondaryPinion: 13, secondaryGear: 42),
                    GearPresetStage(secondaryPinion: 17, secondaryGear: 38),
                    GearPresetStage(secondaryPinion: 21, secondaryGear: 36),
                ]
            )
        case .vespaPx80:
            return GearPresetValues(
                primaryPinion: 20,
                primaryGear: 68,
                stages: [
                    GearPresetStage(secondaryPinion: 10, secondaryGear: 59),
                    GearPresetStage(secondaryPinion: 14, secondaryGear: 55),
                    GearPresetStage(secondaryPinion: 19, secondaryGear: 50),
                    GearPresetStage(secondaryPinion: 23, secondaryGear: 47),
                ]
            )
        default:
            return nil
        }
    }

    private func gearDriveModeLabel(_ mode: GearDriveMode) -> String {
        switch mode {
        case .multiSpeed: return L.t("gear_drive_mode_multi_speed")
        case .fixedTwoStage: return L.t("gear_drive_mode_fixed_two_stage")
        case .singleStage: return L.t("gear_drive_mode_single_stage")
        default: return mode.name
        }
    }

    private func gearDriveModeHint(_ mode: GearDriveMode) -> String {
        switch mode {
        case .multiSpeed: return L.t("gear_drive_mode_multi_speed_hint")
        case .fixedTwoStage: return L.t("gear_drive_mode_fixed_hint")
        case .singleStage: return L.t("gear_drive_mode_single_hint")
        default: return ""
        }
    }

    private func gearPresetLabel(_ preset: GearPreset) -> String {
        switch preset {
        case .vespaPx200: return L.t("gear_preset_px_200")
        case .vespaPx125: return L.t("gear_preset_px_125")
        case .vespaPx80: return L.t("gear_preset_px_80")
        case .custom: return L.t("gear_preset_custom")
        default: return preset.name
        }
    }
}

private struct GearSimpleRatioRow: View {
    @Binding var pinionText: String
    @Binding var gearText: String
    var enabled: Bool

    var body: some View {
        CalculatorFieldRow {
            DecimalField(
                label: L.t("gear_secondary_pinion_label"),
                text: $pinionText,
                suffix: L.t("gear_unit_teeth"),
                enabled: enabled
            )
            DecimalField(
                label: L.t("gear_secondary_gear_label"),
                text: $gearText,
                suffix: L.t("gear_unit_teeth"),
                enabled: enabled
            )
        }
    }
}

private struct GearStageInputBlock: View {
    @Environment(\.themeColors) private var colors
    let stageNumber: Int
    let stage: GearStageUiState
    let canRemove: Bool
    let enabled: Bool
    let onChange: (GearStageUiState) -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(L.tf("gear_stage_label", String(stageNumber)))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(colors.onSurface)
                Spacer()
                if canRemove {
                    Button(action: onRemove) {
                        Image(systemName: "minus.circle")
                    }
                    .buttonStyle(.borderless)
                    .disabled(!enabled)
                }
            }

            CalculatorFieldRow {
                DecimalField(
                    label: L.t("gear_secondary_pinion_label"),
                    text: binding(\.pinionText),
                    suffix: L.t("gear_unit_teeth"),
                    enabled: enabled
                )
                DecimalField(
                    label: L.t("gear_secondary_gear_label"),
                    text: binding(\.gearText),
                    suffix: L.t("gear_unit_teeth"),
                    enabled: enabled
                )
            }

            DecimalField(
                label: L.t("gear_stage_reference_rpm_label"),
                text: binding(\.referenceRpmText),
                suffix: L.t("gear_unit_rpm"),
                enabled: enabled
            )
        }
        .padding(.vertical, 4)
    }

    private func binding(_ keyPath: WritableKeyPath<GearStageUiState, String>) -> Binding<String> {
        Binding(
            get: { stage[keyPath: keyPath] },
            set: { newValue in
                var updated = stage
                updated[keyPath: keyPath] = newValue
                onChange(updated)
            }
        )
    }
}

private struct GearResultsCard: View {
    let result: GearCalculatorResult?
    let hasInput: Bool
    let outputType: GearOutputType

    var body: some View {
        CalculatorResultCard(L.t("gear_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("gear_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("gear_result_invalid"))
            } else if let result {
                if result.driveMode != .multiSpeed {
                    SecondaryResultText(
                        text: result.outputType == .outputRpm
                            ? L.t("gear_result_mode_output_rpm")
                            : L.t("gear_result_mode_vehicle_speed")
                    )
                }
                if result.driveMode == .multiSpeed {
                    ForEach(Array(result.stages.enumerated()), id: \.offset) { index, stage in
                        if index > 0 { CalculatorResultDivider() }
                        GearStageResultBlock(stage: stage, outputType: result.outputType, colorIndex: index)
                    }
                } else if let stage = result.stages.first {
                    GearFixedRatioResultBlock(
                        stage: stage,
                        outputType: result.outputType,
                        referenceRpms: result.fixedReferenceRpms.map(\.asDouble)
                    )
                }
            }
        }
    }
}

private struct GearStageResultBlock: View {
    @Environment(\.themeColors) private var colors
    let stage: GearStageResult
    let outputType: GearOutputType
    let colorIndex: Int

    private var accent: Color {
        let palette: [Color] = [
            colors.primary,
            Color(red: 0.26, green: 0.65, blue: 0.96),
            Color(red: 0.40, green: 0.73, blue: 0.42),
            Color(red: 1.00, green: 0.65, blue: 0.15),
            Color(red: 0.94, green: 0.33, blue: 0.31),
            Color(red: 0.61, green: 0.35, blue: 0.71),
        ]
        return palette[colorIndex % palette.count]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(String(stage.stageNumber))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(colors.onPrimary)
                    .frame(width: 24, height: 24)
                    .background(accent)
                    .clipShape(Circle())
                Text(L.tf("gear_result_stage_header", String(stage.stageNumber)))
                    .font(.subheadline.weight(.semibold))
            }
            CalculatorResultRow(
                label: L.t("gear_result_ratio"),
                value: fmt(stage.gearRatio, decimals: 2)
            )
            CalculatorResultRow(
                label: L.t("gear_result_shift_speed"),
                value: formatGearOutput(stage.shiftSpeedKmh, outputType: outputType)
            )
            if stage.stageNumber > 0, stage.speedsAtReferenceRpmKmh.count >= stage.stageNumber {
                let refSpeed = stage.speedsAtReferenceRpmKmh[Int(stage.stageNumber) - 1]
                CalculatorResultRow(
                    label: referenceRpmShortLabel(stage.referenceLabel),
                    value: formatGearOutput(refSpeed, outputType: outputType),
                    hint: L.tf("gear_result_rpm_value", fmt(stage.referenceRpm, decimals: 0))
                )
            }
            if let jump = stage.speedJumpKmh?.asDouble, let rpmJump = stage.rpmJump?.asDouble {
                CalculatorResultRow(
                    label: L.t("gear_result_speed_jump"),
                    value: L.tf("gear_result_speed_kmh", fmt(jump, decimals: 1))
                )
                CalculatorResultRow(
                    label: L.t("gear_result_rpm_jump"),
                    value: L.tf("gear_result_rpm_value", fmt(rpmJump, decimals: 0))
                )
            }
        }
    }
}

private struct GearFixedRatioResultBlock: View {
    let stage: GearStageResult
    let outputType: GearOutputType
    let referenceRpms: [Double]

    private let labels: [GearReferenceLabel] = [.low, .high, .resonanceEntry, .resonancePeak]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            CalculatorResultRow(
                label: L.t("gear_result_ratio"),
                value: fmt(stage.gearRatio, decimals: 2)
            )
            ForEach(Array(stage.speedsAtReferenceRpmKmh.enumerated()), id: \.offset) { index, value in
                if index < referenceRpms.count {
                    let label = labels.indices.contains(index) ? labels[index] : .additional
                    CalculatorResultRow(
                        label: fixedReferenceOutputLabel(label, outputType: outputType),
                        value: formatGearOutput(value, outputType: outputType),
                        hint: L.tf("gear_result_rpm_value", fmt(referenceRpms[index], decimals: 0))
                    )
                }
            }
        }
    }
}

private func referenceRpmShortLabel(_ label: GearReferenceLabel) -> String {
    switch label {
    case .low: return L.t("gear_result_ref_low_short")
    case .high: return L.t("gear_result_ref_high_short")
    case .resonanceEntry: return L.t("gear_result_ref_reso_entry_short")
    case .resonancePeak: return L.t("gear_result_ref_reso_peak_short")
    case .additional: return L.t("gear_result_ref_additional_short")
    default: return L.t("gear_result_ref_additional_short")
    }
}

private func fixedReferenceOutputLabel(_ label: GearReferenceLabel, outputType: GearOutputType) -> String {
    if outputType == .outputRpm {
        switch label {
        case .low: return L.t("gear_result_output_low_ref")
        case .high: return L.t("gear_result_output_high_ref")
        case .resonanceEntry: return L.t("gear_result_output_reso_entry_ref")
        case .resonancePeak: return L.t("gear_result_output_reso_peak_ref")
        default: return L.t("gear_result_ref_additional_short")
        }
    }
    switch label {
    case .low: return L.t("gear_result_speed_low_ref")
    case .high: return L.t("gear_result_speed_high_ref")
    case .resonanceEntry: return L.t("gear_result_speed_reso_entry")
    case .resonancePeak: return L.t("gear_result_speed_reso_peak")
    default: return L.t("gear_result_ref_additional_short")
    }
}

private func formatGearOutput(_ value: KotlinDouble, outputType: GearOutputType) -> String {
    formatGearOutput(value.asDouble, outputType: outputType)
}

private func formatGearOutput(_ value: Double, outputType: GearOutputType) -> String {
    switch outputType {
    case .outputRpm:
        return L.tf("gear_result_output_rpm_value", fmt(value, decimals: 0))
    default:
        return L.tf("gear_result_speed_kmh", fmt(value, decimals: 1))
    }
}
