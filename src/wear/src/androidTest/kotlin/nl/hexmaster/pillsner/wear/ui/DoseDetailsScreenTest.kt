package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: the watch's read-only details of one dose. */
@RunWith(AndroidJUnit4::class)
class DoseDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val entry = WatchDoseEntry(
        doseId = 1,
        name = "Ibuprofen",
        amountText = "400 mg",
        scheduledAt = Instant.parse("2026-09-13T08:00:00Z"),
        isOverdue = false,
        isTomorrow = false,
        defaultDoseText = "600 mg",
        scheduleLines = listOf("400 mg twice a day"),
        stockText = "24 tablets",
    )

    private fun setScreen(entry: WatchDoseEntry, locale: Locale = Locale.ENGLISH) {
        composeRule.setContent { PillsnerWearTheme { DoseDetailsScreen(entry, locale) } }
    }

    /**
     * Scrolls the details to [matcher] first: on a small round watch the lower lines start below the
     * fold, and "shown" means the user can scroll to them and read them (#104).
     */
    private fun assertShown(matcher: SemanticsMatcher) {
        composeRule.onNodeWithTag(DoseDetailsTestTags.LIST).performScrollToNode(matcher)
        composeRule.onNode(matcher).assertIsDisplayed()
    }

    @Test
    fun theMedicineBehindTheDoseIsShown() {
        setScreen(entry)

        assertShown(hasTestTag(DoseDetailsTestTags.TITLE))
        assertShown(hasText("Ibuprofen"))
        assertShown(hasText("400 mg"))
        assertShown(hasText("600 mg"))
        assertShown(hasText("400 mg twice a day"))
        assertShown(hasText("24 tablets"))
    }

    @Test
    fun aMedicineWithoutStockHasNoStockLine() {
        setScreen(entry.copy(stockText = null))

        assertShown(hasTestTag(DoseDetailsTestTags.SCHEDULE))
        // Title, this dose, due, default dose, schedule: five lines, no sixth for stock.
        assertEquals(5, composeRule.onNodeWithTag(DoseDetailsTestTags.LIST).itemCount())
        composeRule.onAllNodesWithTag(DoseDetailsTestTags.STOCK).assertCountEquals(0)
    }

    @Test
    fun aDoseWithoutDetailsSaysSoRatherThanShowingNothing() {
        setScreen(entry.copy(defaultDoseText = null, scheduleLines = emptyList(), stockText = null))

        assertShown(hasTestTag(DoseDetailsTestTags.UNAVAILABLE))
        assertShown(hasTestTag(DoseDetailsTestTags.AMOUNT))
    }

    @Test
    fun aDoseTomorrowSaysWhichDayItIsOn() {
        setScreen(entry.copy(isTomorrow = true))

        assertShown(hasText("Tomorrow", substring = true))
    }

    @Test
    fun theDetailsAreReadInThePhoneAppsLanguage() {
        setScreen(entry, Locale.forLanguageTag("nl-NL"))

        assertShown(hasText("Standaarddosis"))
        assertShown(hasText("Op voorraad"))
    }
}
