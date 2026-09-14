package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A date in the app's own language, as the rest of the app writes dates.
 *
 * Used for a document's effective date and for the day an acceptance was given, so "14 September
 * 2026" reads the Dutch way to a Dutch reader without either screen deciding that for itself.
 */
@Composable
fun rememberLegalDateFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    }
}
