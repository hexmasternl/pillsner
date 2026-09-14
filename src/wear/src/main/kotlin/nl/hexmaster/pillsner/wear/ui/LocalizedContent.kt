package nl.hexmaster.pillsner.wear.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Reads the watch's strings in the phone app's language (design D7).
 *
 * The amounts arrive already written out by the phone — "2 tabletten" — so a header in the watch's
 * own system language would read as half a translation. The phone's language wins; until a payload
 * has ever arrived there is nothing to follow and the watch keeps its own.
 */
@Composable
fun LocalizedContent(locale: Locale, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localized = remember(locale, configuration) {
        Configuration(configuration).apply { setLocales(LocaleList(locale)) }
    }
    val localizedContext = remember(localized) { context.createConfigurationContext(localized) }

    CompositionLocalProvider(
        LocalConfiguration provides localized,
        LocalContext provides localizedContext,
        content = content,
    )
}
