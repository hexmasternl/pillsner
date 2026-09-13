package nl.hexmaster.pillsner.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * The Settings destination: a list of sections (design D10, app-login task 6.3). Security is the
 * only section until later changes (`app-settings-language` and others) add their own; whichever
 * change lands second inserts its section into this same list rather than recreating it.
 */
@Composable
fun SettingsScreen(
    appLockUiState: AppLockUiState,
    appLockEvents: Flow<AppLockEvent>,
    onSecuritySectionAppeared: () -> Unit,
    onEnablePinLockRequested: () -> Unit,
    onDisableLockPinSubmitted: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    authenticateWithBiometric: suspend () -> BiometricResult,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .padding(horizontal = Spacing.screenEdge)
                .padding(top = Spacing.xxl, bottom = Spacing.xl)
                .semantics { heading() }
                .testTag(NavigationTestTags.SETTINGS_TITLE),
        )

        LazyColumn(modifier = Modifier.padding(horizontal = Spacing.screenEdge)) {
            item {
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
@Composable
private fun SettingsScreenPreview() {
    PillsnerTheme {
        Surface {
            SettingsScreen(
                appLockUiState = AppLockUiState(pinLockEnabled = true, biometricStatus = BiometricStatus.Available),
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

@Preview(fontScale = 2f)
@Composable
private fun SettingsScreenLargeFontPreview() {
    PillsnerTheme {
        Surface {
            SettingsScreen(
                appLockUiState = AppLockUiState(pinLockEnabled = true, biometricStatus = BiometricStatus.Available),
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
