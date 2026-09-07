package com.simplestsoft.twostrokecalc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.simplestsoft.twostrokecalc.ui.AppRoot
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VEHICLE_ID = "vehicle_id"
        const val EXTRA_COMMUNITY_SETUP_ID = "community_setup_id"
    }

    private val mainViewModel: MainViewModel by viewModels()
    private val pendingVehicleIdState = mutableStateOf<String?>(null)
    private val pendingCommunitySetupIdState = mutableStateOf<String?>(null)

    @Inject
    lateinit var languageLocaleApplier: LanguageLocaleApplier

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_App)
        applyComposeInsetsWorkaround()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "Hello from shared module: ${Greeting().greet()}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        applyIntentExtras(intent)

        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val pendingVehicleId by pendingVehicleIdState
            val pendingCommunitySetupId by pendingCommunitySetupIdState

            LaunchedEffect(uiState.preferences.languageMode, uiState.preferences.language) {
                languageLocaleApplier.applyForMode(uiState.preferences.languageMode)
            }

            AppRoot(
                mainViewModel = mainViewModel,
                firebaseAuth = firebaseAuth,
                authRepository = authRepository,
                pendingVehicleId = pendingVehicleId,
                onPendingVehicleConsumed = { pendingVehicleIdState.value = null },
                pendingCommunitySetupId = pendingCommunitySetupId,
                onPendingCommunitySetupConsumed = { pendingCommunitySetupIdState.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntentExtras(intent)
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshSystemLanguage()
    }

    private fun applyIntentExtras(intent: Intent?) {
        pendingVehicleIdState.value = intent?.getStringExtra(EXTRA_VEHICLE_ID)
        val fromExtra = intent?.getStringExtra(EXTRA_COMMUNITY_SETUP_ID)
        val fromUri = intent?.data?.let { uri ->
            if (uri.scheme == "twostrokecalc" &&
                uri.host == "community" &&
                uri.pathSegments.firstOrNull() == "setup"
            ) {
                uri.pathSegments.getOrNull(1)
            } else {
                null
            }
        }
        pendingCommunitySetupIdState.value = fromExtra ?: fromUri
    }

    /**
     * Keep Compose on the newer WindowInsets pipeline.
     *
     * Older ModifierLocal-based insets handling can trigger heavy snapshot churn on the main
     * thread in some edge cases, so we explicitly keep the optimized path enabled when flags are
     * available in the current Compose version.
     */
    private fun applyComposeInsetsWorkaround() {
        runCatching {
            val flagsClass = Class.forName("androidx.compose.foundation.layout.ComposeFoundationLayoutFlags")
            val fieldNames = listOf(
                "isWindowInsetsOptimizationEnabled",
                "isWindowInsetsModifierLocalNodeImplementationEnabled",
            )
            fieldNames.forEach { fieldName ->
                runCatching {
                    val field = flagsClass.getField(fieldName)
                    field.setBoolean(null, true)
                }
            }
        }.onFailure { throwable ->
            Log.w("MainActivity", "Compose WindowInsets workaround unavailable", throwable)
        }
    }
}
