package com.simplestsoft.twostrokecalc.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.simplestsoft.twostrokecalc.MainViewModel
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.UserPreferences
import com.simplestsoft.twostrokecalc.ui.calculator.calculatorDisplayOrder
import com.simplestsoft.twostrokecalc.ui.auth.AuthGateDialog
import com.simplestsoft.twostrokecalc.ui.billing.BillingViewModel
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.ProUpsellDialog
import com.simplestsoft.twostrokecalc.ui.screens.AccountScreen
import com.simplestsoft.twostrokecalc.ui.screens.AdminPanelScreen
import com.simplestsoft.twostrokecalc.ui.screens.CalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.SettingsScreen
import com.simplestsoft.twostrokecalc.ui.SplashScreen
import com.simplestsoft.twostrokecalc.ui.screens.VehicleDetailScreen
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleCostOverviewScreen
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleFuelLogScreen
import com.simplestsoft.twostrokecalc.ui.screens.VehiclesScreen
import com.simplestsoft.twostrokecalc.ui.vehicles.VehiclesViewModel
import com.simplestsoft.twostrokecalc.ui.util.LocalScreenInsets
import com.simplestsoft.twostrokecalc.ui.util.ScreenInsetsConfig
import com.simplestsoft.twostrokecalc.ui.util.findComponentActivity
import com.simplestsoft.twostrokecalc.ui.walkthrough.WalkthroughCoachMarksOverlay
import com.simplestsoft.twostrokecalc.ui.walkthrough.WalkthroughTargets
import com.simplestsoft.twostrokecalc.ui.walkthrough.buildWalkthroughSteps
import com.simplestsoft.twostrokecalc.ui.walkthrough.exampleFreeCalculator
import com.simplestsoft.twostrokecalc.ui.walkthrough.exampleLockedCalculator
import com.simplestsoft.twostrokecalc.ui.walkthrough.hasProWalkthrough
import kotlinx.coroutines.launch

private enum class PendingAuthAction {
    None,
    OpenAccount,
    OpenAdmin,
    LaunchProPurchase,
}

private data class MainDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(
    preferences: UserPreferences,
    mainViewModel: MainViewModel,
    firebaseAuth: FirebaseAuth,
    authRepository: AuthRepository,
    isAuthenticated: Boolean,
    showWalkthrough: Boolean = false,
    onWalkthroughComplete: () -> Unit = {},
    pendingVehicleId: String? = null,
    onPendingVehicleConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val billingViewModel: BillingViewModel = hiltViewModel()
    val proAccess by mainViewModel.proAccessState.collectAsStateWithLifecycle()
    var proUpsellModuleName by remember { mutableStateOf<String?>(null) }
    val proUpsellModule = proUpsellModuleName?.let { ProModuleId.fromName(it) }

    fun openVehiclesProUpsell() {
        proUpsellModuleName = ProModuleId.VEHICLES.name
    }

    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    LaunchedEffect(pendingVehicleId, currentRoute) {
        if (currentRoute == Routes.SPLASH) return@LaunchedEffect
        val vehicleId = pendingVehicleId ?: return@LaunchedEffect
        navController.navigate(Routes.VEHICLES) {
            launchSingleTop = true
            restoreState = true
        }
        navController.navigate(Routes.vehicleDetailRoute(vehicleId))
        onPendingVehicleConsumed()
    }

    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val scope = rememberCoroutineScope()
    var authGateVisible by remember { mutableStateOf(false) }
    var pendingAuthAction by remember { mutableStateOf(PendingAuthAction.None) }
    val walkthroughTargets = remember { mutableStateMapOf<String, Rect>() }
    var walkthroughStepIndex by remember(showWalkthrough, preferences.navStyle, proAccess.hasProWalkthrough()) {
        mutableIntStateOf(0)
    }
    var walkthroughDismissed by remember { mutableStateOf(false) }
    val walkthroughActive = showWalkthrough && !walkthroughDismissed
    val hasProWalkthrough = proAccess.hasProWalkthrough()

    LaunchedEffect(showWalkthrough) {
        if (showWalkthrough) {
            walkthroughDismissed = false
            walkthroughStepIndex = 0
        }
    }

    val walkthroughSteps = remember(proAccess) {
        buildWalkthroughSteps(proAccess)
    }

    val walkthroughFreeCalculator = remember(proAccess) {
        proAccess.exampleFreeCalculator()
    }
    val walkthroughLockedCalculator = remember(proAccess) {
        proAccess.exampleLockedCalculator()
    }
    val walkthroughProListCalculator = remember(proAccess) {
        calculatorDisplayOrder.firstOrNull { proAccess.hasCalculatorAccess(it) }
    }

    val calculatorWalkthroughHighlightModifiers = remember(
        walkthroughFreeCalculator,
        walkthroughLockedCalculator,
        walkthroughProListCalculator,
    ) {
        buildMap {
            walkthroughFreeCalculator?.let { id ->
                put(
                    id,
                    Modifier.onGloballyPositioned { coordinates ->
                        walkthroughTargets[WalkthroughTargets.CALCULATOR_FREE] = coordinates.boundsInRoot()
                    },
                )
            }
            walkthroughLockedCalculator?.let { id ->
                put(
                    id,
                    Modifier.onGloballyPositioned { coordinates ->
                        walkthroughTargets[WalkthroughTargets.CALCULATOR_LOCKED] = coordinates.boundsInRoot()
                    },
                )
            }
            walkthroughProListCalculator?.let { id ->
                put(
                    id,
                    Modifier.onGloballyPositioned { coordinates ->
                        walkthroughTargets[WalkthroughTargets.CALCULATOR_LIST] = coordinates.boundsInRoot()
                    },
                )
            }
        }
    }

    fun isOnMainTabRoute(): Boolean {
        val route = currentDestination?.route
        return route == Routes.CALCULATOR ||
            route == Routes.VEHICLES ||
            route == Routes.VEHICLES_LIST ||
            route == Routes.SETTINGS
    }

    LaunchedEffect(walkthroughActive, currentRoute) {
        if (!walkthroughActive) return@LaunchedEffect
        when (currentRoute) {
            Routes.SPLASH -> {
                navController.navigate(Routes.CALCULATOR) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            null -> Unit
            else -> if (!isOnMainTabRoute()) {
                navController.navigate(Routes.CALCULATOR) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    fun openAuthGate(action: PendingAuthAction) {
        pendingAuthAction = action
        authGateVisible = true
    }

    fun dismissAuthGate() {
        authGateVisible = false
        pendingAuthAction = PendingAuthAction.None
    }

    fun runPendingAuthAction() {
        when (pendingAuthAction) {
            PendingAuthAction.OpenAccount -> navController.navigate(Routes.ACCOUNT)
            PendingAuthAction.OpenAdmin -> navController.navigate(Routes.ADMIN)
            PendingAuthAction.LaunchProPurchase -> {
                activity?.let { billingViewModel.launchProPurchase(it) }
            }
            PendingAuthAction.None -> Unit
        }
        pendingAuthAction = PendingAuthAction.None
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && authGateVisible) {
            authGateVisible = false
            runPendingAuthAction()
        }
    }

    val tabDestinations = buildList {
        add(MainDestination(Routes.CALCULATOR, R.string.nav_calculator, Icons.Default.Speed))
        add(MainDestination(Routes.VEHICLES, R.string.nav_vehicles, Icons.Default.TwoWheeler))
        add(MainDestination(Routes.SETTINGS, R.string.settings_title, Icons.Default.Settings))
        if (preferences.isAdmin && isAuthenticated) {
            add(MainDestination(Routes.ADMIN, R.string.admin_panel_title, Icons.Default.ManageAccounts))
        }
    }

    val isBottomBarVisible =
        currentRoute != null &&
            currentRoute != Routes.SPLASH &&
            !isKeyboardVisible &&
            preferences.navStyle == NavStyle.BOTTOM_BAR
    val isTopBarVisible =
        currentRoute != null &&
            currentRoute != Routes.SPLASH &&
            preferences.navStyle == NavStyle.DRAWER
    val bottomBarHandlesNavInsets = isBottomBarVisible
    val topBarHandlesStatusInsets = isTopBarVisible

    fun navigateToTab(route: String) {
        if (route == Routes.ADMIN && (!isAuthenticated || firebaseAuth.currentUser == null)) {
            openAuthGate(PendingAuthAction.OpenAdmin)
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(walkthroughActive, walkthroughStepIndex, currentDestination?.route, hasProWalkthrough) {
        if (!walkthroughActive) return@LaunchedEffect
        val targetKey = walkthroughSteps.getOrNull(walkthroughStepIndex)?.targetKey
        when (targetKey) {
            WalkthroughTargets.VEHICLES_ADD -> {
                if (hasProWalkthrough &&
                    currentDestination?.route != Routes.VEHICLES &&
                    currentDestination?.route != Routes.VEHICLES_LIST
                ) {
                    navigateToTab(Routes.VEHICLES)
                }
            }
            WalkthroughTargets.SETTINGS_APPEARANCE,
            WalkthroughTargets.SETTINGS_PRO,
            WalkthroughTargets.SETTINGS_NAV,
            -> {
                if (currentDestination?.route != Routes.SETTINGS) {
                    navigateToTab(Routes.SETTINGS)
                }
            }
            WalkthroughTargets.CALCULATOR_LIST,
            WalkthroughTargets.CALCULATOR_FREE,
            WalkthroughTargets.CALCULATOR_LOCKED,
            WalkthroughTargets.CALCULATOR_NAV,
            null,
            -> {
                if (currentDestination?.route != Routes.CALCULATOR) {
                    navigateToTab(Routes.CALCULATOR)
                }
            }
            WalkthroughTargets.VEHICLES_NAV -> {
                if (currentDestination?.route != Routes.VEHICLES &&
                    currentDestination?.route != Routes.VEHICLES_LIST
                ) {
                    navigateToTab(Routes.VEHICLES)
                }
            }
        }
    }

    fun walkthroughNavTargetForRoute(route: String): String? = when (route) {
        Routes.CALCULATOR -> WalkthroughTargets.CALCULATOR_NAV
        Routes.VEHICLES -> WalkthroughTargets.VEHICLES_NAV
        Routes.SETTINGS -> WalkthroughTargets.SETTINGS_NAV
        else -> null
    }

    fun isTabSelected(dest: MainDestination): Boolean =
        currentDestination?.hierarchy?.any { destination ->
            when (dest.route) {
                Routes.VEHICLES -> destination.route == Routes.VEHICLES ||
                    destination.route == Routes.VEHICLES_LIST ||
                    destination.route?.startsWith("vehicles/detail") == true
                else -> destination.route == dest.route
            }
        } == true

    val bottomBar: @Composable () -> Unit = {
        if (isBottomBarVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface())
                    .onGloballyPositioned { coordinates ->
                        walkthroughTargets[WalkthroughTargets.NAV_SURFACE] = coordinates.boundsInRoot()
                    },
            ) {
                HorizontalDivider(color = AppColors.borderSubtle())
                NavigationBar(
                    modifier = Modifier.height(64.dp),
                    containerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    tabDestinations.forEach { dest ->
                        val selected = isTabSelected(dest)
                        val navTargetKey = walkthroughNavTargetForRoute(dest.route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(dest.route) },
                            alwaysShowLabel = true,
                            modifier = if (navTargetKey != null) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    walkthroughTargets[navTargetKey] = coordinates.boundsInRoot()
                                }
                            } else {
                                Modifier
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppColors.primaryBlue(),
                                selectedTextColor = AppColors.primaryBlue(),
                                unselectedIconColor = AppColors.textSecondary(),
                                unselectedTextColor = AppColors.textSecondary(),
                                indicatorColor = Color.Transparent,
                            ),
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(dest.labelRes),
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    val screenInsetsConfig = ScreenInsetsConfig(
        statusBarHandledByChrome = topBarHandlesStatusInsets,
        navigationBarHandledByChrome = bottomBarHandlesNavInsets,
    )

    @Composable
    fun MainNavHost(modifier: Modifier) {
        CompositionLocalProvider(LocalScreenInsets provides screenInsetsConfig) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = modifier,
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onSplashFinished = {
                            navController.navigate(Routes.CALCULATOR) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.CALCULATOR) {
                    val currentWalkthroughTarget = walkthroughSteps.getOrNull(walkthroughStepIndex)?.targetKey
                    CalculatorScreen(
                        resetToOverview = walkthroughActive &&
                            currentWalkthroughTarget in WalkthroughTargets.calculatorOverviewTargets,
                        calculatorHighlightModifiers = calculatorWalkthroughHighlightModifiers,
                        isAuthenticated = isAuthenticated,
                        onSignInRequired = {
                            openAuthGate(PendingAuthAction.LaunchProPurchase)
                        },
                    )
                }
                navigation(
                    route = Routes.VEHICLES,
                    startDestination = Routes.VEHICLES_LIST,
                ) {
                    composable(Routes.VEHICLES_LIST) { backStackEntry ->
                        val vehiclesGraphEntry = remember(backStackEntry) {
                            backStackEntry.parentGraphBackStackEntry(navController)
                        } ?: return@composable
                        val vehiclesViewModel: VehiclesViewModel = hiltViewModel(vehiclesGraphEntry)
                        VehiclesScreen(
                            onVehicleClick = { id ->
                                navController.navigate(Routes.vehicleDetailRoute(id))
                            },
                            onVehicleCreated = { id ->
                                navController.navigate(Routes.vehicleDetailRoute(id))
                            },
                            onEditLocked = ::openVehiclesProUpsell,
                            vehicleAddHighlightModifier = Modifier.onGloballyPositioned { coordinates ->
                                walkthroughTargets[WalkthroughTargets.VEHICLES_ADD] = coordinates.boundsInRoot()
                            },
                            viewModel = vehiclesViewModel,
                        )
                    }
                    composable(
                        route = Routes.VEHICLE_DETAIL,
                        arguments = listOf(
                            navArgument("vehicleId") { type = NavType.StringType },
                        ),
                    ) { backStackEntry ->
                        val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.let(Uri::decode)
                            ?: return@composable
                        val vehiclesGraphEntry = remember(backStackEntry) {
                            backStackEntry.parentGraphBackStackEntry(navController)
                        } ?: return@composable
                        val vehiclesViewModel: VehiclesViewModel = hiltViewModel(vehiclesGraphEntry)
                        VehicleDetailScreen(
                            vehicleId = vehicleId,
                            onBack = { navController.popBackStack() },
                            onOpenFuelLog = {
                                navController.navigate(Routes.vehicleFuelLogRoute(vehicleId))
                            },
                            onOpenCostOverview = {
                                navController.navigate(Routes.vehicleCostOverviewRoute(vehicleId))
                            },
                            onEditLocked = ::openVehiclesProUpsell,
                            viewModel = vehiclesViewModel,
                        )
                    }
                    composable(
                        route = Routes.VEHICLE_COST_OVERVIEW,
                        arguments = listOf(
                            navArgument("vehicleId") { type = NavType.StringType },
                        ),
                    ) { backStackEntry ->
                        val costOverviewVehicleId = backStackEntry.arguments?.getString("vehicleId")?.let(Uri::decode)
                            ?: return@composable
                        val vehiclesGraphEntry = remember(backStackEntry) {
                            backStackEntry.parentGraphBackStackEntry(navController)
                        } ?: return@composable
                        val vehiclesViewModel: VehiclesViewModel = hiltViewModel(vehiclesGraphEntry)
                        VehicleCostOverviewScreen(
                            vehicleId = costOverviewVehicleId,
                            onBack = { navController.popBackStack() },
                            onEditLocked = ::openVehiclesProUpsell,
                            viewModel = vehiclesViewModel,
                        )
                    }
                    composable(
                        route = Routes.VEHICLE_FUEL_LOG,
                        arguments = listOf(
                            navArgument("vehicleId") { type = NavType.StringType },
                        ),
                    ) { backStackEntry ->
                        val fuelLogVehicleId = backStackEntry.arguments?.getString("vehicleId")?.let(Uri::decode)
                            ?: return@composable
                        val vehiclesGraphEntry = remember(backStackEntry) {
                            backStackEntry.parentGraphBackStackEntry(navController)
                        } ?: return@composable
                        val vehiclesViewModel: VehiclesViewModel = hiltViewModel(vehiclesGraphEntry)
                        VehicleFuelLogScreen(
                            vehicleId = fuelLogVehicleId,
                            onBack = { navController.popBackStack() },
                            onEditLocked = ::openVehiclesProUpsell,
                            viewModel = vehiclesViewModel,
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        themeMode = preferences.themeMode,
                        languageMode = preferences.languageMode,
                        navStyle = preferences.navStyle,
                        isProUser = preferences.isPro,
                        isAdmin = preferences.isAdmin,
                        isAuthenticated = isAuthenticated,
                        onTheme = mainViewModel::setTheme,
                        onLanguageMode = mainViewModel::setLanguageMode,
                        onNavStyle = mainViewModel::setNavStyle,
                        onAccount = {
                            if (isAuthenticated) {
                                navController.navigate(Routes.ACCOUNT)
                            } else {
                                openAuthGate(PendingAuthAction.OpenAccount)
                            }
                        },
                        getSettingsMailCooldownRemainingMs = mainViewModel::getSettingsMailCooldownRemainingMs,
                        recordSettingsMailSubmit = mainViewModel::recordSettingsMailSubmit,
                        appearanceHighlightModifier = Modifier.onGloballyPositioned { coordinates ->
                            walkthroughTargets[WalkthroughTargets.SETTINGS_APPEARANCE] = coordinates.boundsInRoot()
                        },
                        proSectionHighlightModifier = Modifier.onGloballyPositioned { coordinates ->
                            walkthroughTargets[WalkthroughTargets.SETTINGS_PRO] = coordinates.boundsInRoot()
                        },
                    )
                }
                composable(Routes.ACCOUNT) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                            openAuthGate(PendingAuthAction.OpenAccount)
                        }
                    } else {
                        AccountScreen(
                            isAdmin = preferences.isAdmin,
                            firebaseAuth = firebaseAuth,
                            displayNameFromPrefs = preferences.displayName,
                            accountEmailFromPrefs = preferences.accountEmail,
                            onSaveDisplayName = mainViewModel::updateDisplayName,
                            onLogout = { scope.launch { authRepository.signOut() } },
                            onAdmin = { navigateToTab(Routes.ADMIN) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(Routes.ADMIN) {
                    if (!isAuthenticated || firebaseAuth.currentUser == null) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                            openAuthGate(PendingAuthAction.OpenAdmin)
                        }
                    } else {
                        AdminPanelScreen()
                    }
                }
            }
        }
    }

    AuthGateDialog(
        visible = authGateVisible,
        onDismiss = ::dismissAuthGate,
    )

    @Composable
    fun WalkthroughOverlayLayer() {
        if (walkthroughActive && isOnMainTabRoute()) {
            WalkthroughCoachMarksOverlay(
                steps = walkthroughSteps,
                currentStepIndex = walkthroughStepIndex,
                targets = walkthroughTargets,
                onBack = { walkthroughStepIndex = (walkthroughStepIndex - 1).coerceAtLeast(0) },
                onNext = { walkthroughStepIndex = (walkthroughStepIndex + 1).coerceAtMost(walkthroughSteps.lastIndex) },
                onSkip = {
                    walkthroughDismissed = true
                    onWalkthroughComplete()
                },
                onFinish = {
                    walkthroughDismissed = true
                    onWalkthroughComplete()
                },
            )
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (preferences.navStyle == NavStyle.DRAWER) {
        val drawerTitleRes = tabDestinations
            .firstOrNull { isTabSelected(it) }
            ?.labelRes
            ?: R.string.app_name

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(300.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .onGloballyPositioned { coordinates ->
                            walkthroughTargets[WalkthroughTargets.NAV_SURFACE] = coordinates.boundsInRoot()
                        },
                ) {
                    tabDestinations.forEach { dest ->
                        val selected = isTabSelected(dest)
                        val navTargetKey = walkthroughNavTargetForRoute(dest.route)
                        NavigationDrawerItem(
                            label = { Text(stringResource(dest.labelRes)) },
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateToTab(dest.route)
                            },
                            modifier = if (navTargetKey != null) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    walkthroughTargets[navTargetKey] = coordinates.boundsInRoot()
                                }
                            } else {
                                Modifier
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                        )
                    }
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = AppColors.background(),
                    topBar = {
                        if (isTopBarVisible) {
                            TopAppBar(
                                title = { Text(stringResource(drawerTitleRes)) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = stringResource(R.string.cd_menu),
                                        )
                                    }
                                },
                            )
                        }
                    },
                ) { paddingValues ->
                    MainNavHost(Modifier.padding(paddingValues))
                }
                WalkthroughOverlayLayer()
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = AppColors.background(),
                bottomBar = bottomBar,
            ) { paddingValues ->
                MainNavHost(Modifier.padding(paddingValues))
            }
            WalkthroughOverlayLayer()
        }
    }

    if (proUpsellModule != null) {
        ProUpsellDialog(
            module = proUpsellModule,
            isAuthenticated = isAuthenticated,
            onDismiss = { proUpsellModuleName = null },
            onSignInRequired = {
                proUpsellModuleName = null
                openAuthGate(PendingAuthAction.LaunchProPurchase)
            },
        )
    }
}

private fun NavBackStackEntry.parentGraphBackStackEntry(navController: NavController): NavBackStackEntry? {
    val parentGraph = destination.parent ?: return null
    return runCatching { navController.getBackStackEntry(parentGraph.id) }.getOrNull()
}
