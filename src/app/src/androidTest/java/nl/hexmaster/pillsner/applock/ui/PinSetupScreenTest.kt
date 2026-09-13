package nl.hexmaster.pillsner.applock.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose semantics tests for [PinSetupScreen] (task 6.9). */
@RunWith(AndroidJUnit4::class)
class PinSetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun enter(digits: String) {
        for (digit in digits) composeRule.onNodeWithText(digit.toString()).performClick()
    }

    @Test
    fun happyPath_matchingEntriesConfirmThePin() {
        var confirmedPin: String? = null
        composeRule.setContent {
            PinSetupScreen(onBack = {}, onPinConfirmed = { confirmedPin = it })
        }

        enter("1234")
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Confirm your PIN").assertIsDisplayed()

        enter("1234")
        composeRule.onNodeWithText("Continue").performClick()

        assert(confirmedPin == "1234") { "Expected 1234 but was $confirmedPin" }
    }

    @Test
    fun mismatchedConfirmationRestartsAtTheFirstStep() {
        var confirmedPin: String? = null
        composeRule.setContent {
            PinSetupScreen(onBack = {}, onPinConfirmed = { confirmedPin = it })
        }

        enter("1234")
        composeRule.onNodeWithText("Continue").performClick()
        enter("9999")
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.onNodeWithText("Those PINs didn't match. Choose a PIN and enter it again.").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a PIN").assertIsDisplayed()
        assert(confirmedPin == null)
    }

    @Test
    fun changingThePinIsTheSameTwoStepsWithNewWording() {
        var confirmedPin: String? = null
        composeRule.setContent {
            PinSetupScreen(
                onBack = {},
                onPinConfirmed = { confirmedPin = it },
                mode = PinSetupMode.CHANGE,
                isPinInUse = { it == "1234" },
            )
        }

        composeRule.onNodeWithText("Choose a new PIN").assertIsDisplayed()
        enter("5678")
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Confirm your new PIN").assertIsDisplayed()

        enter("5678")
        composeRule.onNodeWithText("Continue").performClick()

        assert(confirmedPin == "5678") { "Expected 5678 but was $confirmedPin" }
    }

    @Test
    fun thePinAlreadyInUseIsRefusedAtTheFirstStep() {
        var confirmedPin: String? = null
        composeRule.setContent {
            PinSetupScreen(
                onBack = {},
                onPinConfirmed = { confirmedPin = it },
                mode = PinSetupMode.CHANGE,
                isPinInUse = { it == "1234" },
            )
        }

        enter("1234")
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.onNodeWithText("Choose a PIN that differs from your current one.").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a new PIN").assertIsDisplayed()
        assert(confirmedPin == null)
    }

    @Test
    fun aMismatchWhileChangingLeavesTheCurrentPinAlone() {
        var confirmedPin: String? = null
        composeRule.setContent {
            PinSetupScreen(
                onBack = {},
                onPinConfirmed = { confirmedPin = it },
                mode = PinSetupMode.CHANGE,
                isPinInUse = { it == "1234" },
            )
        }

        enter("5678")
        composeRule.onNodeWithText("Continue").performClick()
        enter("5679")
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.onNodeWithText("Those PINs didn't match. Choose a PIN and enter it again.").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a new PIN").assertIsDisplayed()
        assert(confirmedPin == null)
    }

    @Test
    fun tooFewDigitsIsRejected() {
        composeRule.setContent {
            PinSetupScreen(onBack = {}, onPinConfirmed = {})
        }

        enter("12")

        // Submitting fewer than 4 digits is not accepted: the action itself stays disabled.
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }
}
