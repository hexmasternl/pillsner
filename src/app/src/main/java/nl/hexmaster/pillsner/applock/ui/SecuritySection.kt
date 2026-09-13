package nl.hexmaster.pillsner.applock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the Security section's rows, for semantics tests. */
object SecuritySectionTestTags {
    const val PROTECT_WITH_PIN_SWITCH = "applock_protect_with_pin_switch"
    const val BIOMETRIC_SWITCH = "applock_biometric_switch"
    const val CHANGE_PIN_ROW = "applock_change_pin_row"
}

/**
 * What the identity check reports back. Grouped so the Settings screen passes one thing down
 * instead of five lambdas that only ever travel together.
 */
data class VerifyIdentityCallbacks(
    val onBiometricResult: (BiometricResult) -> Unit,
    val onPinSubmitted: (String) -> Unit,
    val onUsePin: () -> Unit,
    val onUseBiometrics: () -> Unit,
    val onDismissed: () -> Unit,
)

/**
 * The Security section of the Settings screen (app-login design D10, app-settings-security D3):
 * "Protect with PIN", "Change PIN" while the lock is on, and "Unlock with biometrics".
 *
 * Both switches are bound to the persisted state, never to a local one, so a check that is still
 * running or was cancelled can never leave a switch showing something that was not saved.
 */
@Composable
fun SecuritySection(
    uiState: AppLockUiState,
    events: Flow<AppLockEvent>,
    onScreenAppeared: () -> Unit,
    onEnablePinLockRequested: () -> Unit,
    onLockDisableRequested: () -> Unit,
    onChangePinTapped: () -> Unit,
    onBiometricEnabled: () -> Unit,
    onBiometricDisableRequested: () -> Unit,
    verifyCallbacks: VerifyIdentityCallbacks,
    authenticateWithBiometric: suspend () -> BiometricResult,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onScreenAppeared() }

    val scope = rememberCoroutineScope()
    val biometricEligible = uiState.pinLockEnabled && uiState.biometricStatus == BiometricStatus.Available

    Column(modifier) {
        Text(
            text = stringResource(R.string.applock_security_header),
            style = MaterialTheme.typography.headlineSmall,
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.applock_protect_with_pin_title)) },
            supportingContent = { Text(stringResource(R.string.applock_protect_with_pin_supporting)) },
            trailingContent = {
                Switch(
                    checked = uiState.pinLockEnabled,
                    onCheckedChange = { checked ->
                        if (checked) onEnablePinLockRequested() else onLockDisableRequested()
                    },
                    modifier = Modifier.testTag(SecuritySectionTestTags.PROTECT_WITH_PIN_SWITCH),
                )
            },
        )

        if (uiState.pinLockEnabled) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.applock_change_pin_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = { Text(stringResource(R.string.applock_change_pin_supporting)) },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.iconDefault),
                    )
                },
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .clickable(role = Role.Button, onClick = onChangePinTapped)
                    .testTag(SecuritySectionTestTags.CHANGE_PIN_ROW),
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.applock_biometric_title)) },
            supportingContent = {
                Text(
                    if (biometricEligible) {
                        stringResource(R.string.applock_biometric_supporting)
                    } else if (!uiState.pinLockEnabled) {
                        stringResource(R.string.applock_biometric_disabled_no_pin)
                    } else {
                        stringResource(R.string.applock_biometric_disabled_not_enrolled)
                    },
                )
            },
            trailingContent = {
                Switch(
                    checked = uiState.biometricEnabled,
                    enabled = biometricEligible,
                    onCheckedChange = { checked ->
                        if (checked) {
                            // Turning it on proves both things at once: who the user is, and that
                            // the enrolled biometric actually works (design D6).
                            scope.launch {
                                if (authenticateWithBiometric() is BiometricResult.Success) onBiometricEnabled()
                            }
                        } else {
                            onBiometricDisableRequested()
                        }
                    },
                    modifier = Modifier.testTag(SecuritySectionTestTags.BIOMETRIC_SWITCH),
                )
            },
        )

        Text(
            text = stringResource(R.string.applock_screenshot_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }

    VerifyIdentityDialog(
        state = uiState.verify,
        cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
        events = events,
        biometricAvailable = uiState.biometricEnabled && uiState.biometricStatus == BiometricStatus.Available,
        authenticateWithBiometric = authenticateWithBiometric,
        onBiometricResult = verifyCallbacks.onBiometricResult,
        onPinSubmitted = verifyCallbacks.onPinSubmitted,
        onUsePin = verifyCallbacks.onUsePin,
        onUseBiometrics = verifyCallbacks.onUseBiometrics,
        onDismiss = verifyCallbacks.onDismissed,
    )
}

@PreviewLightDark
@Composable
private fun SecuritySectionPreview() {
    PillsnerTheme {
        SecuritySection(
            uiState = AppLockUiState(pinLockEnabled = true, biometricStatus = BiometricStatus.Available),
            events = emptyFlow(),
            onScreenAppeared = {},
            onEnablePinLockRequested = {},
            onLockDisableRequested = {},
            onChangePinTapped = {},
            onBiometricEnabled = {},
            onBiometricDisableRequested = {},
            verifyCallbacks = NoVerifyCallbacks,
            authenticateWithBiometric = { BiometricResult.Cancelled },
        )
    }
}

@Preview(fontScale = 2f)
@Composable
private fun SecuritySectionLargeFontPreview() {
    PillsnerTheme {
        SecuritySection(
            uiState = AppLockUiState(pinLockEnabled = true, biometricStatus = BiometricStatus.Available),
            events = emptyFlow(),
            onScreenAppeared = {},
            onEnablePinLockRequested = {},
            onLockDisableRequested = {},
            onChangePinTapped = {},
            onBiometricEnabled = {},
            onBiometricDisableRequested = {},
            verifyCallbacks = NoVerifyCallbacks,
            authenticateWithBiometric = { BiometricResult.Cancelled },
        )
    }
}

private val NoVerifyCallbacks = VerifyIdentityCallbacks({}, {}, {}, {}, {})
