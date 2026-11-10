package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.domain.AgendaDay
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
        composeRule.setContent { PillsnerWearTheme { UpcomingDosesScreen(uiState, APP_VERSION, onDoseClick) } }
    }

    private fun list() = composeRule.onNodeWithTag(UpcomingDosesTestTags.LIST)

    /**
     * Scrolls the agenda to [matcher] first: on a small round watch later entries start below the
     * fold, and "shown" means the user can scroll to them and read them (#104).
     */
    private fun assertShown(matcher: SemanticsMatcher) {
        list().performScrollToNode(matcher)
        composeRule.onNode(matcher).assertIsDisplayed()
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

        assertShown(hasText("Today"))
        assertShown(hasText("Ibuprofen"))
        assertShown(hasText("Metformin"))
        assertShown(hasText("Tomorrow"))
        assertShown(hasText("Simvastatin"))
        // Today, two times with one dose each; Tomorrow, one time with one dose: two day headings.
        // Then the mark and version, which end every agenda.
        assertItems(9, "two day headings, three time headings, three doses, then the mark and version")
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

        // Today, then one time heading over the two doses at that time, then one over the third:
        // six items, and the mark and version after them. A heading per dose would make eight.
        assertItems(7, "one day heading, two time headings, three doses, then the mark and version")
        assertShown(hasText("Simvastatin"))
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
                    APP_VERSION,
                )
            }
        }

        composeRule.onNodeWithText("Vandaag").assertIsDisplayed()
        composeRule.onNodeWithText("2 tabletten").assertIsDisplayed()
    }

    /**
     * Spec: "App mark and version on the agenda" — the agenda ends with the product mark and, under
     * it, "Version" and the installed version name, both centred, in every state of the screen.
     */
    @Test
    fun theAgendaEndsWithTheMarkAndTheVersion() {
        setScreen(state(agenda(AgendaDay.TODAY to listOf(entry(1, "Examplamol")))))

        // Today, one time, one dose, then the mark and version.
        assertItems(4, "a day heading, a time heading, a dose, then the mark and version")
        assertTheMarkAndVersionEndTheListBelow(hasClickAction(), "the last dose")
    }

    @Test
    fun theEmptyStateEndsWithTheMarkAndTheVersion() {
        setScreen(state())

        // The empty text, then the mark and version.
        assertItems(2, "the empty text, then the mark and version")
        assertTheMarkAndVersionEndTheListBelow(hasTestTag(UpcomingDosesTestTags.EMPTY), "the empty text")
    }

    @Test
    fun theMarkAndTheVersionComeAfterTheConnectionFooter() {
        setScreen(state(agenda(AgendaDay.TODAY to listOf(entry(1, "Examplamol"))), phoneConnected = false))

        // Today, one time, one dose, the footer, then the mark and version.
        assertItems(5, "a day heading, a time heading, a dose, the footer, then the mark and version")
        assertTheMarkAndVersionEndTheListBelow(hasTestTag(UpcomingDosesTestTags.FOOTER), "the footer")
    }

    @Test
    fun aWatchThatHasNeverSyncedEndsWithTheMarkAndTheVersionToo() {
        setScreen(state(phoneConnected = false, hasData = false))

        // The empty text, the footer, then the mark and version.
        assertItems(3, "the empty text, the footer, then the mark and version")
        assertTheMarkAndVersionEndTheListBelow(hasTestTag(UpcomingDosesTestTags.FOOTER), "the footer")
    }

    /** Spec: "Not an action" — tapping the mark or the version does nothing. */
    @Test
    fun theMarkAndTheVersionAreNotAnAction() {
        val opened = mutableListOf<Long>()
        setScreen(
            state(agenda(AgendaDay.TODAY to listOf(entry(7, "Examplamol")))),
            onDoseClick = { opened += it },
        )
        scrollToTheEnd()

        val notTappable = hasClickAction().not() and hasAnyAncestor(hasClickAction()).not()
        version().assert(notTappable).performClick()
        mark().assert(notTappable).performClick()

        assertEquals(emptyList<Long>(), opened)
    }

    /** Spec: "Screen reader" — the version text is announced once and the mark is skipped. */
    @Test
    fun aScreenReaderHearsTheVersionOnceAndNotTheMark() {
        setScreen(state())
        scrollToTheEnd()

        composeRule.onAllNodes(hasText(VERSION_LINE)).assertCountEquals(1)
        composeRule
            .onAllNodes(
                hasContentDescription("Version", substring = true, ignoreCase = true),
                useUnmergedTree = true,
            )
            .assertCountEquals(0)
        // The mark is decorative: the version line under it carries the meaning.
        val markSemantics = mark().fetchSemanticsNode().config
        assertFalse("The mark has a description", markSemantics.contains(SemanticsProperties.ContentDescription))
        assertFalse("The mark has text", markSemantics.contains(SemanticsProperties.Text))
    }

    /** Spec: "Details screen" — neither the mark nor the version is shown there. */
    @Test
    fun theDetailsShowNeitherTheMarkNorTheVersion() {
        val detailed = entry(1, "Examplamol", "2 tablets").copy(
            defaultDoseText = "2 tablets",
            scheduleLines = listOf("2 tablets twice a day"),
            stockText = "30 tablets",
        )
        composeRule.setContent { PillsnerWearTheme { DoseDetailsScreen(detailed, Locale.ENGLISH) } }
        val details = composeRule.onNodeWithTag(DoseDetailsTestTags.LIST)

        // Title, this dose, due, default dose, schedule, stock: six lines and no seventh.
        assertEquals(6, details.itemCount())
        // A lazy column composes only what is near the screen, so look at the top and at the end.
        listOf(0, details.itemCount() - 1).forEach { index ->
            details.performScrollToIndex(index)
            composeRule.onAllNodesWithTag(UpcomingDosesTestTags.MARK, useUnmergedTree = true).assertCountEquals(0)
            composeRule.onAllNodesWithTag(UpcomingDosesTestTags.VERSION, useUnmergedTree = true)
                .assertCountEquals(0)
            composeRule
                .onAllNodes(hasText("Version", substring = true, ignoreCase = true), useUnmergedTree = true)
                .assertCountEquals(0)
        }
    }

    /** The agenda holds exactly [expected] items; [what] names them, so a failure says what is missing. */
    private fun assertItems(expected: Int, what: String) {
        assertEquals("The agenda should hold $expected items: $what", expected, list().itemCount())
    }

    private fun scrollToTheEnd() {
        list().performScrollToIndex(list().itemCount() - 1)
    }

    private fun mark() = composeRule.onNodeWithTag(UpcomingDosesTestTags.MARK, useUnmergedTree = true)

    private fun version() = composeRule.onNodeWithTag(UpcomingDosesTestTags.VERSION, useUnmergedTree = true)

    /**
     * Scrolled to the end, the mark and the version line are on screen, the version reads as the
     * string resource with the version name passed in, the mark sits under [preceding] (the last
     * thing before the item, described by [what]) and above the version, and both are centred.
     */
    private fun assertTheMarkAndVersionEndTheListBelow(preceding: SemanticsMatcher, what: String) {
        scrollToTheEnd()
        version().assertIsDisplayed().assertTextEquals(VERSION_LINE)
        mark().assertIsDisplayed()

        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val markBounds = mark().fetchSemanticsNode().boundsInRoot
        val versionBounds = version().fetchSemanticsNode().boundsInRoot
        val before = composeRule.onAllNodes(preceding).fetchSemanticsNodes().map { it.boundsInRoot }
        assertTrue("Expected $what on the screen when scrolled to the end", before.isNotEmpty())

        assertTrue(
            "The mark at $markBounds is not below $what at $before",
            before.all { markBounds.top >= it.bottom - PIXEL },
        )
        assertTrue(
            "The version at $versionBounds is not under the mark at $markBounds",
            versionBounds.top >= markBounds.bottom - PIXEL,
        )
        assertEquals("The mark is not centred", screen.center.x, markBounds.center.x, 2 * PIXEL)
        assertEquals("The version is not centred", screen.center.x, versionBounds.center.x, 2 * PIXEL)
    }

    private companion object {
        /** A made-up version name; the screen shows whatever the build passes in. */
        const val APP_VERSION = "1.2.3"
        const val VERSION_LINE = "Version 1.2.3"

        /** Rounding room for bounds the scroll transformation has scaled. */
        const val PIXEL = 1f
    }

    /**
     * Spec: "Language follows the phone app" — the watch package must carry every supported
     * language, not only the sources (design D7). Lint reads the sources and cannot see a language
     * the build strips, so this reads the footer as the installed app resolves it.
     */
    @Test
    fun aPortuguesePhoneAppIsNotAnsweredInEnglish() {
        assertTheNeverSyncedFooterIsTranslated("pt")
    }

    @Test
    fun aGermanPhoneAppIsNotAnsweredInEnglish() {
        assertTheNeverSyncedFooterIsTranslated("de")
    }

    private fun assertTheNeverSyncedFooterIsTranslated(languageTag: String) {
        composeRule.setContent {
            PillsnerWearTheme {
                UpcomingDosesScreen(
                    WatchUiState(
                        sections = emptyList(),
                        phoneConnected = false,
                        hasData = false,
                        locale = Locale.forLanguageTag(languageTag),
                    ),
                    APP_VERSION,
                )
            }
        }

        list().performScrollToNode(hasTestTag(UpcomingDosesTestTags.FOOTER))
        val footer = composeRule.onNodeWithTag(UpcomingDosesTestTags.FOOTER).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(" ") { it.text }

        assertNotEquals(
            "The never-synced footer for \"$languageTag\" is the English text: " +
                "the watch package does not carry that language",
            "Open Pillsner on your phone to sync",
            footer,
        )
    }
}
