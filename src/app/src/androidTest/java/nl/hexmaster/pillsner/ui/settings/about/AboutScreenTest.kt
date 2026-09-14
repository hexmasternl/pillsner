package nl.hexmaster.pillsner.ui.settings.about

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the About screen shows and how it reads (app-about-screen tasks 7.2 and 7.3, spec
 * "About screen content" and "About screen is readable and accessible").
 */
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Set from inside the composition so a test can raise the font scale and recompose. */
    private lateinit var fontScale: MutableState<Float>

    private val contentTags = listOf(
        AboutScreenTestTags.NAME_ORIGIN,
        AboutScreenTestTags.VERSION,
        AboutScreenTestTags.APPLICATION_ID,
        AboutScreenTestTags.AUTHOR,
    )

    private fun setScreen() {
        composeRule.setContent {
            fontScale = remember { mutableStateOf(1f) }
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale.value),
            ) {
                AboutScreen(appInfo = PreviewAppInfo, onBack = {})
            }
        }
    }

    private fun scrollTo(tag: String): SemanticsNodeInteraction {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
        return composeRule.onNodeWithTag(tag)
    }

    /** Every string the node and its children carry, in the order the semantics tree reports. */
    private fun SemanticsNodeInteraction.texts(): List<String> = buildList {
        fun collect(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { add(it.text) }
            node.children.forEach(::collect)
        }
        collect(fetchSemanticsNode())
    }

    private fun heightOf(tag: String): Int = scrollTo(tag).fetchSemanticsNode().size.height

    @Test
    fun showsTheProductMarkAndTheName() {
        setScreen()

        composeRule.onNodeWithTag(AboutScreenTestTags.TITLE).assertIsDisplayed()
        // The mark identifies the block, so it carries the app name as its content description.
        composeRule.onNodeWithContentDescription("Pillsner").assertIsDisplayed()
        // And the wordmark beneath it is a separate node reading the same name.
        composeRule.onNodeWithText("Pillsner").assertIsDisplayed()
    }

    @Test
    fun explainsWhereTheNameComesFrom() {
        setScreen()

        val paragraph = scrollTo(AboutScreenTestTags.NAME_ORIGIN).texts().single()

        assertTrue("Expected the paragraph to name Pills, was: $paragraph", paragraph.contains("Pills"))
        assertTrue("Expected the paragraph to name Partner, was: $paragraph", paragraph.contains("Partner"))
    }

    @Test
    fun showsTheVersionTheApplicationIdAndTheAuthor() {
        setScreen()

        assertEquals(listOf("Version", "0.1.0 (1)"), scrollTo(AboutScreenTestTags.VERSION).texts())
        assertEquals(
            listOf("Application id", "nl.hexmaster.pillsner"),
            scrollTo(AboutScreenTestTags.APPLICATION_ID).texts(),
        )
        assertEquals(listOf("Author", "Eduard Keilholz"), scrollTo(AboutScreenTestTags.AUTHOR).texts())
    }

    @Test
    fun factRowsAnnounceTheirLabelBeforeTheirValue() {
        setScreen()

        // A ListItem reads its headline before its supporting content, so label-then-value is the
        // order a screen reader gets (design D7).
        assertEquals(listOf("Author", "Eduard Keilholz"), scrollTo(AboutScreenTestTags.AUTHOR).texts())
    }

    @Test
    fun nothingInTheContentIsClickable() {
        setScreen()

        // The back affordance is the only control; no fact row navigates anywhere (spec "Nothing
        // on the screen navigates away").
        for (tag in contentTags) scrollTo(tag).assertHasNoClickAction()
    }

    @Test
    fun everyPieceOfContentIsReachableAtDoubleFontScale() {
        setScreen()

        composeRule.runOnUiThread { fontScale.value = 2f }
        composeRule.waitForIdle()

        for (tag in contentTags) scrollTo(tag).assertIsDisplayed()
    }

    @Test
    fun contentGrowsWithTheFontScaleRatherThanBeingClamped() {
        setScreen()

        val atNormalScale = contentTags.associateWith(::heightOf)

        composeRule.runOnUiThread { fontScale.value = 2f }
        composeRule.waitForIdle()

        // Nothing here sets maxLines, so every piece of text must get taller when the font does.
        // A row that stayed the same height would be one clamped to a fixed number of lines, which
        // is what truncation looks like from the outside.
        val notGrown = contentTags.filter { heightOf(it) <= atNormalScale.getValue(it) }

        assertEquals("Content that did not grow at double font scale", emptyList<String>(), notGrown)
    }

    @Test
    fun everyValueIsReportedInFullAtDoubleFontScale() {
        setScreen()

        composeRule.runOnUiThread { fontScale.value = 2f }
        composeRule.waitForIdle()

        assertEquals(listOf("Version", "0.1.0 (1)"), scrollTo(AboutScreenTestTags.VERSION).texts())
        assertEquals(
            listOf("Application id", "nl.hexmaster.pillsner"),
            scrollTo(AboutScreenTestTags.APPLICATION_ID).texts(),
        )
        assertEquals(listOf("Author", "Eduard Keilholz"), scrollTo(AboutScreenTestTags.AUTHOR).texts())
    }
}
