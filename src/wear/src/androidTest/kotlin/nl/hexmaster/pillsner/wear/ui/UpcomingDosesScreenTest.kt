package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.domain.AgendaDay
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: the watch's agenda — today and tomorrow grouped by time, the empty state and the footer. */
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
        isTomorrow: Boolean = false,
    ) = WatchDoseEntry(
        doseId = id,
        name = name,
        amountText = amount,
        scheduledAt = morning.plusSeconds(minutes * 60),
        isOverdue = isOverdue,
        isTomorrow = isTomorrow,
    )

    private fun agenda(vararg days: Pair<AgendaDay, List<WatchDoseEntry>>) = days.map { (day, entries) ->
        WatchDaySection(
            day = day,
            groups = entries.groupBy { it.scheduledAt }.map { (at, doses) -> WatchTimeGroup(at, doses) },
        )
    }

    private fun state(
        sections: List<WatchDaySection> = emptyList(),
        phoneConnected: Boolean = true,
        hasData: Boolean = true,
    ) = WatchUiState(sections, phoneConnected, hasData, Locale.ENGLISH)

    private fun setScreen(uiState: WatchUiState, onDoseClick: (Long) -> Unit = {}) {
        composeRule.setContent { PillsnerWearTheme { UpcomingDosesScreen(uiState, onDoseClick) } }
    }

    @Test
    fun theAgendaShowsTodayAndTomorrow() {
        setScreen(
            state(
                agenda(
                    AgendaDay.TODAY to listOf(entry(1, "Ibuprofen"), entry(2, "Metformin", "500 mg", minutes = 120)),
                    AgendaDay.TOMORROW to listOf(
                        entry(3, "Simvastatin", minutes = 24 * 60, isTomorrow = true),
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Tomorrow").assertIsDisplayed()
        composeRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
        composeRule.onNodeWithText("Metformin").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UpcomingDosesTestTags.DAY_HEADER).assertCountEquals(2)
    }

    @Test
    fun dosesDueAtTheSameTimeShareOneTimeHeading() {
        setScreen(
            state(
                agenda(
                    AgendaDay.TODAY to listOf(
                        entry(1, "Ibuprofen"),
                        entry(2, "Metformin", "500 mg"),
                        entry(3, "Simvastatin", minutes = 120),
                    ),
                ),
            ),
        )

        composeRule.onAllNodesWithTag(UpcomingDosesTestTags.TIME_HEADER).assertCountEquals(2)
    }

    @Test
    fun anEntryReadsAsOneSentence() {
        setScreen(state(agenda(AgendaDay.TODAY to listOf(entry(1, "Ibuprofen", "400 mg")))))

        composeRule.onNodeWithContentDescription("Ibuprofen, 400 mg, at", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun aDoseAlreadyPastItsTimeSaysSo() {
        setScreen(
            state(agenda(AgendaDay.TODAY to listOf(entry(1, "Ibuprofen", minutes = -30, isOverdue = true)))),
        )

        composeRule.onNodeWithContentDescription("was due", substring = true).assertIsDisplayed()
    }

    @Test
    fun tappingADoseAsksForItsDetails() {
        val opened = mutableListOf<Long>()
        setScreen(
            state(agenda(AgendaDay.TODAY to listOf(entry(7, "Ibuprofen")))),
            onDoseClick = { opened += it },
        )

        composeRule.onNodeWithContentDescription("Ibuprofen", substring = true).performClick()

        assertEquals(listOf(7L), opened)
    }

    @Test
    fun nothingPlannedForEitherDayIsSaidPlainly() {
        setScreen(state())

        composeRule.onNodeWithTag(UpcomingDosesTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No medicines scheduled for today or tomorrow").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UpcomingDosesTestTags.FOOTER).assertCountEquals(0)
    }

    @Test
    fun aPhoneOutOfReachIsSaidInTheFooter() {
        setScreen(
            state(agenda(AgendaDay.TODAY to listOf(entry(1, "Ibuprofen"))), phoneConnected = false),
        )

        composeRule.onNodeWithTag(UpcomingDosesTestTags.FOOTER).assertIsDisplayed()
        composeRule.onNodeWithText("Phone not connected").assertIsDisplayed()
    }

    @Test
    fun aWatchThatHasNeverSyncedIsToldWhereToLook() {
        setScreen(state(phoneConnected = false, hasData = false))

        composeRule.onNodeWithText("Open Pillsner on your phone to sync").assertIsDisplayed()
    }

    @Test
    fun aDutchPayloadIsReadInDutch() {
        composeRule.setContent {
            PillsnerWearTheme {
                UpcomingDosesScreen(
                    WatchUiState(
                        sections = agenda(AgendaDay.TODAY to listOf(entry(1, "Paracetamol", "2 tabletten"))),
                        phoneConnected = true,
                        hasData = true,
                        locale = Locale.forLanguageTag("nl-NL"),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Vandaag").assertIsDisplayed()
        composeRule.onNodeWithText("2 tabletten").assertIsDisplayed()
    }
}
