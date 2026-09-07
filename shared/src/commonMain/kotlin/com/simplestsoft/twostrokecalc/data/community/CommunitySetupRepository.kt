package com.simplestsoft.twostrokecalc.data.community

import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetup
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupRating
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupStatus
import com.simplestsoft.twostrokecalc.domain.model.community.FavoriteSetup
import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import com.simplestsoft.twostrokecalc.platform.randomUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

enum class CommunitySetupSort {
    RATING,
    NEWEST,
    POPULAR,
}

class CommunitySetupRepository(
    @Suppress("unused") private val authRepository: SharedAuthRepository,
) {
    private val _publishedSetups = MutableStateFlow<List<CommunitySetup>>(emptyList())
    val publishedSetups: StateFlow<List<CommunitySetup>> = _publishedSetups.asStateFlow()

    private val _mySetups = MutableStateFlow<List<CommunitySetup>>(emptyList())
    val mySetups: StateFlow<List<CommunitySetup>> = _mySetups.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _pendingReview = MutableStateFlow<List<CommunitySetup>>(emptyList())
    val pendingReview: StateFlow<List<CommunitySetup>> = _pendingReview.asStateFlow()

    suspend fun refreshPublished() = withContext(Dispatchers.Default) {
        val snapshot = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .where { "status" equalTo CommunitySetupStatus.PUBLISHED.name }
                .get()
        }.getOrNull() ?: return@withContext
        _publishedSetups.value = snapshot.documents.mapNotNull { it.toCommunitySetup() }
    }

    suspend fun refreshMySetups() = withContext(Dispatchers.Default) {
        val uid = currentUserId() ?: run {
            _mySetups.value = emptyList()
            return@withContext
        }
        val snapshot = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .where { "authorId" equalTo uid }
                .get()
        }.getOrNull() ?: return@withContext
        _mySetups.value = snapshot.documents.mapNotNull { it.toCommunitySetup() }
            .sortedByDescending { it.updatedAtMs }
    }

    suspend fun refreshFavorites() = withContext(Dispatchers.Default) {
        val uid = currentUserId() ?: run {
            _favoriteIds.value = emptySet()
            return@withContext
        }
        val snapshot = runCatching {
            Firebase.firestore.collection(SharedAuthRepository.USERS_COLLECTION)
                .document(uid)
                .collection(FAVORITES_COLLECTION)
                .get()
        }.getOrNull() ?: return@withContext
        _favoriteIds.value = snapshot.documents.map { it.id }.toSet()
    }

    suspend fun refreshPendingReview() = withContext(Dispatchers.Default) {
        val snapshot = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .where { "status" equalTo CommunitySetupStatus.PENDING_REVIEW.name }
                .get()
        }.getOrNull() ?: return@withContext
        _pendingReview.value = snapshot.documents.mapNotNull { it.toCommunitySetup() }
            .sortedBy { it.createdAtMs }
    }

    suspend fun getSetup(setupId: String): CommunitySetup? = withContext(Dispatchers.Default) {
        val doc = runCatching {
            Firebase.firestore.collection(COLLECTION).document(setupId).get()
        }.getOrNull() ?: return@withContext null
        if (!doc.exists) return@withContext null
        doc.toCommunitySetup()
    }

    suspend fun submitSetup(setup: CommunitySetup): Result<CommunitySetup> = withContext(Dispatchers.Default) {
        runCatching {
            val uid = currentUserId() ?: error("Not signed in")
            val now = currentTimeMillis()
            val prepared = setup.copy(
                authorId = uid,
                status = CommunitySetupStatus.PENDING_REVIEW,
                createdAtMs = if (setup.createdAtMs > 0) setup.createdAtMs else now,
                updatedAtMs = now,
                ratingAverage = 0.0,
                ratingCount = 0,
            )
            Firebase.firestore.collection(COLLECTION)
                .document(prepared.id)
                .set(CommunitySetup.serializer(), prepared) { encodeDefaults = true }
            refreshMySetups()
            prepared
        }
    }

    suspend fun updateMySetup(setup: CommunitySetup): Result<CommunitySetup> = withContext(Dispatchers.Default) {
        runCatching {
            val uid = currentUserId() ?: error("Not signed in")
            require(setup.authorId == uid) { "Not the author" }
            val prepared = setup.copy(
                status = CommunitySetupStatus.PENDING_REVIEW,
                updatedAtMs = currentTimeMillis(),
            )
            Firebase.firestore.collection(COLLECTION)
                .document(prepared.id)
                .set(CommunitySetup.serializer(), prepared, merge = true) { encodeDefaults = true }
            refreshMySetups()
            prepared
        }
    }

    suspend fun deleteSetup(setupId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            Firebase.firestore.collection(COLLECTION).document(setupId).delete()
            refreshMySetups()
            refreshPublished()
            refreshPendingReview()
        }
    }

    suspend fun setStatus(
        setupId: String,
        status: CommunitySetupStatus,
        moderatorNote: String = "",
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            Firebase.firestore.collection(COLLECTION).document(setupId).set(
                CommunitySetupStatusPatch.serializer(),
                CommunitySetupStatusPatch(
                    status = status.name,
                    moderatorNote = moderatorNote,
                    updatedAtMs = currentTimeMillis(),
                ),
                merge = true,
            )
            refreshPendingReview()
            refreshPublished()
            refreshMySetups()
        }
    }

    suspend fun submitRating(setupId: String, value: Int, comment: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val uid = currentUserId() ?: error("Not signed in")
                val rating = CommunitySetupRating(
                    userId = uid,
                    value = value,
                    comment = comment.trim(),
                    createdAtMs = currentTimeMillis(),
                )
                require(rating.isValid()) { "Invalid rating" }
                Firebase.firestore.collection(COLLECTION)
                    .document(setupId)
                    .collection(RATINGS_COLLECTION)
                    .document(uid)
                    .set(CommunitySetupRating.serializer(), rating) { encodeDefaults = true }
            }
        }

    suspend fun getRatings(setupId: String): List<CommunitySetupRating> = withContext(Dispatchers.Default) {
        val snapshot = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .document(setupId)
                .collection(RATINGS_COLLECTION)
                .get()
        }.getOrNull() ?: return@withContext emptyList()
        snapshot.documents.mapNotNull { it.toCommunitySetupRating() }
            .sortedByDescending { it.createdAtMs }
    }

    suspend fun getMyRating(setupId: String): CommunitySetupRating? = withContext(Dispatchers.Default) {
        val uid = currentUserId() ?: return@withContext null
        val doc = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .document(setupId)
                .collection(RATINGS_COLLECTION)
                .document(uid)
                .get()
        }.getOrNull() ?: return@withContext null
        if (!doc.exists) return@withContext null
        doc.toCommunitySetupRating()
    }

    suspend fun toggleFavorite(setupId: String): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            val uid = currentUserId() ?: error("Not signed in")
            val favRef = Firebase.firestore.collection(SharedAuthRepository.USERS_COLLECTION)
                .document(uid)
                .collection(FAVORITES_COLLECTION)
                .document(setupId)
            val setupRef = Firebase.firestore.collection(COLLECTION).document(setupId)
            val currentlyFavorite = setupId in _favoriteIds.value || runCatching {
                favRef.get().exists
            }.getOrDefault(false)

            if (currentlyFavorite) {
                favRef.delete()
                val current = getSetup(setupId)
                val nextCount = ((current?.favoriteCount ?: 1) - 1).coerceAtLeast(0)
                setupRef.set(
                    CommunitySetupCountPatch.serializer(),
                    CommunitySetupCountPatch(
                        favoriteCount = nextCount,
                        updatedAtMs = currentTimeMillis(),
                    ),
                    merge = true,
                )
                _favoriteIds.value = _favoriteIds.value - setupId
                false
            } else {
                favRef.set(
                    FavoriteSetup.serializer(),
                    FavoriteSetup(
                        setupId = setupId,
                        addedAtMs = currentTimeMillis(),
                    ),
                )
                val current = getSetup(setupId)
                val nextCount = (current?.favoriteCount ?: 0) + 1
                setupRef.set(
                    CommunitySetupCountPatch.serializer(),
                    CommunitySetupCountPatch(
                        favoriteCount = nextCount,
                        updatedAtMs = currentTimeMillis(),
                    ),
                    merge = true,
                )
                _favoriteIds.value = _favoriteIds.value + setupId
                true
            }
        }
    }

    suspend fun reportSetup(setupId: String, reason: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val uid = currentUserId() ?: error("Not signed in")
            val reportId = randomUUID()
            Firebase.firestore.collection(COLLECTION)
                .document(setupId)
                .collection(REPORTS_COLLECTION)
                .document(reportId)
                .set(
                    CommunitySetupReportDoc.serializer(),
                    CommunitySetupReportDoc(
                        id = reportId,
                        reporterId = uid,
                        reason = reason.trim(),
                        createdAtMs = currentTimeMillis(),
                    ),
                )
            val current = getSetup(setupId)
            val nextCount = (current?.reportCount ?: 0) + 1
            Firebase.firestore.collection(COLLECTION).document(setupId).set(
                CommunitySetupReportCountPatch.serializer(),
                CommunitySetupReportCountPatch(
                    reportCount = nextCount,
                    updatedAtMs = currentTimeMillis(),
                ),
                merge = true,
            )
        }
    }

    suspend fun refreshReportedSetups(): List<CommunitySetup> = withContext(Dispatchers.Default) {
        val published = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .where { "status" equalTo CommunitySetupStatus.PUBLISHED.name }
                .get()
        }.getOrNull()
        val hidden = runCatching {
            Firebase.firestore.collection(COLLECTION)
                .where { "status" equalTo CommunitySetupStatus.HIDDEN.name }
                .get()
        }.getOrNull()
        val all = (published?.documents.orEmpty() + hidden?.documents.orEmpty())
            .mapNotNull { it.toCommunitySetup() }
            .filter { it.reportCount > 0 || it.status == CommunitySetupStatus.HIDDEN }
            .sortedByDescending { it.reportCount }
        all
    }

    suspend fun loadFavoriteSetups(): List<CommunitySetup> = withContext(Dispatchers.Default) {
        refreshFavorites()
        _favoriteIds.value.mapNotNull { id -> getSetup(id) }
            .filter { it.status == CommunitySetupStatus.PUBLISHED || it.authorId == currentUserId() }
    }

    fun filterAndSort(
        setups: List<CommunitySetup>,
        query: String = "",
        engineFamilyId: String? = null,
        manufacturerHint: String? = null,
        sort: CommunitySetupSort = CommunitySetupSort.NEWEST,
    ): List<CommunitySetup> {
        val q = query.trim().lowercase()
        val filtered = setups.filter { setup ->
            (engineFamilyId.isNullOrBlank() || setup.engineFamilyId == engineFamilyId) &&
                (manufacturerHint.isNullOrBlank() ||
                    setup.vehicleBrand.contains(manufacturerHint, ignoreCase = true) ||
                    setup.title.contains(manufacturerHint, ignoreCase = true)) &&
                (q.isEmpty() ||
                    setup.title.lowercase().contains(q) ||
                    setup.experienceNotes.lowercase().contains(q) ||
                    setup.tags.any { it.lowercase().contains(q) } ||
                    setup.vehicleContextLabel().lowercase().contains(q) ||
                    setup.resultSummary.lowercase().contains(q))
        }
        return when (sort) {
            CommunitySetupSort.RATING -> filtered.sortedWith(
                compareByDescending<CommunitySetup> { it.ratingAverage }
                    .thenByDescending { it.ratingCount },
            )
            CommunitySetupSort.POPULAR -> filtered.sortedWith(
                compareByDescending<CommunitySetup> { it.favoriteCount }
                    .thenByDescending { it.ratingCount },
            )
            CommunitySetupSort.NEWEST -> filtered.sortedByDescending { it.createdAtMs }
        }
    }

    fun currentUserId(): String? = Firebase.auth.currentUser?.uid

    companion object {
        const val COLLECTION = "communitySetups"
        const val FAVORITES_COLLECTION = "favoriteSetups"
        const val RATINGS_COLLECTION = "ratings"
        const val REPORTS_COLLECTION = "reports"
    }
}

// Prefer typed serializers: doc.data() inferred as Map<String, Any?> throws (no serializer for Any).
private fun DocumentSnapshot.toCommunitySetup(): CommunitySetup? =
    runCatching {
        data(CommunitySetup.serializer()).let { parsed ->
            if (parsed.id.isBlank()) parsed.copy(id = id) else parsed
        }
    }.getOrNull()

private fun DocumentSnapshot.toCommunitySetupRating(): CommunitySetupRating? =
    runCatching {
        data(CommunitySetupRating.serializer()).let { parsed ->
            if (parsed.userId.isBlank()) parsed.copy(userId = id) else parsed
        }
    }.getOrNull()

@Serializable
private data class CommunitySetupStatusPatch(
    val status: String,
    val moderatorNote: String,
    val updatedAtMs: Long,
)

@Serializable
private data class CommunitySetupCountPatch(
    val favoriteCount: Int,
    val updatedAtMs: Long,
)

@Serializable
private data class CommunitySetupReportCountPatch(
    val reportCount: Int,
    val updatedAtMs: Long,
)

@Serializable
private data class CommunitySetupReportDoc(
    val id: String,
    val reporterId: String,
    val reason: String,
    val createdAtMs: Long,
)
