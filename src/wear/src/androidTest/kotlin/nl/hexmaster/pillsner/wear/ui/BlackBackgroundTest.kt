package nl.hexmaster.pillsner.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import nl.hexmaster.pillsner.wear.domain.AgendaDay
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: wearable-app, "Black watch background" (change `wear-black-background`, design D4). Google
 * Play rejects a Wear OS app whose screens are not on black (WO-V13).
 *
 * Each screen state is captured as the user sees it, and every pixel inside the display that isn't
 * covered by one of the elements the spec names must be `#000000`: dose cards, text, icons, the app
 * mark, the scroll indicator and the system time. Those areas come from the semantics bounds of
 * their nodes, padded by 1 dp for anti-aliasing, plus two bands along the edge for the elements
 * that have no semantics node: the scroll indicator on the right and the system time at the top.
 * The display is a circle on a round watch, inset a few pixels so the emulator's anti-aliased mask
 * doesn't count; on a square watch it is the whole screen, inset the same way.
 *
 * The screen is composed over an opaque magenta layer. A pixel the screen itself doesn't paint
 * shows magenta and fails, so the test can't pass on the host window happening to be black.
 */
@RunWith(AndroidJUnit4::class)
class BlackBackgroundTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val morning = Instant.parse("2026-09-13T08:00:00Z")

    private fun entry(id: Long, name: String, minutes: Long, isOverdue: Boolean, isTomorrow: Boolean = false) =
        WatchDoseEntry(
            doseId = id,
            name = name,
            amountText = "2 tablets",
            scheduledAt = morning.plusSeconds(minutes * 60),
            isOverdue = isOverdue,
            isTomorrow = isTomorrow,
            defaultDoseText = "2 tablets",
            scheduleLines = listOf("2 tablets twice a day"),
            stockText = "30 tablets",
        )

    private val overdue = entry(1, "Examplamol", 0, isOverdue = true)
    private val due = entry(2, "Testafen", 240, isOverdue = false)

    /** A due and an overdue dose today, and one tomorrow, so the list has an end to scroll to. */
    private val agenda = listOf(
        WatchDaySection(
            day = AgendaDay.TODAY,
            groups = listOf(
                WatchTimeGroup(overdue.scheduledAt, listOf(overdue)),
                WatchTimeGroup(due.scheduledAt, listOf(due)),
            ),
        ),
        WatchDaySection(
            day = AgendaDay.TOMORROW,
            groups = listOf(
                WatchTimeGroup(
                    morning.plusSeconds(DAY_SECONDS),
                    listOf(entry(3, "Placebotin", DAY_SECONDS / 60, isOverdue = false, isTomorrow = true)),
                ),
            ),
        ),
    )

    @Test
    fun theAgendaScrolledToTheTopIsOnBlack() {
        setAgenda(agenda)
        assertBlackOutsideTheElements("the agenda at the top")
    }

    @Test
    fun theAgendaScrolledToTheEndIsOnBlack() {
        setAgenda(agenda)
        scrollToTheEnd(UpcomingDosesTestTags.LIST)
        assertBlackOutsideTheElements("the agenda at the end")
    }

    @Test
    fun theEmptyStateIsOnBlack() {
        setAgenda(emptyList())
        assertBlackOutsideTheElements("the empty state")
    }

    @Test
    fun theEmptyStateWithTheConnectionFooterIsOnBlack() {
        setAgenda(emptyList(), phoneConnected = false, hasData = true)
        composeRule.onNodeWithTag(UpcomingDosesTestTags.FOOTER, useUnmergedTree = true).fetchSemanticsNode()
        assertBlackOutsideTheElements("the empty state with the footer")
    }

    @Test
    fun theDoseDetailsAreOnBlack() {
        setScreen { DoseDetailsScreen(due, Locale.ENGLISH) }
        assertBlackOutsideTheElements("the dose details")
    }

    /** Spec "Cards stand out on black": a due and an overdue card, each filled with a non-black colour. */
    @Test
    fun theDueAndOverdueCardsDifferFromBlack() {
        setAgenda(agenda)
        composeRule.waitForIdle()
        val image = capture()
        val cards = composeRule.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .filter { it.width > 0f && it.height > 0f }
            .sortedBy { it.top }
        assertTrue("Expected the overdue and the due card on screen, found ${cards.size}", cards.size >= 2)

        cards.take(2).forEachIndexed { index, card ->
            val fill = dominantColour(image, card)
            assertNotEquals("Card ${index + 1} at $card is filled with black", BLACK, fill)
            assertNotEquals("Card ${index + 1} at $card is not filled at all", SENTINEL_ARGB, fill)
        }
    }

    private fun setAgenda(
        sections: List<WatchDaySection>,
        phoneConnected: Boolean = true,
        hasData: Boolean = true,
    ) = setScreen {
        UpcomingDosesScreen(WatchUiState(sections, phoneConnected, hasData, Locale.ENGLISH), APP_VERSION)
    }

    private fun setScreen(screen: @Composable () -> Unit) {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(SENTINEL)) {
                PillsnerWearTheme { screen() }
            }
        }
    }

    private fun scrollToTheEnd(listTag: String) {
        val list = composeRule.onNodeWithTag(listTag)
        list.performScrollToIndex(list.itemCount() - 1)
        list.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, FAR) }
        composeRule.waitForIdle()
    }

    private fun capture(): Bitmap =
        composeRule.onRoot().captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)

    /**
     * Every pixel inside the display and outside the excluded elements is opaque `#000000`. The
     * check can't pass on nothing: most of the display must be left to check.
     */
    private fun assertBlackOutsideTheElements(state: String) {
        composeRule.waitForIdle()
        val image = capture()
        val density = composeRule.density.density
        val pad = 1 * density
        val excluded = excludedElements().map { it.inflate(pad) }

        val isRound = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.isScreenRound
        val cx = image.width / 2f
        val cy = image.height / 2f
        val radius = minOf(image.width, image.height) / 2f - EDGE_INSET_PX
        val scrollBand = SCROLL_INDICATOR_BAND.value * density
        val timeBand = TIME_TEXT_BAND.value * density

        var checked = 0
        var offending = 0
        var box: Rect? = null
        val samples = mutableListOf<String>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val px = x + 0.5f
                val py = y + 0.5f
                val fromCentre = hypot(px - cx, py - cy)
                val onDisplay = if (isRound) {
                    fromCentre <= radius
                } else {
                    x >= EDGE_INSET_PX && y >= EDGE_INSET_PX &&
                        x < image.width - EDGE_INSET_PX && y < image.height - EDGE_INSET_PX
                }
                if (!onDisplay) continue
                if (inScrollIndicatorBand(px, py, cx, cy, radius, scrollBand, isRound, image.width)) continue
                if (inTimeTextBand(px, py, cx, cy, radius, timeBand, isRound)) continue
                if (excluded.any { it.contains(Offset(px, py)) }) continue

                checked++
                val colour = image.getPixel(x, y)
                if (colour != BLACK) {
                    offending++
                    val pixel = Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f)
                    box = box?.let { Rect(minOf(it.left, pixel.left), minOf(it.top, pixel.top), maxOf(it.right, pixel.right), maxOf(it.bottom, pixel.bottom)) } ?: pixel
                    if (samples.size < MAX_SAMPLES) samples += "($x, $y) = ${hex(colour)}"
                }
            }
        }

        val displayArea = if (isRound) Math.PI * radius * radius else (image.width * image.height).toDouble()
        assertTrue(
            "Only $checked pixels of $state were left to check after excluding ${excluded.size} elements",
            checked >= displayArea * MIN_CHECKED_SHARE,
        )
        assertTrue(
            "$offending of $checked background pixels in $state are not #000000, within $box. " +
                "First: ${samples.joinToString()}. Excluded: ${excluded.joinToString()}",
            offending == 0,
        )
    }

    /** The scroll indicator sits along the display's right edge: an arc on a round watch, a strip on a square one. */
    private fun inScrollIndicatorBand(
        x: Float,
        y: Float,
        cx: Float,
        cy: Float,
        radius: Float,
        band: Float,
        isRound: Boolean,
        width: Int,
    ): Boolean = if (isRound) {
        x > cx && hypot(x - cx, y - cy) > radius - band
    } else {
        x > width - EDGE_INSET_PX - band
    }

    /**
     * The system time. `AppScaffold` draws it as curved text along the top edge with no semantics
     * node of its own, so its area is a band along the top of the display rather than node bounds:
     * [TIME_TEXT_BAND] deep, and centred, reaching [TIME_TEXT_HALF_WIDTH] of the radius either side.
     */
    private fun inTimeTextBand(
        x: Float,
        y: Float,
        cx: Float,
        cy: Float,
        radius: Float,
        band: Float,
        isRound: Boolean,
    ): Boolean {
        if (y >= cy || abs(x - cx) > radius * TIME_TEXT_HALF_WIDTH) return false
        return if (isRound) hypot(x - cx, y - cy) > radius - band else y < EDGE_INSET_PX + band
    }

    /** Cards, text, icons and the app mark, by their semantics bounds on the screen. */
    private fun excludedElements(): List<Rect> =
        composeRule.onAllNodes(isAnExcludedElement, useUnmergedTree = true).fetchSemanticsNodes()
            .map(SemanticsNode::boundsInRoot)
            .filter { it.width > 0f && it.height > 0f }

    private val isAnExcludedElement = SemanticsMatcher("is a card, text, icon, the mark or the time") { node ->
        val config = node.config
        config.getOrNull(SemanticsActions.OnClick) != null ||
            config.getOrNull(SemanticsProperties.Text) != null ||
            config.getOrNull(SemanticsProperties.ContentDescription) != null ||
            config.getOrNull(SemanticsActions.GetTextLayoutResult) != null ||
            config.getOrNull(SemanticsProperties.TestTag) == UpcomingDosesTestTags.MARK
    }

    /** The commonest colour inside [area], which for a card is its container fill. */
    private fun dominantColour(image: Bitmap, area: Rect): Int {
        val counts = HashMap<Int, Int>()
        for (y in area.top.toInt() until area.bottom.toInt().coerceAtMost(image.height)) {
            for (x in area.left.toInt() until area.right.toInt().coerceAtMost(image.width)) {
                counts.merge(image.getPixel(x, y), 1, Int::plus)
            }
        }
        return counts.maxBy { it.value }.key
    }

    private fun hex(colour: Int) = "#%08X".format(colour)

    private companion object {
        /** Opaque black as a bitmap pixel, ARGB. */
        const val BLACK = 0xFF000000.toInt()

        /** Behind the screen: a colour no Pillsner screen uses, so an unpainted pixel stands out. */
        val SENTINEL = Color.Magenta
        const val SENTINEL_ARGB = 0xFFFF00FF.toInt()

        /** Off the display's edge, where the emulator's round mask is anti-aliased. */
        const val EDGE_INSET_PX = 3f

        /** The scroll indicator and its gap to the edge, measured inwards from the inset edge. */
        val SCROLL_INDICATOR_BAND = 12.dp

        /** The system time's depth from the top edge; measured at 3.5 to 15.5 dp on a small round watch. */
        val TIME_TEXT_BAND = 20.dp

        /** How far either side of the centre the time may reach, as a share of the radius. */
        const val TIME_TEXT_HALF_WIDTH = 0.6f

        /** At least this share of the display must be left to check, so the test can't pass on nothing. */
        const val MIN_CHECKED_SHARE = 0.25

        const val MAX_SAMPLES = 8
        const val FAR = 10_000f
        const val DAY_SECONDS = 24L * 3600
        const val APP_VERSION = "1.12.3"
    }
}
