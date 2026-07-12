import SwiftUI
import sharedKit

struct AddVehicleDialog: View {
    @Environment(\.themeColors) private var colors
    @Environment(\.dismiss) private var dismiss

    @State private var inputMode: AddVehicleInputMode = .manual
    @State private var name = ""
    @State private var brand = ""
    @State private var model = ""
    @State private var year = ""
    @State private var licensePlate = ""
    @State private var odometer = ""
    @State private var vehicleType = "MOFA"
    @State private var catalogBrand = ""
    @State private var catalogModel = ""
    @State private var catalogYear = ""
    @State private var catalogVariantId = ""
    @State private var catalogSearch = ""
    @State private var showValidationError = false

    let catalogBrands: [String]
    let onConfirm: (VehicleBasicInfo) -> Void

    private var models: [String] {
        catalogBrand.isEmpty ? [] : SharedKitBridge.catalogModels(brand: catalogBrand)
    }

    private var years: [Int] {
        guard !catalogBrand.isEmpty, !catalogModel.isEmpty else { return [] }
        return SharedKitBridge.catalogYears(brand: catalogBrand, model: catalogModel).map(Int.init)
    }

    private var variants: [VehicleCatalogEntry] {
        guard let yearInt = Int(catalogYear), !catalogBrand.isEmpty, !catalogModel.isEmpty else { return [] }
        return IosKoinInitKt.iosCatalogVariants(brand: catalogBrand, model: catalogModel, year: Int32(yearInt)) as [VehicleCatalogEntry]
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Eingabe", selection: $inputMode) {
                    Text("Manuell").tag(AddVehicleInputMode.manual)
                    Text("Katalog").tag(AddVehicleInputMode.catalog)
                }
                .pickerStyle(.segmented)

                if inputMode == .manual {
                    Section("Stammdaten") {
                        TextField("Name", text: $name)
                        TextField("Marke", text: $brand)
                        TextField("Modell", text: $model)
                        TextField("Baujahr", text: $year)
                        TextField("Kennzeichen", text: $licensePlate)
                        TextField("KM-Stand", text: $odometer)
                        Picker("Typ", selection: $vehicleType) {
                            Text("Mofa").tag("MOFA")
                            Text("Mokick").tag("MOKICK")
                            Text("Roller").tag("ROLLER")
                            Text("Cross").tag("CROSS")
                        }
                    }
                } else {
                    Section("Katalog (\(SharedKitBridge.catalogEntryCount()) Modelle)") {
                        TextField("Suche Marke/Modell…", text: $catalogSearch)
                        if !catalogSearch.isEmpty {
                            ForEach(IosKoinInitKt.iosCatalogSearch(query: catalogSearch, limit: 15) as [VehicleCatalogEntry], id: \.id) { entry in
                                Button("\(entry.brand) \(entry.displayModel())") {
                                    catalogBrand = entry.brand
                                    catalogModel = entry.model
                                    catalogVariantId = entry.id
                                }
                            }
                        }
                        Picker("Marke", selection: $catalogBrand) {
                            Text("—").tag("")
                            ForEach(catalogBrands, id: \.self) { Text($0).tag($0) }
                        }
                        .onChange(of: catalogBrand) { _, _ in
                            catalogModel = models.first ?? ""
                            catalogYear = ""
                            catalogVariantId = ""
                        }
                        Picker("Modell", selection: $catalogModel) {
                            Text("—").tag("")
                            ForEach(models, id: \.self) { Text($0).tag($0) }
                        }
                        .onChange(of: catalogModel) { _, _ in
                            catalogYear = years.first.map(String.init) ?? ""
                            catalogVariantId = ""
                        }
                        Picker("Baujahr", selection: $catalogYear) {
                            Text("—").tag("")
                            ForEach(years, id: \.self) { Text(String($0)).tag(String($0)) }
                        }
                        if !variants.isEmpty {
                            Picker("Variante", selection: $catalogVariantId) {
                                ForEach(variants, id: \.id) { entry in
                                    Text(entry.variant.isEmpty ? entry.model : entry.variant).tag(entry.id)
                                }
                            }
                        }
                        TextField("Kennzeichen", text: $licensePlate)
                        TextField("KM-Stand", text: $odometer)
                    }
                }

                if showValidationError {
                    Text("Bitte Name oder Marke+Modell angeben.")
                        .foregroundStyle(colors.error)
                }
            }
            .navigationTitle(S.vehiclesAdd)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(S.save) {
                        let info = buildInfo()
                        guard info.isValid else { showValidationError = true; return }
                        onConfirm(info)
                        dismiss()
                    }
                }
            }
            .onAppear {
                if catalogBrand.isEmpty { catalogBrand = catalogBrands.first ?? "" }
            }
        }
    }

    private func buildInfo() -> VehicleBasicInfo {
        if inputMode == .catalog, let entry = variants.first(where: { $0.id == catalogVariantId }) ?? variants.first {
            return VehicleBasicInfo(
                name: name,
                brand: entry.brand,
                model: entry.model,
                year: catalogYear,
                licensePlate: licensePlate,
                odometerKm: odometer,
                vehicleType: entry.vehicleType,
                catalogEntry: entry
            )
        }
        if inputMode == .catalog {
            return VehicleBasicInfo(
                name: name,
                brand: catalogBrand,
                model: catalogModel,
                year: catalogYear,
                licensePlate: licensePlate,
                odometerKm: odometer,
                vehicleType: vehicleType
            )
        }
        return VehicleBasicInfo(
            name: name, brand: brand, model: model, year: year,
            licensePlate: licensePlate, odometerKm: odometer, vehicleType: vehicleType
        )
    }
}

enum AddVehicleInputMode: String {
    case manual, catalog
}

struct VehicleBasicInfo {
    var name: String
    var brand: String
    var model: String
    var year: String
    var licensePlate: String
    var odometerKm: String
    var vehicleType: String
    var catalogEntry: VehicleCatalogEntry?

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty ||
        (!brand.isEmpty && !model.isEmpty)
    }
}
