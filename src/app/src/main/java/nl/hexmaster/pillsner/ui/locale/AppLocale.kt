package nl.hexmaster.pillsner.ui.locale

import android.content.Context
import android.os.LocaleList
import java.util.Locale
import nl.hexmaster.pillsner.domain.locale.LocaleResolver
import nl.hexmaster.pillsner.domain.model.AppLanguage

/**
 * The language the running app is actually in (design D3).
 *
 * Resolved once, at process start, and then left alone: the user's choice takes effect on the next
 * start, which is what makes the restart notice on the Settings screen truthful. Setting the JVM
 * default locale here is what makes every date, time, weekday name, decimal separator and name
 * sort follow the app language without a single call site having to know about it.
 *
 * A `BroadcastReceiver` has no activity to inherit a configuration from, so anything that reads a
 * string outside the activity — the reminder notification above all — goes through [wrap].
 */
object AppLocale {

    /** The language resolved at process start. */
    @Volatile
    var inEffect: AppLanguage = AppLanguage.ENGLISH
        private set

    /** The locale [inEffect] means. */
    @Volatile
    var current: Locale = Locale.ENGLISH
        private set

    /**
     * Resolves [stored] against the phone's own languages and applies the result to this process.
     *
     * @param systemLocales the phone's preferred languages, most preferred first.
     */
    fun apply(stored: AppLanguage, systemLocales: LocaleList) {
        val tags = (0 until systemLocales.size()).map { systemLocales[it].toLanguageTag() }
        inEffect = LocaleResolver.resolve(stored, tags)
        current = Locale.forLanguageTag(checkNotNull(inEffect.tag) { "A resolved language has a tag" })
        Locale.setDefault(current)
    }

    /**
     * The same context, reading its resources in the app's language. Use it wherever a string is
     * read outside the activity; the activity itself wraps its own base context.
     */
    fun wrap(context: Context): Context {
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(current))
        return context.createConfigurationContext(configuration)
    }
}
