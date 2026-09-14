package nl.hexmaster.pillsner.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The wordmark's contract (docs/design-system.md section 8.13): "Pills" in `secondary`, "ner" in
 * `primary`, in both schemes, as one node whose text is the whole word.
 */
@RunWith(AndroidJUnit4::class)
class PillsnerWordmarkTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightScheme_drawsPillsInSecondaryAndNerInPrimary() {
        assertWordmarkColours(darkTheme = false)
    }

    @Test
    fun darkScheme_drawsPillsInSecondaryAndNerInPrimary() {
        assertWordmarkColours(darkTheme = true)
    }

    @Test
    fun wordmarkIsASingleNodeReadingTheWholeName() {
        setWordmark(darkTheme = false)

        composeRule.onAllNodes(hasText("Pillsner")).assertCountEquals(1)
        composeRule.onAllNodes(hasText("Pills")).assertCountEquals(0)
        composeRule.onAllNodes(hasText("ner")).assertCountEquals(0)
    }

    private fun assertWordmarkColours(darkTheme: Boolean) {
        var secondary = Color.Unspecified
        var primary = Color.Unspecified
        setWordmark(darkTheme) {
            secondary = MaterialTheme.colorScheme.secondary
            primary = MaterialTheme.colorScheme.primary
        }

        val text = composeRule.onNodeWithText("Pillsner")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .single()

        assertEquals("Pillsner", text.text)
        val spans = text.spanStyles
        assertEquals(2, spans.size)

        val lead = spans[0]
        assertEquals("Pills", text.text.substring(lead.start, lead.end))
        assertEquals(secondary, lead.item.color)

        val accent = spans[1]
        assertEquals("ner", text.text.substring(accent.start, accent.end))
        assertEquals(primary, accent.item.color)
    }

    private fun setWordmark(darkTheme: Boolean, capture: @Composable () -> Unit = {}) {
        composeRule.setContent {
            PillsnerTheme(darkTheme = darkTheme) {
                capture()
                Surface { PillsnerWordmark() }
            }
        }
    }
}
