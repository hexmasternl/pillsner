package nl.hexmaster.pillsner.wear.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The watch builds from black (`wearable-app` "Black watch background", design D1 and D3 of
 * `wear-black-background`). Google Play rejects a Wear OS app whose screens are not on a black
 * background (WO-V13), so the roles the scaffolds and lists paint behind content are pinned here,
 * and every text pair the watch draws is checked for WCAG AA against the scheme's own values.
 */
class WearThemeTest {

    private val scheme = PillsnerWearColorScheme

    @Test
    fun background_isBlack() {
        assertEquals(Color.Black, scheme.background)
    }

    @Test
    fun surfaceContainerLow_isBlack() {
        assertEquals(Color.Black, scheme.surfaceContainerLow)
    }

    @Test
    fun raisedSurfaces_stillDifferFromBlack() {
        assertNotEquals(Color.Black, scheme.surfaceContainer)
        assertNotEquals(Color.Black, scheme.surfaceContainerHigh)
    }

    @Test
    fun cardContainers_differFromBlack() {
        assertNotEquals(Color.Black, scheme.secondaryContainer)
        assertNotEquals(Color.Black, scheme.errorContainer)
    }

    @Test
    fun onSurface_onBackground_meetsAa() = assertAa("onSurface on background", scheme.onSurface, scheme.background)

    @Test
    fun onBackground_onBackground_meetsAa() =
        assertAa("onBackground on background", scheme.onBackground, scheme.background)

    @Test
    fun onSurfaceVariant_onBackground_meetsAa() =
        assertAa("onSurfaceVariant on background", scheme.onSurfaceVariant, scheme.background)

    @Test
    fun onSecondaryContainer_onSecondaryContainer_meetsAa() = assertAa(
        "onSecondaryContainer on secondaryContainer",
        scheme.onSecondaryContainer,
        scheme.secondaryContainer,
    )

    @Test
    fun onErrorContainer_onErrorContainer_meetsAa() = assertAa(
        "onErrorContainer on errorContainer",
        scheme.onErrorContainer,
        scheme.errorContainer,
    )

    @Test
    fun contrastRatio_matchesWcagReferenceValues() {
        // Guards the helper itself: white on black is 21 : 1, and a colour against itself is 1 : 1.
        assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
        assertEquals(1.0, contrastRatio(Color.Black, Color.Black), 0.0001)
    }

    private fun assertAa(pair: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$pair is ${"%.2f".format(ratio)} : 1, below $AA_TEXT : 1", ratio >= AA_TEXT)
    }

    /** WCAG 2.x contrast ratio, from the sRGB channels of each colour. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private companion object {
        const val AA_TEXT = 4.5
    }
}
