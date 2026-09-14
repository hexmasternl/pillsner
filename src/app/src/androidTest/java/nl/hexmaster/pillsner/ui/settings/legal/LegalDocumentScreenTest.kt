package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.LegalDocument
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-legal "The documents are readable from Settings", "Legal screens are accessible". */
@RunWith(AndroidJUnit4::class)
class LegalDocumentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(document: LegalDocument) {
        composeRule.setContent {
            PillsnerTheme {
                LegalDocumentScreen(document = document, onBack = {})
            }
        }
    }

    @Test
    fun theDisclaimerShowsItsTitleAndItsFirstSection() {
        setScreen(CurrentLegalDocuments.disclaimer)

        composeRule.onNodeWithTag(LegalDocumentTestTags.TITLE)
            .assertTextContains("Disclaimer")
        composeRule.onNodeWithText("Pillsner is a reminder, not a doctor").assertIsDisplayed()
    }

    @Test
    fun theTermsShowTheirOwnTitleAndFirstSection() {
        setScreen(CurrentLegalDocuments.terms)

        composeRule.onNodeWithTag(LegalDocumentTestTags.TITLE)
            .assertTextContains("Terms of Service")
        composeRule.onNodeWithText("Your licence to use Pillsner").assertIsDisplayed()
    }

    @Test
    fun theVersionAndTheDayItTookEffectAreShownOnce() {
        setScreen(CurrentLegalDocuments.terms)

        composeRule.onNodeWithTag(LegalDocumentTestTags.VERSION)
            .assertTextContains("Version 1", substring = true)
            .assertTextContains("2026", substring = true)
    }

    @Test
    fun everySectionHeadingIsAHeadingToAScreenReader() {
        setScreen(CurrentLegalDocuments.disclaimer)

        val headings = composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNodes()

        // The app bar title is not a heading node, so every heading here is a section of the
        // document; the first screenful of a five-section document shows at least two of them.
        assertTrue("Expected section headings, found ${headings.size}", headings.size >= 2)
    }
}
