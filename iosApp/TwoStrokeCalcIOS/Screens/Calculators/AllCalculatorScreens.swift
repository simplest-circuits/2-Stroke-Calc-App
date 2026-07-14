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
    @State private var roundResults = false

    private var portTimingInput: PortTimingInput? {
        guard let strokeMm = parseDouble(stroke),
              let rodMm = parseDouble(connectingRod),
              let exhaustMm = parseDouble(exhaustPort),
              let transferMm = parseDouble(transferPort),
              let openMm = parseDouble(intakeOpen),
              let closeMm = parseDouble(intakeClose),
              let deckMm = parseDouble(pistonDeck) else { return nil }

        return PortTimingInput(
            strokeMm: strokeMm,
            connectingRodMm: rodMm,
            exhaustPortMm: exhaustMm,
            transferPortMm: transferMm,
            intakeOpenMm: openMm,
            intakeCloseMm: closeMm,
            pistonDeckMm: deckMm,
            intakeSystem: intakeSystem,
            roundResults: roundResults
        )
    }

    private var portTimingResult: PortTimingResult? {
        guard let input = portTimingInput else { return nil }
        return PortTimingCalculator.shared.calculate(input: input)
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

            PortTimingResultsCard(
                result: portTimingResult,
                input: portTimingInput,
                roundResults: roundResults,
                onRoundResultsChange: { roundResults = $0 }
            )

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

    private var hasInput: Bool {
        !strokeText.isEmpty || !connectingRodText.isEmpty || !inputText.isEmpty
    }

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

            CalculatorResultCard(L.t("ignition_result_title")) {
                if !hasInput || !geometryValid || inputValue == nil {
                    CalculatorEmptyResultText(text: L.t("pt_not_calculated"))
                } else if let resultLine {
                    PrimaryResultText(text: resultLine)
                } else {
                    CalculatorEmptyResultText(text: L.t("pt_not_calculated"))
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

    private var hasInput: Bool {
        !voltage.isEmpty || !current.isEmpty || !length.isEmpty || !maxDropPercent.isEmpty
    }

    private var dcCableResult: DcCableCrossSectionCalculatorResult? {
        guard let v = parseDouble(voltage),
              let a = parseDouble(current),
              let len = parseDouble(length),
              let drop = parseDouble(maxDropPercent) else { return nil }

        return DcCableCrossSectionCalculator.shared.calculate(
            voltage: v,
            currentAmps: a,
            oneWayLengthM: len,
            maxDropPercent: drop
        )
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

            DcCableResultCard(result: dcCableResult, hasInput: hasInput)
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

    private var hasInput: Bool {
        !totalLiters.isEmpty || !saltG.isEmpty || !acidConcentration.isEmpty || !current.isEmpty
    }

    private var electrolyteResult: ElectrolyteCalculatorResult? {
        guard let liters = parseDouble(totalLiters),
              let salt = parseDouble(saltG),
              let acid = parseDouble(acidConcentration),
              let amps = parseDouble(current) else { return nil }

        let ref = ElectrolyteCalculator.shared.referenceSliderState()
        guard let synced = ElectrolyteCalculator.shared.syncSliders(
            driver: .total,
            totalLiters: liters,
            waterLiters: ref.waterLiters,
            aceticAcidMl: ref.aceticAcidMl,
            saltG: salt,
            acidConcentrationPercent: acid,
            electrificationCurrentAmps: amps
        ) else { return nil }

        return ElectrolyteCalculator.shared.calculate(state: synced)
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: L.t("electrolyte_total_volume_label"), text: $totalLiters, suffix: "l", enabled: editingEnabled)
            DecimalField(label: L.t("electrolyte_salt_label"), text: $saltG, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
            DecimalField(label: L.t("electrolyte_acid_concentration_label"), text: $acidConcentration, suffix: L.t("squish_band_unit_percent"), enabled: editingEnabled)
            DecimalField(label: L.t("electrolyte_current_label"), text: $current, suffix: L.t("dc_cable_unit_amps"), enabled: editingEnabled)
        }
        ElectrolyteResultCard(result: electrolyteResult, loadVoltage: nil)
    }
}

private struct CleaningAgentCalculatorContent: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var totalLiters = "7.0"
    @State private var concentration = "3.0"

    private var cleaningAgentResult: CleaningAgentCalculatorResult? {
        guard let liters = parseDouble(totalLiters),
              let conc = parseDouble(concentration) else { return nil }

        let input = IosKoinInitKt.iosCleaningAgentInput(totalLiters: liters, concentrationPercent: conc)
        return CleaningAgentCalculator.shared.calculate(input: input)
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: L.t("electrolyte_total_volume_label"), text: $totalLiters, suffix: "l", enabled: editingEnabled)
            DecimalField(label: L.t("cleaning_agent_concentration_label"), text: $concentration, suffix: L.t("squish_band_unit_percent"), enabled: editingEnabled)
        }
        CleaningAgentResultCard(result: cleaningAgentResult)
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

    private var forwardHasInput: Bool {
        !bore.isEmpty || !domeDiameter.isEmpty || !stroke.isEmpty || !domeVolume.isEmpty || !squishBand.isEmpty
    }

    private var targetHasInput: Bool {
        !bore.isEmpty || !stroke.isEmpty || !targetCompression.isEmpty || !pistonConstant.isEmpty
    }

    private var changeHasInput: Bool {
        !bore.isEmpty || !stroke.isEmpty || !currentCompression.isEmpty || !targetCompression.isEmpty
    }

    private var forwardResult: CompressionResult? {
        guard let boreMm = parseDouble(bore),
              let domeD = parseDouble(domeDiameter),
              let strokeMm = parseDouble(stroke),
              let domeV = parseDouble(domeVolume),
              let squish = parseDouble(squishBand) else { return nil }

        return CompressionCalculator.shared.calculate(
            boreMm: boreMm,
            domeDiameterMm: domeD,
            strokeMm: strokeMm,
            domeVolumeMl: domeV,
            squishBandMm: squish
        )
    }

    private var targetResult: CompressionTargetResult? {
        guard let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let target = parseDouble(targetCompression) else { return nil }

        if let piston = parseDouble(pistonConstant) {
            return IosKoinInitKt.iosCompressionCalculateTargetWithPistonConstant(
                boreMm: boreMm,
                strokeMm: strokeMm,
                targetCompressionRatio: target,
                pistonConstantMl: piston
            )
        }
        return IosKoinInitKt.iosCompressionCalculateTarget(
            boreMm: boreMm,
            strokeMm: strokeMm,
            targetCompressionRatio: target
        )
    }

    private var changeResult: CompressionChangeResult? {
        guard let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let current = parseDouble(currentCompression),
              let target = parseDouble(targetCompression) else { return nil }

        return CompressionCalculator.shared.calculateChange(
            boreMm: boreMm,
            strokeMm: strokeMm,
            currentCompressionRatio: current,
            targetCompressionRatio: target
        )
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

            switch mode {
            case .forward:
                CompressionForwardResultCard(result: forwardResult, hasInput: forwardHasInput)
            case .target:
                CompressionTargetResultCard(result: targetResult, hasInput: targetHasInput)
            case .change:
                CompressionChangeResultCard(result: changeResult, hasInput: changeHasInput)
            default:
                EmptyView()
            }
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

    private var hasInput: Bool {
        switch mode {
        case .percent:
            return !bore.isEmpty || !squishPercent.isEmpty
        case .width:
            return !bore.isEmpty || !squishWidth.isEmpty
        default:
            return !bore.isEmpty
        }
    }

    private var squishResult: SquishBandResult? {
        guard let boreMm = parseDouble(bore) else { return nil }

        switch mode {
        case .percent:
            guard let pct = parseDouble(squishPercent) else { return nil }
            return SquishBandCalculator.shared.calculateFromPercent(boreMm: boreMm, squishAreaPercent: pct)
        case .width:
            guard let width = parseDouble(squishWidth) else { return nil }
            return SquishBandCalculator.shared.calculateFromWidth(boreMm: boreMm, squishBandWidthMm: width)
        default:
            return nil
        }
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

            SquishBandResultCard(result: squishResult, hasInput: hasInput, mode: mode)
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

    private var hasInput: Bool {
        !powerPs.isEmpty || !rpm.isEmpty || !bore.isEmpty || !stroke.isEmpty
    }

    private var meanPressureResult: MeanPressureResult? {
        guard let ps = parseDouble(powerPs),
              let rpmVal = parseDouble(rpm),
              let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke) else { return nil }

        return MeanPressureCalculator.shared.calculate(
            powerPs: ps,
            rpm: rpmVal,
            boreMm: boreMm,
            strokeMm: strokeMm
        )
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

            MeanPressureResultCard(result: meanPressureResult, hasInput: hasInput)
        }
    }
}

// MARK: - Counterweight

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

    private var hasInput: Bool {
        !bore.isEmpty || !stroke.isEmpty || !width.isEmpty || !height.isEmpty
            || !channelCount.isEmpty || !duration.isEmpty || !sideAngle.isEmpty
    }

    private var transferResult: PortAreaResult? {
        guard let boreMm = parseDouble(bore),
              let strokeMm = parseDouble(stroke),
              let w = parseDouble(width),
              let h = parseDouble(height),
              let count = Int32(channelCount),
              let dur = parseDouble(duration),
              let angle = parseDouble(sideAngle) else { return nil }

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
        return PortAreaCalculator.shared.calculateTransfer(input: input)
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
        TransferPortAreaResultCard(result: transferResult, hasInput: hasInput)
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

    private var hasInput: Bool {
        !bore.isEmpty || !stroke.isEmpty || !portHeight.isEmpty || !totalSpan.isEmpty
            || !topSpan.isEmpty || !bridgeWidth.isEmpty || !duration.isEmpty
    }

    private var exhaustResult: PortAreaResult? {
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
        return PortAreaCalculator.shared.calculateExhaust(input: input)
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
        ExhaustPortAreaResultCard(
            result: exhaustResult,
            hasInput: hasInput,
            layout: exhaustLayout,
            boreMm: parseDouble(bore)
        )
    }
}

// MARK: - Counterweight

struct CounterweightCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var pistonWeight = "120"
    @State private var rodHalf = "80"
    @State private var bigEnd = "180"

    private var hasInput: Bool {
        !pistonWeight.isEmpty || !rodHalf.isEmpty || !bigEnd.isEmpty
    }

    private var factor: Double? {
        guard let piston = parseDouble(pistonWeight),
              let half = parseDouble(rodHalf),
              let big = parseDouble(bigEnd) else { return nil }

        return CounterweightFactorCalculator.shared.factorPercent(
            pistonWeightGrams: piston,
            connectingRodHalfGrams: half,
            bigEndWeightGrams: big
        )
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.counterweight),
                subtitle: S.calculatorDescription(.counterweight)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: L.t("counterweight_piston_label"), text: $pistonWeight, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
                DecimalField(label: L.t("counterweight_connecting_rod_half_label"), text: $rodHalf, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
                DecimalField(label: L.t("counterweight_big_end_label"), text: $bigEnd, suffix: L.t("vehicle_dynamics_unit_g"), enabled: editingEnabled)
            }

            CounterweightResultCard(factorPercent: factor, hasInput: hasInput)
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

    private var hasInput: Bool {
        !currentGrams.isEmpty || !currentRpm.isEmpty || !targetRpm.isEmpty
    }

    private var variatorResult: VariatorWeightCalculatorResult? {
        guard let grams = parseDouble(currentGrams),
              let current = parseDouble(currentRpm),
              let target = parseDouble(targetRpm) else { return nil }

        return VariatorWeightCalculator.shared.calculate(
            currentGrams: grams,
            currentRpm: current,
            targetRpm: target,
            rollerType: rollerType
        )
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

            VariatorWeightResultCard(
                result: variatorResult,
                currentWeight: parseDouble(currentGrams),
                hasInput: hasInput
            )
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

    private var hasInput: Bool {
        !outerDiameter.isEmpty || !innerDiameter.isEmpty || !length.isEmpty || !rollerRadius.isEmpty
    }

    private var flywheelResult: FlywheelInertiaResult? {
        guard let outer = parseDouble(outerDiameter),
              let inner = parseDouble(innerDiameter),
              let len = parseDouble(length),
              let roller = parseDouble(rollerRadius) else { return nil }

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
        return FlywheelInertiaCalculator.shared.inertiaFromGeometry(input: input)
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

            FlywheelGeometryResultCard(result: flywheelResult, hasInput: hasInput)
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

    private var hasInput: Bool {
        !mass.isEmpty || !powerPs.isEmpty || !engineRpm.isEmpty || !maxRpm.isEmpty
            || !wheelCircumference.isEmpty || !primaryPinion.isEmpty || !primaryGear.isEmpty
            || !secondaryPinion.isEmpty || !secondaryGear.isEmpty || !shiftRpm.isEmpty
            || !analysisSpeed.isEmpty || !targetSpeed.isEmpty
    }

    private var vehicleDynamicsResult: VehicleDynamicsResult? {
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
              let target = parseDouble(targetSpeed) else { return nil }

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
        return VehicleDynamicsCalculator.shared.calculate(input: input)
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

            VehicleDynamicsResultsSection(
                result: vehicleDynamicsResult,
                hasInput: hasInput,
                targetSpeed: parseDouble(targetSpeed)
            )
        }
    }
}
