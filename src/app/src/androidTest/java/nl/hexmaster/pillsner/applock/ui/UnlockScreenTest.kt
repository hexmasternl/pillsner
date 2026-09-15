package nl.hexmaster.pillsner.applock.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.LockState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose semantics tests for [UnlockScreen] (task 5.6). */
@RunWith(AndroidJUnit4::class)
class UnlockScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun submittingAPinInvokesTheCallback() {
        var submittedPin: String? = null

        composeRule.setContent {
            UnlockScreen(
                uiState = AppLockUiState(lockState = LockState.Locked()),
                events = emptyFlow(),
                onScreenAppeared = {},
                onPinSubmitted = { submittedPin = it },
                onBiometricResult = {},
                onBiometricPromptShown = {},
                onRetryBiometricsClicked = {},
                onRecoveryConfirmed = {},
                authenticateWithBiometric = { BiometricResult.Cancelled },
                authenticateWithDeviceCredential = { BiometricResult.Cancelled },
            )
        }

        composeRule.onNodeWithText("1").performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("3").performClick()
        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        assert(submittedPin == "1234") { "Expected 1234 but was $submittedPin" }
    }

    @Test
    fun fewerThanFourDigitsCannotBeSubmitted() {
        composeRule.setContent {
            UnlockScreen(
                uiState = AppLockUiState(lockState = LockState.Locked()),
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

        for (digit in "123") composeRule.onNodeWithText(digit.toString()).performClick()
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()

        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("Continue").assertIsEnabled()
    }

    @Test
    fun theEntryStopsAtSixDigits() {
        var submittedPin: String? = null

        composeRule.setContent {
            UnlockScreen(
                uiState = AppLockUiState(lockState = LockState.Locked()),
                events = emptyFlow(),
                onScreenAppeared = {},
                onPinSubmitted = { submittedPin = it },
                onBiometricResult = {},
                onBiometricPromptShown = {},
                onRetryBiometricsClicked = {},
                onRecoveryConfirmed = {},
                authenticateWithBiometric = { BiometricResult.Cancelled },
                authenticateWithDeviceCredential = { BiometricResult.Cancelled },
            )
        }

        for (digit in "123456789") composeRule.onNodeWithText(digit.toString()).performClick()
        composeRule.onNodeWithText("Continue").performClick()

        assert(submittedPin == "123456") { "Expected the entry to stop at 123456 but was $submittedPin" }
    }

    /**
     * The view model turns [AppLockUiState.shouldPromptBiometricNow] off the moment the prompt is
     * shown, so that it is not re-presented. The screen must keep waiting for the prompt's answer
     * across that change instead of taking the prompt back down with it.
     */
    @Test
    fun theBiometricPromptOutlivesTheFlagThatRaisedIt() {
        val promptAnswer = CompletableDeferred<BiometricResult>()
        var promptShown = false
        var reported: BiometricResult? = null
        val uiState = mutableStateOf(
            AppLockUiState(
                lockState = LockState.Locked(),
                pinLockEnabled = true,
                biometricEnabled = true,
                biometricStatus = BiometricStatus.Available,
                shouldPromptBiometricNow = true,
            ),
        )

        composeRule.setContent {
            UnlockScreen(
                uiState = uiState.value,
                events = emptyFlow(),
                onScreenAppeared = {},
                onPinSubmitted = {},
                onBiometricResult = { reported = it },
                onBiometricPromptShown = {
                    promptShown = true
                    uiState.value = uiState.value.copy(shouldPromptBiometricNow = false)
                },
                onRetryBiometricsClicked = {},
                onRecoveryConfirmed = {},
                authenticateWithBiometric = { promptAnswer.await() },
                authenticateWithDeviceCredential = { BiometricResult.Cancelled },
            )
        }

        composeRule.waitUntil { promptShown }
        promptAnswer.complete(BiometricResult.Success)
        composeRule.waitUntil { reported != null }

        assert(reported == BiometricResult.Success) { "Expected the prompt's success to be reported, was $reported" }
    }

    @Test
    fun wrongPinEventShowsTheErrorMessage() {
        val events = MutableSharedFlow<AppLockEvent>(extraBufferCapacity = 1)

        composeRule.setContent {
            UnlockScreen(
                uiState = AppLockUiState(lockState = LockState.Locked()),
                events = events,
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

        composeRule.runOnUiThread { events.tryEmit(AppLockEvent.WrongPin) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That PIN doesn't match. Try again.").assertIsDisplayed()
    }

    @Test
    fun activeCooldownDisablesTheKeypadAndShowsTheCountdown() {
        composeRule.setContent {
            UnlockScreen(
                uiState = AppLockUiState(lockState = LockState.Locked(), cooldownRemainingSeconds = 30),
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

        composeRule.onNodeWithText("Too many wrong attempts. Try again in 30 seconds.").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsNotEnabled()
    }

    @Test
    fun recoveringStateShowsResetActionAndHidesTheKeypad() {
        composeRule.setContent {
            UnlockScreen(
                uiState = AppLockUiState(lockState = LockState.Recovering),
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

        composeRule.onNodeWithText("Reset app lock").assertIsDisplayed()
        val keypadDigits = composeRule.onAllNodesWithText("1").fetchSemanticsNodes().size
        assert(keypadDigits == 0) { "Expected no PIN keypad while recovering, found $keypadDigits matches for '1'" }
    }
}
