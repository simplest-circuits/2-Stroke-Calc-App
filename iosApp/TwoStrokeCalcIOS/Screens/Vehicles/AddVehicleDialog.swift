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
        return SharedKitBridge.catalogYears(brand: catalogBrand, model: catalogModel)
    }

    private var variants: [VehicleCatalogEntry] {
        guard let yearInt = Int(catalogYear), !catalogBrand.isEmpty, !catalogModel.isEmpty else { return [] }
        return IosKoinInitKt.iosCatalogVariants(brand: catalogBrand, model: catalogModel, year: Int32(yearInt)) as [VehicleCatalogEntry]
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker(L.t("vehicles_add_input_mode"), selection: $inputMode) {
                    Text(L.t("vehicles_add_mode_manual")).tag(AddVehicleInputMode.manual)
                    Text(L.t("vehicles_add_mode_catalog")).tag(AddVehicleInputMode.catalog)
                }
                .pickerStyle(.segmented)

                if inputMode == .manual {
                    Section(L.t("vehicles_tab_basic")) {
                        TextField(L.t("vehicles_field_name"), text: $name)
                        TextField(L.t("vehicles_field_brand"), text: $brand)
                        TextField(L.t("vehicles_field_model"), text: $model)
                        TextField(L.t("vehicles_field_year"), text: $year)
                        TextField(L.t("vehicles_field_license_plate"), text: $licensePlate)
                        TextField(L.t("vehicles_field_odometer"), text: $odometer)
                        Picker(L.t("vehicles_field_type"), selection: $vehicleType) {
                            Text(L.t("vehicles_type_mofa")).tag("MOFA")
                            Text(L.t("vehicles_type_mokick")).tag("MOKICK")
                            Text(L.t("vehicle_dynamics_preset_roller")).tag("ROLLER")
                            Text(L.t("exhaust_preset_cross")).tag("CROSS")
                        }
                    }
                } else {
                    Section(L.tf("vehicles_catalog_section_title", String(SharedKitBridge.catalogEntryCount()))) {
                        TextField(L.t("vehicles_catalog_search_hint"), text: $catalogSearch)
                        if !catalogSearch.isEmpty {
                            ForEach(IosKoinInitKt.iosCatalogSearch(query: catalogSearch, limit: 15) as [VehicleCatalogEntry], id: \.id) { entry in
                                Button("\(entry.brand) \(entry.displayModel())") {
                                    catalogBrand = entry.brand
                                    catalogModel = entry.model
                                    catalogVariantId = entry.id
                                }
                            }
                        }
                        Picker(L.t("vehicles_field_brand"), selection: $catalogBrand) {
                            Text(L.t("gear_result_table_no_jump")).tag("")
                            ForEach(catalogBrands, id: \.self) { Text($0).tag($0) }
                        }
                        .onChange(of: catalogBrand) { _, _ in
                            catalogModel = models.first ?? ""
                            catalogYear = ""
                            catalogVariantId = ""
                        }
                        Picker(L.t("vehicles_field_model"), selection: $catalogModel) {
                            Text(L.t("gear_result_table_no_jump")).tag("")
                            ForEach(models, id: \.self) { Text($0).tag($0) }
                        }
                        .onChange(of: catalogModel) { _, _ in
                            catalogYear = years.first.map(String.init) ?? ""
                            catalogVariantId = ""
                        }
                        Picker(L.t("vehicles_field_year"), selection: $catalogYear) {
                            Text(L.t("gear_result_table_no_jump")).tag("")
                            ForEach(years, id: \.self) { Text(String($0)).tag(String($0)) }
                        }
                        if !variants.isEmpty {
                            Picker(L.t("vehicles_catalog_variant"), selection: $catalogVariantId) {
                                ForEach(variants, id: \.id) { entry in
                                    Text(entry.variant.isEmpty ? entry.model : entry.variant).tag(entry.id)
                                }
                            }
                        }
                        TextField(L.t("vehicles_field_license_plate"), text: $licensePlate)
                        TextField(L.t("vehicles_field_odometer"), text: $odometer)
                    }
                }

                if showValidationError {
                    Text(L.t("vehicles_add_validation_error"))
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
