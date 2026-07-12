import Foundation
import sharedKit

enum VehicleMapper {
    static func toItem(_ vehicle: Vehicle) -> VehicleItem {
        VehicleItem(
            id: vehicle.id,
            name: vehicle.name,
            brand: vehicle.brand,
            model: vehicle.model,
            licensePlate: vehicle.licensePlate,
            year: vehicle.year,
            odometerKm: vehicle.currentOdometerKm,
            vehicle: vehicle
        )
    }

    static func fromBasicInfo(_ info: VehicleBasicInfo) -> Vehicle {
        Vehicle(
            id: UUID().uuidString,
            name: info.name,
            brand: info.brand,
            model: info.model,
            year: info.year,
            licensePlate: info.licensePlate,
            currentOdometerKm: info.odometerKm,
            vehicleType: vehicleType(from: info.vehicleType)
        )
    }

    private static func vehicleType(from raw: String) -> VehicleType {
        switch raw.uppercased() {
        case "ROLLER": return .roller
        case "MOKICK": return .mokick
        case "CROSS": return .cross
        case "FOUR_WHEEL": return .fourWheel
        default: return .mofa
        }
    }
}
