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
        IosKoinInitKt.iosNewVehicle(
            id: UUID().uuidString,
            name: info.name,
            brand: info.brand,
            model: info.model,
            year: info.year,
            licensePlate: info.licensePlate,
            currentOdometerKm: info.odometerKm,
            vehicleTypeName: info.vehicleType
        )
    }
}
