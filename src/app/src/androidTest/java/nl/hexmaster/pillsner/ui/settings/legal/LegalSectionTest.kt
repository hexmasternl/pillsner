package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-legal "The documents are readable from Settings", "Settings shows the state of acceptance". */
@RunWith(AndroidJUnit4::class)
class LegalSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val opened = mutableListOf<LegalDocumentId>()

    private fun setSection(state: LegalAcceptanceState) {
        composeRule.setContent {
            PillsnerTheme {
                LegalSection(state = state, onOpenDocument = { opened += it })
            }
        }
    }

    @Test
    fun bothDocumentsAreOffered() {
        setSection(LegalAcceptanceState.NeverAccepted)

        composeRule.onNodeWithTag(LegalSectionTestTags.DISCLAIMER_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(LegalSectionTestTags.TERMS_ROW).assertIsDisplayed()
    }

    @Test
    fun eachRowAnnouncesItselfAsAButton() {
        setSection(LegalAcceptanceState.NeverAccepted)

        listOf(LegalSectionTestTags.DISCLAIMER_ROW, LegalSectionTestTags.TERMS_ROW).forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        }
    }

    @Test
    fun eachRowOpensItsOwnDocument() {
        setSection(LegalAcceptanceState.NeverAccepted)

        composeRule.onNodeWithTag(LegalSectionTestTags.TERMS_ROW).performClick()
        composeRule.onNodeWithTag(LegalSectionTestTags.DISCLAIMER_ROW).performClick()

        assertEquals(listOf(LegalDocumentId.TERMS, LegalDocumentId.DISCLAIMER), opened)
    }

    @Test
    fun neverAccepted_saysSo() {
        setSection(LegalAcceptanceState.NeverAccepted)

        composeRule.onNodeWithTag(LegalSectionTestTags.STATUS)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Not yet accepted").assertIsDisplayed()
    }

    // The day itself is written by the platform in the device's own format, so these assert the
    // sentence that was chosen rather than how the date came out.

    @Test
    fun accepted_namesTheDay() {
        setSection(LegalAcceptanceState.Accepted(ACCEPTED_ON))

        composeRule.onNodeWithTag(LegalSectionTestTags.STATUS)
            .assertTextContains("Accepted on", substring = true)
            .assertTextContains("2026", substring = true)
    }

    @Test
    fun revisedSince_namesTheDayAndSaysTheDocumentsChanged() {
        setSection(LegalAcceptanceState.RevisedSince(ACCEPTED_ON))

        composeRule.onNodeWithTag(LegalSectionTestTags.STATUS)
            .assertTextContains("Accepted on", substring = true)
            .assertTextContains("these documents have changed since", substring = true)
    }

    @Test
    fun thereIsNoWayToAcceptOrWithdrawHere() {
        setSection(LegalAcceptanceState.Accepted(ACCEPTED_ON))

        // Only the two document rows are actionable; the status line is text and nothing else.
        composeRule.onNodeWithTag(LegalSectionTestTags.STATUS).assertHasNoClickAction()
    }

    private companion object {
        val ACCEPTED_ON: LocalDate = LocalDate.of(2026, 9, 14)
    }
}
