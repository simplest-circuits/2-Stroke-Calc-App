package com.simplestsoft.twostrokecalc.ui.vehicles

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.data.community.SharedEngineCatalogRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleCatalogRepository
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleRepository
import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachment
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleEngineSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleType
import com.simplestsoft.twostrokecalc.domain.model.community.EngineFamilyEntry
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleCatalogMapper
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleOdometerSync
import com.simplestsoft.twostrokecalc.domain.vehicles.defaultEngineCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: VehicleRepository,
    private val catalogRepository: VehicleCatalogRepository,
    private val engineCatalogRepository: SharedEngineCatalogRepository,
    proAccessRepository: ProAccessRepository,
) : ViewModel() {

    fun catalogBrands(): List<String> = catalogRepository.brands()

    fun catalogModels(brand: String): List<String> = catalogRepository.modelsForBrand(brand)

    fun catalogYears(brand: String, model: String): List<Int> =
        catalogRepository.yearsForBrandModel(brand, model)

    fun catalogVariants(brand: String, model: String, year: Int): List<VehicleCatalogEntry> =
        catalogRepository.variantsFor(brand, model, year)

    fun catalogEntryCount(): Int = catalogRepository.allEntries().size

    fun searchEngineFamilies(query: String, limit: Int = 40): List<EngineFamilyEntry> =
        engineCatalogRepository.search(query, limit)

    fun allEngineFamilies(): List<EngineFamilyEntry> = engineCatalogRepository.allEntries()

    fun engineFamily(id: String): EngineFamilyEntry? = engineCatalogRepository.findById(id)

    fun engineFamilyLabel(vehicle: Vehicle): String =
        vehicle.engineFamilyDisplayLabel(engineFamily(vehicle.engineFamilyId)?.displayTitle())

    val canEditVehicles = proAccessRepository.state
        .map { it.canEditVehicles() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val vehicles = repository.vehiclesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _uiState = MutableStateFlow(VehiclesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            seedEngineCatalogFromAssets()
            runCatching { engineCatalogRepository.ensureSynced() }
            repository.vehiclesFlow.collect { list ->
                _uiState.update { it.copy(vehicles = list, loading = false) }
            }
        }
    }

    fun createVehicle(
        basicInfo: VehicleBasicInfo,
        catalogEntry: VehicleCatalogEntry? = null,
    ): Vehicle {
        val vehicle = if (catalogEntry != null) {
            VehicleCatalogMapper.toVehicle(
                entry = catalogEntry,
                year = basicInfo.year,
                name = basicInfo.name,
                licensePlate = basicInfo.licensePlate,
                currentOdometerKm = basicInfo.currentOdometerKm,
                currentOperatingHours = basicInfo.currentOperatingHours,
            )
        } else {
            Vehicle(
                name = basicInfo.name,
                brand = basicInfo.brand,
                model = basicInfo.model,
                year = basicInfo.year,
                vehicleType = basicInfo.vehicleType,
                licensePlate = basicInfo.licensePlate,
                currentOdometerKm = basicInfo.currentOdometerKm,
                currentOperatingHours = basicInfo.currentOperatingHours,
                engine = VehicleEngineSpecs(cycleType = basicInfo.vehicleType.defaultEngineCycle()),
            )
        }
        viewModelScope.launch { repository.saveVehicle(vehicle) }
        return vehicle
    }

    fun saveVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.saveVehicle(vehicle) }
    }

    fun deleteVehicle(id: String) {
        viewModelScope.launch { repository.deleteVehicle(id) }
    }

    fun addMaintenanceEntry(vehicleId: String, entry: MaintenanceEntry) {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            repository.saveVehicle(
                vehicle.copy(maintenanceLog = vehicle.maintenanceLog + entry),
            )
        }
    }

    fun updateMaintenanceEntry(vehicleId: String, entry: MaintenanceEntry) {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            repository.saveVehicle(
                vehicle.copy(
                    maintenanceLog = vehicle.maintenanceLog.map {
                        if (it.id == entry.id) entry else it
                    },
                ),
            )
        }
    }

    fun deleteMaintenanceEntry(vehicleId: String, entryId: String) {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            repository.saveVehicle(
                vehicle.copy(
                    maintenanceLog = vehicle.maintenanceLog.filterNot { it.id == entryId },
                ),
            )
        }
    }

    fun addFuelLogEntry(vehicleId: String, entry: FuelLogEntry) {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            repository.saveVehicle(vehicle.withSyncedFuelLog(vehicle.fuelLog + entry))
        }
    }

    fun updateFuelLogEntry(vehicleId: String, entry: FuelLogEntry) {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            val fuelLog = vehicle.fuelLog.map { if (it.id == entry.id) entry else it }
            repository.saveVehicle(vehicle.withSyncedFuelLog(fuelLog))
        }
    }

    fun deleteFuelLogEntry(vehicleId: String, entryId: String) {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            repository.saveVehicle(
                vehicle.withSyncedFuelLog(vehicle.fuelLog.filterNot { it.id == entryId }),
            )
        }
    }

    private fun Vehicle.withSyncedFuelLog(fuelLog: List<FuelLogEntry>): Vehicle {
        val synced = VehicleOdometerSync.syncFromFuelLog(
            currentOdometerKm = currentOdometerKm,
            currentOperatingHours = currentOperatingHours,
            fuelLog = fuelLog,
        )
        return copy(
            fuelLog = fuelLog,
            currentOdometerKm = synced.currentOdometerKm,
            currentOperatingHours = synced.currentOperatingHours,
        )
    }

    fun addAttachment(vehicleId: String, sourceUri: Uri, onResult: (VehicleAttachment?) -> Unit = {}) {
        viewModelScope.launch {
            val attachment = repository.addAttachment(vehicleId, sourceUri)
            onResult(attachment)
        }
    }

    fun removeAttachment(vehicleId: String, attachmentId: String) {
        viewModelScope.launch {
            repository.removeAttachment(vehicleId, attachmentId)
        }
    }

    fun updateAttachment(vehicleId: String, attachment: VehicleAttachment) {
        viewModelScope.launch {
            repository.updateAttachment(vehicleId, attachment)
        }
    }

    fun openAttachment(vehicleId: String, attachment: VehicleAttachment): Boolean {
        val file = repository.getAttachmentFile(vehicleId, attachment.fileName)
        if (!file.exists()) return false
        val uri = repository.getAttachmentShareUri(vehicleId, attachment.fileName)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            appContext.startActivity(Intent.createChooser(intent, attachment.displayName))
        }.isSuccess
    }

    fun getProfileImageFile(vehicle: Vehicle) =
        vehicle.profileImageFileName?.let { fileName ->
            repository.getProfileImageFile(vehicle.id, fileName)?.takeIf { it.exists() }
        }

    fun setProfileImage(vehicleId: String, sourceUri: Uri, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val fileName = repository.setProfileImage(vehicleId, sourceUri)
            onResult(fileName)
        }
    }

    fun clearProfileImage(vehicleId: String, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearProfileImage(vehicleId)
            onResult()
        }
    }

    private fun seedEngineCatalogFromAssets() {
        runCatching {
            appContext.assets.open(ENGINE_ASSET).bufferedReader().use { it.readText() }
        }.onSuccess { raw ->
            engineCatalogRepository.seedFromBundledJson(raw)
        }
    }

    companion object {
        private const val ENGINE_ASSET = "engine_catalog.json"
    }
}
