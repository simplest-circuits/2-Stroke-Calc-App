import SwiftUI
import sharedKit

// MARK: - Port area helpers

func portAssessmentLabel(_ assessment: PortAreaAssessment) -> String {
    switch assessment {
    case .criticalLow: return L.t("port_area_assessment_critical")
    case .series: return L.t("port_area_assessment_series")
    case .sporty: return L.t("port_area_assessment_sporty")
    case .aggressive: return L.t("port_area_assessment_aggressive")
    default: return assessment.name
    }
}

func portAssessmentColor(_ assessment: PortAreaAssessment, colors: ThemeColors) -> Color {
    switch assessment {
    case .criticalLow: return colors.error
    case .series: return colors.onSurfaceVariant
    case .sporty: return colors.primary
    case .aggressive: return .orange
    default: return colors.onSurface
    }
}

private func formatCrossSection(_ mm2: Double) -> String {
    mm2 < 10.0 ? fmt(mm2, decimals: 2) : fmt(mm2, decimals: 1)
}

private func formatPortAreaDecimal(_ value: Double, decimals: Int) -> String {
    fmt(value, decimals: decimals)
}

private func formatElectrolyteLiters(_ value: Double) -> String {
    fmt(value, decimals: 1)
}

private func formatElectrolyteMilliliters(_ value: Double) -> String {
    value >= 100.0 ? fmt(value, decimals: 0) : fmt(value, decimals: 1)
}

private func formatElectrolyteGrams(_ value: Double) -> String {
    value >= 100.0 ? fmt(value, decimals: 0) : fmt(value, decimals: 1)
}

private func formatElectrolytePercent(_ value: Double) -> String {
    fmt(value.rounded(), decimals: 0)
}

private func formatCleaningPercent(_ value: Double) -> String {
    fmt(value, decimals: 1)
}

private func formatSignedRpm(_ value: Double) -> String {
    let formatted = fmt(abs(value), decimals: 0)
    if value > 0 { return "+\(formatted)" }
    if value < 0 { return "-\(formatted)" }
    return "0"
}

private func formatWeightChange(currentWeight: Double, newWeight: Double) -> String {
    let delta = VariatorWeightCalculator.shared.roundToTenth(grams: newWeight - currentWeight)
    if delta < 0 {
        return L.tf("variator_weight_result_lighter", fmt(abs(delta), decimals: 1))
    }
    if delta > 0 {
        return L.tf("variator_weight_result_heavier", fmt(delta, decimals: 1))
    }
    return L.t("variator_weight_result_unchanged")
}

private func formatElectrificationDuration(hours: Double) -> String {
    let totalMinutes = Int((hours * 60.0).rounded())
    let displayHours = totalMinutes / 60
    let displayMinutes = totalMinutes % 60
    if displayHours > 0 {
        return L.tf("electrolyte_zinc_time_value_hours_minutes", String(displayHours), String(displayMinutes))
    }
    return L.tf("electrolyte_zinc_time_value_minutes", String(displayMinutes))
}

private func chordRatingLabel(_ rating: ExhaustChordRating) -> String {
    switch rating {
    case .conservative: return L.t("port_area_exhaust_chord_rating_conservative")
    case .everyday: return L.t("port_area_exhaust_chord_rating_everyday")
    case .seriesSport: return L.t("port_area_exhaust_chord_rating_series")
    case .tuner: return L.t("port_area_exhaust_chord_rating_tuner")
    default: return rating.name
    }
}

// MARK: - DC cable

struct DcCableResultCard: View {
    let result: DcCableCrossSectionCalculatorResult?
    let hasInput: Bool
    var showCurrentRow: Bool = false

    var body: some View {
        CalculatorResultCard(L.t("dc_cable_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("dc_cable_result_empty"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf(
                        "dc_cable_result_recommended",
                        formatCrossSection(result.recommendedCrossSectionMm2)
                    )
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("dc_cable_result_minimum_label"),
                    value: L.tf(
                        "dc_cable_result_minimum_value",
                        formatCrossSection(result.minimumCrossSectionMm2)
                    )
                )
                if showCurrentRow {
                    CalculatorResultRow(
                        label: L.t("dc_cable_result_current_label"),
                        value: L.tf(
                            "dc_cable_result_current_value",
                            fmt(result.currentAmps, decimals: 2)
                        )
                    )
                }
                CalculatorResultRow(
                    label: L.t("dc_cable_result_voltage_drop_label"),
                    value: L.tf(
                        "dc_cable_result_voltage_drop_value",
                        fmt(result.voltageDropVolts, decimals: 2),
                        fmt(result.voltageDropPercent, decimals: 2)
                    )
                )
                SecondaryResultText(
                    text: L.tf(
                        "dc_cable_result_drop_limit",
                        fmt(result.maxAllowedDropVolts, decimals: 2),
                        fmt(result.maxAllowedDropPercent, decimals: 1)
                    )
                )
            } else {
                CalculatorEmptyResultText(text: L.t("dc_cable_result_empty"))
            }
        }
    }
}

// MARK: - Mean pressure

struct MeanPressureResultCard: View {
    let result: MeanPressureResult?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("mean_pressure_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("mean_pressure_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("mean_pressure_result_invalid"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf("mean_pressure_result_bar", fmt(result.meanPressureBar, decimals: 2))
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("mean_pressure_result_power_watts"),
                    value: L.tf("mean_pressure_result_value_watts", fmt(result.powerWatts, decimals: 0))
                )
                CalculatorResultRow(
                    label: L.t("mean_pressure_result_torque"),
                    value: L.tf("mean_pressure_result_value_nm", fmt(result.torqueNm, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.t("mean_pressure_result_displacement"),
                    value: L.tf("mean_pressure_result_value_ccm", fmt(result.displacementCcm, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.t("mean_pressure_result_specific_power"),
                    value: L.tf(
                        "mean_pressure_result_value_ps_per_l",
                        fmt(result.specificPowerPsPerLiter, decimals: 1)
                    )
                )
            }
        }
    }
}

// MARK: - Squish band

struct SquishBandResultCard: View {
    let result: SquishBandResult?
    let hasInput: Bool
    let mode: SquishBandCalculationMode

    var body: some View {
        CalculatorResultCard(L.t("squish_band_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: emptyText)
            } else if result == nil {
                CalculatorInvalidResultText(text: invalidText)
            } else if let result {
                switch mode {
                case .percent:
                    PrimaryResultText(
                        text: L.tf("squish_band_result_dome", fmt(result.domeDiameterMm, decimals: 2))
                    )
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label: L.t("squish_band_result_width"),
                        value: L.tf("squish_band_result_value_mm", fmt(result.squishBandWidthMm, decimals: 2))
                    )
                case .width:
                    PrimaryResultText(
                        text: L.tf("squish_band_result_area", fmt(result.squishAreaPercent, decimals: 1))
                    )
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label: L.t("squish_band_result_dome_label"),
                        value: L.tf("squish_band_result_value_mm", fmt(result.domeDiameterMm, decimals: 2))
                    )
                default:
                    EmptyView()
                }
            }
        }
    }

    private var emptyText: String {
        switch mode {
        case .percent: return L.t("squish_band_result_empty_percent")
        case .width: return L.t("squish_band_result_empty_width")
        default: return L.t("squish_band_result_empty_percent")
        }
    }

    private var invalidText: String {
        switch mode {
        case .percent: return L.t("squish_band_result_invalid_percent")
        case .width: return L.t("squish_band_result_invalid_width")
        default: return L.t("squish_band_result_invalid_percent")
        }
    }
}

// MARK: - Compression

struct CompressionForwardResultCard: View {
    let result: CompressionResult?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("compression_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("compression_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("compression_result_invalid"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf("compression_result_ratio", fmt(result.compressionRatio, decimals: 2))
                )
                CalculatorTertiaryResultText(text: L.t("compression_result_ratio_hint"))
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("compression_result_displacement"),
                    value: L.tf("compression_result_value_ml", fmt(result.displacementMl, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.t("compression_result_squish_volume"),
                    value: L.tf("compression_result_value_ml", fmt(result.squishBandVolumeMl, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.t("compression_result_squish_area"),
                    value: L.tf("compression_result_value_percent", fmt(result.squishAreaPercent, decimals: 1)),
                    hint: L.t("compression_result_squish_area_hint")
                )
                CalculatorResultRow(
                    label: L.t("compression_result_bore_stroke_ratio"),
                    value: fmt(result.boreStrokeRatio, decimals: 2),
                    hint: L.t("compression_result_bore_stroke_hint")
                )
            }
        }
    }
}

struct CompressionTargetResultCard: View {
    let result: CompressionTargetResult?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("compression_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("compression_target_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("compression_target_result_invalid"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf(
                        "compression_target_result_ratio",
                        fmt(result.targetCompressionRatio, decimals: 2)
                    )
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("compression_result_displacement"),
                    value: L.tf("compression_result_value_ml", fmt(result.displacementMl, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.t("compression_target_result_total_chamber"),
                    value: L.tf("compression_result_value_ml", fmt(result.totalChamberVolumeMl, decimals: 2)),
                    hint: L.t("compression_target_result_total_chamber_hint")
                )
                if let domeVolume = result.domeVolumeMl {
                    CalculatorResultRow(
                        label: L.t("compression_target_result_dome"),
                        value: L.tf("compression_result_value_ml", fmt(domeVolume, decimals: 2)),
                        hint: L.t("compression_target_result_dome_hint")
                    )
                }
            }
        }
    }
}

struct CompressionChangeResultCard: View {
    let result: CompressionChangeResult?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("compression_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("compression_change_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("compression_change_result_invalid"))
            } else if let result {
                let removingVolume = result.volumeToRemoveMl >= 0
                PrimaryResultText(
                    text: removingVolume
                        ? L.tf("compression_change_result_remove_depth", fmt(result.millingDepthMm, decimals: 2))
                        : L.tf("compression_change_result_add_depth", fmt(result.millingDepthMm, decimals: 2))
                )
                CalculatorTertiaryResultText(text: L.t("compression_change_result_depth_hint"))
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: removingVolume
                        ? L.t("compression_change_result_volume_remove")
                        : L.t("compression_change_result_volume_add"),
                    value: L.tf(
                        "compression_result_value_ml",
                        fmt(abs(result.volumeToRemoveMl), decimals: 2)
                    ),
                    hint: removingVolume
                        ? L.t("compression_change_result_remove_hint")
                        : L.t("compression_change_result_add_hint")
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("compression_result_displacement"),
                    value: L.tf("compression_result_value_ml", fmt(result.displacementMl, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.tf(
                        "compression_change_result_current_chamber",
                        fmt(result.currentCompressionRatio, decimals: 2)
                    ),
                    value: L.tf("compression_result_value_ml", fmt(result.currentChamberVolumeMl, decimals: 2))
                )
                CalculatorResultRow(
                    label: L.tf(
                        "compression_change_result_target_chamber",
                        fmt(result.targetCompressionRatio, decimals: 2)
                    ),
                    value: L.tf("compression_result_value_ml", fmt(result.targetChamberVolumeMl, decimals: 2))
                )
            }
        }
    }
}

// MARK: - Electrolyte

struct ElectrolyteResultCard: View {
    let result: ElectrolyteCalculatorResult?
    let loadVoltage: Double?

    var body: some View {
        CalculatorResultCard(L.t("electrolyte_result_title")) {
            if result == nil {
                CalculatorInvalidResultText(text: L.t("electrolyte_result_invalid"))
            } else if let result {
                CalculatorResultRow(
                    label: L.t("electrolyte_result_acid_label"),
                    value: L.tf(
                        "electrolyte_result_acid_value",
                        formatElectrolyteMilliliters(result.aceticAcidMl),
                        formatElectrolytePercent(result.acidConcentrationPercent)
                    )
                )
                CalculatorResultRow(
                    label: L.t("electrolyte_result_water_label"),
                    value: L.tf("electrolyte_result_water_value", formatElectrolyteLiters(result.waterLiters))
                )
                CalculatorResultRow(
                    label: L.t("electrolyte_result_salt_label"),
                    value: L.tf("electrolyte_result_salt_value", formatElectrolyteGrams(result.saltG))
                )
                if let loadVoltage {
                    CalculatorResultRow(
                        label: L.t("electrolyte_voltage_label"),
                        value: L.tf("electrolyte_voltage_value", fmt(loadVoltage, decimals: 1))
                    )
                }
                if let zinc = result.zincDissolution {
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label: L.t("electrolyte_zinc_target_label"),
                        value: L.tf(
                            "electrolyte_zinc_target_value",
                            formatElectrolyteGrams(zinc.targetDissolvedZincG)
                        )
                    )
                    CalculatorResultRow(
                        label: L.t("electrolyte_zinc_rate_label"),
                        value: L.tf(
                            "electrolyte_zinc_rate_value",
                            fmt(zinc.dissolutionRateGPerHour, decimals: 1)
                        )
                    )
                    CalculatorResultRow(
                        label: L.t("electrolyte_zinc_time_label"),
                        value: formatElectrificationDuration(hours: zinc.electrificationHours)
                    )
                    CalculatorResultRow(
                        label: L.t("electrolyte_zinc_power_label"),
                        value: L.tf("electrolyte_zinc_power_value", fmt(zinc.powerWatts, decimals: 1))
                    )
                }
            }
        }
    }
}

// MARK: - Cleaning agent

struct CleaningAgentResultCard: View {
    let result: CleaningAgentCalculatorResult?

    var body: some View {
        CalculatorResultCard(L.t("cleaning_agent_result_title")) {
            if let result {
                CalculatorResultRow(
                    label: L.t("cleaning_agent_result_agent_label"),
                    value: L.tf(
                        "cleaning_agent_result_agent_value",
                        formatElectrolyteMilliliters(result.cleaningAgentMl),
                        formatCleaningPercent(result.concentrationPercent)
                    )
                )
                CalculatorResultRow(
                    label: L.t("cleaning_agent_result_water_label"),
                    value: L.tf(
                        "cleaning_agent_result_water_value",
                        formatElectrolyteLiters(result.waterLiters)
                    )
                )
            }
        }
    }
}

// MARK: - Counterweight

struct CounterweightResultCard: View {
    let factorPercent: Double?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("counterweight_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("counterweight_result_empty"))
            } else if factorPercent == nil {
                CalculatorInvalidResultText(text: L.t("counterweight_result_invalid"))
            } else if let factorPercent {
                PrimaryResultText(
                    text: L.tf("counterweight_result_factor", fmt(factorPercent, decimals: 2))
                )
            }
        }
    }
}

// MARK: - Variator weight

struct VariatorWeightResultCard: View {
    let result: VariatorWeightCalculatorResult?
    let currentWeight: Double?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("variator_weight_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("variator_weight_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("variator_weight_result_invalid"))
            } else if let result {
                let physicsRounded = VariatorWeightCalculator.shared.roundToTenth(grams: result.physicsWeightGrams)
                let empiricalRounded = VariatorWeightCalculator.shared.roundToTenth(grams: result.empiricalWeightGrams)

                PrimaryResultText(
                    text: L.tf("variator_weight_result_physics", fmt(physicsRounded, decimals: 1))
                )
                if let currentWeight {
                    SecondaryResultText(
                        text: formatWeightChange(currentWeight: currentWeight, newWeight: physicsRounded)
                    )
                }
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("variator_weight_result_empirical_label"),
                    value: L.tf("variator_weight_result_empirical_value", fmt(empiricalRounded, decimals: 1))
                )
                if let currentWeight {
                    CalculatorResultRow(
                        label: L.t("variator_weight_result_change_label"),
                        value: formatWeightChange(currentWeight: currentWeight, newWeight: empiricalRounded)
                    )
                }
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("variator_weight_result_target_rpm_label"),
                    value: fmt(result.targetRpm, decimals: 0)
                )
                CalculatorResultRow(
                    label: L.t("variator_weight_result_rpm_delta_label"),
                    value: formatSignedRpm(result.rpmDelta)
                )
            }
        }
    }
}

// MARK: - Flywheel geometry

struct FlywheelGeometryResultCard: View {
    let result: FlywheelInertiaResult?
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("flywheel_result_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("flywheel_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("flywheel_result_invalid"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf("flywheel_result_inertia", fmt(result.inertiaKgm2, decimals: 4))
                )
                if let mass = result.massKg {
                    CalculatorResultRow(
                        label: L.t("flywheel_result_mass_label"),
                        value: L.tf("flywheel_result_mass_value", fmt(mass, decimals: 1))
                    )
                }
                if let equivalentMass = result.equivalentMassKg {
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label: L.t("flywheel_result_equivalent_mass_label"),
                        value: L.tf("flywheel_result_equivalent_mass_value", fmt(equivalentMass, decimals: 1)),
                        hint: L.t("flywheel_result_equivalent_mass_hint")
                    )
                }
            }
        }
    }
}

// MARK: - Port area

private struct PortAreaAssessmentText: View {
    @Environment(\.themeColors) private var colors
    let assessment: PortAreaAssessment

    var body: some View {
        Text(portAssessmentLabel(assessment))
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(portAssessmentColor(assessment, colors: colors))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct TransferPortAreaResultCard: View {
    let result: PortAreaResult?
    let hasInput: Bool

    var body: some View {
        PortAreaResultCardContent(
            title: L.t("port_area_transfer_result_title"),
            result: result,
            hasInput: hasInput,
            ratioLabel: L.t("port_area_transfer_bore_ratio_label"),
            layout: nil,
            boreMm: nil
        )
    }
}

struct ExhaustPortAreaResultCard: View {
    let result: PortAreaResult?
    let hasInput: Bool
    var layout: PortGeometryLayout? = nil
    var boreMm: Double? = nil

    var body: some View {
        PortAreaResultCardContent(
            title: L.t("port_area_exhaust_result_title"),
            result: result,
            hasInput: hasInput,
            ratioLabel: L.t("port_area_exhaust_bore_ratio_label"),
            layout: layout,
            boreMm: boreMm
        )
    }
}

private struct PortAreaResultCardContent: View {
    @Environment(\.themeColors) private var colors

    let title: String
    let result: PortAreaResult?
    let hasInput: Bool
    let ratioLabel: String
    let layout: PortGeometryLayout?
    let boreMm: Double?

    private var chordMetrics: ExhaustChordMetrics? {
        guard let layout, let boreMm, boreMm > 0 else { return nil }
        return PortAreaCalculator.shared.exhaustChordMetrics(boreMm: boreMm, layout: layout)
    }

    var body: some View {
        CalculatorResultCard(title) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("port_area_result_empty"))
            } else if result == nil {
                CalculatorInvalidResultText(text: L.t("port_area_result_invalid"))
            } else if let result {
                PrimaryResultText(
                    text: L.tf(
                        "port_area_result_time_area",
                        formatPortAreaDecimal(result.timeAreaMm2Deg, decimals: 0)
                    )
                )
                PortAreaAssessmentText(assessment: result.assessment)
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("port_area_result_area"),
                    value: L.tf(
                        "port_area_result_area_value",
                        formatPortAreaDecimal(result.areaMm2, decimals: 0)
                    )
                )
                CalculatorResultRow(
                    label: ratioLabel,
                    value: formatPortAreaDecimal(result.areaToBoreRatio, decimals: 3)
                )
                CalculatorResultRow(
                    label: L.t("port_area_result_ta_per_cc"),
                    value: L.tf(
                        "port_area_result_ta_per_cc_value",
                        formatPortAreaDecimal(result.timeAreaPerCc, decimals: 0)
                    ),
                    hint: L.t("port_area_result_ta_per_cc_hint")
                )
                CalculatorResultRow(
                    label: L.t("port_area_result_displacement"),
                    value: L.tf(
                        "port_area_result_displacement_value",
                        formatPortAreaDecimal(result.displacementCc, decimals: 1)
                    )
                )
                if let chordMetrics {
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label: L.t("port_area_exhaust_chord_label"),
                        value: L.tf(
                            "port_area_exhaust_chord_value",
                            formatPortAreaDecimal(chordMetrics.topChordMm, decimals: 1),
                            formatPortAreaDecimal(chordMetrics.chordPercentOfBore, decimals: 0)
                        ),
                        hint: L.t("port_area_exhaust_chord_percent_hint")
                    )
                    CalculatorResultRow(
                        label: L.t("port_area_exhaust_ut_width_label"),
                        value: L.tf(
                            "port_area_exhaust_ut_width_value",
                            formatPortAreaDecimal(chordMetrics.bottomWidthUtMm, decimals: 1)
                        ),
                        hint: L.t("port_area_exhaust_ut_width_result_hint")
                    )
                    if let rating = PortAreaCalculator.shared.rateChordPercent(chordPercent: chordMetrics.chordPercentOfBore) {
                        CalculatorTertiaryResultText(text: chordRatingLabel(rating))
                    }
                }
            }
        }
    }
}

// MARK: - Vehicle dynamics

struct VehicleDynamicsResultsSection: View {
    let result: VehicleDynamicsResult?
    let hasInput: Bool
    var targetSpeed: Double? = nil

    var body: some View {
        if !hasInput {
            EmptyView()
        } else if result == nil {
            CalculatorResultCard(L.t("vehicle_dynamics_result_instant_title")) {
                CalculatorInvalidResultText(text: L.t("vehicle_dynamics_result_invalid"))
            }
        } else if let result {
            if result.selectedGear != nil {
                VehicleDynamicsInstantResultCard(result: result, hasInput: hasInput)
            }
            if !result.gearSnapshots.isEmpty {
                VehicleDynamicsGearComparisonCard(result: result, hasInput: hasInput)
            }
            if result.topSpeedKmh != nil {
                VehicleDynamicsTopSpeedCard(result: result, hasInput: hasInput)
            }
            if result.sprintToTarget != nil {
                VehicleDynamicsSprintCard(result: result, hasInput: hasInput, targetSpeed: targetSpeed)
            }
            if result.quarterMileEstimateSeconds != nil {
                VehicleDynamicsQuarterMileCard(result: result, hasInput: hasInput)
            }
        }
    }
}

private struct VehicleDynamicsInstantResultCard: View {
    let result: VehicleDynamicsResult
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("vehicle_dynamics_result_instant_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("vehicle_dynamics_result_empty"))
            } else if let gear = result.selectedGear {
                PrimaryResultText(
                    text: L.tf(
                        "vehicle_dynamics_result_acceleration_g",
                        fmt(gear.accelerationG, decimals: 2)
                    )
                )
                if gear.tractionLimited {
                    CalculatorTertiaryResultText(text: L.t("vehicle_dynamics_traction_limited_hint"))
                }
                CalculatorResultDivider()
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_acceleration_ms2"),
                    value: "\(fmt(gear.accelerationMs2, decimals: 2)) m/s²"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_drive_force"),
                    value: "\(fmt(gear.forces.driveForceN, decimals: 0)) N"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_net_force"),
                    value: "\(fmt(gear.forces.netForceN, decimals: 0)) N"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_torque"),
                    value: "\(fmt(result.torqueAtEngineNm, decimals: 1)) Nm"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_rpm_at_speed"),
                    value: "\(fmt(gear.engineRpmAtAnalysisSpeed, decimals: 0)) \(L.t("vehicle_dynamics_unit_rpm"))"
                )
            } else {
                CalculatorInvalidResultText(text: L.t("vehicle_dynamics_result_invalid"))
            }
        }
    }
}

private struct VehicleDynamicsGearComparisonCard: View {
    let result: VehicleDynamicsResult
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("vehicle_dynamics_result_gear_comparison_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("vehicle_dynamics_result_empty"))
            } else {
                ForEach(Array(result.gearSnapshots.enumerated()), id: \.offset) { _, gear in
                    CalculatorResultRow(
                        label: L.tf(
                            "vehicle_dynamics_gear_row_label",
                            String(gear.stageNumber),
                            fmt(gear.gearRatio, decimals: 2)
                        ),
                        value: L.tf(
                            "vehicle_dynamics_gear_row_value",
                            fmt(gear.speedAtEngineRpmKmh, decimals: 1),
                            fmt(gear.accelerationG, decimals: 2)
                        ),
                        hint: L.tf(
                            "vehicle_dynamics_gear_row_hint",
                            fmt(gear.maxSpeedInGearKmh ?? 0, decimals: 1)
                        )
                    )
                }
            }
        }
    }
}

private struct VehicleDynamicsTopSpeedCard: View {
    let result: VehicleDynamicsResult
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("vehicle_dynamics_result_top_speed_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("vehicle_dynamics_result_empty"))
            } else if let topSpeed = result.topSpeedKmh {
                PrimaryResultText(
                    text: L.tf("vehicle_dynamics_result_top_speed_value", fmt(topSpeed, decimals: 1))
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_top_speed_gear"),
                    value: L.tf("vehicle_dynamics_gear_chip", String(result.topSpeedGear ?? 0))
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_top_speed_limit"),
                    value: topSpeedLimitText(result.topSpeedLimit)
                )
            } else {
                CalculatorInvalidResultText(text: L.t("vehicle_dynamics_result_invalid"))
            }
        }
    }

    private func topSpeedLimitText(_ limit: TopSpeedLimit?) -> String {
        switch limit {
        case .aerodynamic: return L.t("vehicle_dynamics_limit_aero")
        case .rpm: return L.t("vehicle_dynamics_limit_rpm")
        default: return "–"
        }
    }
}

private struct VehicleDynamicsSprintCard: View {
    let result: VehicleDynamicsResult
    let hasInput: Bool
    let targetSpeed: Double?

    var body: some View {
        CalculatorResultCard(L.t("vehicle_dynamics_result_sprint_title")) {
            if !hasInput || targetSpeed == nil {
                CalculatorEmptyResultText(text: L.t("vehicle_dynamics_result_empty"))
            } else if let sprint = result.sprintToTarget, let targetSpeed {
                PrimaryResultText(
                    text: L.tf(
                        "vehicle_dynamics_result_sprint_time",
                        fmt(sprint.timeSeconds, decimals: 1),
                        fmt(targetSpeed, decimals: 0)
                    )
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_sprint_distance"),
                    value: "\(fmt(sprint.distanceMeters, decimals: 0)) m"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_sprint_final_speed"),
                    value: "\(fmt(sprint.finalSpeedKmh, decimals: 1)) km/h"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_sprint_shifts"),
                    value: String(sprint.gearShifts)
                )
                if !sprint.reachedTarget {
                    CalculatorTertiaryResultText(text: L.t("vehicle_dynamics_sprint_not_reached_hint"))
                }
            } else {
                CalculatorInvalidResultText(text: L.t("vehicle_dynamics_result_invalid"))
            }
        }
    }
}

private struct VehicleDynamicsQuarterMileCard: View {
    let result: VehicleDynamicsResult
    let hasInput: Bool

    var body: some View {
        CalculatorResultCard(L.t("vehicle_dynamics_result_quarter_mile_title")) {
            if !hasInput {
                CalculatorEmptyResultText(text: L.t("vehicle_dynamics_result_empty"))
            } else if let et = result.quarterMileEstimateSeconds {
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_quarter_mile_et"),
                    value: "\(fmt(et, decimals: 2)) s"
                )
                CalculatorResultRow(
                    label: L.t("vehicle_dynamics_result_quarter_mile_trap"),
                    value: "\(fmt(result.quarterMileTrapSpeedKmh ?? 0, decimals: 1)) km/h"
                )
                CalculatorTertiaryResultText(text: L.t("vehicle_dynamics_quarter_mile_hint"))
            } else {
                CalculatorInvalidResultText(text: L.t("vehicle_dynamics_result_invalid"))
            }
        }
    }
}
