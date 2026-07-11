package com.simplestsoft.twostrokecalc.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.simplestsoft.twostrokecalc.MainViewModel
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.ui.navigation.MainAppNavigation
import com.simplestsoft.twostrokecalc.ui.screens.FirstInstallPermissionsScreen
import com.simplestsoft.twostrokecalc.ui.theme.AppTheme

@Composable
fun AppRoot(
    mainViewModel: MainViewModel,
    firebaseAuth: FirebaseAuth,
    authRepository: AuthRepository,
    pendingVehicleId: String? = null,
    onPendingVehicleConsumed: () -> Unit = {},
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences

    LaunchedEffect(prefs.themeMode) {
        AppCompatDelegate.setDefaultNightMode(prefs.themeMode.toNightMode())
    }
    AppTheme(themeMode = prefs.themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (uiState.showFirstInstallPermissionsScreen) {
                FirstInstallPermissionsScreen(
                    onComplete = mainViewModel::completeFirstInstallPermissions,
                )
            } else {
                MainAppNavigation(
                    preferences = prefs,
                    mainViewModel = mainViewModel,
                    firebaseAuth = firebaseAuth,
                    authRepository = authRepository,
                    isAuthenticated = uiState.isAuthenticated,
                    showWalkthrough = uiState.showWelcome,
                    onWalkthroughComplete = mainViewModel::completeWelcome,
                    pendingVehicleId = pendingVehicleId,
                    onPendingVehicleConsumed = onPendingVehicleConsumed,
                )
            }
        }
    }
}

private fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
}
