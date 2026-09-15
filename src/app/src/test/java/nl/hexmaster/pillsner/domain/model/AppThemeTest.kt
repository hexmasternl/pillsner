package nl.hexmaster.pillsner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: app-theme, "Theme options" — the resolution truth table and the stored-value rules. */
class AppThemeTest {

    @Test
    fun `system follows a light phone`() {
        assertFalse(AppTheme.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `system follows a dark phone`() {
        assertTrue(AppTheme.SYSTEM.isDark(systemInDarkTheme = true))
    }

    @Test
    fun `light stays light on a light phone`() {
        assertFalse(AppTheme.LIGHT.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `light stays light on a dark phone`() {
        assertFalse(AppTheme.LIGHT.isDark(systemInDarkTheme = true))
    }

    @Test
    fun `dark stays dark on a light phone`() {
        assertTrue(AppTheme.DARK.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `dark stays dark on a dark phone`() {
        assertTrue(AppTheme.DARK.isDark(systemInDarkTheme = true))
    }

    @Test
    fun `each theme is found by its own key`() {
        assertEquals(AppTheme.LIGHT, AppTheme.ofKey("light"))
        assertEquals(AppTheme.DARK, AppTheme.ofKey("dark"))
    }

    @Test
    fun `an absent value follows the phone`() {
        assertEquals(AppTheme.SYSTEM, AppTheme.ofKey(null))
    }

    @Test
    fun `an empty value follows the phone`() {
        assertEquals(AppTheme.SYSTEM, AppTheme.ofKey(""))
    }

    @Test
    fun `an unrecognised value follows the phone`() {
        assertEquals(AppTheme.SYSTEM, AppTheme.ofKey("amoled"))
    }

    @Test
    fun `the options are system light and dark in that order`() {
        assertEquals(listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK), AppTheme.entries)
    }
}
