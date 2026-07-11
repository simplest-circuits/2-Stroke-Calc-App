package com.simplestsoft.twostrokecalc.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.simplestsoft.twostrokecalc.ui.auth.AuthViewModel
import com.simplestsoft.twostrokecalc.ui.screens.auth.ForgotPasswordScreen
import com.simplestsoft.twostrokecalc.ui.screens.auth.LoginScreen
import com.simplestsoft.twostrokecalc.ui.screens.auth.RegisterScreen
import com.simplestsoft.twostrokecalc.ui.util.findComponentActivity

@Composable
fun AuthNavHost(
    modifier: Modifier = Modifier,
    @StringRes subtitleRes: Int = com.simplestsoft.twostrokecalc.R.string.login_subtitle,
    showHeader: Boolean = true,
    embedded: Boolean = false,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context.findComponentActivity() ?: return
    val authViewModel: AuthViewModel = hiltViewModel(activity)
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry?.destination?.route) {
        authViewModel.clearError()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                loading = authState.loading,
                error = authState.error,
                subtitleRes = subtitleRes,
                showHeader = showHeader,
                embedded = embedded,
                onLogin = { e, p -> authViewModel.signIn(e, p) },
                onGoogle = {
                    context.findComponentActivity()?.let { authViewModel.googleSignIn(it) }
                },
                onForgot = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onRegister = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                loading = authState.loading,
                error = authState.error,
                embedded = embedded,
                onRegister = { email, password, name ->
                    authViewModel.register(email, password, name)
                },
                onLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                loading = authState.loading,
                error = authState.error,
                success = authState.forgotEmailSent,
                embedded = embedded,
                onSend = { authViewModel.forgotPassword(it) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
