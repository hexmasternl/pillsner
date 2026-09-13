package nl.hexmaster.pillsner.ui.medicines.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.DayOfWeek
import java.time.format.TextStyle
import nl.hexmaster.pillsner.ui.medicines.ScheduleDescriptionFormatter
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the weekday chips. */
object WeekdayChipsTestTags {
    const val ROW = "weekday_chips"

    /** The chip for one day; append the `DayOfWeek` name, for example `weekday_chip_MONDAY`. */
    const val CHIP_PREFIX = "weekday_chip_"
}

/**
 * The days a schedule fires on: seven filter chips in the locale's own week order, wrapping rather
 * than scrolling so all seven stay reachable at the largest font scale.
 *
 * Each chip shows the short day name but speaks the full one, so a screen reader says "Wednesday",
 * not "Wed", and Material's selected state is announced with it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekdayChips(
    selected: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag(WeekdayChipsTestTags.ROW),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ScheduleDescriptionFormatter.orderedWeek(locale).forEach { day ->
            val fullName = day.getDisplayName(TextStyle.FULL, locale)
            FilterChip(
                selected = day in selected,
                onClick = { onDayToggled(day) },
                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .semantics { contentDescription = fullName }
                    .testTag(WeekdayChipsTestTags.CHIP_PREFIX + day.name),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WeekdayChipsPreview() {
    PillsnerTheme {
        Surface {
            WeekdayChips(
                selected = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                onDayToggled = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
