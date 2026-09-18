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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.navigation.toRoute
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.applock.ui.PinSetupMode
import nl.hexmaster.pillsner.applock.ui.PinSetupScreen
import nl.hexmaster.pillsner.applock.ui.UnlockScreen
import nl.hexmaster.pillsner.applock.ui.VerifyIdentityCallbacks
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.domain.model.AppInfo
import nl.hexmaster.pillsner.ui.dose.DoseDetailEffect
import nl.hexmaster.pillsner.ui.dose.DoseDetailScreen
import nl.hexmaster.pillsner.ui.dose.DoseDetailViewModel
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
import nl.hexmaster.pillsner.ui.navigation.About
import nl.hexmaster.pillsner.ui.navigation.ReminderDiagnostics
import nl.hexmaster.pillsner.ui.navigation.DoseDetail
import nl.hexmaster.pillsner.ui.navigation.AcceptLegal
import nl.hexmaster.pillsner.ui.navigation.LegalDocumentRoute
import nl.hexmaster.pillsner.ui.navigation.MedicationFormGraph
import nl.hexmaster.pillsner.ui.navigation.toMedicationFormRoute
import nl.hexmaster.pillsner.ui.navigation.Home
import nl.hexmaster.pillsner.ui.navigation.Medicines
import nl.hexmaster.pillsner.ui.navigation.PinSetup
import nl.hexmaster.pillsner.ui.navigation.Settings
import nl.hexmaster.pillsner.ui.navigation.TopLevelDestination
import nl.hexmaster.pillsner.ui.navigation.topLevelDestinations
import nl.hexmaster.pillsner.ui.settings.SettingsScreen
import nl.hexmaster.pillsner.ui.settings.about.AboutScreen
import nl.hexmaster.pillsner.ui.settings.diagnostics.ReminderDiagnosticsScreen
import nl.hexmaster.pillsner.ui.settings.diagnostics.ReminderDiagnosticsViewModel
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionViewModel
import nl.hexmaster.pillsner.ui.settings.theme.ThemeSectionViewModel
import nl.hexmaster.pillsner.ui.settings.legal.AcceptLegalScreen
import nl.hexmaster.pillsner.ui.settings.legal.LegalDocumentScreen
import nl.hexmaster.pillsner.ui.settings.legal.LegalViewModel
import nl.hexmaster.pillsner.ui.settings.reset.ResetViewModel

/**
 * The app's root composable. Gains the app lock's root gate here (app-login design D1): the
 * navigation suite and `NavHost` are composed only while the lock is [LockState.Unlocked] or
 * [LockState.Disabled]. Every other state renders the unlock screen (or a blank surface while
 * [LockState.Loading]) instead, and the navigation host is not composed alongside it, so no
 * content is ever reachable behind the lock.
 *
 * @param viewModelFactory creates view models from the app's `AppContainer`.
 * @param appInfo what the build says about itself, shown on Settings and About. Passed down rather
 * than held in a view model because it never changes (app-about-screen design D3).
 * @param navController injectable so tests can observe navigation; created above this gate so
 * navigation state survives a relock (design D1).
 */
@Composable
fun PillsnerApp(
    viewModelFactory: ViewModelProvider.Factory,
    appLockViewModel: AppLockViewModel,
    biometricAuthenticator: BiometricAuthenticator,
    appInfo: AppInfo,
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
            appInfo = appInfo,
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
    appInfo: AppInfo,
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

    // The legal documents are read here rather than inside a destination (app-legal-information
    // design D5): Settings shows the state of acceptance and the Medicines add button decides on
    // it, so one view model at this level keeps both looking at the same answer, current at the
    // moment of the tap.
    val legalViewModel: LegalViewModel = viewModel(factory = viewModelFactory)
    val legalState by legalViewModel.uiState.collectAsStateWithLifecycle()

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
                    label = {
                        Text(
                            stringResource(destination.label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
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
                        // Tapping is the acknowledgement, which is what clears a missed reminder
                        // from the banner (design D5).
                        viewModel.onReminderBannerActivated(uiState.reminderProblem)
                        context.openReminderSettings(uiState.reminderProblem)
                    },
                    onOpenDose = { navController.navigate(DoseDetail(it.value)) },
                )
            }
            composable<DoseDetail> {
                val viewModel: DoseDetailViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val close = { navController.popBackStack(); Unit }
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            DoseDetailEffect.Close -> close()
                        }
                    }
                }
                DoseDetailScreen(
                    uiState = uiState,
                    onAnswer = viewModel::onAnswer,
                    onClose = close,
                )
            }
            composable<Medicines> {
                val viewModel: MedicinesViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MedicinesScreen(
                    uiState = uiState,
                    // The gate, and the whole of it (design D5): adding a medicine is the one thing
                    // acceptance guards. Opening a medicine that already exists, below, is not.
                    onAddMedicine = {
                        if (legalState.accepted) {
                            navController.navigate(MedicationFormGraph())
                        } else {
                            navController.navigate(AcceptLegal)
                        }
                    },
                    onSetActive = viewModel::onSetActive,
                    onOpenMedication = { id ->
                        navController.navigate(MedicationFormGraph(medicationId = id.value))
                    },
                    legalAccepted = legalState.accepted,
                    onLegalRequired = { navController.navigate(AcceptLegal) },
                    onScanLabel = viewModel::scanLabel,
                    onScanResult = { result -> navController.navigate(result.toMedicationFormRoute()) },
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
                val languageViewModel: LanguageSectionViewModel = viewModel(factory = viewModelFactory)
                val languageState by languageViewModel.state.collectAsStateWithLifecycle()
                val themeViewModel: ThemeSectionViewModel = viewModel(factory = viewModelFactory)
                val themeState by themeViewModel.state.collectAsStateWithLifecycle()
                val resetViewModel: ResetViewModel = viewModel(factory = viewModelFactory)
                val resetState by resetViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    languageState = languageState,
                    onLanguageSelected = languageViewModel::onLanguageSelected,
                    themeState = themeState,
                    onThemeSelected = themeViewModel::onThemeSelected,
                    appLockUiState = appLockUiState,
                    appLockEvents = appLockViewModel.eventFlow,
                    securityEffects = appLockViewModel.securityEffects,
                    onSecuritySectionAppeared = appLockViewModel::refreshBiometricAvailability,
                    onEnablePinLockRequested = { navController.navigate(PinSetup()) },
                    onLockDisableRequested = appLockViewModel::onLockDisableRequested,
                    onChangePinTapped = appLockViewModel::onChangePinTapped,
                    onStartPinChange = { navController.navigate(PinSetup(PinSetupMode.CHANGE)) },
                    onBiometricEnabled = appLockViewModel::onBiometricEnabled,
                    onBiometricDisableRequested = appLockViewModel::onBiometricDisableRequested,
                    verifyCallbacks = VerifyIdentityCallbacks(
                        onBiometricResult = appLockViewModel::onVerifyBiometricResult,
                        onPinSubmitted = appLockViewModel::onVerifyPinSubmitted,
                        onUsePin = appLockViewModel::onVerifyUsePin,
                        onUseBiometrics = appLockViewModel::onVerifyUseBiometrics,
                        onDismissed = appLockViewModel::onVerifyDismissed,
                    ),
                    authenticateWithBiometric = biometricAuthenticator::authenticateWithBiometric,
                    legalState = legalState.acceptance,
                    onOpenLegalDocument = { navController.navigate(LegalDocumentRoute(it)) },
                    appInfo = appInfo,
                    onAboutTapped = { navController.navigate(About) },
                    onReminderLogTapped = { navController.navigate(ReminderDiagnostics) },
                    resetState = resetState,
                    resetEffects = resetViewModel.effects,
                    onResetTapped = resetViewModel::onResetTapped,
                    onResetConfirmationToggled = resetViewModel::onConfirmationToggled,
                    onResetConfirmed = resetViewModel::onConfirmed,
                    onResetDismissed = resetViewModel::onDismiss,
                )
            }
            composable<About> {
                AboutScreen(appInfo = appInfo, onBack = { navController.popBackStack() })
            }
            composable<ReminderDiagnostics> {
                val viewModel: ReminderDiagnosticsViewModel = viewModel(factory = viewModelFactory)
                val entries by viewModel.entries.collectAsStateWithLifecycle()
                ReminderDiagnosticsScreen(
                    entries = entries,
                    copyText = viewModel::asText,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<LegalDocumentRoute> { backStackEntry ->
                val document = backStackEntry.toRoute<LegalDocumentRoute>().document
                LegalDocumentScreen(
                    document = CurrentLegalDocuments[document],
                    onBack = { navController.popBackStack() },
                )
            }
            composable<AcceptLegal> {
                AcceptLegalScreen(
                    disclaimer = CurrentLegalDocuments.disclaimer,
                    onBack = { navController.popBackStack() },
                    onReadTerms = {
                        navController.navigate(LegalDocumentRoute(LegalDocumentId.TERMS))
                    },
                    onAccept = {
                        legalViewModel.accept()
                        // The gate leaves the back stack as it is entered: back from the form
                        // returns to Medicines, never into the acceptance screen again.
                        navController.navigate(MedicationFormGraph()) {
                            popUpTo<AcceptLegal> { inclusive = true }
                        }
                    },
                )
            }
            composable<PinSetup> { backStackEntry ->
                val mode = backStackEntry.toRoute<PinSetup>().mode
                PinSetupScreen(
                    onBack = { navController.popBackStack() },
                    onPinConfirmed = { pin ->
                        when (mode) {
                            PinSetupMode.SET_UP -> appLockViewModel.onPinLockEnabled(pin)
                            PinSetupMode.CHANGE -> appLockViewModel.onNewPinConfirmed(pin)
                        }
                        navController.popBackStack()
                    },
                    mode = mode,
                    isPinInUse = appLockViewModel::isPinInUse,
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
