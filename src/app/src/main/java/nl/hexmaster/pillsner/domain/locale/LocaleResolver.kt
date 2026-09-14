package nl.hexmaster.pillsner.domain.locale

import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.SupportedLanguages

/**
 * Decides which language the app reads in (design D1).
 *
 * A stored choice always wins. Otherwise the phone's own preference list decides, matched on the
 * language and not the region, so someone whose phone is set to Belgian Dutch gets Dutch rather
 * than English. A phone speaking neither gets English.
 *
 * Deliberately takes plain tags rather than `java.util.Locale`, so the rules are the rules and not
 * whatever the platform's matching happens to do this release.
 */
object LocaleResolver {

    /**
     * @param stored what the user chose in Settings, or [AppLanguage.SYSTEM] if they have not.
     * @param systemLanguageTags the phone's preferred languages, most preferred first, as tags
     *   such as `nl-BE` or `en-GB`.
     */
    fun resolve(stored: AppLanguage, systemLanguageTags: List<String>): AppLanguage {
        if (stored != AppLanguage.SYSTEM) return stored

        systemLanguageTags.forEach { tag ->
            val language = tag.substringBefore('-').lowercase()
            SupportedLanguages.all.firstOrNull { it.tag == language }?.let { return it }
        }
        return SupportedLanguages.fallback
    }
}
