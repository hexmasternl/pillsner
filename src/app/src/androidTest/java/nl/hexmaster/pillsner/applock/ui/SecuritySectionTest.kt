package nl.hexmaster.pillsner.applock.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose semantics tests for [SecuritySection] (task 6.9). */
@RunWith(AndroidJUnit4::class)
class SecuritySectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun enter(digits: String) {
        for (digit in digits) composeRule.onNodeWithText(digit.toString()).performClick()
    }

    @Test
    fun disablingWithTheCorrectPinSubmitsIt() {
        var submittedPin: String? = null
        composeRule.setContent {
            SecuritySection(
                uiState = AppLockUiState(pinLockEnabled = true),
                events = emptyFlow(),
                onScreenAppeared = {},
                onEnablePinLockRequested = {},
                onDisableLockPinSubmitted = { submittedPin = it },
                onBiometricToggle = {},
                authenticateWithBiometric = { BiometricResult.Cancelled },
            )
        }

        composeRule.onNodeWithTag(SecuritySectionTestTags.PROTECT_WITH_PIN_SWITCH).performClick()
        composeRule.onNodeWithText("Enter your PIN to turn off the app lock").assertIsDisplayed()

        enter("1234")
        composeRule.onNodeWithText("Continue").performClick()

        assert(submittedPin == "1234") { "Expected 1234 but was $submittedPin" }
    }

    @Test
    fun wrongPinEventShowsTheErrorInsideTheDialog() {
        val events = MutableSharedFlow<AppLockEvent>(extraBufferCapacity = 1)
        composeRule.setContent {
            SecuritySection(
                uiState = AppLockUiState(pinLockEnabled = true),
                events = events,
                onScreenAppeared = {},
                onEnablePinLockRequested = {},
                onDisableLockPinSubmitted = {},
                onBiometricToggle = {},
                authenticateWithBiometric = { BiometricResult.Cancelled },
            )
        }

        composeRule.onNodeWithTag(SecuritySectionTestTags.PROTECT_WITH_PIN_SWITCH).performClick()
        // The dialog collects `events` from a LaunchedEffect. A shared flow with no replay drops
        // anything emitted before that collector attaches, so wait for the subscription first.
        composeRule.waitUntil(timeoutMillis = 5_000) { events.subscriptionCount.value > 0 }
        composeRule.runOnUiThread { events.tryEmit(AppLockEvent.WrongPin) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That PIN doesn't match. Try again.").assertIsDisplayed()
    }

    @Test
    fun biometricSwitchIsDisabledWhilePinLockIsOff() {
        composeRule.setContent {
            SecuritySection(
                uiState = AppLockUiState(pinLockEnabled = false, biometricStatus = BiometricStatus.Available),
                events = emptyFlow(),
                onScreenAppeared = {},
                onEnablePinLockRequested = {},
                onDisableLockPinSubmitted = {},
                onBiometricToggle = {},
                authenticateWithBiometric = { BiometricResult.Cancelled },
            )
        }

        composeRule.onNodeWithText("Turn on Protect with PIN to use this.").assertIsDisplayed()
        composeRule.onNodeWithTag(SecuritySectionTestTags.BIOMETRIC_SWITCH).assertIsNotEnabled()
    }
}
