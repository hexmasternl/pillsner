package nl.hexmaster.pillsner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.ui.AppLockEvent
import nl.hexmaster.pillsner.applock.ui.AppLockUiState
import nl.hexmaster.pillsner.applock.ui.BiometricResult
import nl.hexmaster.pillsner.applock.ui.SecuritySection
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.language.LanguageSection
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

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
    onSecuritySectionAppeared: () -> Unit,
    onEnablePinLockRequested: () -> Unit,
    onDisableLockPinSubmitted: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    authenticateWithBiometric: suspend () -> BiometricResult,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
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
                    onDisableLockPinSubmitted = onDisableLockPinSubmitted,
                    onBiometricToggle = onBiometricToggle,
                    authenticateWithBiometric = authenticateWithBiometric,
                )
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
                onSecuritySectionAppeared = {},
                onEnablePinLockRequested = {},
                onDisableLockPinSubmitted = {},
                onBiometricToggle = {},
                authenticateWithBiometric = { BiometricResult.Cancelled },
            )
        }
    }
}
