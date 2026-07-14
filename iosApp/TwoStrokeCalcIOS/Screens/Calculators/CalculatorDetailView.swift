import SwiftUI
import sharedKit

struct CalculatorDetailView: View {
    let calculatorId: CalculatorId

    var body: some View {
        Group {
            switch calculatorId {
            case .timing:
                PortTimingCalculatorView()
            case .portArea:
                PortAreaCalculatorView()
            case .ignition:
                IgnitionTimingCalculatorView()
            case .dcCable:
                DcCableCalculatorView()
            case .fluid:
                FluidCalculatorView()
            case .compression:
                CompressionCalculatorView()
            case .squishBand:
                SquishBandCalculatorView()
            case .meanPressure:
                MeanPressureCalculatorView()
            case .gear:
                GearCalculatorView()
            case .exhaust:
                ExhaustCalculatorView()
            case .counterweight:
                CounterweightCalculatorView()
            case .variatorWeight:
                VariatorWeightCalculatorView()
            case .fuelMix:
                FuelMixCalculatorView()
            case .carbJet:
                CarbJetCalculatorView()
            case .dynoInertia:
                FlywheelInertiaCalculatorView()
            case .vehicleDynamics:
                VehicleDynamicsCalculatorView()
            default:
                CalculatorScaffold {
                    CalculatorHeader(title: S.calculatorTab(calculatorId), subtitle: nil)
                    CalculatorResultCard(S.resultTitle) {
                        CalculatorEmptyResultText(text: L.t("calculator_not_available"))
                    }
                }
            }
        }
    }
}
