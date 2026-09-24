package nl.hexmaster.pillsner.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nl.hexmaster.pillsner.wear.R
import nl.hexmaster.pillsner.wear.ui.theme.WearDimens

/**
 * One dose under its time heading: name, amount, and the same two intake colours the phone uses
 * for these two states (design system section 2.3) — due in `secondaryContainer`, a dose already
 * past its time in `errorContainer`.
 *
 * The whole card is one node for a screen reader: "Ibuprofen, 400 mg, at 14:00". Tapping it opens
 * the read-only details of the medicine behind it; that is the only thing a tap does, because the
 * watch app shows and never changes — answering a dose stays on the phone's bridged notification.
 */
@Composable
fun DoseCard(entry: WatchDoseEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container = if (entry.isOverdue) colors.errorContainer else colors.secondaryContainer
    val onContainer = if (entry.isOverdue) colors.onErrorContainer else colors.onSecondaryContainer
    val icon = if (entry.isOverdue) R.drawable.ic_error_filled else R.drawable.ic_schedule

    val time = entry.timeText()
    val description = stringResource(
        if (entry.isOverdue) R.string.dose_description_overdue else R.string.dose_description,
        entry.name,
        entry.amountText,
        time,
    )
    val openLabel = stringResource(R.string.dose_open_details)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WearDimens.minTouchTarget)
            .background(container, MaterialTheme.shapes.large)
            .clickable(onClickLabel = openLabel, onClick = onClick)
            .padding(horizontal = WearDimens.cardPaddingHorizontal, vertical = WearDimens.cardPaddingVertical)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(WearDimens.cardPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(WearDimens.iconSize),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                color = onContainer,
            )
            Text(
                text = entry.amountText,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer,
            )
        }
    }
}

/** The scheduled time in the payload's language, with "Tomorrow" where the day differs. */
@Composable
internal fun WatchDoseEntry.timeText(): String {
    val formatted = formatTime(scheduledAt, rememberTimeFormatter())
    return if (isTomorrow) stringResource(R.string.upcoming_tomorrow_at, formatted) else formatted
}

/** The short time format of the payload's language, in the watch's own zone. */
@Composable
internal fun rememberTimeFormatter(): DateTimeFormatter {
    // LocalizedContent has already put the phone app's language here.
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    return remember(locale, zone) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone)
    }
}

internal fun formatTime(instant: Instant, formatter: DateTimeFormatter): String = formatter.format(instant)
