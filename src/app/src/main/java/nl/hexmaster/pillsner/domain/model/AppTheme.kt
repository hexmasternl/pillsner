package nl.hexmaster.pillsner.domain.model

/**
 * The colour scheme Pillsner renders in.
 *
 * @property key the stored value, or null for [SYSTEM], which is not a scheme of its own but an
 *   instruction to follow the phone. Storing it as the absence of a key means an absent, empty or
 *   corrupt value all read back as "follow the phone" without a special case.
 */
enum class AppTheme(val key: String?) {
    /** Follow the phone's own light/dark setting. */
    SYSTEM(null),
    LIGHT("light"),
    DARK("dark"),
    ;

    /**
     * Whether this choice means the dark scheme.
     *
     * @param systemInDarkTheme whether the phone is currently in dark mode. Read for every choice
     *   and deliberately ignored by [LIGHT] and [DARK] — that is what "the phone is ignored" means.
     */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** The theme stored under [key], or [SYSTEM] for an absent or unrecognised value. */
        fun ofKey(key: String?): AppTheme =
            entries.firstOrNull { it.key != null && it.key == key } ?: SYSTEM
    }
}
