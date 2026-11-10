package nl.hexmaster.pillsner.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
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
 * to be scrolled to, not content cut off. The end of a list is checked scrolled to its last item
 * (issue #116): the outline of the last dose card, the last line of the details, and the product
 * mark and version line that end the agenda.
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

    /** More doses than a small round screen shows at once, so the last one is reached by scrolling. */
    private val longAgenda = listOf(
        WatchDaySection(
            day = AgendaDay.TODAY,
            groups = (0L until 5L).map { hour ->
                WatchTimeGroup(
                    morning.plusSeconds(hour * 3600),
                    listOf(entry(10 + hour, "Examplamol", hour * 60), entry(20 + hour, "Testafen", hour * 60)),
                )
            },
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
        locale: Locale = Locale.ENGLISH,
    ) = setContent(fontScale) {
        UpcomingDosesScreen(WatchUiState(sections, phoneConnected, hasData, locale), APP_VERSION)
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
        assertTheEmptyStateFitsAtRestAndTheMarkAndVersionAtTheEnd()
    }

    @Test
    fun aPhoneOutOfReachFitsTheRoundScreen() {
        setAgenda(DEFAULT_FONT, phoneConnected = false, hasData = true)
        assertLinesInsideTheCircle()
    }

    @Test
    fun aPhoneOutOfReachFitsTheRoundScreenAtTheLargestFont() {
        setAgenda(LARGEST_FONT, phoneConnected = false, hasData = true)
        assertTheEmptyStateFitsAtRestAndTheMarkAndVersionAtTheEnd()
    }

    /**
     * Design D8: at the largest font the empty text, the footer and the list's last item are
     * together taller than the screen. The empty text and the footer keep their place — every line
     * of them inside the circle at rest, with no scroll allowance — and the mark and version are
     * reached by scrolling, where they must fit in turn.
     */
    private fun assertTheEmptyStateFitsAtRestAndTheMarkAndVersionAtTheEnd() {
        assertLinesInsideTheCircle(label = "at rest", exceptTag = UpcomingDosesTestTags.VERSION)
        assertTheMarkAndVersionInsideTheCircle(label = "at the end")
    }

    /** The tallest empty state, in every language the watch ships, longest words included. */
    @Test
    fun everyLanguageFitsTheRoundScreenAtTheLargestFont() {
        var locale by mutableStateOf(Locale.ENGLISH)
        setContent(LARGEST_FONT) {
            UpcomingDosesScreen(
                WatchUiState(emptyList(), phoneConnected = false, hasData = false, locale = locale),
                APP_VERSION,
            )
        }

        listOf("en", "nl", "de", "fr", "es", "pt").forEach { tag ->
            locale = Locale.forLanguageTag(tag)
            val list = composeRule.onNodeWithTag(UpcomingDosesTestTags.LIST)
            list.performScrollToIndex(0)
            assertLinesInsideTheCircle(Region.UPPER_HALF, label = "$tag at the top")
            // Longer languages overflow at this size; their last lines are read scrolled to the end:
            // the column's last item, whatever its index is in this state (design D6).
            scrollToTheEnd()
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

    /**
     * Issue #116: scrolled to the end of a list longer than the screen, the last dose card lies
     * entirely inside the circle — its rounded container too, not only its text — so the round
     * screen never cuts off an entry ("the list scrolls to reveal every entry").
     */
    @Test
    fun theLastDoseOfALongAgendaClearsTheBezel() {
        setAgendaCapturingTheCardShape(DEFAULT_FONT)
        scrollToTheEnd()
        assertTheLastDoseCardInsideTheCircle()
    }

    @Test
    fun theLastDoseOfALongAgendaClearsTheBezelAtTheLargestFont() {
        setAgendaCapturingTheCardShape(LARGEST_FONT)
        scrollToTheEnd()
        assertTheLastDoseCardInsideTheCircle()
    }

    private var cardShape: Shape? = null

    private fun setAgendaCapturingTheCardShape(fontScale: Float) = setContent(fontScale) {
        // DoseCard draws its container in MaterialTheme.shapes.large.
        cardShape = MaterialTheme.shapes.large
        UpcomingDosesScreen(
            WatchUiState(longAgenda, phoneConnected = true, hasData = true, locale = Locale.ENGLISH),
            APP_VERSION,
        )
    }

    /**
     * Scrolls the column tagged [listTag] to its very end: to its last item, however many items it
     * holds, and then as far as the list still moves. Scrolling to an item alone brings a tall last
     * item to the middle of the screen, which is not yet the end of the list.
     */
    private fun scrollToTheEnd(listTag: String = UpcomingDosesTestTags.LIST) {
        val list = composeRule.onNodeWithTag(listTag)
        list.performScrollToIndex(list.itemCount() - 1)
        list.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, FAR) }
        composeRule.waitForIdle()
    }

    /**
     * The last dose card of the list must fit the circle, wherever it comes to rest (design D5).
     * Dose cards are the only things on the agenda that can be tapped, so once the list is scrolled
     * to its end the last one is the click-action node with the greatest bottom edge; an item under
     * it may leave it above the middle of the screen. A card's outline is a rounded rectangle; its
     * points farthest from the screen's centre lie on the corner arcs, so a corner fits when the
     * distance to its arc's centre plus the arc's radius is within the circle.
     */
    private fun assertTheLastDoseCardInsideTheCircle() {
        composeRule.waitForIdle()
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val center = screen.center
        val radius = minOf(screen.width, screen.height) / 2f
        val density = composeRule.density
        val shape = checkNotNull(cardShape)

        val lastCard = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .maxByOrNull { it.boundsInRoot.bottom }
        assertTrue("Expected a dose card on the screen when scrolled to the end", lastCard != null)
        val cards = listOfNotNull(lastCard)

        val outside = cards.mapNotNull { card ->
            val drawn = card.boundsInRoot
            val scale = drawn.width / card.size.width
            val outline = shape.createOutline(
                Size(card.size.width.toFloat(), card.size.height.toFloat()),
                LayoutDirection.Ltr,
                density,
            )
            val corner = (outline as? Outline.Rounded)?.roundRect?.bottomLeftCornerRadius?.x?.times(scale) ?: 0f
            val arcCentres = listOf(
                Offset(drawn.left + corner, drawn.top + corner),
                Offset(drawn.right - corner, drawn.top + corner),
                Offset(drawn.left + corner, drawn.bottom - corner),
                Offset(drawn.right - corner, drawn.bottom - corner),
            )
            val farthest = arcCentres.maxOf { hypot(it.x - center.x, it.y - center.y) + corner }
            if (farthest <= radius) null else "card at $drawn reaches ${farthest}px from the centre"
        }

        assertTrue(
            "A dose card crosses the edge of the round screen at the end of the list " +
                "(radius $radius around $center):\n" + outside.joinToString("\n"),
            outside.isEmpty(),
        )
    }

    /**
     * Spec: "Mark and version clear the bezel" — the agenda ends with the product mark and the
     * version line, and scrolled to the end both lie inside the circle, in each state, font size
     * and language of design D6's table.
     */
    @Test
    fun theMarkAndVersionOfALongAgendaClearTheBezel() {
        setAgenda(DEFAULT_FONT, sections = longAgenda)
        assertTheMarkAndVersionInsideTheCircle()
    }

    @Test
    fun theMarkAndVersionOfALongAgendaClearTheBezelAtTheLargestFont() {
        setAgenda(LARGEST_FONT, sections = longAgenda)
        assertTheMarkAndVersionInsideTheCircle()
    }

    @Test
    fun theMarkAndVersionUnderTheFooterOfALongAgendaClearTheBezelAtTheLargestFont() {
        setAgenda(LARGEST_FONT, sections = longAgenda, phoneConnected = false)
        assertTheMarkAndVersionInsideTheCircle()
    }

    @Test
    fun theMarkAndVersionOfTheEmptyStateClearTheBezel() {
        setAgenda(DEFAULT_FONT)
        assertTheMarkAndVersionInsideTheCircle()
    }

    @Test
    fun theMarkAndVersionOfTheEmptyStateClearTheBezelAtTheLargestFont() {
        setAgenda(LARGEST_FONT)
        assertTheMarkAndVersionInsideTheCircle()
    }

    @Test
    fun theMarkAndVersionOfAWatchThatHasNeverSyncedClearTheBezel() {
        setAgenda(DEFAULT_FONT, phoneConnected = false, hasData = false)
        assertTheMarkAndVersionInsideTheCircle()
    }

    /** The tallest state with the longest footer, so the one checked in every language. */
    @Test
    fun theMarkAndVersionClearTheBezelInEveryLanguageAtTheLargestFont() {
        var locale by mutableStateOf(Locale.ENGLISH)
        setContent(LARGEST_FONT) {
            UpcomingDosesScreen(
                WatchUiState(emptyList(), phoneConnected = false, hasData = false, locale = locale),
                APP_VERSION,
            )
        }

        listOf("en", "nl", "de", "fr", "es", "pt").forEach { tag ->
            locale = Locale.forLanguageTag(tag)
            assertTheMarkAndVersionInsideTheCircle(label = "in $tag")
        }
    }

    /**
     * Scrolls the agenda to its last item and checks the mark and the version line there. The mark
     * is checked by the four corners of its bounding box, which is stricter than its own round
     * shape; the version by every laid-out line, as the rest of the text is.
     */
    private fun assertTheMarkAndVersionInsideTheCircle(label: String = "") {
        composeRule.waitForIdle()
        scrollToTheEnd()
        composeRule.waitForIdle()
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val center = screen.center
        val radius = minOf(screen.width, screen.height) / 2f

        val marks = composeRule.onAllNodesWithTag(UpcomingDosesTestTags.MARK, useUnmergedTree = true)
            .fetchSemanticsNodes()
        val versions = composeRule.onAllNodesWithTag(UpcomingDosesTestTags.VERSION, useUnmergedTree = true)
            .fetchSemanticsNodes()
        val where = if (label.isEmpty()) "" else " $label"
        assertTrue(
            "The agenda does not end with the product mark$where: expected one node tagged " +
                "\"${UpcomingDosesTestTags.MARK}\" at the end of the list, found ${marks.size}",
            marks.size == 1,
        )
        assertTrue(
            "The agenda does not end with the version line$where: expected one node tagged " +
                "\"${UpcomingDosesTestTags.VERSION}\" at the end of the list, found ${versions.size}",
            versions.size == 1,
        )

        val mark = marks.single().drawnBox()
        val versionLines = versions.single().lineBoxes()
        assertTrue("The product mark has no size$where: $mark", mark.width > 0f && mark.height > 0f)
        assertTrue("The version line has no laid-out text$where", versionLines.isNotEmpty())

        assertTrue(
            "The product mark crosses the edge of the round screen at the end of the list$where " +
                "(radius $radius around $center): mark at $mark",
            mark.isInsideCircle(center, radius),
        )
        val outside = versionLines.filter { line -> !line.isInsideCircle(center, radius) }
        assertTrue(
            "The version crosses the edge of the round screen at the end of the list$where " +
                "(radius $radius around $center):\n" +
                outside.joinToString("\n") { line -> "\"${versions.single().text()}\" line at $line" },
            outside.isEmpty(),
        )
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

    /**
     * The end of the details screen (design D9): with details long enough to scroll at the largest
     * font, the last line on the screen lies inside the circle once the list is scrolled to its end.
     */
    @Test
    fun theEndOfTheDetailsClearsTheBezelAtTheLargestFont() {
        setContent(LARGEST_FONT) { DoseDetailsScreen(entryWithLongDetails, Locale.ENGLISH) }
        scrollToTheEnd(DoseDetailsTestTags.LIST)
        assertTheLastLineInsideTheCircle()
    }

    /** Made-up details, several schedule lines long, so the details screen has to scroll. */
    private val entryWithLongDetails = entry(1, "Examplamol", 0).copy(
        scheduleLines = listOf(
            "2 tablets every morning",
            "1 tablet every evening",
            "1 tablet on Sunday afternoon",
        ),
        stockText = "30 tablets",
    )

    /**
     * The lowest line of text on the screen, wherever it came to rest, must fit the circle. It is
     * picked by its bottom edge rather than by a region, so the check cannot pass on an empty set.
     */
    private fun assertTheLastLineInsideTheCircle() {
        composeRule.waitForIdle()
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val center = screen.center
        val radius = minOf(screen.width, screen.height) / 2f

        val lines = textNodes().flatMap { node -> node.lineBoxes().map { line -> node.text() to line } }
        assertTrue("Expected text on the screen when scrolled to the end", lines.isNotEmpty())
        val (text, line) = lines.maxBy { (_, box) -> box.bottom }

        assertTrue(
            "The last line crosses the edge of the round screen at the end of the list " +
                "(radius $radius around $center):\n\"$text\" line at $line",
            line.isInsideCircle(center, radius),
        )
    }

    /**
     * Every laid-out line in [region] lies inside the circle. [exceptTag] leaves out the one text
     * tagged with it, for a state where that text is checked scrolled to the end instead.
     */
    private fun assertLinesInsideTheCircle(
        region: Region = Region.WHOLE,
        label: String = "",
        exceptTag: String? = null,
    ) {
        composeRule.waitForIdle()
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val center = screen.center
        val radius = minOf(screen.width, screen.height) / 2f

        val checked = textNodes().filter { node ->
            exceptTag == null || node.config.getOrNull(SemanticsProperties.TestTag) != exceptTag
        }
        val outside = checked.flatMap { node ->
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

    /**
     * This node's whole box where it is drawn, in root coordinates: its laid-out size carried
     * through the transformations of its layers, and not cut down to the part that is visible. A
     * node half below the screen keeps its full box here, so it cannot pass by being clipped.
     */
    private fun SemanticsNode.drawnBox(): Rect {
        val coordinates = layoutInfo.coordinates
        val topLeft = coordinates.localToRoot(Offset.Zero)
        val bottomRight = coordinates.localToRoot(Offset(size.width.toFloat(), size.height.toFloat()))
        return Rect(topLeft, bottomRight)
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

        /** More pixels than any list here is long, to scroll as far as it goes. */
        const val FAR = 10_000f

        /** A made-up version name with a two-digit part, as wide as a version line plausibly gets. */
        const val APP_VERSION = "1.12.3"
    }
}
