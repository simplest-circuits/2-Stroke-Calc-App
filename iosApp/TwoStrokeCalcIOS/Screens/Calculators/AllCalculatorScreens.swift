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
    case .criticalLow: return "Kritisch niedrig"
    case .series: return "Serie"
    case .sporty: return "Sportlich"
    case .aggressive: return "Aggressiv"
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
            lines.append("Auslass: \(fmt(exhaust.openBeforeBdc, decimals: 1))° vUT, Dauer \(fmt(exhaust.duration, decimals: 1))°")
        }
        if let transfer = result.transfer {
            lines.append("Transfer: \(fmt(transfer.openBeforeBdc, decimals: 1))° vUT, Dauer \(fmt(transfer.duration, decimals: 1))°")
        }
        if let blowdown = result.blowdown {
            lines.append("Vorauslass: \(fmt(blowdown, decimals: 1))°")
        }
        if let intake = result.intake {
            lines.append("Einlass vOT: \(fmt(intake.openBeforeTdc, decimals: 1))°")
            lines.append("Einlass nOT: \(fmt(intake.closeAfterTdc, decimals: 1))°")
            lines.append("Einlasszeit: \(fmt(intake.duration, decimals: 1))°")
        }
        return lines
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.timing),
                subtitle: "Steuerzeiten aus Stichmaßen und Kolbenstand berechnen."
            )

            CalculatorSection("Einlassart") {
                Picker("Einlassart", selection: $intakeSystem) {
                    Text("Drehschieber").tag(IntakeSystem.rotaryValve)
                    Text("Membran").tag(IntakeSystem.reedValve)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Hub", text: $stroke, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Pleuel", text: $connectingRod, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Auslass", text: $exhaustPort, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Transfer", text: $transferPort, suffix: "mm", enabled: editingEnabled)
                if intakeSystem == .rotaryValve {
                    DecimalField(label: "Einlass auf", text: $intakeOpen, suffix: "mm", enabled: editingEnabled)
                    DecimalField(label: "Einlass zu", text: $intakeClose, suffix: "mm", enabled: editingEnabled)
                }
                DecimalField(
                    label: "Kolbenstand",
                    text: $pistonDeck,
                    suffix: "mm",
                    hint: "Abstand Kolbenoberkante zur Zylinderoberkante bei UT; negativ = Überstand",
                    enabled: editingEnabled
                )
            }

            ResultCard(title: S.resultTitle, lines: resultLines)

            if let result = portTimingResult {
                CalculatorSection("Diagramm") {
                    PortTimingDiagramView(result: result, showIntake: intakeSystem == .rotaryValve)
                }
            }
        }
    }
}

// MARK: - Ignition timing

struct IgnitionTimingCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var stroke = "57"
    @State private var connectingRod = "110"
    @State private var degrees = "2.5"
    @State private var millimeters = ""

    private var resultLines: [String] {
        guard let strokeMm = parseDouble(stroke),
              let rodMm = parseDouble(connectingRod) else { return [] }

        if let deg = parseDouble(degrees), !degrees.isEmpty {
            if let mm = IgnitionTimingCalculator.shared.degreesBeforeTdcToMm(
                strokeMm: strokeMm,
                connectingRodMm: rodMm,
                degreesBeforeTdc: deg
            ) {
                return ["Kolbenstand vor OT: \(fmt(mm, decimals: 2)) mm"]
            }
        } else if let mm = parseDouble(millimeters), !millimeters.isEmpty {
            if let deg = IgnitionTimingCalculator.shared.mmBeforeTdcToDegrees(
                strokeMm: strokeMm,
                connectingRodMm: rodMm,
                mmBeforeTdc: mm
            ) {
                return ["Zündzeitpunkt vor OT: \(fmt(deg, decimals: 2))°"]
            }
        }
        return []
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.ignition),
                subtitle: "Zündzeitpunkt vor OT in Grad und Millimeter gegenseitig umrechnen."
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Hub", text: $stroke, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Pleuel", text: $connectingRod, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Zündzeitpunkt vor OT", text: $degrees, suffix: "°", enabled: editingEnabled)
                DecimalField(label: "Kolbenstand vor OT", text: $millimeters, suffix: "mm", enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
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
            "Mindestquerschnitt: \(fmt(result.minimumCrossSectionMm2, decimals: 2)) mm²",
            "Empfohlen (Norm): \(fmt(result.recommendedCrossSectionMm2, decimals: 2)) mm²",
            "Spannungsfall: \(fmt(result.voltageDropVolts, decimals: 2)) V (\(fmt(result.voltageDropPercent, decimals: 2)) %)",
            "Zulässig: \(fmt(result.maxAllowedDropVolts, decimals: 2)) V (\(fmt(result.maxAllowedDropPercent, decimals: 1)) %)",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.dcCable),
                subtitle: "Mindestquerschnitt für Gleichstrom-Leitungen nach Spannungsfall."
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Betriebsspannung", text: $voltage, suffix: "V", enabled: editingEnabled)
                DecimalField(label: "Strom", text: $current, suffix: "A", enabled: editingEnabled)
                DecimalField(label: "Leitungslänge (einfach)", text: $length, suffix: "m", enabled: editingEnabled)
                DecimalField(label: "Max. Spannungsfall", text: $maxDropPercent, suffix: "%", enabled: editingEnabled)
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

            Picker("Tab", selection: $selectedTab) {
                Text("Elektrolyt").tag(0)
                Text("Reinigungsmittel").tag(1)
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
            "Essigsäure: \(fmt(result.aceticAcidMl, decimals: 0)) ml (\(fmt(result.acidConcentrationPercent, decimals: 0)) %)",
            "Wasser: \(fmt(result.waterLiters, decimals: 2)) l",
            "Salz: \(fmt(result.saltG, decimals: 0)) g (\(fmt(result.saltGPerL, decimals: 1)) g/l)",
            "Gesamt: \(fmt(result.totalLiquidLiters, decimals: 1)) l",
            "Essigsäureanteil: \(fmt(result.finalAceticAcidPercent, decimals: 1)) %",
        ]
        if let zinc = result.zincDissolution {
            lines += [
                "Zinkauflösung: \(fmt(zinc.targetDissolvedZincG, decimals: 0)) g",
                "Spannung: \(fmt(zinc.voltageVolts, decimals: 1)) V bei \(fmt(zinc.currentAmps, decimals: 1)) A",
                "Dauer: \(fmt(zinc.electrificationHours, decimals: 1)) h",
            ]
        }
        return lines
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: "Gesamtvolumen", text: $totalLiters, suffix: "l", enabled: editingEnabled)
            DecimalField(label: "Salz", text: $saltG, suffix: "g", enabled: editingEnabled)
            DecimalField(label: "Säurekonzentration", text: $acidConcentration, suffix: "%", enabled: editingEnabled)
            DecimalField(label: "Stromstärke", text: $current, suffix: "A", enabled: editingEnabled)
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
            "Reinigungsmittel: \(fmt(result.cleaningAgentMl, decimals: 0)) ml",
            "Wasser: \(fmt(result.waterLiters, decimals: 2)) l",
            "Konzentration: \(fmt(result.concentrationPercent, decimals: 1)) %",
            "Gesamt: \(fmt(result.totalLiters, decimals: 1)) l",
        ]
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: "Gesamtvolumen", text: $totalLiters, suffix: "l", enabled: editingEnabled)
            DecimalField(label: "Konzentration", text: $concentration, suffix: "%", enabled: editingEnabled)
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
                "Verdichtung: \(fmt(result.compressionRatio, decimals: 2)):1",
                "Hubraum: \(fmt(result.displacementMl, decimals: 1)) ml",
                "Quetschband-Volumen: \(fmt(result.squishBandVolumeMl, decimals: 2)) ml",
                "Quetschfläche: \(fmt(result.squishAreaPercent, decimals: 1)) %",
                "Bohrung/Hub: \(fmt(result.boreStrokeRatio, decimals: 2))",
            ]

        case .target:
            guard let target = parseDouble(targetCompression),
                  let result = CompressionCalculator.shared.calculateTarget(
                    boreMm: boreMm,
                    strokeMm: strokeMm,
                    targetCompressionRatio: target,
                    pistonConstantMl: parseDouble(pistonConstant)
                  ) else { return [] }
            var lines = [
                "Brennraum gesamt: \(fmt(result.totalChamberVolumeMl, decimals: 2)) ml",
                "Hubraum: \(fmt(result.displacementMl, decimals: 1)) ml",
                "Ziel-Verdichtung: \(fmt(result.targetCompressionRatio, decimals: 2)):1",
            ]
            if let dome = result.domeVolumeMl {
                lines.append("Glockenvolumen: \(fmt(dome, decimals: 2)) ml")
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
                "Volumen entfernen: \(fmt(result.volumeToRemoveMl, decimals: 2)) ml",
                "Frästiefe (Plan): \(fmt(result.millingDepthMm, decimals: 3)) mm",
                "Aktuell: \(fmt(result.currentCompressionRatio, decimals: 2)):1 → Ziel: \(fmt(result.targetCompressionRatio, decimals: 2)):1",
                "Brennraum aktuell: \(fmt(result.currentChamberVolumeMl, decimals: 2)) ml",
                "Brennraum Ziel: \(fmt(result.targetChamberVolumeMl, decimals: 2)) ml",
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

            CalculatorSection("Modus") {
                Picker("Modus", selection: $mode) {
                    Text("Vorwärts").tag(CompressionCalculationMode.forward)
                    Text("Ziel-CR").tag(CompressionCalculationMode.target)
                    Text("Ändern").tag(CompressionCalculationMode.change)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Bohrung", text: $bore, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Hub", text: $stroke, suffix: "mm", enabled: editingEnabled)

                if mode == .forward {
                    DecimalField(label: "Glockendurchmesser", text: $domeDiameter, suffix: "mm", enabled: editingEnabled)
                    DecimalField(label: "Glockenvolumen", text: $domeVolume, suffix: "ml", enabled: editingEnabled)
                    DecimalField(label: "Quetschband", text: $squishBand, suffix: "mm", enabled: editingEnabled)
                }
                if mode == .target {
                    DecimalField(label: "Ziel-Verdichtung", text: $targetCompression, suffix: ":1", enabled: editingEnabled)
                    DecimalField(label: "Kolbenkonstante (optional)", text: $pistonConstant, suffix: "ml", enabled: editingEnabled)
                }
                if mode == .change {
                    DecimalField(label: "Aktuelle Verdichtung", text: $currentCompression, suffix: ":1", enabled: editingEnabled)
                    DecimalField(label: "Ziel-Verdichtung", text: $targetCompression, suffix: ":1", enabled: editingEnabled)
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
            "Quetschfläche: \(fmt(r.squishAreaPercent, decimals: 1)) %",
            "Quetschkante: \(fmt(r.squishBandWidthMm, decimals: 2)) mm",
            "Glockendurchmesser: \(fmt(r.domeDiameterMm, decimals: 2)) mm",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.squishBand),
                subtitle: S.calculatorDescription(.squishBand)
            )

            CalculatorSection("Modus") {
                Picker("Modus", selection: $mode) {
                    Text("Prozent").tag(SquishBandCalculationMode.percent)
                    Text("Breite").tag(SquishBandCalculationMode.width)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Bohrung", text: $bore, suffix: "mm", enabled: editingEnabled)
                if mode == .percent {
                    DecimalField(label: "Quetschfläche", text: $squishPercent, suffix: "%", enabled: editingEnabled)
                } else {
                    DecimalField(label: "Quetschkantenbreite", text: $squishWidth, suffix: "mm", enabled: editingEnabled)
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
            "Mitteldruck: \(fmt(result.meanPressureBar, decimals: 2)) bar",
            "Drehmoment: \(fmt(result.torqueNm, decimals: 2)) Nm",
            "Hubraum: \(fmt(result.displacementCcm, decimals: 1)) ccm",
            "Literleistung: \(fmt(result.specificPowerPsPerLiter, decimals: 1)) PS/l",
            "Leistung: \(fmt(result.powerWatts / 1000.0, decimals: 2)) kW",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.meanPressure),
                subtitle: S.calculatorDescription(.meanPressure)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Leistung", text: $powerPs, suffix: "PS", enabled: editingEnabled)
                DecimalField(label: "Drehzahl", text: $rpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Bohrung", text: $bore, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Hub", text: $stroke, suffix: "mm", enabled: editingEnabled)
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
            let speed = stage.speedsAtReferenceRpmKmh.last?.asDouble ?? stage.shiftSpeedKmh
            var line = "Gang \(index + 1): \(fmt(speed, decimals: 1)) km/h (i=\(fmt(stage.gearRatio, decimals: 2)))"
            if let jump = stage.speedJumpKmh?.asDouble {
                line += ", Sprung +\(fmt(jump, decimals: 1)) km/h"
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
                DecimalField(label: "Radumfang", text: $wheelCircumference, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Primär Ritzel", text: $primaryPinion, enabled: editingEnabled)
                DecimalField(label: "Primär Rad", text: $primaryGear, enabled: editingEnabled)
                DecimalField(label: "Sekundär Ritzel", text: $secondaryPinion, enabled: editingEnabled)
                DecimalField(label: "Sekundär Rad", text: $secondaryGear, enabled: editingEnabled)
                DecimalField(label: "Referenz-Drehzahl", text: $referenceRpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Schalt-Drehzahl", text: $shiftRpm, suffix: "U/min", enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)

            if !resultLines.isEmpty {
                CalculatorSection("Diagramm") {
                    GearChartView(speedLines: resultLines)
                }
            }
        }
    }
}

// MARK: - Exhaust (expansion chamber)

struct ExhaustCalculatorView: View {
    @Environment(\.calculatorEditingEnabled) private var editingEnabled

    @State private var portWidth = "60"
    @State private var portHeight = "28"
    @State private var exhaustDuration = "180"
    @State private var transferDuration = "120"
    @State private var rpm = "9000"
    @State private var powerPs = "45"
    @State private var displacementCc = "125"
    @State private var diffuserStages = ExpansionChamberDiffuserStages.two

    private var resultLines: [String] {
        guard let width = parseDouble(portWidth),
              let height = parseDouble(portHeight),
              let exhaustDeg = parseDouble(exhaustDuration),
              let transferDeg = parseDouble(transferDuration),
              let rpmVal = parseDouble(rpm),
              let ps = parseDouble(powerPs),
              let cc = parseDouble(displacementCc) else { return [] }

        let coeffs = ExpansionChamberCoefficients(
            k0: 0.70,
            k1: 1.125,
            k2: 2.25,
            hornCoefficient: 1.5
        )
        let input = ExpansionChamberInput(
            exhaustPortWidthMm: width,
            exhaustPortHeightMm: height,
            exhaustPortDurationDeg: exhaustDeg,
            transferPortDurationDeg: transferDeg,
            rpm: rpmVal,
            powerPs: ps,
            displacementCc: cc,
            diffuserStages: diffuserStages,
            coefficients: coeffs,
            optionalInputs: ExpansionChamberOptionalInputs()
        )
        guard let result = ExpansionChamberCalculator.shared.calculate(input: input) else { return [] }

        return [
            "Abstimm-Länge: \(fmt(result.tunedLengthMm, decimals: 0)) mm",
            "Gesamtlänge: \(fmt(result.totalLengthMm, decimals: 0)) mm",
            "Portfläche: \(fmt(result.portAreaMm2, decimals: 1)) mm²",
            "Äquivalent-Ø: \(fmt(result.equivalentPortDiameterMm, decimals: 1)) mm",
            "BMEP: \(fmt(result.bmepBar, decimals: 2)) bar",
            "Schallgeschwindigkeit: \(fmt(result.speedOfSoundMs, decimals: 0)) m/s",
            "Diffusor-Segmente: \(result.diffuserStageCount)",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.exhaust),
                subtitle: S.calculatorDescription(.exhaust)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Auslass Breite", text: $portWidth, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Auslass Höhe", text: $portHeight, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Auslasszeit", text: $exhaustDuration, suffix: "°", enabled: editingEnabled)
                DecimalField(label: "Transferzeit", text: $transferDuration, suffix: "°", enabled: editingEnabled)
                DecimalField(label: "Drehzahl", text: $rpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Leistung", text: $powerPs, suffix: "PS", enabled: editingEnabled)
                DecimalField(label: "Hubraum", text: $displacementCc, suffix: "ccm", enabled: editingEnabled)

                Picker("Diffusor", selection: $diffuserStages) {
                    Text("1-stufig").tag(ExpansionChamberDiffuserStages.one)
                    Text("2-stufig").tag(ExpansionChamberDiffuserStages.two)
                    Text("3-stufig").tag(ExpansionChamberDiffuserStages.three)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)

            if let result = expansionResult {
                CalculatorSection("Skizze") {
                    ExhaustPipeDiagramView(
                        headerLength: result.tunedLengthMm * 0.12,
                        diffuserLength: result.tunedLengthMm * 0.55,
                        tailLength: max(0, result.totalLengthMm - result.tunedLengthMm * 0.67)
                    )
                }
            }
        }
    }

    private var expansionResult: ExpansionChamberResult? {
        guard let width = parseDouble(portWidth),
              let height = parseDouble(portHeight),
              let exhaustDeg = parseDouble(exhaustDuration),
              let transferDeg = parseDouble(transferDuration),
              let rpmVal = parseDouble(rpm),
              let ps = parseDouble(powerPs),
              let cc = parseDouble(displacementCc) else { return nil }

        return ExpansionChamberCalculator.shared.calculate(input: ExpansionChamberInput(
            exhaustPortWidthMm: width,
            exhaustPortHeightMm: height,
            exhaustPortDurationDeg: exhaustDeg,
            transferPortDurationDeg: transferDeg,
            rpm: rpmVal,
            powerPs: ps,
            displacementCc: cc,
            diffuserStages: diffuserStages,
            coefficients: ExpansionChamberCoefficients(k0: 0.70, k1: 1.125, k2: 2.25, hornCoefficient: 1.5),
            optionalInputs: ExpansionChamberOptionalInputs()
        ))
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

            Picker("Tab", selection: $selectedTab) {
                Text("Transfer").tag(0)
                Text("Auslass").tag(1)
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

        let input = TransferPortInput(
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
            "Fläche: \(fmt(result.areaMm2, decimals: 1)) mm²",
            "Time-Area: \(fmt(result.timeAreaMm2Deg, decimals: 0)) mm²·°",
            "Fläche/Bohrung: \(fmt(result.areaToBoreRatio * 100, decimals: 1)) %",
            "TA/ccm: \(fmt(result.timeAreaPerCc, decimals: 0))",
            "Hubraum: \(fmt(result.displacementCc, decimals: 1)) ccm",
            "Bewertung: \(portAssessmentLabel(result.assessment))",
        ]
    }

    var body: some View {
        CalculatorSection(S.sectionInput) {
            DecimalField(label: "Bohrung", text: $bore, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Hub", text: $stroke, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Breite", text: $width, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Höhe", text: $height, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Kanäle", text: $channelCount, enabled: editingEnabled)
            DecimalField(label: "Seitenwinkel", text: $sideAngle, suffix: "°", enabled: editingEnabled)
            DecimalField(label: "Steuerzeit", text: $duration, suffix: "°", enabled: editingEnabled)
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
            "Fläche: \(fmt(result.areaMm2, decimals: 1)) mm²",
            "Time-Area: \(fmt(result.timeAreaMm2Deg, decimals: 0)) mm²·°",
            "Fläche/Bohrung: \(fmt(result.areaToBoreRatio * 100, decimals: 1)) %",
            "TA/ccm: \(fmt(result.timeAreaPerCc, decimals: 0))",
            "Hubraum: \(fmt(result.displacementCc, decimals: 1)) ccm",
            "Bewertung: \(portAssessmentLabel(result.assessment))",
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
            DecimalField(label: "Bohrung", text: $bore, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Hub", text: $stroke, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Port-Höhe", text: $portHeight, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "UT-Spannweite", text: $totalSpan, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "OT-Spannweite (Sehne)", text: $topSpan, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Stegbreite", text: $bridgeWidth, suffix: "mm", enabled: editingEnabled)
            DecimalField(label: "Steuerzeit", text: $duration, suffix: "°", enabled: editingEnabled)
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

        return ["Wuchtfaktor: \(fmt(factor, decimals: 1)) %"]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.counterweight),
                subtitle: S.calculatorDescription(.counterweight)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Kolbengewicht", text: $pistonWeight, suffix: "g", enabled: editingEnabled)
                DecimalField(label: "Pleuelhälfte", text: $rodHalf, suffix: "g", enabled: editingEnabled)
                DecimalField(label: "Meistergewicht", text: $bigEnd, suffix: "g", enabled: editingEnabled)
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
            "Physik: \(fmt(VariatorWeightCalculator.shared.roundToTenth(grams: result.physicsWeightGrams), decimals: 1)) g",
            "Empirie: \(fmt(VariatorWeightCalculator.shared.roundToTenth(grams: result.empiricalWeightGrams), decimals: 1)) g",
            "Δ Drehzahl: \(fmt(result.rpmDelta, decimals: 0)) U/min",
            "Ziel-Drehzahl: \(fmt(result.targetRpm, decimals: 0)) U/min",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.variatorWeight),
                subtitle: S.calculatorDescription(.variatorWeight)
            )

            CalculatorSection(S.sectionInput) {
                Picker("Typ", selection: $rollerType) {
                    Text("Rollen").tag(VariatorWeightRollerType.rollers)
                    Text("Gleitstücke").tag(VariatorWeightRollerType.sliders)
                }
                .pickerStyle(.segmented)
                .disabled(!editingEnabled)

                DecimalField(label: "Aktuelles Gewicht", text: $currentGrams, suffix: "g", enabled: editingEnabled)
                DecimalField(label: "Aktuelle Drehzahl", text: $currentRpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Ziel-Drehzahl", text: $targetRpm, suffix: "U/min", enabled: editingEnabled)
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
            "Ölmenge: \(fmt(oilMl, decimals: 0)) ml",
            "Benzin: \(fmt(liters, decimals: 1)) l",
            "Mischverhältnis: 1:\(ratio)",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.fuelMix),
                subtitle: S.calculatorDescription(.fuelMix)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Benzinmenge", text: $fuelLiters, suffix: "l", enabled: editingEnabled)
                DecimalField(label: "Mischverhältnis 1:", text: $ratio, enabled: editingEnabled)
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
            "Korrigierte Hauptdüse: \(result.correctedMainJet) (exakt \(fmt(result.correctedMainJetExact, decimals: 1)))",
            "Korrekturfaktor: \(fmt(result.correctionFactor, decimals: 4))",
            "Luftdichte Referenz: \(fmt(result.referenceAirDensity, decimals: 2))",
            "Luftdichte Ziel: \(fmt(result.targetAirDensity, decimals: 2))",
            "Druck Referenz: \(fmt(result.referencePressureMbar, decimals: 1)) mbar",
            "Druck Ziel: \(fmt(result.targetPressureMbar, decimals: 1)) mbar",
        ]
    }

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(
                title: S.calculatorTab(.carbJet),
                subtitle: S.calculatorDescription(.carbJet)
            )

            CalculatorSection(S.sectionInput) {
                DecimalField(label: "Basis-Hauptdüse", text: $baseJet, enabled: editingEnabled)
                DecimalField(label: "Referenz-Höhe", text: $refAltitude, suffix: "m", enabled: editingEnabled)
                DecimalField(label: "Referenz-Temperatur", text: $refTemp, suffix: "°C", enabled: editingEnabled)
                DecimalField(label: "Ziel-Höhe", text: $targetAltitude, suffix: "m", enabled: editingEnabled)
                DecimalField(label: "Ziel-Temperatur", text: $targetTemp, suffix: "°C", enabled: editingEnabled)
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

        let input = FlywheelGeometryInput(
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

        var lines = ["Trägheitsmoment: \(fmt(result.inertiaKgm2, decimals: 4)) kg·m²"]
        if let mass = result.massKg {
            lines.append("Masse: \(fmt(mass, decimals: 2)) kg")
        }
        if let eq = result.equivalentMassKg {
            lines.append("Äquivalente Masse: \(fmt(eq, decimals: 1)) kg")
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
                DecimalField(label: "Außendurchmesser", text: $outerDiameter, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Innendurchmesser", text: $innerDiameter, suffix: "mm", hint: "0 = Vollzylinder", enabled: editingEnabled)
                DecimalField(label: "Länge", text: $length, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Walzenradius", text: $rollerRadius, suffix: "mm", enabled: editingEnabled)
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
            powerPs: ps,
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
            let limit = result.topSpeedLimit == TopSpeedLimit.aerodynamic ? "aero" : "Drehzahl"
            lines.append("Höchstgeschwindigkeit: \(fmt(top, decimals: 1)) km/h (Gang \(result.topSpeedGear ?? 0), \(limit))")
        }
        if let gear = result.selectedGear {
            lines.append("Beschleunigung @ \(fmt(analysis, decimals: 0)) km/h: \(fmt(gear.accelerationG, decimals: 2)) g")
            lines.append("Zugkraft netto: \(fmt(gear.forces.netForceN, decimals: 0)) N")
        }
        if let sprint = result.sprintToTarget {
            lines.append("Sprint auf \(fmt(target, decimals: 0)) km/h: \(fmt(sprint.timeSeconds, decimals: 1)) s")
        }
        if let qm = result.quarterMileEstimateSeconds, let trap = result.quarterMileTrapSpeedKmh {
            lines.append("¼ Meile: \(fmt(qm, decimals: 1)) s @ \(fmt(trap, decimals: 0)) km/h")
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
                DecimalField(label: "Fahrzeugmasse", text: $mass, suffix: "kg", enabled: editingEnabled)
                DecimalField(label: "Leistung", text: $powerPs, suffix: "PS", enabled: editingEnabled)
                DecimalField(label: "Motordrehzahl", text: $engineRpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Max. Drehzahl", text: $maxRpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Radumfang", text: $wheelCircumference, suffix: "mm", enabled: editingEnabled)
                DecimalField(label: "Primär Ritzel/Rad", text: $primaryPinion, enabled: editingEnabled)
                DecimalField(label: "", text: $primaryGear, enabled: editingEnabled)
                DecimalField(label: "Sekundär Ritzel/Rad", text: $secondaryPinion, enabled: editingEnabled)
                DecimalField(label: "", text: $secondaryGear, enabled: editingEnabled)
                DecimalField(label: "Schalt-Drehzahl", text: $shiftRpm, suffix: "U/min", enabled: editingEnabled)
                DecimalField(label: "Analyse-Geschwindigkeit", text: $analysisSpeed, suffix: "km/h", enabled: editingEnabled)
                DecimalField(label: "Ziel-Geschwindigkeit", text: $targetSpeed, suffix: "km/h", enabled: editingEnabled)
            }

            ResultCard(title: S.resultTitle, lines: resultLines)
        }
    }
}
