package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
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

    @Test
    fun theMedicineBehindTheDoseIsShown() {
        setScreen(entry)

        composeRule.onNodeWithTag(DoseDetailsTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
        composeRule.onNodeWithText("400 mg").assertIsDisplayed()
        composeRule.onNodeWithText("600 mg").assertIsDisplayed()
        composeRule.onNodeWithText("400 mg twice a day").assertIsDisplayed()
        composeRule.onNodeWithText("24 tablets").assertIsDisplayed()
    }

    @Test
    fun aMedicineWithoutStockHasNoStockLine() {
        setScreen(entry.copy(stockText = null))

        composeRule.onAllNodesWithTag(DoseDetailsTestTags.STOCK).assertCountEquals(0)
        composeRule.onNodeWithTag(DoseDetailsTestTags.SCHEDULE).assertIsDisplayed()
    }

    @Test
    fun aDoseWithoutDetailsSaysSoRatherThanShowingNothing() {
        setScreen(entry.copy(defaultDoseText = null, scheduleLines = emptyList(), stockText = null))

        composeRule.onNodeWithTag(DoseDetailsTestTags.UNAVAILABLE).assertIsDisplayed()
        composeRule.onNodeWithTag(DoseDetailsTestTags.AMOUNT).assertIsDisplayed()
    }

    @Test
    fun aDoseTomorrowSaysWhichDayItIsOn() {
        setScreen(entry.copy(isTomorrow = true))

        composeRule.onNodeWithText("Tomorrow", substring = true).assertIsDisplayed()
    }

    @Test
    fun theDetailsAreReadInThePhoneAppsLanguage() {
        setScreen(entry, Locale.forLanguageTag("nl-NL"))

        composeRule.onNodeWithText("Standaarddosis").assertIsDisplayed()
        composeRule.onNodeWithText("Op voorraad").assertIsDisplayed()
    }
}
