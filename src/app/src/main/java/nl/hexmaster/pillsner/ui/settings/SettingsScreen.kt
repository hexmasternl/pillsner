package nl.hexmaster.pillsner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.ui.AppLockEvent
import nl.hexmaster.pillsner.applock.ui.AppLockUiState
import nl.hexmaster.pillsner.applock.ui.BiometricResult
import nl.hexmaster.pillsner.applock.ui.SecurityEffect
import nl.hexmaster.pillsner.applock.ui.SecuritySection
import nl.hexmaster.pillsner.applock.ui.VerifyIdentityCallbacks
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.domain.model.AppInfo
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.about.AboutSection
import nl.hexmaster.pillsner.ui.settings.about.PreviewAppInfo
import nl.hexmaster.pillsner.ui.settings.language.LanguageSection
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionState
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceState
import nl.hexmaster.pillsner.ui.settings.legal.LegalSection
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the Settings screen itself. */
object SettingsScreenTestTags {
    const val SNACKBAR = "settings_snackbar"
}

/**
 * The Settings destination: a list of sections (design D5, and app-login D10).
 *
 * Each section is a self-contained composable with its own state, so a later change adds one by
 * adding it to this list rather than by touching the others. Language comes first because it
 * decides how everything below it reads.
 */
@Composable
fun SettingsScreen(
    languageState: LanguageSectionState,
    onLanguageSelected: (AppLanguage) -> Unit,
    appLockUiState: AppLockUiState,
    appLockEvents: Flow<AppLockEvent>,
    securityEffects: Flow<SecurityEffect>,
    onSecuritySectionAppeared: () -> Unit,
    onEnablePinLockRequested: () -> Unit,
    onLockDisableRequested: () -> Unit,
    onChangePinTapped: () -> Unit,
    onStartPinChange: () -> Unit,
    onBiometricEnabled: () -> Unit,
    onBiometricDisableRequested: () -> Unit,
    verifyCallbacks: VerifyIdentityCallbacks,
    authenticateWithBiometric: suspend () -> BiometricResult,
    legalState: LegalAcceptanceState,
    onOpenLegalDocument: (LegalDocumentId) -> Unit,
    appInfo: AppInfo,
    onAboutTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pinChanged = stringResource(R.string.applock_pin_changed_message)
    val biometricsOff = stringResource(R.string.applock_biometrics_off_message)
    val lockDisabled = stringResource(R.string.applock_lock_disabled_message)

    LaunchedEffect(securityEffects) {
        securityEffects.collect { effect ->
            when (effect) {
                SecurityEffect.StartPinChange -> onStartPinChange()
                SecurityEffect.PinChanged -> snackbarHostState.showSnackbar(pinChanged)
                SecurityEffect.BiometricsTurnedOff -> snackbarHostState.showSnackbar(biometricsOff)
                SecurityEffect.LockDisabled -> snackbarHostState.showSnackbar(lockDisabled)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = {
            SnackbarHost(snackbarHostState, Modifier.testTag(SettingsScreenTestTags.SNACKBAR))
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= Spacing.contentMaxWidth
            val sidePadding = if (isWide) Spacing.screenEdgeWide else Spacing.screenEdge

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Spacing.contentMaxWidth),
                contentPadding = PaddingValues(start = sidePadding, end = sidePadding, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                item(key = "title") {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier
                            .semantics { heading() }
                            .testTag(NavigationTestTags.SETTINGS_TITLE),
                    )
                }

                item(key = "language") {
                    LanguageSection(state = languageState, onLanguageSelected = onLanguageSelected)
                }

                item(key = "security") {
                    SecuritySection(
                        uiState = appLockUiState,
                        events = appLockEvents,
                        onScreenAppeared = onSecuritySectionAppeared,
                        onEnablePinLockRequested = onEnablePinLockRequested,
                        onLockDisableRequested = onLockDisableRequested,
                        onChangePinTapped = onChangePinTapped,
                        onBiometricEnabled = onBiometricEnabled,
                        onBiometricDisableRequested = onBiometricDisableRequested,
                        verifyCallbacks = verifyCallbacks,
                        authenticateWithBiometric = authenticateWithBiometric,
                    )
                }

                item(key = "legal") {
                    LegalSection(state = legalState, onOpenDocument = onOpenLegalDocument)
                }

                item(key = "about") {
                    AboutSection(appInfo = appInfo, onAboutTapped = onAboutTapped)
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun SettingsScreenPreview() {
    PillsnerTheme {
        Surface {
            SettingsScreen(
                languageState = LanguageSectionState(
                    selected = AppLanguage.DUTCH,
                    restartRequired = true,
                ),
                onLanguageSelected = {},
                appLockUiState = AppLockUiState(
                    pinLockEnabled = true,
                    biometricStatus = BiometricStatus.Available,
                ),
                appLockEvents = emptyFlow(),
                securityEffects = emptyFlow(),
                onSecuritySectionAppeared = {},
                onEnablePinLockRequested = {},
                onLockDisableRequested = {},
                onChangePinTapped = {},
                onStartPinChange = {},
                onBiometricEnabled = {},
                onBiometricDisableRequested = {},
                verifyCallbacks = VerifyIdentityCallbacks({}, {}, {}, {}, {}),
                authenticateWithBiometric = { BiometricResult.Cancelled },
                legalState = LegalAcceptanceState.Accepted(LocalDate.of(2026, 9, 14)),
                onOpenLegalDocument = {},
                appInfo = PreviewAppInfo,
                onAboutTapped = {},
            )
        }
    }
}
