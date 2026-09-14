package nl.hexmaster.pillsner.applock.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.SecurityAction
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityRequest
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose semantics tests for [SecuritySection] and the identity check it puts in front of every
 * change (app-login task 6.9, app-settings-security task 5.8).
 */
@RunWith(AndroidJUnit4::class)
class SecuritySectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var submittedPin: String? = null
    private var changePinTapped = 0
    private var lockDisableRequested = 0
    private var biometricDisableRequested = 0
    private var dismissed = 0

    private fun enter(digits: String) {
        for (digit in digits) composeRule.onNodeWithText(digit.toString()).performClick()
    }

    private fun setSection(
        uiState: AppLockUiState,
        events: Flow<AppLockEvent> = emptyFlow(),
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                SecuritySection(
                    uiState = uiState,
                    events = events,
                    onScreenAppeared = {},
                    onEnablePinLockRequested = {},
                    onLockDisableRequested = { lockDisableRequested++ },
                    onChangePinTapped = { changePinTapped++ },
                    onBiometricEnabled = {},
                    onBiometricDisableRequested = { biometricDisableRequested++ },
                    verifyCallbacks = VerifyIdentityCallbacks(
                        onBiometricResult = {},
                        onPinSubmitted = { submittedPin = it },
                        onUsePin = {},
                        onUseBiometrics = {},
                        onDismissed = { dismissed++ },
                    ),
                    authenticateWithBiometric = { BiometricResult.Cancelled },
                )
            }
        }
    }

    private fun verifying(
        purpose: SecurityAction,
        allowBiometric: Boolean,
        biometricEnabled: Boolean = false,
    ) = AppLockUiState(
        pinLockEnabled = true,
        biometricEnabled = biometricEnabled,
        biometricStatus = BiometricStatus.Available,
        verify = VerifyIdentityState.AwaitingPin(VerifyIdentityRequest(purpose, allowBiometric)),
    )

    @Test
    fun thereIsNoChangePinActionWhileTheLockIsOff() {
        setSection(AppLockUiState(pinLockEnabled = false))

        composeRule.onAllNodesWithTag(SecuritySectionTestTags.CHANGE_PIN_ROW).assertCountEquals(0)
    }

    @Test
    fun changePinSitsBetweenTheTwoSwitchesWhileTheLockIsOn() {
        setSection(AppLockUiState(pinLockEnabled = true))

        composeRule.onNodeWithTag(SecuritySectionTestTags.CHANGE_PIN_ROW)
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Change PIN").assertIsDisplayed()
    }

    @Test
    fun tappingChangePinAsksForTheIdentityCheck() {
        setSection(AppLockUiState(pinLockEnabled = true))

        composeRule.onNodeWithTag(SecuritySectionTestTags.CHANGE_PIN_ROW).performClick()

        assert(changePinTapped == 1) { "Expected one request but was $changePinTapped" }
    }

    @Test
    fun turningTheLockOffAsksForTheIdentityCheck() {
        setSection(AppLockUiState(pinLockEnabled = true))

        composeRule.onNodeWithTag(SecuritySectionTestTags.PROTECT_WITH_PIN_SWITCH).performClick()

        assert(lockDisableRequested == 1) { "Expected one request but was $lockDisableRequested" }
    }

    @Test
    fun turningBiometricsOffAsksForTheIdentityCheck() {
        setSection(
            AppLockUiState(
                pinLockEnabled = true,
                biometricEnabled = true,
                biometricStatus = BiometricStatus.Available,
            ),
        )

        composeRule.onNodeWithTag(SecuritySectionTestTags.BIOMETRIC_SWITCH).performClick()

        assert(biometricDisableRequested == 1) { "Expected one request but was $biometricDisableRequested" }
    }

    @Test
    fun turningTheLockOffOffersNoBiometricWayIn() {
        setSection(
            verifying(SecurityAction.DISABLE_LOCK, allowBiometric = false, biometricEnabled = true),
        )

        composeRule.onNodeWithText("Confirm it's you to turn off the app lock").assertIsDisplayed()
        composeRule.onAllNodesWithTag(VerifyIdentityDialogTestTags.USE_BIOMETRICS).assertCountEquals(0)
    }

    @Test
    fun theCurrentPinIsSubmittedFromTheCheck() {
        setSection(verifying(SecurityAction.DISABLE_LOCK, allowBiometric = false))

        enter("1234")
        composeRule.onNodeWithText("Continue").performClick()

        assert(submittedPin == "1234") { "Expected 1234 but was $submittedPin" }
    }

    @Test
    fun aWrongPinIsAnnouncedInsideTheCheck() {
        val events = MutableSharedFlow<AppLockEvent>(extraBufferCapacity = 1)
        setSection(verifying(SecurityAction.CHANGE_PIN, allowBiometric = false), events = events)

        // The dialog collects `events` from a LaunchedEffect. A shared flow with no replay drops
        // anything emitted before that collector attaches, so wait for the subscription first.
        composeRule.waitUntil(timeoutMillis = 5_000) { events.subscriptionCount.value > 0 }
        composeRule.runOnUiThread { events.tryEmit(AppLockEvent.WrongPin) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That PIN doesn't match. Try again.").assertIsDisplayed()
    }

    @Test
    fun aCoolingDownCheckRefusesEntryAndSaysHowLong() {
        setSection(
            verifying(SecurityAction.DISABLE_LOCK, allowBiometric = false)
                .copy(cooldownRemainingSeconds = 30),
        )

        composeRule.onNodeWithText("Too many wrong attempts. Try again in 30 seconds.").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun cancellingTheCheckReportsIt() {
        setSection(verifying(SecurityAction.DISABLE_BIOMETRICS, allowBiometric = true, biometricEnabled = true))

        composeRule.onNodeWithText("Cancel").performClick()

        assert(dismissed == 1) { "Expected one dismissal but was $dismissed" }
    }

    @Test
    fun theCheckStaysUsableAtDoubleFontScale() {
        setSection(verifying(SecurityAction.CHANGE_PIN, allowBiometric = false), fontScale = 2f)

        composeRule.onNodeWithText("Confirm it's you to change your PIN").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun biometricSwitchIsDisabledWhilePinLockIsOff() {
        setSection(AppLockUiState(pinLockEnabled = false, biometricStatus = BiometricStatus.Available))

        composeRule.onNodeWithText("Turn on Protect with PIN to use this.").assertIsDisplayed()
        composeRule.onNodeWithTag(SecuritySectionTestTags.BIOMETRIC_SWITCH).assertIsNotEnabled()
    }
}
