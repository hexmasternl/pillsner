package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.LegalDocument
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.domain.legal.LegalSection
import nl.hexmaster.pillsner.domain.legal.TextRef
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-legal "The acceptance screen", "Legal screens are accessible". */
@RunWith(AndroidJUnit4::class)
class AcceptLegalScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var accepted = 0
    private var termsOpened = 0
    private var backs = 0
    private val scrollState = ScrollState(initial = 0)

    private fun setScreen(disclaimer: LegalDocument = CurrentLegalDocuments.disclaimer) {
        composeRule.setContent {
            PillsnerTheme {
                AcceptLegalScreen(
                    disclaimer = disclaimer,
                    onBack = { backs++ },
                    onReadTerms = { termsOpened++ },
                    onAccept = { accepted++ },
                    scrollState = scrollState,
                )
            }
        }
    }

    @Test
    fun theDisclaimerIsShownInFull() {
        setScreen()

        composeRule.onNodeWithTag(AcceptLegalTestTags.BODY).assertIsDisplayed()
        composeRule.onNodeWithText("Pillsner is a reminder, not a doctor").assertIsDisplayed()
    }

    @Test
    fun beforeScrolling_acceptIsDisabledAndSaysWhy() {
        setScreen()

        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertIsNotEnabled()
        composeRule.onNodeWithTag(AcceptLegalTestTags.SCROLL_HINT)
            .assertTextContains("Read the disclaimer to the end to continue")
    }

    @Test
    fun afterScrollingToTheEnd_acceptIsEnabledAndTheHintIsGone() {
        setScreen()

        scrollToEnd()

        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertIsEnabled()
        composeRule.onNodeWithTag(AcceptLegalTestTags.SCROLL_HINT)
            .assertTextContains("You can now accept")
    }

    @Test
    fun aDisclaimerThatFitsEnablesAcceptAtOnce() {
        setScreen(disclaimer = shortDocument())

        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertIsEnabled()
    }

    @Test
    fun acceptingReportsItOnce() {
        setScreen()
        scrollToEnd()

        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).performClick()

        assertEquals(1, accepted)
    }

    @Test
    fun theTermsAreOneTapAway() {
        setScreen()

        composeRule.onNodeWithTag(AcceptLegalTestTags.TERMS_LINK).performScrollTo().performClick()

        assertEquals(1, termsOpened)
    }

    @Test
    fun theBackArrowIsHowTheUserDeclines() {
        setScreen()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
        assertEquals("Leaving records nothing", 0, accepted)
    }

    @Test
    fun thereIsNoDeclineActionBesideAccept() {
        setScreen()

        // The back arrow is the decline; a second button meaning "do not add a medicine" would
        // only undo the tap that opened this screen.
        composeRule.onNodeWithText("Decline").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    /** The last pixel, not merely the last thing on screen: that is what enables the action. */
    private fun scrollToEnd() {
        composeRule.runOnIdle { runBlocking { scrollState.scrollTo(scrollState.maxValue) } }
        composeRule.waitForIdle()
    }

    /** A one-line document: short enough that the screen never scrolls. */
    private fun shortDocument() = LegalDocument(
        id = LegalDocumentId.DISCLAIMER,
        title = TextRef(R.string.legal_disclaimer_title),
        version = 1,
        effectiveDate = CurrentLegalDocuments.disclaimer.effectiveDate,
        sections = listOf(
            LegalSection(
                heading = TextRef(R.string.legal_disclaimer_s1_heading),
                paragraphs = listOf(TextRef(R.string.legal_status_not_accepted)),
            ),
        ),
    )
}
