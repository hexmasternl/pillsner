package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: the watch's one screen — the six-hour list, the empty state and the footer. */
@RunWith(AndroidJUnit4::class)
class UpcomingDosesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val morning = Instant.parse("2026-09-13T08:00:00Z")

    private fun entry(
        id: Long,
        name: String,
        amount: String = "400 mg",
        minutes: Long = 60,
        isOverdue: Boolean = false,
    ) = WatchDoseEntry(
        doseId = id,
        name = name,
        amountText = amount,
        scheduledAt = morning.plusSeconds(minutes * 60),
        isOverdue = isOverdue,
        isTomorrow = false,
    )

    private fun setScreen(state: WatchUiState) {
        composeRule.setContent { PillsnerWearTheme { UpcomingDosesScreen(state) } }
    }

    @Test
    fun theListShowsWhatIsComingUp() {
        setScreen(
            WatchUiState(
                entries = listOf(
                    entry(1, "Ibuprofen"),
                    entry(2, "Metformin", "500 mg", minutes = 120),
                ),
                phoneConnected = true,
                hasData = true,
                locale = Locale.ENGLISH,
            ),
        )

        composeRule.onNodeWithTag(UpcomingDosesTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithText("Next 6 hours").assertIsDisplayed()
        composeRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
        composeRule.onNodeWithText("Metformin").assertIsDisplayed()
    }

    @Test
    fun anEntryReadsAsOneSentence() {
        setScreen(
            WatchUiState(
                entries = listOf(entry(1, "Ibuprofen", "400 mg")),
                phoneConnected = true,
                hasData = true,
                locale = Locale.ENGLISH,
            ),
        )

        composeRule.onNodeWithContentDescription("Ibuprofen, 400 mg, at", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun aDoseAlreadyPastItsTimeSaysSo() {
        setScreen(
            WatchUiState(
                entries = listOf(entry(1, "Ibuprofen", "400 mg", minutes = -30, isOverdue = true)),
                phoneConnected = true,
                hasData = true,
                locale = Locale.ENGLISH,
            ),
        )

        composeRule.onNodeWithContentDescription("was due", substring = true).assertIsDisplayed()
    }

    @Test
    fun nothingInTheNextSixHoursIsSaidPlainly() {
        setScreen(WatchUiState(phoneConnected = true, hasData = true, locale = Locale.ENGLISH))

        composeRule.onNodeWithTag(UpcomingDosesTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No medicines scheduled for the upcoming 6 hours").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UpcomingDosesTestTags.FOOTER).assertCountEquals(0)
    }

    @Test
    fun aPhoneOutOfReachIsSaidInTheFooter() {
        setScreen(
            WatchUiState(
                entries = listOf(entry(1, "Ibuprofen")),
                phoneConnected = false,
                hasData = true,
                locale = Locale.ENGLISH,
            ),
        )

        composeRule.onNodeWithTag(UpcomingDosesTestTags.FOOTER).assertIsDisplayed()
        composeRule.onNodeWithText("Phone not connected").assertIsDisplayed()
    }

    @Test
    fun aWatchThatHasNeverSyncedIsToldWhereToLook() {
        setScreen(WatchUiState(phoneConnected = false, hasData = false, locale = Locale.ENGLISH))

        composeRule.onNodeWithText("Open Pillsner on your phone to sync").assertIsDisplayed()
    }

    @Test
    fun aDutchPayloadIsReadInDutch() {
        setScreen(
            WatchUiState(
                entries = listOf(entry(1, "Paracetamol", "2 tabletten")),
                phoneConnected = true,
                hasData = true,
                locale = Locale.forLanguageTag("nl-NL"),
            ),
        )

        composeRule.onNodeWithText("Komende 6 uur").assertIsDisplayed()
        composeRule.onNodeWithText("2 tabletten").assertIsDisplayed()
    }
}
