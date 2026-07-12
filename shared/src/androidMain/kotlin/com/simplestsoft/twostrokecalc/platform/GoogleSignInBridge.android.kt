package com.simplestsoft.twostrokecalc.platform

actual suspend fun clearGoogleCredentialState() {
    // No-op on Android; Credential Manager cleared from androidApp AuthRepository.
}
