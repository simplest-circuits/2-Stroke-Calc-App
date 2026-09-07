package com.simplestsoft.twostrokecalc.ui.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.data.community.CommunitySetupCloudStorage
import com.simplestsoft.twostrokecalc.data.community.CommunitySetupRepository
import com.simplestsoft.twostrokecalc.data.community.CommunitySetupSort
import com.simplestsoft.twostrokecalc.data.community.SharedEngineCatalogRepository
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleRepository
import com.simplestsoft.twostrokecalc.domain.community.fromVehicle
import com.simplestsoft.twostrokecalc.domain.community.suggestedCommunitySubmitStep
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetup
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupImage
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupLink
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupLinkType
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupRating
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupStatus
import com.simplestsoft.twostrokecalc.domain.model.community.EngineFamilyEntry
import com.simplestsoft.twostrokecalc.domain.model.community.EngineOrientation
import com.simplestsoft.twostrokecalc.domain.model.community.EngineTransmissionType
import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import com.simplestsoft.twostrokecalc.platform.randomUUID
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CommunityBrowseSegment {
    ALL,
    FAVORITES,
    MINE,
}

data class CommunityUiState(
    val loading: Boolean = false,
    val segment: CommunityBrowseSegment = CommunityBrowseSegment.ALL,
    val query: String = "",
    val selectedEngineFamilyId: String? = null,
    val selectedOrientation: EngineOrientation? = null,
    val sort: CommunitySetupSort = CommunitySetupSort.NEWEST,
    val setups: List<CommunitySetup> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val engineFamilies: List<EngineFamilyEntry> = emptyList(),
    val selectedSetup: CommunitySetup? = null,
    val selectedRatings: List<CommunitySetupRating> = emptyList(),
    val myRating: CommunitySetupRating? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val draft: CommunitySetup = CommunitySetup(),
    val submitStep: Int = 0,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CommunitySetupsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val setupRepository: CommunitySetupRepository,
    private val engineCatalogRepository: SharedEngineCatalogRepository,
    private val vehicleRepository: VehicleRepository,
    private val preferencesManager: PreferencesManager,
    private val cloudStorage: CommunitySetupCloudStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    val filteredSetups: StateFlow<List<CommunitySetup>> = combine(
        _state,
        setupRepository.publishedSetups,
        setupRepository.mySetups,
        setupRepository.favoriteIds,
    ) { ui, published, mine, favorites ->
        val source = when (ui.segment) {
            CommunityBrowseSegment.ALL -> published
            CommunityBrowseSegment.MINE -> mine
            CommunityBrowseSegment.FAVORITES -> published.filter { it.id in favorites } +
                mine.filter { it.id in favorites && it.status != CommunitySetupStatus.PUBLISHED }
        }.distinctBy { it.id }
        val engineFiltered = if (ui.selectedOrientation == null && ui.selectedEngineFamilyId == null) {
            source
        } else {
            source.filter { setup ->
                val family = engineCatalogRepository.findById(setup.engineFamilyId)
                (ui.selectedEngineFamilyId == null || setup.engineFamilyId == ui.selectedEngineFamilyId) &&
                    (ui.selectedOrientation == null || family?.orientation == ui.selectedOrientation)
            }
        }
        setupRepository.filterAndSort(
            setups = engineFiltered,
            query = ui.query,
            engineFamilyId = ui.selectedEngineFamilyId,
            sort = ui.sort,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            seedEngineCatalogFromAssets()
            runCatching { engineCatalogRepository.ensureSynced() }
            _state.update {
                it.copy(engineFamilies = engineCatalogRepository.allEntries())
            }
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                setupRepository.refreshPublished()
                setupRepository.refreshMySetups()
                setupRepository.refreshFavorites()
                vehicleRepository.getVehicles()
            }.onSuccess { vehicles ->
                _state.update {
                    it.copy(
                        loading = false,
                        setups = setupRepository.publishedSetups.value,
                        favoriteIds = setupRepository.favoriteIds.value,
                        vehicles = vehicles,
                        engineFamilies = engineCatalogRepository.allEntries(),
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Laden fehlgeschlagen")
                }
            }
        }
    }

    fun setSegment(segment: CommunityBrowseSegment) {
        _state.update { it.copy(segment = segment) }
        if (segment == CommunityBrowseSegment.FAVORITES) {
            viewModelScope.launch { setupRepository.refreshFavorites() }
        }
        if (segment == CommunityBrowseSegment.MINE) {
            viewModelScope.launch { setupRepository.refreshMySetups() }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setSort(sort: CommunitySetupSort) = _state.update { it.copy(sort = sort) }

    fun setEngineFamilyFilter(id: String?) = _state.update { it.copy(selectedEngineFamilyId = id) }

    fun setOrientationFilter(orientation: EngineOrientation?) =
        _state.update { it.copy(selectedOrientation = orientation) }

    fun openSetup(setupId: String) {
        // Show list cache immediately so DETAIL doesn't bounce back while the network fetch runs.
        val cached = _state.value.setups.firstOrNull { it.id == setupId }
            ?: setupRepository.publishedSetups.value.firstOrNull { it.id == setupId }
        _state.update {
            it.copy(
                loading = true,
                error = null,
                selectedSetup = cached ?: it.selectedSetup?.takeIf { setup -> setup.id == setupId },
                selectedRatings = if (cached != null || it.selectedSetup?.id == setupId) {
                    it.selectedRatings
                } else {
                    emptyList()
                },
                myRating = if (cached != null || it.selectedSetup?.id == setupId) it.myRating else null,
            )
        }
        viewModelScope.launch {
            val setup = setupRepository.getSetup(setupId)
            if (setup == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Setup nicht gefunden",
                        selectedSetup = null,
                        selectedRatings = emptyList(),
                        myRating = null,
                    )
                }
                return@launch
            }
            val ratings = setupRepository.getRatings(setupId)
            val myRating = setupRepository.getMyRating(setupId)
            _state.update {
                it.copy(
                    loading = false,
                    selectedSetup = setup,
                    selectedRatings = ratings,
                    myRating = myRating,
                    favoriteIds = setupRepository.favoriteIds.value,
                )
            }
        }
    }

    fun clearSelectedSetup() = _state.update {
        it.copy(selectedSetup = null, selectedRatings = emptyList(), myRating = null)
    }

    fun startNewDraft(fromVehicle: Vehicle? = null, advanceToStep: Int? = null) {
        viewModelScope.launch {
            val displayName = preferencesManager.preferencesFlow.first().displayName.orEmpty()
            val draft = if (fromVehicle != null) {
                CommunitySetup.fromVehicle(fromVehicle, authorDisplayName = displayName)
            } else {
                CommunitySetup(authorDisplayName = displayName, showAuthorName = false)
            }
            val resolvedStep = when {
                advanceToStep != null -> advanceToStep
                fromVehicle != null -> suggestedCommunitySubmitStep(fromVehicle)
                else -> 0
            }
            _state.update {
                it.copy(
                    draft = draft,
                    submitStep = resolvedStep.coerceIn(0, 6),
                    // Keep browse filters separate from engine-picker search.
                    query = if (resolvedStep >= 1) "" else it.query,
                    error = null,
                    message = null,
                )
            }
        }
    }

    /** Load an owned setup into the submit wizard; save will re-queue as pending review. */
    fun startEditDraft(setup: CommunitySetup) {
        _state.update {
            it.copy(
                draft = setup,
                submitStep = 1,
                query = "",
                error = null,
                message = null,
            )
        }
    }

    fun isEditingDraft(): Boolean {
        val draft = _state.value.draft
        val uid = currentUserId() ?: return false
        return draft.authorId.isNotBlank() && draft.authorId == uid
    }

    fun updateDraft(transform: (CommunitySetup) -> CommunitySetup) {
        _state.update { it.copy(draft = transform(it.draft)) }
    }

    fun setSubmitStep(step: Int) = _state.update { it.copy(submitStep = step.coerceIn(0, 6)) }

    fun submitDraft() {
        viewModelScope.launch {
            val draft = _state.value.draft
            if (draft.engineFamilyId.isBlank()) {
                _state.update { it.copy(error = "Bitte einen Motortyp wählen") }
                return@launch
            }
            if (draft.transmissionType == EngineTransmissionType.UNKNOWN) {
                _state.update { it.copy(error = "Bitte Variomatik oder Getriebe wählen") }
                return@launch
            }
            if (draft.engine.displacementCc.isBlank()) {
                _state.update { it.copy(error = "Bitte Hubraum angeben") }
                return@launch
            }
            if (draft.title.isBlank()) {
                _state.update { it.copy(error = "Bitte einen Titel eingeben") }
                return@launch
            }
            if (draft.experienceNotes.isBlank()) {
                _state.update { it.copy(error = "Bitte einen Erfahrungsbericht eingeben") }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            val editing = isEditingDraft()
            val result = if (editing) {
                setupRepository.updateMySetup(draft)
            } else {
                setupRepository.submitSetup(draft)
            }
            result
                .onSuccess {
                    _state.update {
                        it.copy(
                            loading = false,
                            message = if (editing) {
                                "Änderungen eingereicht – erneut in Prüfung."
                            } else {
                                "Setup eingereicht – es wird geprüft."
                            },
                            draft = CommunitySetup(),
                            submitStep = 0,
                            selectedSetup = null,
                            selectedRatings = emptyList(),
                            myRating = null,
                            segment = CommunityBrowseSegment.MINE,
                        )
                    }
                    setupRepository.refreshMySetups()
                    setupRepository.refreshPublished()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: if (editing) {
                                "Aktualisieren fehlgeschlagen"
                            } else {
                                "Einreichen fehlgeschlagen"
                            },
                        )
                    }
                }
        }
    }

    fun toggleFavorite(setupId: String) {
        viewModelScope.launch {
            setupRepository.toggleFavorite(setupId)
                .onSuccess { isFavorite ->
                    _state.update {
                        it.copy(
                            favoriteIds = if (isFavorite) it.favoriteIds + setupId else it.favoriteIds - setupId,
                            message = if (isFavorite) "Zu Favoriten hinzugefügt" else "Aus Favoriten entfernt",
                        )
                    }
                    setupRepository.refreshPublished()
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Favorit konnte nicht gespeichert werden") }
                }
        }
    }

    fun submitRating(value: Int, comment: String) {
        viewModelScope.launch {
            val setupId = _state.value.selectedSetup?.id ?: return@launch
            _state.update { it.copy(loading = true, error = null) }
            setupRepository.submitRating(setupId, value, comment)
                .onSuccess {
                    openSetup(setupId)
                    setupRepository.refreshPublished()
                    _state.update { it.copy(message = "Bewertung gespeichert") }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Bewertung fehlgeschlagen")
                    }
                }
        }
    }

    fun reportSelected(reason: String) {
        viewModelScope.launch {
            val setupId = _state.value.selectedSetup?.id ?: return@launch
            setupRepository.reportSetup(setupId, reason)
                .onSuccess {
                    _state.update { it.copy(message = "Meldung gesendet") }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Meldung fehlgeschlagen") }
                }
        }
    }

    fun deleteMySetup(setupId: String) {
        viewModelScope.launch {
            setupRepository.deleteSetup(setupId)
                .onSuccess {
                    clearSelectedSetup()
                    refreshAll()
                    _state.update { it.copy(message = "Setup gelöscht") }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Löschen fehlgeschlagen") }
                }
        }
    }

    fun addLinkToDraft(url: String, label: String, youtube: Boolean) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val normalized = normalizeExternalUrl(trimmed)
        val isYoutube = youtube || looksLikeYoutubeUrl(normalized)
        val link = CommunitySetupLink(
            type = if (isYoutube) CommunitySetupLinkType.YOUTUBE else CommunitySetupLinkType.OTHER,
            url = normalized,
            label = label.trim().ifBlank { if (isYoutube) "YouTube" else "Link" },
        )
        updateDraft { it.copy(externalLinks = it.externalLinks + link) }
    }

    fun removeLinkFromDraft(linkId: String) {
        updateDraft { it.copy(externalLinks = it.externalLinks.filterNot { link -> link.id == linkId }) }
    }

    fun uploadDraftImage(localFile: File, mimeType: String) {
        viewModelScope.launch {
            val setupId = _state.value.draft.id
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val fileName = "${randomUUID()}.jpg"
                val path = cloudStorage.uploadImage(setupId, localFile, fileName, mimeType)
                CommunitySetupImage(
                    storagePath = path,
                    addedAtMs = currentTimeMillis(),
                )
            }.onSuccess { image ->
                updateDraft { it.copy(images = it.images + image) }
                _state.update { it.copy(loading = false) }
            }.onFailure { e ->
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Bild-Upload fehlgeschlagen")
                }
            }
        }
    }

    fun removeDraftImage(imageId: String) {
        updateDraft { it.copy(images = it.images.filterNot { img -> img.id == imageId }) }
    }

    fun engineFamily(id: String): EngineFamilyEntry? = engineCatalogRepository.findById(id)

    fun engineTitle(setup: CommunitySetup): String =
        setup.engineDisplayLabel(engineFamily(setup.engineFamilyId)?.displayTitle())

    fun searchEngines(query: String): List<EngineFamilyEntry> =
        engineCatalogRepository.search(query)

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    fun currentUserId(): String? = setupRepository.currentUserId()

    private fun seedEngineCatalogFromAssets() {
        runCatching {
            context.assets.open(ENGINE_ASSET).bufferedReader().use { it.readText() }
        }.onSuccess { raw ->
            engineCatalogRepository.seedFromBundledJson(raw)
        }
    }

    companion object {
        private const val ENGINE_ASSET = "engine_catalog.json"

        private fun normalizeExternalUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                return trimmed
            }
            return "https://$trimmed"
        }

        private fun looksLikeYoutubeUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains("youtube.com") || lower.contains("youtu.be")
        }
    }
}
