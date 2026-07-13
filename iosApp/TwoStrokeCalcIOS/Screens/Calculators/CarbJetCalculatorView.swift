import SwiftUI
import sharedKit

struct CarbJetCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var baseJet = "165"
    @State private var refAltitude = "0"
    @State private var refTemp = "15"
    @State private var targetAltitude = "500"
    @State private var targetTemp = "20"

    private var baseJetValue: Int32? { parsePositiveInt(baseJet) }
    private var referenceAltitude: Double? { parseDouble(refAltitude) }
    private var referenceTemperature: Double? { parseDouble(refTemp) }
    private var targetAltitudeValue: Double? { parseDouble(targetAltitude) }
    private var targetTemperature: Double? { parseDouble(targetTemp) }

    private var hasInput: Bool {
        baseJetValue != nil
            && referenceAltitude != nil
            && referenceTemperature != nil
            && targetAltitudeValue != nil
            && targetTemperature != nil
    }

    private var result: CarbJetCorrectionResult? {
        guard hasInput,
              let jet = baseJetValue,
              let refAlt = referenceAltitude,
              let refT = referenceTemperature,
              let tgtAlt = targetAltitudeValue,
              let tgtT = targetTemperature else { return nil }
        return CarbJetCorrectionCalculator.shared.calculate(
            baseMainJet: jet,
            referenceAltitudeM: refAlt,
            referenceTemperatureC: refT,
            targetAltitudeM: tgtAlt,
            targetTemperatureC: tgtT
        )
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: L.t("carb_jet_calculator_title"),
                subtitle: L.t("carb_jet_calculator_subtitle")
            )

            CalculatorSection(L.t("carb_jet_section_reference")) {
                DecimalField(
                    label: L.t("carb_jet_base_main_jet_label"),
                    text: $baseJet,
                    hint: L.t("carb_jet_base_main_jet_hint"),
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("carb_jet_reference_altitude_label"),
                    text: $refAltitude,
                    suffix: L.t("carb_jet_unit_m"),
                    hint: L.t("carb_jet_reference_altitude_hint"),
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("carb_jet_reference_temperature_label"),
                    text: $refTemp,
                    suffix: L.t("carb_jet_unit_celsius"),
                    enabled: editingEnabled
                )
            }

            CalculatorSection(L.t("carb_jet_section_target")) {
                DecimalField(
                    label: L.t("carb_jet_target_altitude_label"),
                    text: $targetAltitude,
                    suffix: L.t("carb_jet_unit_m"),
                    enabled: editingEnabled
                )
                DecimalField(
                    label: L.t("carb_jet_target_temperature_label"),
                    text: $targetTemp,
                    suffix: L.t("carb_jet_unit_celsius"),
                    enabled: editingEnabled
                )
            }

            Text(L.t("carb_jet_formula_hint"))
                .font(.caption)
                .foregroundStyle(.secondary)

            CarbJetResultCard(result: result, hasInput: hasInput)
        }
    }
}

private struct CarbJetResultCard: View {
    let result: CarbJetCorrectionResult?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("carb_jet_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("carb_jet_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("carb_jet_result_invalid"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf("carb_jet_result_main_jet", String(result.correctedMainJet))
                )
                SecondaryResultText(
                    text: L.tf(
                        "carb_jet_result_main_jet_exact",
                        fmt(result.correctedMainJetExact, decimals: 1)
                    )
                )
                SecondaryResultText(
                    text: L.tf(
                        "carb_jet_result_factor",
                        fmt(result.correctionFactor * 100, decimals: 1)
                    )
                )
                SecondaryResultText(text: L.t("carb_jet_result_rounding_hint"))
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("carb_jet_result_reference_pressure"),
                    value: L.tf(
                        "carb_jet_result_pressure_value",
                        fmt(result.referencePressureMbar, decimals: 0)
                    )
                )
                CalculatorResultRow(
                    label: L.t("carb_jet_result_target_pressure"),
                    value: L.tf(
                        "carb_jet_result_pressure_value",
                        fmt(result.targetPressureMbar, decimals: 0)
                    )
                )
            }
        }
    }
}
