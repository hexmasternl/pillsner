package nl.hexmaster.pillsner.domain.locale

import nl.hexmaster.pillsner.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: app-language startup resolution. */
class LocaleResolverTest {

    @Test
    fun `a stored language beats the phone`() {
        val resolved = LocaleResolver.resolve(AppLanguage.DUTCH, listOf("en-GB"))

        assertEquals(AppLanguage.DUTCH, resolved)
    }

    @Test
    fun `a stored language is kept even when the phone speaks it too`() {
        val resolved = LocaleResolver.resolve(AppLanguage.ENGLISH, listOf("en-GB", "nl-NL"))

        assertEquals(AppLanguage.ENGLISH, resolved)
    }

    @Test
    fun `following the phone picks its first supported language`() {
        val resolved = LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("nl-NL", "en-GB"))

        assertEquals(AppLanguage.DUTCH, resolved)
    }

    @Test
    fun `a language the app does not ship is skipped for one it does`() {
        val resolved = LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("de-DE", "fr-FR", "en-GB"))

        assertEquals(AppLanguage.ENGLISH, resolved)
    }

    @Test
    fun `a region is not part of the match`() {
        assertEquals(AppLanguage.DUTCH, LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("nl-BE")))
        assertEquals(AppLanguage.ENGLISH, LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("en-AU")))
    }

    @Test
    fun `a bare language tag matches`() {
        assertEquals(AppLanguage.DUTCH, LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("nl")))
    }

    @Test
    fun `a phone speaking nothing the app ships reads English`() {
        val resolved = LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("de-DE", "fr-FR"))

        assertEquals(AppLanguage.ENGLISH, resolved)
    }

    @Test
    fun `a phone with no languages at all reads English`() {
        assertEquals(AppLanguage.ENGLISH, LocaleResolver.resolve(AppLanguage.SYSTEM, emptyList()))
    }

    @Test
    fun `an unrecognised stored tag is treated as following the phone`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.ofTag("de"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.ofTag(null))
        assertEquals(
            AppLanguage.DUTCH,
            LocaleResolver.resolve(AppLanguage.ofTag("de"), listOf("nl-NL")),
        )
    }

    @Test
    fun `a stored tag round-trips`() {
        assertEquals(AppLanguage.DUTCH, AppLanguage.ofTag(AppLanguage.DUTCH.tag))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.ofTag(AppLanguage.ENGLISH.tag))
    }
}
