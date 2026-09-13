package nl.hexmaster.pillsner.applock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/** Stable tags for the Security section's switches, for semantics tests. */
object SecuritySectionTestTags {
    const val PROTECT_WITH_PIN_SWITCH = "applock_protect_with_pin_switch"
    const val BIOMETRIC_SWITCH = "applock_biometric_switch"
}

/**
 * The Security section of the Settings screen (design D10, task 6.3): "Protect with PIN" and
 * "Unlock with biometrics" as switch rows, sharing the same failed-attempt cooldown as the unlock
 * screen (design D7) through [uiState] and [events].
 */
@Composable
fun SecuritySection(
    uiState: AppLockUiState,
    events: Flow<AppLockEvent>,
    onScreenAppeared: () -> Unit,
    onEnablePinLockRequested: () -> Unit,
    onDisableLockPinSubmitted: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    authenticateWithBiometric: suspend () -> BiometricResult,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onScreenAppeared() }

    var showDisableDialog by remember { mutableStateOf(false) }
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
                        if (checked) onEnablePinLockRequested() else showDisableDialog = true
                    },
                    modifier = Modifier.testTag(SecuritySectionTestTags.PROTECT_WITH_PIN_SWITCH),
                )
            },
        )

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
                            scope.launch {
                                if (authenticateWithBiometric() is BiometricResult.Success) onBiometricToggle(true)
                            }
                        } else {
                            onBiometricToggle(false)
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

    if (showDisableDialog) {
        DisableLockDialog(
            cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
            events = events,
            onDismiss = { showDisableDialog = false },
            onPinSubmitted = onDisableLockPinSubmitted,
        )
    }

    // The shared disable-lock dialog above always closes itself once the lock actually turns off.
    LaunchedEffect(uiState.pinLockEnabled) {
        if (!uiState.pinLockEnabled) showDisableDialog = false
    }
}

@Composable
private fun DisableLockDialog(
    cooldownRemainingSeconds: Long,
    events: Flow<AppLockEvent>,
    onDismiss: () -> Unit,
    onPinSubmitted: (String) -> Unit,
) {
    var enteredPin by remember { mutableStateOf("") }
    var wrongPinShown by remember { mutableStateOf(false) }
    val cooldownActive = cooldownRemainingSeconds > 0

    LaunchedEffect(Unit) {
        events.collect { event ->
            if (event is AppLockEvent.WrongPin) {
                wrongPinShown = true
                enteredPin = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.applock_disable_dialog_title), style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column {
                if (wrongPinShown || cooldownActive) {
                    ErrorMessage(
                        text = if (cooldownActive) {
                            pluralStringResource(
                                R.plurals.applock_cooldown_message,
                                cooldownRemainingSeconds.toInt(),
                                cooldownRemainingSeconds.toInt(),
                            )
                        } else {
                            stringResource(R.string.applock_wrong_pin_message)
                        },
                    )
                }
                PinKeypad(
                    enteredLength = enteredPin.length,
                    maxLength = MAX_PIN_LENGTH,
                    enabled = !cooldownActive,
                    submitEnabled = !cooldownActive && enteredPin.length >= MIN_PIN_LENGTH,
                    onDigit = { digit ->
                        if (enteredPin.length < MAX_PIN_LENGTH) {
                            wrongPinShown = false
                            enteredPin += digit
                        }
                    },
                    onBackspace = {
                        wrongPinShown = false
                        enteredPin = enteredPin.dropLast(1)
                    },
                    onSubmit = {
                        onPinSubmitted(enteredPin)
                        enteredPin = ""
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
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
            onDisableLockPinSubmitted = {},
            onBiometricToggle = {},
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
            onDisableLockPinSubmitted = {},
            onBiometricToggle = {},
            authenticateWithBiometric = { BiometricResult.Cancelled },
        )
    }
}
