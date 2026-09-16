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
        val resolved = LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("it-IT", "pl-PL", "en-GB"))

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
        val resolved = LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("it-IT", "pl-PL"))

        assertEquals(AppLanguage.ENGLISH, resolved)
    }

    @Test
    fun `a phone with no languages at all reads English`() {
        assertEquals(AppLanguage.ENGLISH, LocaleResolver.resolve(AppLanguage.SYSTEM, emptyList()))
    }

    @Test
    fun `an unrecognised stored tag is treated as following the phone`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.ofTag("it"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.ofTag(null))
        assertEquals(
            AppLanguage.DUTCH,
            LocaleResolver.resolve(AppLanguage.ofTag("it"), listOf("nl-NL")),
        )
    }

    @Test
    fun `a stored tag round-trips`() {
        assertEquals(AppLanguage.DUTCH, AppLanguage.ofTag(AppLanguage.DUTCH.tag))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.ofTag(AppLanguage.ENGLISH.tag))
    }

    @Test
    fun `a new supported language is picked when it is the phone's first match`() {
        val resolved = LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("it-IT", "de-DE", "nl-NL"))

        assertEquals(AppLanguage.GERMAN, resolved)
    }

    @Test
    fun `a regional variant of a new language matches on language only`() {
        assertEquals(
            AppLanguage.PORTUGUESE,
            LocaleResolver.resolve(AppLanguage.SYSTEM, listOf("pt-BR")),
        )
    }

    @Test
    fun `every new language round-trips its tag`() {
        assertEquals(AppLanguage.GERMAN, AppLanguage.ofTag(AppLanguage.GERMAN.tag))
        assertEquals(AppLanguage.FRENCH, AppLanguage.ofTag(AppLanguage.FRENCH.tag))
        assertEquals(AppLanguage.SPANISH, AppLanguage.ofTag(AppLanguage.SPANISH.tag))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.ofTag(AppLanguage.PORTUGUESE.tag))
    }
}
