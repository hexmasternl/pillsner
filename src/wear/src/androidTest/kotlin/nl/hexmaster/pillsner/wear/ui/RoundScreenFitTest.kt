package nl.hexmaster.pillsner.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.util.Locale
import kotlin.math.hypot
import nl.hexmaster.pillsner.wear.domain.AgendaDay
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: wearable-app, "Wear form factor and accessibility" — on a round watch no text is cut off by
 * the screen edge (Play's Wear app quality guideline on watch shapes, issue #107).
 *
 * Every line of text that is on screen must lie inside the circle of the display. The check uses the
 * laid-out lines, not the node bounds, because centred text in a full-width box only reaches the
 * edge when its own line does. Lines are mapped to the screen through each node's drawn bounds, so
 * the scroll transformation's scaling at the edges of a list is accounted for.
 *
 * What is checked: the empty and footer states, which fit without scrolling, whole; and a list
 * scrolled to the top, in its upper half only. A line in the lower half of a list is content still
 * to be scrolled to, not content cut off.
 */
@RunWith(AndroidJUnit4::class)
class RoundScreenFitTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun onlyOnARoundWatch() {
        val configuration = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
        assumeTrue("Round-screen check needs a round watch", configuration.isScreenRound)
    }

    private val morning = Instant.parse("2026-09-13T08:00:00Z")

    private fun entry(id: Long, name: String, minutes: Long) = WatchDoseEntry(
        doseId = id,
        name = name,
        amountText = "2 tablets",
        scheduledAt = morning.plusSeconds(minutes * 60),
        isOverdue = false,
        isTomorrow = false,
        defaultDoseText = "2 tablets",
        scheduleLines = listOf("2 tablets twice a day"),
        stockText = "30 tablets",
    )

    private val agenda = listOf(
        WatchDaySection(
            day = AgendaDay.TODAY,
            groups = listOf(
                WatchTimeGroup(morning, listOf(entry(1, "Examplamol", 0), entry(2, "Testafen", 0))),
                WatchTimeGroup(morning.plusSeconds(4 * 3600), listOf(entry(3, "Placebotin", 240))),
            ),
        ),
    )

    private fun setContent(fontScale: Float, content: @Composable () -> Unit) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                PillsnerWearTheme { content() }
            }
        }
    }

    private fun setAgenda(
        fontScale: Float,
        sections: List<WatchDaySection> = emptyList(),
        phoneConnected: Boolean = true,
        hasData: Boolean = true,
    ) = setContent(fontScale) {
        UpcomingDosesScreen(WatchUiState(sections, phoneConnected, hasData, Locale.ENGLISH))
    }

    @Test
    fun theEmptyStateFitsTheRoundScreen() {
        setAgenda(DEFAULT_FONT)
        assertLinesInsideTheCircle()
    }

    @Test
    fun theEmptyStateFitsTheRoundScreenAtTheLargestFont() {
        setAgenda(LARGEST_FONT)
        assertLinesInsideTheCircle()
    }

    @Test
    fun aWatchThatHasNeverSyncedFitsTheRoundScreen() {
        setAgenda(DEFAULT_FONT, phoneConnected = false, hasData = false)
        assertLinesInsideTheCircle()
    }

    @Test
    fun aWatchThatHasNeverSyncedFitsTheRoundScreenAtTheLargestFont() {
        setAgenda(LARGEST_FONT, phoneConnected = false, hasData = false)
        assertLinesInsideTheCircle()
    }

    @Test
    fun aPhoneOutOfReachFitsTheRoundScreen() {
        setAgenda(DEFAULT_FONT, phoneConnected = false, hasData = true)
        assertLinesInsideTheCircle()
    }

    @Test
    fun aPhoneOutOfReachFitsTheRoundScreenAtTheLargestFont() {
        setAgenda(LARGEST_FONT, phoneConnected = false, hasData = true)
        assertLinesInsideTheCircle()
    }

    /** The tallest empty state, in every language the watch ships, longest words included. */
    @Test
    fun everyLanguageFitsTheRoundScreenAtTheLargestFont() {
        var locale by mutableStateOf(Locale.ENGLISH)
        setContent(LARGEST_FONT) {
            UpcomingDosesScreen(WatchUiState(emptyList(), phoneConnected = false, hasData = false, locale = locale))
        }

        listOf("en", "nl", "de", "fr", "es", "pt").forEach { tag ->
            locale = Locale.forLanguageTag(tag)
            val list = composeRule.onNodeWithTag(UpcomingDosesTestTags.LIST)
            list.performScrollToIndex(0)
            assertLinesInsideTheCircle(Region.UPPER_HALF, label = "$tag at the top")
            // Longer languages overflow at this size; their last lines are read scrolled to the end.
            list.performScrollToIndex(1)
            assertLinesInsideTheCircle(Region.LOWER_HALF, label = "$tag at the end")
        }
    }

    @Test
    fun theTopOfTheAgendaClearsTheBezel() {
        setAgenda(DEFAULT_FONT, sections = agenda)
        assertLinesInsideTheCircle(Region.UPPER_HALF)
    }

    @Test
    fun theTopOfTheAgendaClearsTheBezelAtTheLargestFont() {
        setAgenda(LARGEST_FONT, sections = agenda)
        assertLinesInsideTheCircle(Region.UPPER_HALF)
    }

    @Test
    fun theTopOfTheDetailsClearsTheBezel() {
        setContent(DEFAULT_FONT) { DoseDetailsScreen(entry(1, "Examplamol", 0), Locale.ENGLISH) }
        assertLinesInsideTheCircle(Region.UPPER_HALF)
    }

    @Test
    fun theTopOfTheDetailsClearsTheBezelAtTheLargestFont() {
        setContent(LARGEST_FONT) { DoseDetailsScreen(entry(1, "Examplamol", 0), Locale.ENGLISH) }
        assertLinesInsideTheCircle(Region.UPPER_HALF)
    }

    private fun assertLinesInsideTheCircle(region: Region = Region.WHOLE, label: String = "") {
        composeRule.waitForIdle()
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val center = screen.center
        val radius = minOf(screen.width, screen.height) / 2f

        val outside = textNodes().flatMap { node ->
            node.lineBoxes()
                .filter { line -> region.contains(line, center.y) }
                .filter { line -> !line.isInsideCircle(center, radius) }
                .map { line -> "\"${node.text()}\" line at $line" }
        }

        assertTrue(
            "Text crosses the edge of the round screen $label (radius $radius around $center):\n" +
                outside.joinToString("\n"),
            outside.isEmpty(),
        )
    }

    private fun textNodes(): List<SemanticsNode> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun SemanticsNode.text(): String =
        config.getOrNull(SemanticsProperties.Text)
            ?.joinToString { it.text }
            .orEmpty()

    /**
     * Each laid-out line of this text node, as a rectangle in root coordinates. The node's drawn
     * bounds give the offset and the scale its layers apply, so a line is placed where it is seen.
     */
    private fun SemanticsNode.lineBoxes(): List<Rect> {
        val results = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.firstOrNull() ?: return emptyList()
        val drawn = boundsInRoot
        if (drawn.isEmpty || size.width == 0 || size.height == 0) return emptyList()
        val scaleX = drawn.width / size.width
        val scaleY = drawn.height / size.height
        return (0 until layout.lineCount).map { line ->
            Rect(
                left = drawn.left + layout.getLineLeft(line) * scaleX,
                top = drawn.top + layout.getLineTop(line) * scaleY,
                right = drawn.left + layout.getLineRight(line) * scaleX,
                bottom = drawn.top + layout.getLineBottom(line) * scaleY,
            )
        }
    }

    private fun Rect.isInsideCircle(center: Offset, radius: Float): Boolean =
        listOf(topLeft, topRight, bottomLeft, bottomRight).all { corner ->
            hypot(corner.x - center.x, corner.y - center.y) <= radius
        }

    /** Which lines a check covers: all of them, or those wholly above or below the middle. */
    private enum class Region {
        WHOLE,
        UPPER_HALF,
        LOWER_HALF;

        fun contains(line: Rect, middle: Float): Boolean = when (this) {
            WHOLE -> true
            UPPER_HALF -> line.bottom <= middle
            LOWER_HALF -> line.top >= middle
        }
    }

    private companion object {
        const val DEFAULT_FONT = 1.0f

        /** The largest of Wear OS's font sizes. */
        const val LARGEST_FONT = 1.24f
    }
}
