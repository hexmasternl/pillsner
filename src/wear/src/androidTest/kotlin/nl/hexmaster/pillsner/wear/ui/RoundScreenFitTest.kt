package nl.hexmaster.pillsner.wear.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import kotlin.math.hypot
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
 * edge when its own line does. Only screens whose content fits without scrolling are checked: a
 * list item half under the bottom edge is content being scrolled, not content cut off.
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

    private fun setScreen(phoneConnected: Boolean, hasData: Boolean) {
        composeRule.setContent {
            PillsnerWearTheme {
                UpcomingDosesScreen(
                    WatchUiState(
                        sections = emptyList(),
                        phoneConnected = phoneConnected,
                        hasData = hasData,
                        locale = Locale.ENGLISH,
                    ),
                )
            }
        }
    }

    @Test
    fun theEmptyStateFitsTheRoundScreen() {
        setScreen(phoneConnected = true, hasData = true)

        assertEveryLineInsideTheCircle()
    }

    @Test
    fun aWatchThatHasNeverSyncedFitsTheRoundScreen() {
        setScreen(phoneConnected = false, hasData = false)

        assertEveryLineInsideTheCircle()
    }

    @Test
    fun aPhoneOutOfReachFitsTheRoundScreen() {
        setScreen(phoneConnected = false, hasData = true)

        assertEveryLineInsideTheCircle()
    }

    private fun assertEveryLineInsideTheCircle() {
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val center = screen.center
        val radius = minOf(screen.width, screen.height) / 2f

        val outside = textNodes().flatMap { node ->
            node.lineBoxes().filter { line -> !line.isInsideCircle(center, radius) }
                .map { line -> "\"${node.text()}\" line at $line" }
        }

        assertTrue(
            "Text crosses the edge of the round screen (radius $radius around $center):\n" +
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

    /** Each laid-out line of this text node, as a rectangle in root coordinates. */
    private fun SemanticsNode.lineBoxes(): List<Rect> {
        val results = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.firstOrNull() ?: return emptyList()
        val origin = positionInRoot
        return (0 until layout.lineCount).map { line ->
            Rect(
                left = origin.x + layout.getLineLeft(line),
                top = origin.y + layout.getLineTop(line),
                right = origin.x + layout.getLineRight(line),
                bottom = origin.y + layout.getLineBottom(line),
            )
        }
    }

    private fun Rect.isInsideCircle(center: Offset, radius: Float): Boolean =
        listOf(topLeft, topRight, bottomLeft, bottomRight).all { corner ->
            hypot(corner.x - center.x, corner.y - center.y) <= radius
        }
}
