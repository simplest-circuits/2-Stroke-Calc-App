import SwiftUI
import sharedKit

// MARK: - Exhaust (expansion chamber)

struct ExhaustCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    @State private var presetName = ExpansionChamberPreset.enduro.name
    @State private var exhaustWidthText = "40"
    @State private var exhaustHeightText = "20"
    @State private var exhaustDurationText = "180"
    @State private var transferDurationText = "130"
    @State private var rpmText = "9000"
    @State private var powerPsText = "30"
    @State private var displacementText = "125"
    @State private var hornCoeffText = "1.5"
    @State private var k0Text = "0.70"
    @State private var k1Text = "1.125"
    @State private var k2Text = "2.25"
    @State private var bmepOverrideText = ""
    @State private var exhaustTempOverrideText = ""
    @State private var speedOfSoundOverrideText = ""
    @State private var gammaText = "1.4"
    @State private var gasConstantText = "287"
    @State private var portAreaOverrideText = ""
    @State private var eqPortDiameterOverrideText = ""
    @State private var diffuserStagesName = ExpansionChamberDiffuserStages.three.name
    @State private var suppressCustomMark = false

    private let allPresets: [ExpansionChamberPreset] = [.enduro, .cross, .grandPrix, .custom]
    private let allDiffuserStages: [ExpansionChamberDiffuserStages] = [.one, .two, .three]

    private var preset: ExpansionChamberPreset {
        allPresets.first(where: { $0.name == presetName }) ?? .enduro
    }

    private var diffuserStages: ExpansionChamberDiffuserStages {
        allDiffuserStages.first(where: { $0.name == diffuserStagesName }) ?? .three
    }

    private var isLandscape: Bool {
        verticalSizeClass == .compact
    }

    private var exhaustWidth: Double? { parseDouble(exhaustWidthText) }
    private var exhaustHeight: Double? { parseDouble(exhaustHeightText) }
    private var exhaustDuration: Double? { parseDouble(exhaustDurationText) }
    private var transferDuration: Double? { parseDouble(transferDurationText) }
    private var rpm: Double? { parseDouble(rpmText) }
    private var powerPs: Double? { parseDouble(powerPsText) }
    private var displacement: Double? { parseDouble(displacementText) }
    private var hornCoeff: Double? { parseDouble(hornCoeffText) }
    private var k0: Double? { parseDouble(k0Text) }
    private var k1: Double? { parseDouble(k1Text) }
    private var k2: Double? { parseDouble(k2Text) }
    private var bmepOverride: Double? { parseDouble(bmepOverrideText) }
    private var exhaustTempOverride: Double? { parseDouble(exhaustTempOverrideText) }
    private var speedOfSoundOverride: Double? { parseDouble(speedOfSoundOverrideText) }
    private var gamma: Double? { parseDouble(gammaText) }
    private var gasConstant: Double? { parseDouble(gasConstantText) }
    private var portAreaOverride: Double? { parseDouble(portAreaOverrideText) }
    private var eqPortDiameterOverride: Double? { parseDouble(eqPortDiameterOverrideText) }

    private var autoBmep: Double? {
        guard let powerPs, let displacement, let rpm else { return nil }
        return ExpansionChamberCalculator.shared.calculateBmepBar(
            powerPs: powerPs,
            displacementCc: displacement,
            rpm: rpm
        )?.asDouble
    }

    private var autoExhaustTemp: Double? {
        if let bmepOverride {
            return ExpansionChamberCalculator.shared.exhaustTemperatureFromBmep(bmepBar: bmepOverride).asDouble
        }
        if let autoBmep {
            return ExpansionChamberCalculator.shared.exhaustTemperatureFromBmep(bmepBar: autoBmep).asDouble
        }
        return nil
    }

    private var autoSpeedOfSound: Double? {
        let temp = exhaustTempOverride ?? autoExhaustTemp
        guard let temp else { return nil }
        return ExpansionChamberCalculator.shared.calculateSpeedOfSoundMs(
            exhaustGasTempCelsius: temp,
            specificHeatRatio: gamma ?? 1.4,
            gasConstant: gasConstant ?? 287
        )?.asDouble
    }

    private var autoPortArea: Double? {
        guard let exhaustWidth, let exhaustHeight else { return nil }
        return exhaustWidth * exhaustHeight
    }

    private var autoEqPortDiameter: Double? {
        if let autoPortArea {
            return ExpansionChamberCalculator.shared.calculateEquivalentPortDiameterMm(portAreaMm2: autoPortArea)?.asDouble
        }
        return eqPortDiameterOverride
    }

    private var hasInput: Bool {
        [
            exhaustWidthText, exhaustHeightText, exhaustDurationText, transferDurationText,
            rpmText, powerPsText, displacementText, hornCoeffText, k0Text, k1Text, k2Text,
            bmepOverrideText, exhaustTempOverrideText, speedOfSoundOverrideText,
            gammaText, gasConstantText, portAreaOverrideText, eqPortDiameterOverrideText,
        ].contains { !$0.isEmpty }
    }

    private var hasPortGeometry: Bool {
        (exhaustWidth != nil && exhaustHeight != nil && (exhaustWidth ?? 0) > 0 && (exhaustHeight ?? 0) > 0)
            || (portAreaOverride != nil && (portAreaOverride ?? 0) > 0)
            || (eqPortDiameterOverride != nil && (eqPortDiameterOverride ?? 0) > 0)
    }

    private var hasWaveInputs: Bool {
        (bmepOverride != nil && (bmepOverride ?? 0) > 0)
            || (powerPs != nil && displacement != nil && (powerPs ?? 0) > 0 && (displacement ?? 0) > 0)
    }

    private var result: ExpansionChamberResult? {
        guard hasPortGeometry,
              let exhaustDuration, let transferDuration, let rpm,
              rpm > 0, exhaustDuration > 0, transferDuration > 0,
              hasWaveInputs,
              let k0, let k1, let k2,
              k0 > 0, k1 > 0, k2 > 0,
              diffuserStages != .one || (hornCoeff != nil && (hornCoeff ?? 0) > 0),
              gamma == nil || (gamma ?? 0) > 0,
              gasConstant == nil || (gasConstant ?? 0) > 0
        else { return nil }

        let horn = hornCoeff ?? 0
        if diffuserStages != .one && horn <= 0 { return nil }

        let input = ExpansionChamberInput(
            exhaustPortWidthMm: positiveKotlinDouble(exhaustWidth),
            exhaustPortHeightMm: positiveKotlinDouble(exhaustHeight),
            exhaustPortDurationDeg: exhaustDuration,
            transferPortDurationDeg: transferDuration,
            rpm: rpm,
            powerPs: positiveKotlinDouble(powerPs),
            displacementCc: positiveKotlinDouble(displacement),
            diffuserStages: diffuserStages,
            coefficients: ExpansionChamberCoefficients(
                k0: k0,
                k1: k1,
                k2: k2,
                hornCoefficient: horn
            ),
            optionalInputs: ExpansionChamberOptionalInputs(
                bmepBar: positiveKotlinDouble(bmepOverride),
                exhaustGasTempCelsius: kotlinTemp(exhaustTempOverride),
                speedOfSoundMs: positiveKotlinDouble(speedOfSoundOverride),
                specificHeatRatio: positiveKotlinDouble(gamma),
                gasConstant: positiveKotlinDouble(gasConstant),
                portAreaMm2: positiveKotlinDouble(portAreaOverride),
                equivalentPortDiameterMm: positiveKotlinDouble(eqPortDiameterOverride)
            )
        )
        return ExpansionChamberCalculator.shared.calculate(input: input)
    }

    var body: some View {
        Group {
            if isLandscape, let result {
                ExhaustPipeLandscapeView(result: result)
            } else if !isLandscape {
                calculatorContent
            } else {
                landscapeEmptyState
            }
        }
    }

    private var landscapeEmptyState: some View {
        VStack(spacing: 12) {
            Text(L.t("calculator_tab_exhaust"))
                .font(.headline)
            Text(L.t("exhaust_fullscreen_empty"))
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var calculatorContent: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.exhaust),
                subtitle: S.calculatorDescription(.exhaust)
            )

            CalculatorSection(L.t("exhaust_section_design")) {
                CalculatorChoiceField(
                    label: L.t("exhaust_diffuser_stages_label"),
                    options: allDiffuserStages.map { stages in
                        (key: stages.name, title: "\(stages.stageCount) Segmente")
                    },
                    selectedKey: diffuserStages.name,
                    supportingText: diffuserStagesHint(diffuserStages),
                    enabled: editingEnabled,
                    onOptionSelected: { key in
                        diffuserStagesName = key
                        applyPreset(.custom)
                    }
                )
            }

            CalculatorSection(L.t("exhaust_section_preset")) {
                CalculatorChoiceField(
                    label: "",
                    options: allPresets.map { item in
                        (key: item.name, title: exhaustPresetLabel(item))
                    },
                    selectedKey: preset.name,
                    enabled: editingEnabled,
                    onOptionSelected: { key in
                        if let selected = allPresets.first(where: { $0.name == key }) {
                            applyPreset(selected)
                        }
                    }
                )
            }

            CalculatorSection(L.t("exhaust_section_port")) {
                CalculatorFieldRow {
                    DecimalField(label: L.t("exhaust_port_width_label"), text: boundText($exhaustWidthText), suffix: L.t("pt_mm"), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                    DecimalField(label: L.t("exhaust_port_height_label"), text: boundText($exhaustHeightText), suffix: L.t("pt_mm"), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                }
                CalculatorFieldRow {
                    DecimalField(label: L.t("pt_exhaust_duration"), text: boundText($exhaustDurationText), suffix: L.t("exhaust_unit_deg"), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                    DecimalField(label: L.t("pt_transfer_duration"), text: boundText($transferDurationText), suffix: L.t("exhaust_unit_deg"), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                }
            }

            CalculatorSection(L.t("vehicles_tab_engine")) {
                DecimalField(label: L.t("exhaust_rpm_label"), text: boundText($rpmText), suffix: L.t("gear_unit_rpm"), enabled: editingEnabled)
                CalculatorFieldRow {
                    DecimalField(label: L.t("dc_cable_input_mode_power"), text: boundText($powerPsText), suffix: L.t("flywheel_unit_ps"), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                    DecimalField(label: L.t("compression_result_displacement"), text: boundText($displacementText), suffix: L.t("exhaust_unit_cc"), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                }
            }

            CalculatorSection(L.t("exhaust_section_coefficients")) {
                if diffuserStages != .one {
                    DecimalField(
                        label: L.t("exhaust_horn_coeff_label"),
                        text: boundText($hornCoeffText),
                        hint: L.t("exhaust_horn_coeff_hint"),
                        enabled: editingEnabled
                    )
                }
                CalculatorFieldRow {
                    DecimalField(label: L.t("exhaust_k1_label"), text: boundText($k1Text), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                    DecimalField(label: L.t("exhaust_k2_label"), text: boundText($k2Text), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                    DecimalField(label: L.t("exhaust_k0_label"), text: boundText($k0Text), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                }
                Text(L.t("exhaust_formula_hint"))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            CalculatorCollapsibleSection(L.t("exhaust_section_advanced")) {
                Text(L.t("exhaust_section_advanced_hint"))
                    .font(.caption2)
                    .foregroundStyle(.secondary)

                DecimalField(
                    label: L.t("exhaust_result_bmep"),
                    text: boundText($bmepOverrideText),
                    suffix: L.t("exhaust_unit_bar"),
                    hint: bmepOverrideText.isEmpty ? autoHint(autoBmep, decimals: 2) : nil,
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("exhaust_result_exhaust_temp"),
                    text: boundText($exhaustTempOverrideText),
                    suffix: L.t("carb_jet_unit_celsius"),
                    hint: exhaustTempOverrideText.isEmpty ? autoHint(autoExhaustTemp, decimals: 0) : nil,
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("exhaust_optional_speed_of_sound_label"),
                    text: boundText($speedOfSoundOverrideText),
                    suffix: L.t("exhaust_unit_ms"),
                    hint: speedOfSoundOverrideText.isEmpty ? autoHint(autoSpeedOfSound, decimals: 1) : nil,
                    enabled: editingEnabled
                )
                CalculatorFieldRow {
                    DecimalField(label: L.t("exhaust_result_gamma"), text: boundText($gammaText), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                    DecimalField(label: L.t("exhaust_result_gas_constant"), text: boundText($gasConstantText), enabled: editingEnabled)
                        .frame(maxWidth: .infinity)
                }
                CalculatorFieldRow {
                    DecimalField(
                        label: L.t("exhaust_result_port_area"),
                        text: boundText($portAreaOverrideText),
                        suffix: L.t("exhaust_unit_mm2"),
                        hint: portAreaOverrideText.isEmpty ? autoHint(autoPortArea, decimals: 0) : nil,
                        enabled: editingEnabled
                    )
                    .frame(maxWidth: .infinity)
                    DecimalField(
                        label: L.t("exhaust_optional_eq_port_diameter_label"),
                        text: boundText($eqPortDiameterOverrideText),
                        suffix: L.t("pt_mm"),
                        hint: eqPortDiameterOverrideText.isEmpty ? autoHint(autoEqPortDiameter, decimals: 2) : nil,
                        enabled: editingEnabled
                    )
                    .frame(maxWidth: .infinity)
                }
            }

            exhaustResultsCard

            CalculatorSection(L.t("exhaust_section_preview")) {
                if let result {
                    ExhaustPipeDiagramView(result: result)
                    Text(L.t("exhaust_preview_rotate_hint"))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
                } else {
                    CalculatorEmptyResultText(L.t("exhaust_result_empty"))
                }
            }
        }
    }

    private func boundText(_ storage: Binding<String>) -> Binding<String> {
        Binding(
            get: { storage.wrappedValue },
            set: { newValue in
                storage.wrappedValue = newValue
                markCustom()
            }
        )
    }

    private func applyPreset(_ selected: ExpansionChamberPreset) {
        suppressCustomMark = true
        presetName = selected.name
        guard selected != .custom else {
            suppressCustomMark = false
            return
        }
        switch selected {
        case .enduro:
            hornCoeffText = "1.5"
            k0Text = "0.70"
            k1Text = "1.125"
            k2Text = "2.25"
        case .cross:
            hornCoeffText = "1.8"
            k0Text = "0.62"
            k1Text = "1.10"
            k2Text = "2.50"
        case .grandPrix:
            hornCoeffText = "1.0"
            k0Text = "0.58"
            k1Text = "1.05"
            k2Text = "3.00"
        default:
            break
        }
        suppressCustomMark = false
    }

    private func markCustom() {
        guard !suppressCustomMark, preset != .custom else { return }
        presetName = ExpansionChamberPreset.custom.name
    }

    @ViewBuilder
    private var exhaustResultsCard: some View {
        CalculatorResultCard(L.t("pt_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(L.t("exhaust_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(L.t("exhaust_result_invalid"))
            } else if let result {
                let manualHint = L.t("exhaust_optional_manual_hint")
                CalculatorResultRow(
                    label: L.t("exhaust_result_port_area"),
                    value: "\(fmt(result.portAreaMm2, decimals: 0)) mm²",
                    hint: result.usedManualPortArea ? manualHint : nil
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_eq_port_diameter"),
                    value: "\(fmt(result.equivalentPortDiameterMm, decimals: 2)) mm",
                    hint: result.usedManualEquivalentPortDiameter ? manualHint : nil
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_diffuser_stages"),
                    value: "\(result.diffuserStageCount) Segmente"
                )
                if result.diffuserStageCount > 1 {
                    CalculatorResultRow(
                        label: L.t("exhaust_horn_coeff_label"),
                        value: fmt(result.hornCoefficient, decimals: 2)
                    )
                }
                CalculatorResultRow(
                    label: L.t("exhaust_result_tuned_length"),
                    value: "\(fmt(result.tunedLengthMm, decimals: 0)) mm"
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_total_length"),
                    value: "\(fmt(result.totalLengthMm, decimals: 0)) mm"
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("exhaust_result_bmep"),
                    value: "\(fmt(result.bmepBar, decimals: 2)) bar",
                    hint: result.usedManualBmep ? manualHint : nil
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_exhaust_temp"),
                    value: "\(fmt(result.exhaustGasTempCelsius, decimals: 0)) °C",
                    hint: result.usedManualExhaustTemp ? manualHint : nil
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_speed_of_sound"),
                    value: "\(fmt(result.speedOfSoundMs, decimals: 1)) m/s",
                    hint: result.usedManualSpeedOfSound ? manualHint : nil
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_gamma"),
                    value: fmt(result.specificHeatRatio, decimals: 2)
                )
                CalculatorResultRow(
                    label: L.t("exhaust_result_gas_constant"),
                    value: "\(fmt(result.gasConstant, decimals: 0)) J/(kg·K)"
                )
                CalculatorResultDivider()
                ForEach(Array(result.segments.enumerated()), id: \.offset) { index, segment in
                    if index > 0 {
                        CalculatorResultDivider()
                    }
                    CalculatorResultRow(
                        label: exhaustSegmentLabel(segment.id),
                        value: L.tf("exhaust_segment_value", fmt(segment.lengthMm, decimals: 0), fmt(segment.startDiameterMm, decimals: 1), fmt(segment.endDiameterMm, decimals: 1))
                    )
                }
            }
        }
    }

    private func autoHint(_ value: Double?, decimals: Int) -> String? {
        guard let value else { return nil }
        return L.tf("exhaust_optional_auto_value", fmt(value, decimals: decimals))
    }

    private func exhaustPresetLabel(_ preset: ExpansionChamberPreset) -> String {
        switch preset {
        case .enduro: return L.t("exhaust_preset_enduro")
        case .cross: return L.t("exhaust_preset_cross")
        case .grandPrix: return L.t("exhaust_preset_gp")
        case .custom: return L.t("gear_preset_custom")
        default: return preset.name
        }
    }

    private func diffuserStagesHint(_ stages: ExpansionChamberDiffuserStages) -> String {
        switch stages {
        case .one:
            return L.t("exhaust_diffuser_stages_hint_one")
        case .two:
            return L.t("exhaust_diffuser_stages_hint_two")
        case .three:
            return L.t("exhaust_diffuser_stages_hint_three")
        default:
            return ""
        }
    }
}

private func positiveKotlinDouble(_ value: Double?) -> KotlinDouble? {
    guard let value, value > 0 else { return nil }
    return KotlinDouble(value: value)
}

private func kotlinTemp(_ value: Double?) -> KotlinDouble? {
    guard let value, value > -273 else { return nil }
    return KotlinDouble(value: value)
}
