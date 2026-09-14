package nl.hexmaster.pillsner.applock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.SecurityAction
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityRequest
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/** Stable tags for the identity check, for semantics tests. */
object VerifyIdentityDialogTestTags {
    const val DIALOG = "applock_verify_dialog"
    const val USE_PIN = "applock_verify_use_pin"
    const val USE_BIOMETRICS = "applock_verify_use_biometrics"
}

/**
 * The identity check shown before a Security change (design D4): the biometric prompt with a
 * "Use PIN" way out, or the keypad, depending on [state]. It owns no rules — it renders the state
 * the view model gives it and reports back what the user did.
 *
 * Renders nothing while no check is running, so the caller can compose it unconditionally.
 */
@Composable
fun VerifyIdentityDialog(
    state: VerifyIdentityState,
    cooldownRemainingSeconds: Long,
    events: Flow<AppLockEvent>,
    biometricAvailable: Boolean,
    authenticateWithBiometric: suspend () -> BiometricResult,
    onBiometricResult: (BiometricResult) -> Unit,
    onPinSubmitted: (String) -> Unit,
    onUsePin: () -> Unit,
    onUseBiometrics: () -> Unit,
    onDismiss: () -> Unit,
) {
    val request = state.requestOrNull() ?: return

    var enteredPin by remember { mutableStateOf("") }
    var wrongPinShown by remember { mutableStateOf(false) }
    // The system refuses further prompts after too many biometric failures, so offering a retry
    // then would only lead back to the same refusal (spec "Biometric failure falls back to PIN").
    var biometricLockedOut by remember { mutableStateOf(false) }
    val cooldownActive = cooldownRemainingSeconds > 0

    LaunchedEffect(events) {
        events.collect { event ->
            if (event is AppLockEvent.WrongPin) {
                wrongPinShown = true
                enteredPin = ""
            }
        }
    }

    if (state is VerifyIdentityState.AwaitingBiometric) {
        LaunchedEffect(state) {
            val result = authenticateWithBiometric()
            biometricLockedOut = result is BiometricResult.LockedOut
            onBiometricResult(result)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(VerifyIdentityDialogTestTags.DIALOG),
        title = {
            Text(
                text = stringResource(request.purpose.titleRes()),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (state) {
                    is VerifyIdentityState.AwaitingBiometric -> {
                        Text(
                            text = stringResource(R.string.applock_verify_biometric_instruction),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        FilledTonalButton(
                            onClick = onUsePin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(VerifyIdentityDialogTestTags.USE_PIN),
                        ) {
                            Text(stringResource(R.string.applock_use_pin))
                        }
                    }

                    is VerifyIdentityState.AwaitingPin -> {
                        Text(
                            text = stringResource(R.string.applock_verify_pin_instruction),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(Spacing.md))

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
                            Spacer(Modifier.height(Spacing.md))
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

                        if (request.allowBiometric && biometricAvailable && !biometricLockedOut) {
                            Spacer(Modifier.height(Spacing.lg))
                            FilledTonalButton(
                                onClick = onUseBiometrics,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(VerifyIdentityDialogTestTags.USE_BIOMETRICS),
                            ) {
                                Text(stringResource(R.string.applock_use_biometrics))
                            }
                        }
                    }

                    else -> Unit
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** The check being run, or null while none is (Idle, or one already consumed). */
private fun VerifyIdentityState.requestOrNull(): VerifyIdentityRequest? = when (this) {
    is VerifyIdentityState.AwaitingBiometric -> request
    is VerifyIdentityState.AwaitingPin -> request
    else -> null
}

private fun SecurityAction.titleRes(): Int = when (this) {
    SecurityAction.CHANGE_PIN -> R.string.applock_verify_change_pin_title
    SecurityAction.DISABLE_BIOMETRICS -> R.string.applock_verify_disable_biometrics_title
    SecurityAction.DISABLE_LOCK -> R.string.applock_verify_disable_lock_title
}

@PreviewLightDark
@Composable
private fun VerifyIdentityDialogPinPreview() {
    PillsnerTheme {
        VerifyIdentityDialog(
            state = VerifyIdentityState.AwaitingPin(
                VerifyIdentityRequest(SecurityAction.DISABLE_LOCK, allowBiometric = false),
            ),
            cooldownRemainingSeconds = 0,
            events = emptyFlow(),
            biometricAvailable = false,
            authenticateWithBiometric = { BiometricResult.Cancelled },
            onBiometricResult = {},
            onPinSubmitted = {},
            onUsePin = {},
            onUseBiometrics = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun VerifyIdentityDialogBiometricPreview() {
    PillsnerTheme {
        VerifyIdentityDialog(
            state = VerifyIdentityState.AwaitingBiometric(
                VerifyIdentityRequest(SecurityAction.CHANGE_PIN, allowBiometric = true),
            ),
            cooldownRemainingSeconds = 0,
            events = emptyFlow(),
            biometricAvailable = true,
            authenticateWithBiometric = { BiometricResult.Cancelled },
            onBiometricResult = {},
            onPinSubmitted = {},
            onUsePin = {},
            onUseBiometrics = {},
            onDismiss = {},
        )
    }
}

@Preview(fontScale = 2f)
@Composable
private fun VerifyIdentityDialogLargeFontPreview() {
    PillsnerTheme {
        VerifyIdentityDialog(
            state = VerifyIdentityState.AwaitingPin(
                VerifyIdentityRequest(SecurityAction.CHANGE_PIN, allowBiometric = true),
            ),
            cooldownRemainingSeconds = 0,
            events = emptyFlow(),
            biometricAvailable = true,
            authenticateWithBiometric = { BiometricResult.Cancelled },
            onBiometricResult = {},
            onPinSubmitted = {},
            onUsePin = {},
            onUseBiometrics = {},
            onDismiss = {},
        )
    }
}
