package nl.hexmaster.pillsner.applock.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
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
