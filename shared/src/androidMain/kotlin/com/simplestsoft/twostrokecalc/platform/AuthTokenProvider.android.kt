package com.simplestsoft.twostrokecalc.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

actual suspend fun fetchAuthIdToken(forceRefresh: Boolean): String? =
    runCatching {
        Firebase.auth.currentUser?.getIdToken(forceRefresh)
    }.getOrNull()

actual fun currentAuthUserId(): String? = Firebase.auth.currentUser?.uid
