package nl.hexmaster.pillsner.domain.model

/**
 * A language Pillsner can be read in.
 *
 * @property tag the ISO language tag, or null for [SYSTEM], which is not a language of its own but
 *   an instruction to follow the phone.
 */
enum class AppLanguage(val tag: String?) {
    /** Follow the phone's own language list. */
    SYSTEM(null),
    ENGLISH("en"),
    DUTCH("nl"),
    ;

    companion object {
        /** The language stored under [tag], or [SYSTEM] for an absent or unrecognised value. */
        fun ofTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && it.tag == tag } ?: SYSTEM
    }
}

/**
 * The languages the app actually ships.
 *
 * Adding one is: a value above, an entry here, a `values-xx/strings.xml`, and its own name in the
 * language array. Nothing else changes, because the dropdown and the resolver both read this list.
 */
object SupportedLanguages {

    /** In the order the dropdown offers them. */
    val all: List<AppLanguage> = listOf(AppLanguage.ENGLISH, AppLanguage.DUTCH)

    /** What a phone speaking none of the above gets. */
    val fallback: AppLanguage = AppLanguage.ENGLISH
}
