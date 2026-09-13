package nl.hexmaster.pillsner.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.applock.ui.PinSetupScreen
import nl.hexmaster.pillsner.applock.ui.UnlockScreen
import nl.hexmaster.pillsner.ui.home.HomeViewModel
import nl.hexmaster.pillsner.ui.home.NotificationPermissionEffect
import nl.hexmaster.pillsner.ui.home.openReminderSettings
import nl.hexmaster.pillsner.ui.home.WelcomeScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import nl.hexmaster.pillsner.ui.medicines.MedicinesEffect
import nl.hexmaster.pillsner.ui.medicines.MedicinesScreen
import nl.hexmaster.pillsner.ui.medicines.MedicinesViewModel
import nl.hexmaster.pillsner.ui.medicines.form.medicationFormGraph
import nl.hexmaster.pillsner.ui.navigation.MedicationFormGraph
import nl.hexmaster.pillsner.ui.navigation.Home
import nl.hexmaster.pillsner.ui.navigation.Medicines
import nl.hexmaster.pillsner.ui.navigation.PinSetup
import nl.hexmaster.pillsner.ui.navigation.Settings
import nl.hexmaster.pillsner.ui.navigation.TopLevelDestination
import nl.hexmaster.pillsner.ui.navigation.topLevelDestinations
import nl.hexmaster.pillsner.ui.settings.SettingsScreen

/**
 * The app's root composable. Gains the app lock's root gate here (app-login design D1): the
 * navigation suite and `NavHost` are composed only while the lock is [LockState.Unlocked] or
 * [LockState.Disabled]. Every other state renders the unlock screen (or a blank surface while
 * [LockState.Loading]) instead, and the navigation host is not composed alongside it, so no
 * content is ever reachable behind the lock.
 *
 * @param viewModelFactory creates view models from the app's `AppContainer`.
 * @param navController injectable so tests can observe navigation; created above this gate so
 * navigation state survives a relock (design D1).
 */
@Composable
fun PillsnerApp(
    viewModelFactory: ViewModelProvider.Factory,
    appLockViewModel: AppLockViewModel,
    biometricAuthenticator: BiometricAuthenticator,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()

    when (appLockUiState.lockState) {
        LockState.Loading -> Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {}

        is LockState.Locked, LockState.Recovering -> UnlockScreen(
            uiState = appLockUiState,
            events = appLockViewModel.eventFlow,
            onScreenAppeared = appLockViewModel::refreshBiometricAvailability,
            onPinSubmitted = appLockViewModel::onUnlockPinSubmitted,
            onBiometricResult = appLockViewModel::onBiometricResult,
            onBiometricPromptShown = appLockViewModel::onBiometricPromptShown,
            onRetryBiometricsClicked = appLockViewModel::onRetryBiometricsClicked,
            onRecoveryConfirmed = appLockViewModel::onRecoveryConfirmed,
            authenticateWithBiometric = biometricAuthenticator::authenticateWithBiometric,
            authenticateWithDeviceCredential = biometricAuthenticator::authenticateWithDeviceCredential,
            modifier = modifier,
        )

        LockState.Unlocked, LockState.Disabled -> PillsnerAppContent(
            viewModelFactory = viewModelFactory,
            appLockViewModel = appLockViewModel,
            biometricAuthenticator = biometricAuthenticator,
            modifier = modifier,
            navController = navController,
        )
    }
}

/**
 * The app shell (design D2, D3): a navigation bar on compact widths, a rail from medium width up,
 * wrapped around a `NavHost` with Home, Medicines and Settings. The suite is shown only while the
 * current destination is one of the three top-level routes, so later nested screens hide it for free.
 */
@Composable
private fun PillsnerAppContent(
    viewModelFactory: ViewModelProvider.Factory,
    appLockViewModel: AppLockViewModel,
    biometricAuthenticator: BiometricAuthenticator,
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTopLevelDestination = topLevelDestinations.any { currentDestination.isOn(it) }

    val layoutType = if (onTopLevelDestination) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    } else {
        NavigationSuiteType.None
    }

    // The form flow closes itself when a medicine cannot be opened, so the message it wants to show
    // belongs to the screen the user lands back on.
    val overviewEffects = remember { MutableSharedFlow<MedicinesEffect>(extraBufferCapacity = 1) }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        navigationSuiteItems = {
            topLevelDestinations.forEach { destination ->
                val selected = currentDestination.isOn(destination)
                item(
                    selected = selected,
                    onClick = { navController.navigateTopLevel(destination) },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.filledIcon else destination.outlinedIcon),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag(destination.testTag),
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = Home) {
            composable<Home> {
                val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val shouldRequest by viewModel.shouldRequestNotificationPermission.collectAsStateWithLifecycle()
                NotificationPermissionEffect(
                    shouldRequest = shouldRequest,
                    onPermissionChanged = viewModel::onNotificationPermissionChecked,
                    onRequested = viewModel::onNotificationPermissionRequested,
                )
                WelcomeScreen(
                    uiState = uiState,
                    onOpenReminderSettings = {
                        context.openReminderSettings(uiState.notificationsAllowed)
                    },
                )
            }
            composable<Medicines> {
                val viewModel: MedicinesViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MedicinesScreen(
                    uiState = uiState,
                    onAddMedicine = { navController.navigate(MedicationFormGraph()) },
                    onSetActive = viewModel::onSetActive,
                    onOpenMedication = { id ->
                        navController.navigate(MedicationFormGraph(medicationId = id.value))
                    },
                    effects = merge(viewModel.effects, overviewEffects),
                )
            }
            medicationFormGraph(
                navController = navController,
                viewModelFactory = viewModelFactory,
                onOpenFailed = { overviewEffects.tryEmit(MedicinesEffect.OpenFailed) },
            )
            composable<Settings> {
                val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    appLockUiState = appLockUiState,
                    appLockEvents = appLockViewModel.eventFlow,
                    onSecuritySectionAppeared = appLockViewModel::refreshBiometricAvailability,
                    onEnablePinLockRequested = { navController.navigate(PinSetup) },
                    onDisableLockPinSubmitted = appLockViewModel::onDisableLockPinSubmitted,
                    onBiometricToggle = appLockViewModel::onBiometricToggle,
                    authenticateWithBiometric = biometricAuthenticator::authenticateWithBiometric,
                )
            }
            composable<PinSetup> {
                PinSetupScreen(
                    onBack = { navController.popBackStack() },
                    onPinConfirmed = { pin ->
                        appLockViewModel.onPinLockEnabled(pin)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/**
 * Material's pattern for top-level navigation: pop to the start destination saving state,
 * never stack a duplicate, and restore the target's saved state (design D2).
 */
private fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
