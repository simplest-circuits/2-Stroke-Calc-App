import SwiftUI
import sharedKit

// MARK: - Editing environment

private struct CalculatorEditingEnabledKey: EnvironmentKey {
    static let defaultValue = true
}

extension EnvironmentValues {
    var calculatorEditingEnabled: Bool {
        get { self[CalculatorEditingEnabledKey.self] }
        set { self[CalculatorEditingEnabledKey.self] = newValue }
    }
}

// MARK: - Shared helpers

private func portAssessmentLabel(_ assessment: PortAreaAssessment) -> String {
    switch assessment {
    case .criticalLow: return L.t("port_area_assessment_critical")
    case .series: return L.t("port_area_assessment_series")
    case .sporty: return L.t("port_area_assessment_sporty")
    case .aggressive: return L.t("port_area_assessment_aggressive")
    default: return assessment.name
    }
}

// MARK: - Port timing

struct PortTimingCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var stroke = "57"
    @State private var connectingRod = "110"
    @State private var exhaustPort = "36.2"
    @State private var transferPort = "47.5"
    @State private var intakeOpen = "44.2"
    @State private var intakeClose = "15.9"
    @State private var pistonDeck = "-0.5"
    @State private var intakeSystem = IntakeSystem.rotaryValve

    private var portTimingResult: PortTimingResult? {
        guard let strokeMm = parseDouble(stroke),
              let rodMm = parseDouble(connectingRod),
              let exhaustMm = parseDouble(exhaustPort),
              let transferMm = parseDouble(transferPort),
              let openMm = parseDouble(intakeOpen),
              let closeMm = parseDouble(intakeClose),
              let deckMm = parseDouble(pistonDeck) else { return nil }

        return PortTimingCalculator.shared.calculate(input: PortTimingInput(
            strokeMm: strokeMm,
            connectingRodMm: rodMm,
            exhaustPortMm: exhaustMm,
            transferPortMm: transferMm,
            intakeOpenMm: openMm,
            intakeCloseMm: closeMm,
            pistonDeckMm: deckMm,
            intakeSystem: intakeSystem,
            roundResults: false
        ))
    }

    private var resultLines: [String] {
        guard let result = portTimingResult else { return [] }
        var lines: [String] = []

        if let exhaust = result.exhaust {
            lines.append("\(L.t("pt_exhaust")): \(fmt(exhaust.openBeforeBdc, decimals: 1))° vUT, \(L.t("pt_exhaust_duration")) \(fmt(exhaust.duration, decimals: 1))°")
        }
        if let transfer = result.transfer {
            lines.append("\(L.t("pt_transfer")): \(fmt(transfer.openBeforeBdc, decimals: 1))° vUT, \(L.t("pt_transfer_duration")) \(fmt(transfer.duration, decimals: 1))°")
        }
        if let blowdown = result.blowdown {
            lines.append("\(L.t("pt_blowdown")): \(fmt(blowdown, decimals: 1))°")
        }
        if let intake = result.intake {
            lines.append("\(L.t("pt_intake_before_tdc")): \(fmt(intake.openBeforeTdc, decimals: 1))°")
            lines.append("\(L.t("pt_intake_after_tdc")): \(fmt(intake.closeAfterTdc, decimals: 1))°")
            lines.append("\(L.t("pt_intake_duration")): \(fmt(intake.duration, decimals: 1))°")
        }
        return lines
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.timing),
                subtitle: L.t("calculator_overview_timing_desc")
            )

            CalculatorSection(L.t("pt_intake_system")) {
                Picker(L.t("pt_intake_system"), selection: $intakeSystem) {
                    Text(L.t("pt_rotary_valve")).tag(IntakeSystem.rotaryValve)
                    Text(L.t("pt_reed_valve")).tag(IntakeSystem.reedValve)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("pt_stroke"), text: $stroke, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("pt_connecting_rod"), text: $connectingRod, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("pt_exhaust"), text: $exhaustPort, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("pt_transfer"), text: $transferPort, suffix: L.t("pt_mm"), enabled: editingEnabled)
                if intakeSystem == .rotaryValve {
                    DecimalField(label: L.t("pt_intake_open"), text: $intakeOpen, suffix: L.t("pt_mm"), enabled: editingEnabled)
                    DecimalField(label: L.t("pt_intake_close"), text: $intakeClose, suffix: L.t("pt_mm"), enabled: editingEnabled)
                }
                DecimalField(
                    label: L.t("pt_piston_deck"),
                    text: $pistonDeck,
                    suffix: L.t("pt_mm"),
                    hint: L.t("pt_piston_deck_hint"),
                    enabled: editingEnabled
                )
            }

            ResultCard(title: S.resultTitle, lines: resultLines)

            if let result = portTimingResult {
                CalculatorSection(L.t("pt_section_diagram")) {
                    PortTimingDiagramView(result: result, showIntake: intakeSystem == .rotaryValve)
                }
            }
        }
    }
}

// MARK: - Ignition timing

private enum IgnitionInputMode {
    case degrees
    case millimeters
}

struct IgnitionTimingCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled
    @Environment(\.themeColors) private var colors

    @State private var strokeText = "57"
    @State private var connectingRodText = "110"
    @State private var inputMode: IgnitionInputMode = .degrees
    @State private var inputText = ""

    private var stroke: Double? { parseDouble(strokeText) }
    private var connectingRod: Double? { parseDouble(connectingRodText) }
    private var inputValue: Double? { parseDouble(inputText) }

    private var geometryValid: Bool {
        guard let stroke, let connectingRod else { return false }
        return stroke > 0 && connectingRod > 0
    }

    private var useDegrees: Bool { inputMode == .degrees }

    private var resultLine: String? {
        guard geometryValid, let inputValue, let stroke, let connectingRod else { return nil }
        switch inputMode {
        case .degrees:
            guard let millimeters = IgnitionTimingCalculator.shared.degreesBeforeTdcToMm(
                strokeMm: stroke,
                connectingRodMm: connectingRod,
                degreesBeforeTdc: inputValue
            ) else { return nil }
            return L.tf("ignition_result_degrees", formatIgnitionMillimeters(millimeters.asDouble))
        case .millimeters:
            guard let degrees = IgnitionTimingCalculator.shared.mmBeforeTdcToDegrees(
                strokeMm: stroke,
                connectingRodMm: connectingRod,
                mmBeforeTdc: inputValue
            ) else { return nil }
            return L.tf("ignition_result_millimeters", formatIgnitionDegrees(degrees.asDouble))
        }
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.ignition),
                subtitle: L.t("calculator_overview_ignition_desc")
            )

            HStack(alignment: .top, spacing: 12) {
                DecimalField(label: L.t("pt_stroke"), text: $strokeText, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("pt_connecting_rod"), text: $connectingRodText, suffix: L.t("pt_mm"), enabled: editingEnabled)
            }

            CalculatorBidirectionalField(
                optionALabel: L.t("ignition_input_mode_degrees"),
                optionBLabel: L.t("ignition_input_mode_millimeters"),
                useOptionA: useDegrees,
                onUseOptionAChange: handleInputModeChange,
                text: $inputText,
                fieldLabelA: L.t("ignition_degrees_label"),
                fieldLabelB: L.t("ignition_millimeters_label"),
                suffixA: L.t("ignition_unit_degrees"),
                suffixB: L.t("pt_mm"),
                enabled: editingEnabled
            )

            CalculatorSection(S.resultTitle) {
                if let resultLine {
                    PrimaryResultText(text: resultLine)
                } else {
                    Text(L.t("pt_not_calculated"))
                        .foregroundStyle(colors.onSurfaceVariant)
                }
            }
        }
    }

    private func handleInputModeChange(_ useDegreesMode: Bool) {
        let newMode: IgnitionInputMode = useDegreesMode ? .degrees : .millimeters
        guard newMode != inputMode else { return }

        if geometryValid, let currentInput = inputValue {
            switch (inputMode, newMode) {
            case (.degrees, .millimeters):
                if let converted = IgnitionTimingCalculator.shared.degreesBeforeTdcToMm(
                    strokeMm: stroke!,
                    connectingRodMm: connectingRod!,
                    degreesBeforeTdc: currentInput
                ) {
                    inputText = fmt(converted, decimals: 2)
                }
            case (.millimeters, .degrees):
                if let converted = IgnitionTimingCalculator.shared.mmBeforeTdcToDegrees(
                    strokeMm: stroke!,
                    connectingRodMm: connectingRod!,
                    mmBeforeTdc: currentInput
                ) {
                    inputText = fmt(converted, decimals: 2)
                }
            default:
                break
            }
        }

        inputMode = newMode
    }
}

// MARK: - DC cable

struct DcCableCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var voltage = "12"
    @State private var current = "15"
    @State private var length = "3"
    @State private var maxDropPercent = "3"

    private var resultLines: [String] {
        guard let v = parseDouble(voltage),
              let a = parseDouble(current),
              let len = parseDouble(length),
              let drop = parseDouble(maxDropPercent),
              let result = DcCableCrossSectionCalculator.shared.calculate(
                voltage: v,
                currentAmps: a,
                oneWayLengthM: len,
                maxDropPercent: drop
              ) else { return [] }

        return [
            "\(L.t("dc_cable_result_minimum_label")): \(L.tf("dc_cable_result_minimum_value", fmt(result.minimumCrossSectionMm2, decimals: 2)))",
            L.tf("dc_cable_result_recommended", fmt(result.recommendedCrossSectionMm2, decimals: 2)),
            "\(L.t("dc_cable_result_voltage_drop_label")): \(L.tf("dc_cable_result_voltage_drop_value", fmt(result.voltageDropVolts, decimals: 2), fmt(result.voltageDropPercent, decimals: 2)))",
            L.tf("dc_cable_result_drop_limit", fmt(result.maxAllowedDropVolts, decimals: 2), fmt(result.maxAllowedDropPercent, decimals: 1)),
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.dcCable),
                subtitle: L.t("calculator_overview_dc_cable_desc")
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("dc_cable_voltage_label"), text: $voltage, suffix: L.t("dc_cable_unit_volts"), enabled: editingEnabled)
                DecimalField(label: L.t("dc_cable_current_label"), text: $current, suffix: L.t("dc_cable_unit_amps"), enabled: editingEnabled)
                DecimalField(label: L.t("dc_cable_length_label"), text: $length, suffix: L.t("carb_jet_unit_m"), enabled: editingEnabled)
                DecimalField(label: L.t("dc_cable_drop_label"), text: $maxDropPercent, suffix: L.t("squish_band_unit_percent"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Fluid (electrolyte + cleaning agent)

struct FluidCalculatorView: View {
    @State private var selectedTab = 0

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.fluid),
                subtitle: S.calculatorDescription(.fluid)
            )

            Picker(L.t("calculator_tab_fluid"), selection: $selectedTab) {
                Text(L.t("electrolyte_calculator_title")).tag(0)
                Text(L.t("fluid_calculator_subtab_cleaning_agent")).tag(1)
            }
            .pickerStyle(.segmented)

            if selectedTab == 0 {
                ElectrolyteCalculatorContent()
            } else {
                CleaningAgentCalculatorContent()
            }
        }
    }
}

private struct ElectrolyteCalculatorContent: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var totalLiters = "3.0"
    @State private var saltG = "122"
    @State private var acidConcentration = "60"
    @State private var current = "1.6"

    private var resultLines: [String] {
        guard let liters = parseDouble(totalLiters),
              let salt = parseDouble(saltG),
              let acid = parseDouble(acidConcentration),
              let amps = parseDouble(current) else { return [] }

        let ref = ElectrolyteCalculator.shared.referenceSliderState()
        guard let synced = ElectrolyteCalculator.shared.syncSliders(
            driver: .total,
            totalLiters: liters,
            waterLiters: ref.waterLiters,
            aceticAcidMl: ref.aceticAcidMl,
            saltG: salt,
            acidConcentrationPercent: acid,
            electrificationCurrentAmps: amps
        ), let result = ElectrolyteCalculator.shared.calculate(state: synced) else { return [] }

        var lines = [
            L.tf("electrolyte_result_acid_value", fmt(result.aceticAcidMl, decimals: 0), fmt(result.acidConcentrationPercent, decimals: 0)),
            L.tf("electrolyte_result_water_value", fmt(result.waterLiters, decimals: 2)),
            L.tf("electrolyte_result_salt_value", fmt(result.saltG, decimals: 0)),
            "\(L.t("electrolyte_total_volume_label")): \(L.tf("electrolyte_total_volume_value", fmt(result.totalLiquidLiters, decimals: 1)))",
            "\(L.t("electrolyte_result_acid_label")): \(fmt(result.finalAceticAcidPercent, decimals: 1)) %",
        ]
        if let zinc = result.zincDissolution {
            lines += [
                L.tf("electrolyte_zinc_target_value", fmt(zinc.targetDissolvedZincG, decimals: 0)),
                "\(L.t("electrolyte_voltage_label")): \(fmt(zinc.voltageVolts, decimals: 1)) V \(L.t("electrolyte_zinc_time_label")) \(fmt(zinc.currentAmps, decimals: 1)) A",
                "\(L.t("electrolyte_zinc_time_label")): \(fmt(zinc.electrificationHours, decimals: 1)) h",
            ]
        }
        return lines
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: L.t("electrolyte_total_volume_label"), text: $totalLiters, suffix: "l", enabled: editingEnabled)
            DecimalField(label: L.t("electrolyte_salt_label"), text: $saltG, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
            DecimalField(label: L.t("electrolyte_acid_concentration_label"), text: $acidConcentration, suffix: L.t("squish_band_unit_percent"), enabled: editingEnabled)
            DecimalField(label: L.t("electrolyte_current_label"), text: $current, suffix: L.t("dc_cable_unit_amps"), enabled: editingEnabled)
        }
        ResultCard(title: S.resultTitle, lines: resultLines)
    }
}

private struct CleaningAgentCalculatorContent: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var totalLiters = "7.0"
    @State private var concentration = "3.0"

    private var resultLines: [String] {
        guard let liters = parseDouble(totalLiters),
              let conc = parseDouble(concentration) else { return [] }

        let input = IosKoinInitKt.iosCleaningAgentInput(totalLiters: liters, concentrationPercent: conc)
        guard let result = CleaningAgentCalculator.shared.calculate(input: input) else { return [] }

        return [
            L.tf("cleaning_agent_result_agent_value", fmt(result.cleaningAgentMl, decimals: 0), fmt(result.concentrationPercent, decimals: 1)),
            L.tf("cleaning_agent_result_water_value", fmt(result.waterLiters, decimals: 2)),
            "\(L.t("cleaning_agent_concentration_label")): \(fmt(result.concentrationPercent, decimals: 1)) %",
            "\(L.t("dc_cable_result_title")): \(fmt(result.totalLiters, decimals: 1)) l",
        ]
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: L.t("electrolyte_total_volume_label"), text: $totalLiters, suffix: "l", enabled: editingEnabled)
            DecimalField(label: L.t("cleaning_agent_concentration_label"), text: $concentration, suffix: L.t("squish_band_unit_percent"), enabled: editingEnabled)
        }
        ResultCard(title: S.resultTitle, lines: resultLines)
    }
}

// MARK: - Compression

struct CompressionCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var mode = CompressionCalculationMode.forward
    @State private var bore = "72"
    @State private var domeDiameter = "50"
    @State private var stroke = "62"
    @State private var domeVolume = "18"
    @State private var squishBand = "1.2"
    @State private var targetCompression = "7.5"
    @State private var pistonConstant = ""
    @State private var currentCompression = "7.2"

    private var resultLines: [String] {
        guard let boreMm = parseDouble(bore), let strokeMm = parseDouble(stroke) else { return [] }

        switch mode {
        case .forward:
            guard let domeD = parseDouble(domeDiameter),
                  let domeV = parseDouble(domeVolume),
                  let squish = parseDouble(squishBand),
                  let result = CompressionCalculator.shared.calculate(
                    boreMm: boreMm,
                    domeDiameterMm: domeD,
                    strokeMm: strokeMm,
                    domeVolumeMl: domeV,
                    squishBandMm: squish
                  ) else { return [] }
            return [
                L.tf("compression_result_ratio", fmt(result.compressionRatio, decimals: 2)),
                "\(L.t("compression_result_displacement")): \(L.tf("compression_result_value_ml", fmt(result.displacementMl, decimals: 1)))",
                "\(L.t("compression_result_squish_volume")): \(L.tf("compression_result_value_ml", fmt(result.squishBandVolumeMl, decimals: 2)))",
                "\(L.t("compression_result_squish_area")): \(L.tf("compression_result_value_percent", fmt(result.squishAreaPercent, decimals: 1)))",
                "\(L.t("compression_result_bore_stroke_ratio")): \(fmt(result.boreStrokeRatio, decimals: 2))",
            ]

        case .target:
            guard let target = parseDouble(targetCompression) else { return [] }
            let result: CompressionTargetResult?
            if let piston = parseDouble(pistonConstant) {
                result = IosKoinInitKt.iosCompressionCalculateTargetWithPistonConstant(
                    boreMm: boreMm,
                    strokeMm: strokeMm,
                    targetCompressionRatio: target,
                    pistonConstantMl: piston
                )
            } else {
                result = IosKoinInitKt.iosCompressionCalculateTarget(
                    boreMm: boreMm,
                    strokeMm: strokeMm,
                    targetCompressionRatio: target
                )
            }
            guard let result else { return [] }
            var lines = [
                "\(L.t("compression_target_result_total_chamber")): \(L.tf("compression_result_value_ml", fmt(result.totalChamberVolumeMl, decimals: 2)))",
                "\(L.t("compression_result_displacement")): \(L.tf("compression_result_value_ml", fmt(result.displacementMl, decimals: 1)))",
                L.tf("compression_target_result_ratio", fmt(result.targetCompressionRatio, decimals: 2)),
            ]
            if let dome = result.domeVolumeMl {
                lines.append("\(L.t("compression_target_result_dome")): \(L.tf("compression_result_value_ml", fmt(dome, decimals: 2)))")
            }
            return lines

        case .change:
            guard let current = parseDouble(currentCompression),
                  let target = parseDouble(targetCompression),
                  let result = CompressionCalculator.shared.calculateChange(
                    boreMm: boreMm,
                    strokeMm: strokeMm,
                    currentCompressionRatio: current,
                    targetCompressionRatio: target
                  ) else { return [] }
            return [
                L.tf("compression_change_result_remove", fmt(result.volumeToRemoveMl, decimals: 2)),
                L.tf("compression_change_result_remove_depth", fmt(result.millingDepthMm, decimals: 3)),
                "\(L.t("compression_current_ratio_label")): \(fmt(result.currentCompressionRatio, decimals: 2)):1 → \(L.t("compression_target_ratio_label")): \(fmt(result.targetCompressionRatio, decimals: 2)):1",
                L.tf("compression_change_result_current_chamber", fmt(result.currentCompressionRatio, decimals: 2)),
                L.tf("compression_change_result_target_chamber", fmt(result.targetCompressionRatio, decimals: 2)),
            ]

        default:
            return []
        }
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.compression),
                subtitle: S.calculatorDescription(.compression)
            )

            CalculatorSection(L.t("flywheel_section_mode")) {
                Picker(L.t("flywheel_section_mode"), selection: $mode) {
                    Text(L.t("compression_mode_forward")).tag(CompressionCalculationMode.forward)
                    Text(L.t("compression_mode_target")).tag(CompressionCalculationMode.target)
                    Text(L.t("compression_mode_change")).tag(CompressionCalculationMode.change)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("vehicles_field_bore"), text: $bore, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("pt_stroke"), text: $stroke, suffix: L.t("pt_mm"), enabled: editingEnabled)

                if mode == .forward {
                    DecimalField(label: L.t("compression_dome_diameter_label"), text: $domeDiameter, suffix: L.t("pt_mm"), enabled: editingEnabled)
                    DecimalField(label: L.t("compression_dome_volume_label"), text: $domeVolume, suffix: L.t("compression_unit_ml"), enabled: editingEnabled)
                    DecimalField(label: L.t("compression_squish_band_label"), text: $squishBand, suffix: L.t("pt_mm"), enabled: editingEnabled)
                }
                if mode == .target {
                    DecimalField(label: L.t("compression_target_ratio_label"), text: $targetCompression, suffix: ":1", enabled: editingEnabled)
                    DecimalField(label: L.t("compression_piston_constant_label"), text: $pistonConstant, suffix: L.t("compression_unit_ml"), enabled: editingEnabled)
                }
                if mode == .change {
                    DecimalField(label: L.t("compression_current_ratio_label"), text: $currentCompression, suffix: ":1", enabled: editingEnabled)
                    DecimalField(label: L.t("compression_target_ratio_label"), text: $targetCompression, suffix: ":1", enabled: editingEnabled)
                }
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Squish band

struct SquishBandCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var mode = SquishBandCalculationMode.percent
    @State private var bore = "72"
    @State private var squishPercent = "40"
    @State private var squishWidth = "1.2"

    private var resultLines: [String] {
        guard let boreMm = parseDouble(bore) else { return [] }

        let result: SquishBandResult?
        switch mode {
        case .percent:
            guard let pct = parseDouble(squishPercent) else { return [] }
            result = SquishBandCalculator.shared.calculateFromPercent(boreMm: boreMm, squishAreaPercent: pct)
        case .width:
            guard let width = parseDouble(squishWidth) else { return [] }
            result = SquishBandCalculator.shared.calculateFromWidth(boreMm: boreMm, squishBandWidthMm: width)
        default:
            result = nil
        }

        guard let r = result else { return [] }
        return [
            "\(L.t("compression_result_squish_area")): \(L.tf("compression_result_value_percent", fmt(r.squishAreaPercent, decimals: 1)))",
            "\(L.t("squish_band_mode_width")): \(fmt(r.squishBandWidthMm, decimals: 2)) \(L.t("pt_mm"))",
            "\(L.t("compression_dome_diameter_label")): \(fmt(r.domeDiameterMm, decimals: 2)) \(L.t("pt_mm"))",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.squishBand),
                subtitle: S.calculatorDescription(.squishBand)
            )

            CalculatorSection(L.t("flywheel_section_mode")) {
                Picker(L.t("flywheel_section_mode"), selection: $mode) {
                    Text(L.t("squish_band_mode_percent")).tag(SquishBandCalculationMode.percent)
                    Text(L.t("squish_band_mode_width")).tag(SquishBandCalculationMode.width)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("vehicles_field_bore"), text: $bore, suffix: L.t("pt_mm"), enabled: editingEnabled)
                if mode == .percent {
                    DecimalField(label: L.t("squish_band_percent_label"), text: $squishPercent, suffix: L.t("squish_band_unit_percent"), enabled: editingEnabled)
                } else {
                    DecimalField(label: L.t("squish_band_mode_width"), text: $squishWidth, suffix: L.t("pt_mm"), enabled: editingEnabled)
                }
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Mean pressure

struct MeanPressureCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var powerPs = "25"
    @State private var rpm = "8000"
    @State private var bore = "72"
    @State private var stroke = "62"

    private var resultLines: [String] {
        guard let ps = parseDouble(powerPs),
              let rpmVal = parseDouble(rpm),
              let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let result = MeanPressureCalculator.shared.calculate(
                powerPs: ps,
                rpm: rpmVal,
                boreMm: boreMm,
                strokeMm: strokeMm
              ) else { return [] }

        return [
            L.tf("mean_pressure_result_bar", fmt(result.meanPressureBar, decimals: 2)),
            "\(L.t("mean_pressure_result_torque")): \(L.tf("mean_pressure_result_value_nm", fmt(result.torqueNm, decimals: 2)))",
            "\(L.t("mean_pressure_result_displacement")): \(L.tf("mean_pressure_result_value_ccm", fmt(result.displacementCcm, decimals: 1)))",
            "\(L.t("mean_pressure_result_specific_power")): \(L.tf("mean_pressure_result_value_ps_per_l", fmt(result.specificPowerPsPerLiter, decimals: 1)))",
            "\(L.t("mean_pressure_result_power_watts")): \(fmt(result.powerWatts / 1000.0, decimals: 2)) kW",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.meanPressure),
                subtitle: S.calculatorDescription(.meanPressure)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("dc_cable_input_mode_power"), text: $powerPs, suffix: L.t("flywheel_unit_ps"), enabled: editingEnabled)
                DecimalField(label: L.t("mean_pressure_rpm_label"), text: $rpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                DecimalField(label: L.t("vehicles_field_bore"), text: $bore, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("pt_stroke"), text: $stroke, suffix: L.t("pt_mm"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Gear

struct GearCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var wheelCircumference = "1307"
    @State private var primaryPinion = "15"
    @State private var primaryGear = "40"
    @State private var secondaryPinion = "14"
    @State private var secondaryGear = "36"
    @State private var referenceRpm = "8000"
    @State private var shiftRpm = "8000"

    private var resultLines: [String] {
        guard let wheel = parseDouble(wheelCircumference),
              let pPin = Int32(primaryPinion),
              let pGear = Int32(primaryGear),
              let sPin = Int32(secondaryPinion),
              let sGear = Int32(secondaryGear),
              let refRpm = parseDouble(referenceRpm),
              let shift = parseDouble(shiftRpm) else { return [] }

        let stage = GearStageConfig(
            secondaryPinion: sPin,
            secondaryGear: sGear,
            referenceRpm: refRpm
        )
        let input = GearCalculatorInput(
            driveMode: .multiSpeed,
            outputType: .vehicleSpeed,
            wheelCircumferenceMm: wheel,
            rpmConstant: 16000,
            shiftRpm: shift,
            primaryPinion: pPin,
            primaryGear: pGear,
            stages: [stage, stage],
            fixedReferenceRpms: []
        )
        guard let result = GearCalculator.shared.calculate(input: input) else { return [] }

        return result.stages.enumerated().map { index, stage in
            let speed = stage.speedsAtReferenceRpmKmh.last?.asDouble ?? stage.shiftSpeedKmh.asDouble
            var line = "\(L.tf("gear_result_stage_header", index + 1)): \(L.tf("gear_result_speed_kmh", fmt(speed, decimals: 1))) (i=\(fmt(stage.gearRatio, decimals: 2)))"
            if let jump = stage.speedJumpKmh?.asDouble {
                line += ", Δ +\(fmt(jump, decimals: 1)) km/h"
            }
            return line
        }
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.gear),
                subtitle: S.calculatorDescription(.gear)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("gear_wheel_circumference_label"), text: $wheelCircumference, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("vehicles_field_front_sprocket"), text: $primaryPinion, enabled: editingEnabled)
                DecimalField(label: L.t("gear_primary_gear_label"), text: $primaryGear, enabled: editingEnabled)
                DecimalField(label: L.t("vehicles_field_rear_sprocket"), text: $secondaryPinion, enabled: editingEnabled)
                DecimalField(label: L.t("gear_secondary_gear_label"), text: $secondaryGear, enabled: editingEnabled)
                DecimalField(label: L.t("gear_stage_reference_rpm_label"), text: $referenceRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                DecimalField(label: L.t("gear_shift_rpm_label"), text: $shiftRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)

            if !resultLines.isEmpty {
                CalculatorSection(L.t("gear_chart_title")) {
                    GearChartView(speedLines: resultLines)
                }
            }
        }
    }
}

// MARK: - Port area

struct PortAreaCalculatorView: View {
    @State private var selectedTab = 0

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.portArea),
                subtitle: S.calculatorDescription(.portArea)
            )

            Picker(L.t("calculator_tab_port_area"), selection: $selectedTab) {
                Text(L.t("pt_transfer")).tag(0)
                Text(L.t("pt_exhaust")).tag(1)
            }
            .pickerStyle(.segmented)

            if selectedTab == 0 {
                TransferPortAreaContent()
            } else {
                ExhaustPortAreaContent()
            }
        }
    }
}

private struct TransferPortAreaContent: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var bore = "54"
    @State private var stroke = "54"
    @State private var width = "26"
    @State private var height = "18"
    @State private var channelCount = "2"
    @State private var duration = "120"
    @State private var sideAngle = "15"

    private var resultLines: [String] {
        guard let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let w = parseDouble(width),
              let h = parseDouble(height),
              let count = Int32(channelCount),
              let dur = parseDouble(duration),
              let angle = parseDouble(sideAngle) else { return [] }

        let input = IosKoinInitKt.iosTransferPortInput(
            boreMm: boreMm,
            strokeMm: strokeMm,
            widthMm: w,
            heightMm: h,
            channelCount: count,
            sideAngleDeg: angle,
            correctionFactor: 1.0,
            durationDeg: dur
        )
        guard let result = PortAreaCalculator.shared.calculateTransfer(input: input) else { return [] }

        return [
            "\(L.t("port_area_result_area")): \(L.tf("port_area_result_area_value", fmt(result.areaMm2, decimals: 1)))",
            L.tf("port_area_result_time_area", fmt(result.timeAreaMm2Deg, decimals: 0)),
            "\(L.t("port_area_result_area"))/\(L.t("vehicles_field_bore")): \(fmt(result.areaToBoreRatio * 100, decimals: 1)) %",
            "\(L.t("port_area_result_ta_per_cc")): \(L.tf("port_area_result_ta_per_cc_value", fmt(result.timeAreaPerCc, decimals: 0)))",
            "\(L.t("port_area_result_displacement")): \(L.tf("port_area_result_displacement_value", fmt(result.displacementCc, decimals: 1)))",
            portAssessmentLabel(result.assessment),
        ]
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: L.t("vehicles_field_bore"), text: $bore, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("pt_stroke"), text: $stroke, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_transfer_width_label"), text: $width, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_transfer_height_label"), text: $height, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_transfer_channels_label"), text: $channelCount, enabled: editingEnabled)
            DecimalField(label: L.t("port_area_transfer_side_angle_label"), text: $sideAngle, suffix: L.t("port_area_unit_deg"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_transfer_duration_label"), text: $duration, suffix: L.t("port_area_unit_deg"), enabled: editingEnabled)
        }
        ResultCard(title: S.resultTitle, lines: resultLines)
    }
}

private struct ExhaustPortAreaContent: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var bore = "54"
    @State private var stroke = "54"
    @State private var portHeight = "20"
    @State private var totalSpan = "28"
    @State private var topSpan = "48"
    @State private var bridgeWidth = "8"
    @State private var duration = "166"

    private var resultLines: [String] {
        guard let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let height = parseDouble(portHeight),
              let bottom = parseDouble(totalSpan),
              let top = parseDouble(topSpan),
              let bridge = parseDouble(bridgeWidth),
              let dur = parseDouble(duration) else { return [] }

        let input = IosKoinInitKt.iosSimpleExhaustPortInput(
            boreMm: boreMm,
            strokeMm: strokeMm,
            type: .twin,
            shape: .trapezoid,
            portHeightMm: height,
            totalSpanMm: bottom,
            topSpanMm: top,
            bridgeWidthMm: bridge,
            durationDeg: dur
        )
        guard let result = PortAreaCalculator.shared.calculateExhaust(input: input) else { return [] }

        return [
            "\(L.t("port_area_result_area")): \(L.tf("port_area_result_area_value", fmt(result.areaMm2, decimals: 1)))",
            L.tf("port_area_result_time_area", fmt(result.timeAreaMm2Deg, decimals: 0)),
            "\(L.t("port_area_result_area"))/\(L.t("vehicles_field_bore")): \(fmt(result.areaToBoreRatio * 100, decimals: 1)) %",
            "\(L.t("port_area_result_ta_per_cc")): \(L.tf("port_area_result_ta_per_cc_value", fmt(result.timeAreaPerCc, decimals: 0)))",
            "\(L.t("port_area_result_displacement")): \(L.tf("port_area_result_displacement_value", fmt(result.displacementCc, decimals: 1)))",
            portAssessmentLabel(result.assessment),
        ]
    }

    private var exhaustLayout: PortGeometryLayout? {
        guard let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let height = parseDouble(portHeight),
              let bottom = parseDouble(totalSpan),
              let top = parseDouble(topSpan),
              let bridge = parseDouble(bridgeWidth),
              let dur = parseDouble(duration) else { return nil }
        let input = IosKoinInitKt.iosSimpleExhaustPortInput(
            boreMm: boreMm,
            strokeMm: strokeMm,
            type: .twin,
            shape: .trapezoid,
            portHeightMm: height,
            totalSpanMm: bottom,
            topSpanMm: top,
            bridgeWidthMm: bridge,
            durationDeg: dur
        )
        return PortAreaCalculator.shared.calculateExhaustLayout(input: input)
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: L.t("vehicles_field_bore"), text: $bore, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("pt_stroke"), text: $stroke, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_exhaust_height_label"), text: $portHeight, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_exhaust_bottom_span_label"), text: $totalSpan, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_exhaust_top_span_label"), text: $topSpan, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_exhaust_bridge_label"), text: $bridgeWidth, suffix: L.t("pt_mm"), enabled: editingEnabled)
            DecimalField(label: L.t("port_area_exhaust_duration_label"), text: $duration, suffix: L.t("port_area_unit_deg"), enabled: editingEnabled)
        }
        if let layout = exhaustLayout {
            ExhaustPortGeometryDiagramView(layout: layout)
        }
        ResultCard(title: S.resultTitle, lines: resultLines)
    }
}

// MARK: - Counterweight

struct CounterweightCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var pistonWeight = "120"
    @State private var rodHalf = "80"
    @State private var bigEnd = "180"

    private var resultLines: [String] {
        guard let piston = parseDouble(pistonWeight),
              let half = parseDouble(rodHalf),
              let big = parseDouble(bigEnd),
              let factor = CounterweightFactorCalculator.shared.factorPercent(
                pistonWeightGrams: piston,
                connectingRodHalfGrams: half,
                bigEndWeightGrams: big
              ) else { return [] }

        return [L.tf("counterweight_result_factor", fmt(factor, decimals: 1))]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.counterweight),
                subtitle: S.calculatorDescription(.counterweight)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("counterweight_piston_weight_label"), text: $pistonWeight, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
                DecimalField(label: L.t("counterweight_rod_half_label"), text: $rodHalf, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
                DecimalField(label: L.t("counterweight_big_end_label"), text: $bigEnd, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Variator weight

struct VariatorWeightCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var currentGrams = "6.0"
    @State private var currentRpm = "6500"
    @State private var targetRpm = "7000"
    @State private var rollerType = VariatorWeightRollerType.rollers

    private var resultLines: [String] {
        guard let grams = parseDouble(currentGrams),
              let current = parseDouble(currentRpm),
              let target = parseDouble(targetRpm),
              let result = VariatorWeightCalculator.shared.calculate(
                currentGrams: grams,
                currentRpm: current,
                targetRpm: target,
                rollerType: rollerType
              ) else { return [] }

        return [
            L.tf("variator_weight_result_physics", fmt(VariatorWeightCalculator.shared.roundToTenth(grams: result.physicsWeightGrams), decimals: 1)),
            "\(L.t("variator_weight_result_empirical_label")): \(L.tf("variator_weight_result_empirical_value", fmt(VariatorWeightCalculator.shared.roundToTenth(grams: result.empiricalWeightGrams), decimals: 1)))",
            "\(L.t("variator_weight_result_rpm_delta_label")): \(L.tf("gear_result_rpm_value", fmt(result.rpmDelta, decimals: 0)))",
            "\(L.t("variator_weight_result_target_rpm_label")): \(L.tf("gear_result_rpm_value", fmt(result.targetRpm, decimals: 0)))",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.variatorWeight),
                subtitle: S.calculatorDescription(.variatorWeight)
            )

            CalculatorSection(S.sectionInput) {
                Picker(L.t("variator_weight_roller_type_label"), selection: $rollerType) {
                    Text(L.t("variator_weight_roller_type_rollers")).tag(VariatorWeightRollerType.rollers)
                    Text(L.t("vehicles_field_variator_rollers")).tag(VariatorWeightRollerType.sliders)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)

                DecimalField(label: L.t("variator_weight_current_weight_label"), text: $currentGrams, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
                DecimalField(label: L.t("variator_weight_current_rpm_label"), text: $currentRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                DecimalField(label: L.t("variator_weight_target_mode_rpm"), text: $targetRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Fuel mix

struct FuelMixCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var fuelLiters = "5"
    @State private var ratio = "50"

    private var resultLines: [String] {
        guard let liters = parseDouble(fuelLiters),
              let ratioParts = Int32(ratio),
              let oilMl = FuelMixCalculator.shared.oilMillilitersFromFuelLiters(
                fuelLiters: liters,
                ratioPartsFuel: ratioParts
              ) else { return [] }

        return [
            L.tf("fuel_mix_result_oil_amount", fmt(oilMl, decimals: 0)),
            L.tf("fuel_mix_result_fuel_amount", fmt(liters, decimals: 1)),
            L.tf("fuel_mix_ratio_display", Int32(ratio) ?? 0),
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.fuelMix),
                subtitle: S.calculatorDescription(.fuelMix)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("fuel_mix_fuel_amount_label"), text: $fuelLiters, suffix: L.t("fuel_mix_unit_liters"), enabled: editingEnabled)
                DecimalField(label: L.t("fuel_mix_custom_ratio_label"), text: $ratio, enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Carb jet

struct CarbJetCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var baseJet = "120"
    @State private var refAltitude = "0"
    @State private var refTemp = "20"
    @State private var targetAltitude = "1500"
    @State private var targetTemp = "25"

    private var resultLines: [String] {
        guard let jet = Int32(baseJet),
              let refAlt = parseDouble(refAltitude),
              let refT = parseDouble(refTemp),
              let tgtAlt = parseDouble(targetAltitude),
              let tgtT = parseDouble(targetTemp),
              let result = CarbJetCorrectionCalculator.shared.calculate(
                baseMainJet: jet,
                referenceAltitudeM: refAlt,
                referenceTemperatureC: refT,
                targetAltitudeM: tgtAlt,
                targetTemperatureC: tgtT
              ) else { return [] }

        return [
            L.tf("carb_jet_result_corrected_jet", result.correctedMainJet, fmt(result.correctedMainJetExact, decimals: 1)),
            L.tf("carb_jet_result_factor", fmt(result.correctionFactor * 100, decimals: 4)),
            "\(L.t("carb_jet_result_reference_density")): \(fmt(result.referenceAirDensity, decimals: 2))",
            "\(L.t("carb_jet_result_target_density")): \(fmt(result.targetAirDensity, decimals: 2))",
            "\(L.t("carb_jet_result_reference_pressure")): \(L.tf("carb_jet_result_pressure_value", fmt(result.referencePressureMbar, decimals: 1)))",
            "\(L.t("carb_jet_result_target_pressure")): \(L.tf("carb_jet_result_pressure_value", fmt(result.targetPressureMbar, decimals: 1)))",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.carbJet),
                subtitle: S.calculatorDescription(.carbJet)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("carb_jet_base_main_jet_label"), text: $baseJet, enabled: editingEnabled)
                DecimalField(label: L.t("carb_jet_reference_altitude_label"), text: $refAltitude, suffix: L.t("carb_jet_unit_m"), enabled: editingEnabled)
                DecimalField(label: L.t("carb_jet_reference_temperature_label"), text: $refTemp, suffix: L.t("carb_jet_unit_celsius"), enabled: editingEnabled)
                DecimalField(label: L.t("carb_jet_target_altitude_label"), text: $targetAltitude, suffix: L.t("carb_jet_unit_m"), enabled: editingEnabled)
                DecimalField(label: L.t("carb_jet_target_temperature_label"), text: $targetTemp, suffix: L.t("carb_jet_unit_celsius"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Flywheel inertia

struct FlywheelInertiaCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var outerDiameter = "300"
    @State private var innerDiameter = "0"
    @State private var length = "100"
    @State private var rollerRadius = "150"

    private var resultLines: [String] {
        guard let outer = parseDouble(outerDiameter),
              let inner = parseDouble(innerDiameter),
              let len = parseDouble(length),
              let roller = parseDouble(rollerRadius) else { return [] }

        let input = IosKoinInitKt.iosFlywheelGeometryInput(
            cylinderType: inner > 0 ? .hollow : .solid,
            outerDiameterMm: outer,
            innerDiameterMm: inner,
            lengthMm: len,
            material: .steel,
            customDensityKgM3: 7850,
            rollerRadiusMm: roller,
            additionalInertiaKgm2: 0
        )
        guard let result = FlywheelInertiaCalculator.shared.inertiaFromGeometry(input: input) else { return [] }

        var lines = [L.tf("flywheel_result_inertia", fmt(result.inertiaKgm2, decimals: 4))]
        if let mass = result.massKg {
            lines.append("\(L.t("flywheel_result_mass_label")): \(L.tf("flywheel_result_mass_value", fmt(mass, decimals: 2)))")
        }
        if let eq = result.equivalentMassKg {
            lines.append("\(L.t("flywheel_result_equivalent_mass_label")): \(L.tf("flywheel_result_equivalent_mass_value", fmt(eq, decimals: 1)))")
        }
        return lines
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.dynoInertia),
                subtitle: S.calculatorDescription(.dynoInertia)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("flywheel_outer_diameter_label"), text: $outerDiameter, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("flywheel_inner_diameter_label"), text: $innerDiameter, suffix: L.t("pt_mm"), hint: L.t("flywheel_cylinder_solid"), enabled: editingEnabled)
                DecimalField(label: L.t("exhaust_segment_detail_length"), text: $length, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("flywheel_roller_radius_label"), text: $rollerRadius, suffix: L.t("pt_mm"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}

// MARK: - Vehicle dynamics

struct VehicleDynamicsCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var mass = "180"
    @State private var powerPs = "12"
    @State private var engineRpm = "7500"
    @State private var maxRpm = "9500"
    @State private var wheelCircumference = "1307"
    @State private var primaryPinion = "15"
    @State private var primaryGear = "40"
    @State private var secondaryPinion = "14"
    @State private var secondaryGear = "36"
    @State private var shiftRpm = "8000"
    @State private var analysisSpeed = "60"
    @State private var targetSpeed = "80"

    private var resultLines: [String] {
        guard let massKg = parseDouble(mass),
              let ps = parseDouble(powerPs),
              let rpm = parseDouble(engineRpm),
              let max = parseDouble(maxRpm),
              let wheel = parseDouble(wheelCircumference),
              let pPin = Int32(primaryPinion),
              let pGear = Int32(primaryGear),
              let sPin = Int32(secondaryPinion),
              let sGear = Int32(secondaryGear),
              let shift = parseDouble(shiftRpm),
              let analysis = parseDouble(analysisSpeed),
              let target = parseDouble(targetSpeed) else { return [] }

        let preset = VehicleDynamicsPreset.roller.values()
        let stage = VehicleDynamicsGearInput(secondaryPinion: sPin, secondaryGear: sGear)
        let input = VehicleDynamicsInput(
            massKg: massKg,
            engineMode: .power,
            powerPs: KotlinDouble(value: ps),
            torqueNm: nil,
            engineRpm: rpm,
            maxEngineRpm: max,
            primaryPinion: pPin,
            primaryGear: pGear,
            stages: [stage],
            wheelCircumferenceMm: wheel,
            shiftRpm: shift,
            drivetrainEfficiency: 0.9,
            dragCoefficient: preset.dragCoefficient,
            frontalAreaM2: preset.frontalAreaM2,
            rollingResistance: preset.rollingResistance,
            airDensityKgM3: 1.204,
            massFactor: preset.massFactor,
            gradientPercent: 0,
            tractionLimitG: nil,
            analysisSpeedKmh: analysis,
            targetSpeedKmh: target,
            selectedGearIndex: 0,
            rpmConstant: 16000
        )
        guard let result = VehicleDynamicsCalculator.shared.calculate(input: input) else { return [] }

        var lines: [String] = []
        if let top = result.topSpeedKmh {
            let limit = result.topSpeedLimit == TopSpeedLimit.aerodynamic
                ? L.t("vehicle_dynamics_limit_aero")
                : L.t("vehicle_dynamics_limit_rpm")
            lines.append("\(L.t("vehicle_dynamics_result_top_speed_title")): \(L.tf("vehicle_dynamics_result_top_speed_value", fmt(top, decimals: 1))) (\(L.t("vehicle_dynamics_result_top_speed_gear")) \(result.topSpeedGear ?? 0), \(limit))")
        }
        if let gear = result.selectedGear {
            lines.append("\(L.tf("vehicle_dynamics_result_power_at_speed", fmt(analysis, decimals: 0))): \(L.tf("vehicle_dynamics_result_acceleration_g", fmt(gear.accelerationG, decimals: 2)))")
            lines.append("\(L.t("vehicle_dynamics_result_net_force")): \(fmt(gear.forces.netForceN, decimals: 0)) N")
        }
        if let sprint = result.sprintToTarget {
            lines.append(L.tf("vehicle_dynamics_result_sprint_time", fmt(sprint.timeSeconds, decimals: 1), fmt(target, decimals: 0)))
        }
        if let qm = result.quarterMileEstimateSeconds, let trap = result.quarterMileTrapSpeedKmh {
            lines.append("\(L.t("vehicle_dynamics_result_quarter_mile_title")): \(fmt(qm, decimals: 1)) s @ \(L.tf("vehicle_dynamics_result_top_speed_value", fmt(trap, decimals: 0)))")
        }
        return lines
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.vehicleDynamics),
                subtitle: S.calculatorDescription(.vehicleDynamics)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("vehicle_dynamics_mass_label"), text: $mass, suffix: L.t("flywheel_unit_kg"), enabled: editingEnabled)
                DecimalField(label: L.t("dc_cable_input_mode_power"), text: $powerPs, suffix: L.t("flywheel_unit_ps"), enabled: editingEnabled)
                DecimalField(label: L.t("vehicle_dynamics_result_rpm_at_speed"), text: $engineRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                DecimalField(label: L.t("vehicle_dynamics_max_rpm_label"), text: $maxRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                DecimalField(label: L.t("gear_wheel_circumference_label"), text: $wheelCircumference, suffix: L.t("pt_mm"), enabled: editingEnabled)
                DecimalField(label: L.t("gear_primary_pinion_label"), text: $primaryPinion, enabled: editingEnabled)
                DecimalField(label: L.t("gear_primary_gear_label"), text: $primaryGear, enabled: editingEnabled)
                DecimalField(label: L.t("gear_secondary_pinion_label"), text: $secondaryPinion, enabled: editingEnabled)
                DecimalField(label: L.t("gear_secondary_gear_label"), text: $secondaryGear, enabled: editingEnabled)
                DecimalField(label: L.t("gear_shift_rpm_label"), text: $shiftRpm, suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                DecimalField(label: L.t("vehicle_dynamics_analysis_speed_label"), text: $analysisSpeed, suffix: L.t("gear_chart_axis_speed"), enabled: editingEnabled)
                DecimalField(label: L.t("vehicle_dynamics_target_speed_label"), text: $targetSpeed, suffix: L.t("gear_chart_axis_speed"), enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}
