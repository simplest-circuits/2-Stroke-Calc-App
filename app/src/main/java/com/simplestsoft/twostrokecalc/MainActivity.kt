package com.simplestsoft.twostrokecalc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.data.localization.LanguageLocaleApplier
import com.simplestsoft.twostrokecalc.domain.localization.AppLanguageProfile
import com.simplestsoft.twostrokecalc.ui.AppRoot
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VEHICLE_ID = "vehicle_id"
    }

    private val mainViewModel: MainViewModel by viewModels()
    private val pendingVehicleIdState = mutableStateOf<String?>(null)

    @Inject
    lateinit var languageLocaleApplier: LanguageLocaleApplier

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_App)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        pendingVehicleIdState.value = intent?.getStringExtra(EXTRA_VEHICLE_ID)

        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val pendingVehicleId by pendingVehicleIdState

            LaunchedEffect(uiState.preferences.language) {
                languageLocaleApplier.apply(
                    AppLanguageProfile.from(uiState.preferences.language),
                )
            }

            AppRoot(
                mainViewModel = mainViewModel,
                firebaseAuth = firebaseAuth,
                authRepository = authRepository,
                pendingVehicleId = pendingVehicleId,
                onPendingVehicleConsumed = { pendingVehicleIdState.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingVehicleIdState.value = intent.getStringExtra(EXTRA_VEHICLE_ID)
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshSystemLanguage()
    }
}
