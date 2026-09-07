package com.simplestsoft.twostrokecalc.data.tools

import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Syncs tool measurement sessions to Firestore under users/{uid}/toolSessions/{sessionId}.
 */
class ToolSessionFirestoreSync(
    private val sessionRepository: ToolSessionRepository,
) {
    suspend fun pushAll() = withContext(Dispatchers.Default) {
        val uid = Firebase.auth.currentUser?.uid ?: return@withContext
        val collection = Firebase.firestore
            .collection(SharedAuthRepository.USERS_COLLECTION)
            .document(uid)
            .collection(COLLECTION)
        sessionRepository.sessions.value.forEach { session ->
            runCatching {
                collection.document(session.id).set(
                    mapOf(
                        "id" to session.id,
                        "toolId" to session.toolId.name,
                        "vehicleId" to (session.vehicleId ?: ""),
                        "createdAt" to session.createdAt,
                        "title" to session.title,
                        "notes" to session.notes,
                        "resultJson" to session.resultJson,
                        "rawDataPath" to (session.rawDataPath ?: ""),
                        "updatedAt" to currentTimeMillis(),
                    ),
                    merge = true,
                )
            }
        }
    }

    suspend fun pullAndMerge() = withContext(Dispatchers.Default) {
        val uid = Firebase.auth.currentUser?.uid ?: return@withContext
        val snapshot = runCatching {
            Firebase.firestore
                .collection(SharedAuthRepository.USERS_COLLECTION)
                .document(uid)
                .collection(COLLECTION)
                .get()
        }.getOrNull() ?: return@withContext

        val remote = snapshot.documents.mapNotNull { doc ->
            val toolName = doc.get<String>("toolId") ?: return@mapNotNull null
            val toolId = ToolId.fromName(toolName) ?: return@mapNotNull null
            ToolMeasurementSession(
                id = doc.get<String>("id") ?: doc.id,
                toolId = toolId,
                vehicleId = doc.get<String>("vehicleId")?.takeIf { it.isNotBlank() },
                createdAt = doc.get<Long>("createdAt") ?: currentTimeMillis(),
                title = doc.get<String>("title") ?: "",
                notes = doc.get<String>("notes") ?: "",
                resultJson = doc.get<String>("resultJson") ?: "",
                rawDataPath = doc.get<String>("rawDataPath")?.takeIf { it.isNotBlank() },
            )
        }
        if (remote.isEmpty()) return@withContext
        val merged = (sessionRepository.sessions.value + remote)
            .associateBy { it.id }
            .values
            .toList()
        sessionRepository.replaceAll(merged)
    }

    companion object {
        private const val COLLECTION = "toolSessions"
    }
}
