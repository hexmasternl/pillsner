package nl.hexmaster.pillsner.ui.insets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The inset helper itself: it must catch content under a bar, and only that (design D3). */
@RunWith(AndroidJUnit4::class)
class EdgeToEdgeHostTest {

    @get:Rule
    val host = EdgeToEdgeHost(ComponentActivity::class.java)

    @Test
    fun aNodePaddedForSafeDrawingPasses() {
        host.show(InsetPreset.THREE_BUTTON) { BottomStrip(Modifier.safeDrawingPadding()) }

        host.composeRule.onNodeWithTag(STRIP).assertInsideSafeDrawingArea(host)
    }

    @Test
    fun anUnpaddedNodeAtTheBottomEdgeFails() {
        host.show(InsetPreset.THREE_BUTTON) { BottomStrip(Modifier) }

        assertThrows(AssertionError::class.java) {
            host.composeRule.onNodeWithTag(STRIP).assertInsideSafeDrawingArea(host)
        }
    }

    @Test
    fun theKeyboardCountsAsUnsafe() {
        host.show(InsetPreset.KEYBOARD_OPEN) { BottomStrip(Modifier.safeDrawingPadding()) }

        val strip = host.composeRule.onNodeWithTag(STRIP).assertInsideSafeDrawingArea(host)
        val keyboardTop = host.activity.window.decorView.height - 300.dp.value * host.density
        assertTrue(strip.unclippedBoundsInWindow().bottom <= keyboardTop + host.tolerancePx)
    }

    @Test
    fun theLandscapePresetRotatesAndReachesComposition() {
        host.show(InsetPreset.LANDSCAPE_CUTOUT) { BottomStrip(Modifier.safeDrawingPadding()) }

        val area = host.safeDrawingArea()
        assertTrue("the window should be wider than tall", area.width > area.height)
        host.composeRule.onNodeWithTag(STRIP).assertInsideSafeDrawingArea(host)
    }

    @Test
    fun theGesturePresetReachesComposition() {
        host.show(InsetPreset.GESTURE) { BottomStrip(Modifier.safeDrawingPadding()) }

        // safeDrawingArea() fails when what composition read differs from the preset.
        host.safeDrawingArea()
    }

    @Composable
    private fun BottomStrip(padding: Modifier) {
        Box(Modifier.fillMaxSize().then(padding)) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag(STRIP),
            )
        }
    }

    private companion object {
        const val STRIP = "inset_helper_strip"
    }
}
