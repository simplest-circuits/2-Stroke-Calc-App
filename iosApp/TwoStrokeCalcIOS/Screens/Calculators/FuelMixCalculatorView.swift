import SwiftUI
import sharedKit

private let fuelMixCustomPresetKey = "custom"
private let mlPerFluidOunce = 29.5735
private let litersPerUsGallon = 3.78541

private enum FuelInputMode: String {
    case fuel
    case oil
}

struct FuelMixCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var selectedPresetKey = FuelMixPreset.standard.name
    @State private var customRatioText = ""
    @State private var inputMode: FuelInputMode = .fuel
    @State private var inputAmount: Float = 5

    private let minFuelLiters = 0.5
    private let maxFuelLiters = 20.0
    private let fuelStepLiters = 0.1
    private let minOilMl = 5.0
    private let maxOilMl = 800.0
    private let oilStepMl = 1.0

    private let fuelPresets: [FuelMixPreset] = [
        .standard, .older, .classic, .racing, .synthetic,
    ]

    private var ratioPartsFuel: Int32? {
        if selectedPresetKey == fuelMixCustomPresetKey {
            guard let parsed = parseDouble(customRatioText) else { return nil }
            let rounded = Int32(parsed.rounded())
            return rounded > 0 ? rounded : nil
        }
        return fuelPresets.first(where: { $0.name == selectedPresetKey })?.ratio
    }

    private var snappedFuelLiters: Double {
        FuelMixCalculator.shared.snapFuelLiters(fuelLiters: Double(inputAmount))
    }

    private var snappedOilMl: Double {
        FuelMixCalculator.shared.snapOilMl(oilMl: Double(inputAmount))
    }

    private var resultOilMl: Double? {
        guard let ratio = ratioPartsFuel else { return nil }
        switch inputMode {
        case .fuel:
            return FuelMixCalculator.shared.oilMillilitersFromFuelLiters(
                fuelLiters: snappedFuelLiters,
                ratioPartsFuel: ratio
            )?.asDouble
        case .oil:
            return snappedOilMl
        }
    }

    private var resultFuelL: Double? {
        guard let ratio = ratioPartsFuel else { return nil }
        switch inputMode {
        case .oil:
            return FuelMixCalculator.shared.fuelLitersFromOilMilliliters(
                oilMilliliters: snappedOilMl,
                ratioPartsFuel: ratio
            )?.asDouble
        case .fuel:
            return snappedFuelLiters
        }
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: L.t("fuel_mix_calculator_title"),
                subtitle: L.t("fuel_mix_calculator_subtitle")
            )

            let presetOptions = fuelPresets.map { preset in
                (key: preset.name, title: fuelPresetLabel(preset))
            } + [(key: fuelMixCustomPresetKey, title: L.t("fuel_mix_preset_custom"))]

            CalculatorChoiceField(
                label: L.t("fuel_mix_presets_label"),
                options: presetOptions,
                selectedKey: selectedPresetKey,
                supportingText: fuelPresetSupportingText,
                enabled: editingEnabled,
                onOptionSelected: { selectedPresetKey = $0 }
            )

            if selectedPresetKey == fuelMixCustomPresetKey {
                DecimalField(
                    label: L.t("fuel_mix_custom_ratio_label"),
                    text: $customRatioText,
                    hint: L.t("fuel_mix_custom_ratio_hint"),
                    enabled: editingEnabled
                )
            } else if let ratio = ratioPartsFuel {
                Text(L.tf("fuel_mix_ratio_display", String(ratio)))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            CalculatorInputModeSwitch(
                optionALabel: L.t("fuel_mix_input_mode_fuel"),
                optionBLabel: L.t("fuel_mix_input_mode_oil"),
                useOptionA: inputMode == .fuel,
                enabled: editingEnabled,
                onUseOptionAChange: { useFuel in
                    switchInputMode(useFuel ? .fuel : .oil)
                }
            )

            if inputMode == .fuel {
                CalculatorSliderField(
                    label: L.t("fuel_mix_fuel_amount_label"),
                    value: Binding(
                        get: { Double(inputAmount) },
                        set: { inputAmount = Float($0) }
                    ),
                    range: minFuelLiters...maxFuelLiters,
                    step: fuelStepLiters,
                    valueDisplay: L.tf(
                        "fuel_mix_fuel_slider_value",
                        formatFuelLiters(Double(inputAmount))
                    ),
                    enabled: editingEnabled
                )
            } else {
                CalculatorSliderField(
                    label: L.t("fuel_mix_oil_amount_label"),
                    value: Binding(
                        get: { Double(inputAmount) },
                        set: { inputAmount = Float($0) }
                    ),
                    range: minOilMl...maxOilMl,
                    step: oilStepMl,
                    valueDisplay: L.tf(
                        "fuel_mix_oil_slider_value",
                        formatOilMilliliters(Double(inputAmount))
                    ),
                    enabled: editingEnabled
                )
            }

            FuelMixResultCard(
                ratioPartsFuel: ratioPartsFuel,
                inputMode: inputMode,
                resultOilMl: resultOilMl,
                resultFuelL: resultFuelL
            )
        }
    }

    private var fuelPresetSupportingText: String? {
        if selectedPresetKey == fuelMixCustomPresetKey {
            return L.t("fuel_mix_custom_ratio_hint")
        }
        guard let preset = fuelPresets.first(where: { $0.name == selectedPresetKey }) else { return nil }
        return fuelPresetDescription(preset)
    }

    private func switchInputMode(_ newMode: FuelInputMode) {
        guard newMode != inputMode, let ratio = ratioPartsFuel else {
            inputMode = newMode
            return
        }
        switch (inputMode, newMode) {
        case (.fuel, .oil):
            if let oil = FuelMixCalculator.shared.oilMillilitersFromFuelLiters(
                fuelLiters: snappedFuelLiters,
                ratioPartsFuel: ratio
            ) {
                inputAmount = Float(FuelMixCalculator.shared.snapOilMl(oilMl: oil.asDouble))
            }
        case (.oil, .fuel):
            if let fuel = FuelMixCalculator.shared.fuelLitersFromOilMilliliters(
                oilMilliliters: snappedOilMl,
                ratioPartsFuel: ratio
            ) {
                inputAmount = Float(FuelMixCalculator.shared.snapFuelLiters(fuelLiters: fuel.asDouble))
            }
        default:
            break
        }
        inputMode = newMode
    }

    private func fuelPresetLabel(_ preset: FuelMixPreset) -> String {
        switch preset {
        case .standard: return L.t("fuel_mix_preset_standard")
        case .older: return L.t("fuel_mix_preset_older")
        case .classic: return L.t("fuel_mix_preset_classic")
        case .racing: return L.t("fuel_mix_preset_racing")
        case .synthetic: return L.t("fuel_mix_preset_synthetic")
        default: return preset.name
        }
    }

    private func fuelPresetDescription(_ preset: FuelMixPreset) -> String {
        switch preset {
        case .standard: return L.t("fuel_mix_preset_standard_desc")
        case .older: return L.t("fuel_mix_preset_older_desc")
        case .classic: return L.t("fuel_mix_preset_classic_desc")
        case .racing: return L.t("fuel_mix_preset_racing_desc")
        case .synthetic: return L.t("fuel_mix_preset_synthetic_desc")
        default: return ""
        }
    }

    private func formatFuelLiters(_ value: Double) -> String {
        fmt(FuelMixCalculator.shared.snapFuelLiters(fuelLiters: value), decimals: 1)
    }

    private func formatOilMilliliters(_ value: Double) -> String {
        let snapped = FuelMixCalculator.shared.snapOilMl(oilMl: value)
        return snapped >= 100 ? fmt(snapped, decimals: 0) : fmt(snapped, decimals: 1)
    }
}

private struct FuelMixResultCard: View {
    let ratioPartsFuel: Int32?
    let inputMode: FuelInputMode
    let resultOilMl: Double?
    let resultFuelL: Double?

    var body: some View {
        CalculatorResultCard("") {
            if let ratioPartsFuel {
                Text(L.tf("fuel_mix_result_header", String(ratioPartsFuel)))
                    .font(.headline)

                switch inputMode {
                case .fuel:
                    if let oil = resultOilMl {
                        PrimaryResultText(
                            text: L.tf("fuel_mix_result_oil_amount", formatAmount(oil))
                        )
                        SecondaryResultText(
                            text: L.tf(
                                "fuel_mix_result_oil_fluid_ounces",
                                formatAmount(oil / mlPerFluidOunce)
                            )
                        )
                    }
                case .oil:
                    if let fuel = resultFuelL {
                        PrimaryResultText(
                            text: L.tf("fuel_mix_result_fuel_amount", formatAmount(fuel))
                        )
                        SecondaryResultText(
                            text: L.tf(
                                "fuel_mix_result_fuel_us_gallons",
                                formatAmount(fuel / litersPerUsGallon)
                            )
                        )
                    }
                }
            } else {
                CalculatorEmptyResultText(text: L.t("fuel_mix_result_invalid_ratio"))
            }
        }
    }

    private func formatAmount(_ value: Double) -> String {
        fmt(value, decimals: 2)
    }
}
