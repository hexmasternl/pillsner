package nl.hexmaster.pillsner.applock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/**
 * The unlock screen (design D10): shown whenever [nl.hexmaster.pillsner.ui.PillsnerApp]'s root
 * gate is not [LockState.Unlocked] or [LockState.Disabled], with nothing else composed behind it.
 * Renders the PIN keypad for [LockState.Locked] and the device-credential reset for
 * [LockState.Recovering] (design D5). Stateless: [AppLockGate] supplies real behaviour, previews
 * supply fakes.
 *
 * @param authenticateWithBiometric shows the automatic class 2 biometric prompt.
 * @param authenticateWithDeviceCredential shows the recovery prompt (design D5).
 */
@Composable
fun UnlockScreen(
    uiState: AppLockUiState,
    events: Flow<AppLockEvent>,
    onScreenAppeared: () -> Unit,
    onPinSubmitted: (String) -> Unit,
    onBiometricResult: (BiometricResult) -> Unit,
    onBiometricPromptShown: () -> Unit,
    onRetryBiometricsClicked: () -> Unit,
    onRecoveryConfirmed: () -> Unit,
    authenticateWithBiometric: suspend () -> BiometricResult,
    authenticateWithDeviceCredential: suspend () -> BiometricResult,
    modifier: Modifier = Modifier,
) {
    var wrongPinShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onScreenAppeared()
        events.collect { event ->
            if (event is AppLockEvent.WrongPin) wrongPinShown = true
        }
    }

    LaunchedEffect(uiState.shouldPromptBiometricNow) {
        if (uiState.shouldPromptBiometricNow) {
            onBiometricPromptShown()
            onBiometricResult(authenticateWithBiometric())
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (uiState.lockState is LockState.Recovering) {
            RecoveryContent(
                onResetClicked = {
                    val result = authenticateWithDeviceCredential()
                    if (result is BiometricResult.Success) onRecoveryConfirmed()
                },
            )
        } else {
            var enteredPin by remember { mutableStateOf("") }
            val biometricAvailable = uiState.biometricEnabled && uiState.biometricStatus == BiometricStatus.Available

            LockedContent(
                enteredPin = enteredPin,
                cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
                wrongPinShown = wrongPinShown,
                biometricAvailable = biometricAvailable,
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
                onUseBiometricsClicked = onRetryBiometricsClicked,
            )
        }
    }
}

@Composable
private fun LockedContent(
    enteredPin: String,
    cooldownRemainingSeconds: Long,
    wrongPinShown: Boolean,
    biometricAvailable: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onUseBiometricsClicked: () -> Unit,
) {
    val cooldownActive = cooldownRemainingSeconds > 0

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.screenEdge)
            .padding(top = Spacing.xxl, bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_pillsner_logo),
            contentDescription = null,
            modifier = Modifier.size(Sizes.logoHeader),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.applock_unlock_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.weight(1f))

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
            onDigit = onDigit,
            onBackspace = onBackspace,
            onSubmit = onSubmit,
        )

        if (biometricAvailable) {
            Spacer(Modifier.height(Spacing.lg))
            FilledTonalButton(onClick = onUseBiometricsClicked, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.applock_use_biometrics))
            }
        }
    }
}

@Composable
private fun RecoveryContent(onResetClicked: suspend () -> Unit) {
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.screenEdge)
            .padding(top = Spacing.xxl, bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.applock_recovery_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.applock_recovery_explanation),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = { scope.launch { onResetClicked() } },
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.primaryActionHeight),
        ) {
            Text(stringResource(R.string.applock_reset_action))
        }
    }
}

/** Shared with [PinSetupScreen] and the disable-lock dialog: the `error`-role, live-announced message. */
@Composable
internal fun ErrorMessage(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_error_filled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
}

@PreviewLightDark
@Composable
private fun UnlockScreenLockedPreview() {
    PillsnerTheme {
        UnlockScreen(
            uiState = AppLockUiState(lockState = LockState.Locked(), pinLockEnabled = true, biometricEnabled = true),
            events = emptyFlow(),
            onScreenAppeared = {},
            onPinSubmitted = {},
            onBiometricResult = {},
            onBiometricPromptShown = {},
            onRetryBiometricsClicked = {},
            onRecoveryConfirmed = {},
            authenticateWithBiometric = { BiometricResult.Cancelled },
            authenticateWithDeviceCredential = { BiometricResult.Cancelled },
        )
    }
}

@Preview(fontScale = 2f)
@Composable
private fun UnlockScreenLargeFontPreview() {
    PillsnerTheme {
        UnlockScreen(
            uiState = AppLockUiState(lockState = LockState.Locked(), pinLockEnabled = true),
            events = emptyFlow(),
            onScreenAppeared = {},
            onPinSubmitted = {},
            onBiometricResult = {},
            onBiometricPromptShown = {},
            onRetryBiometricsClicked = {},
            onRecoveryConfirmed = {},
            authenticateWithBiometric = { BiometricResult.Cancelled },
            authenticateWithDeviceCredential = { BiometricResult.Cancelled },
        )
    }
}

@PreviewLightDark
@Composable
private fun UnlockScreenRecoveringPreview() {
    PillsnerTheme {
        UnlockScreen(
            uiState = AppLockUiState(lockState = LockState.Recovering, pinLockEnabled = true),
            events = emptyFlow(),
            onScreenAppeared = {},
            onPinSubmitted = {},
            onBiometricResult = {},
            onBiometricPromptShown = {},
            onRetryBiometricsClicked = {},
            onRecoveryConfirmed = {},
            authenticateWithBiometric = { BiometricResult.Cancelled },
            authenticateWithDeviceCredential = { BiometricResult.Cancelled },
        )
    }
}
