package nl.hexmaster.pillsner.ui.medicines.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.UsageBucket
import nl.hexmaster.pillsner.domain.model.UsageHistory
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.intakeStatusColors

/** Test tags for the chart, so semantics tests can reach one bar. */
object UsageChartTestTags {
    const val CHART = "usage_chart"
    const val BAR_PREFIX = "usage_chart_bar_"
}

/**
 * The period as one bar per bucket, left to right in time order (app-medicine-usage-history
 * design D5).
 *
 * Layout composables rather than a `Canvas`: this chart has no curve to draw, and a `Row` of
 * columns gives right-to-left, theming and a semantics node per bar for nothing. Each column is
 * stacked bottom-up taken, skipped, unanswered, missed - missed last because that is where the eye
 * lands - and is as tall a fraction of the row as its bucket is of the busiest one.
 *
 * No bar carries a visible date label: thirteen of them cannot survive 200 % font scale. The first
 * and last bucket dates sit beneath the row instead, and every bar states its own date and counts
 * to a screen reader.
 */
@Composable
fun UsageChart(history: UsageHistory, modifier: Modifier = Modifier) {
    val busiest = history.buckets.maxOfOrNull { it.scheduled } ?: 0
    // Dates in the user's language, never assembled by hand (design system 3.3).
    val locale = LocalConfiguration.current.locales[0]
    val dayFormat = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val spokenFormat = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Sizes.usageChartHeight)
                .testTag(UsageChartTestTags.CHART),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            history.buckets.forEachIndexed { index, bucket ->
                UsageBar(
                    bucket = bucket,
                    busiest = busiest,
                    description = bucket.spokenDescription(spokenFormat),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(UsageChartTestTags.BAR_PREFIX + index),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = history.firstDay.format(dayFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = history.lastDay.format(dayFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One column: an empty space, then the bucket's stack at the bottom. A bucket with nothing
 * scheduled keeps its place in the row so the shape of the period stays honest.
 */
@Composable
private fun UsageBar(
    bucket: UsageBucket,
    busiest: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (bucket.scheduled == 0 || busiest == 0) return@Column

        val headroom = (busiest - bucket.scheduled).toFloat()
        // Only when there is headroom: a zero weight is not a thing Compose lays out.
        if (headroom > 0f) Spacer(Modifier.weight(headroom))

        Column(
            Modifier
                .weight(bucket.scheduled.toFloat())
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .border(BAR_BORDER, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Drawn top down so the stack reads bottom-up as taken, skipped, unanswered, missed.
            UsageCategory.entries.reversed().forEach { category ->
                val count = category.countIn(bucket)
                if (count == 0) return@forEach
                Spacer(
                    Modifier
                        .weight(count.toFloat())
                        .fillMaxWidth()
                        .background(intakeStatusColors(category.status).container),
                )
            }
        }
    }
}

/** "9 September, 2 of 2 doses taken", or the week and the empty wordings (design D5). */
@Composable
private fun UsageBucket.spokenDescription(format: DateTimeFormatter): String {
    val date = start.format(format)
    return when {
        scheduled == 0 && isWeek -> stringResource(R.string.usage_history_bar_week_empty, date)
        scheduled == 0 -> stringResource(R.string.usage_history_bar_day_empty, date)
        // The scheduled total is what the sentence counts, so it is what chooses the plural form.
        isWeek -> pluralStringResource(R.plurals.usage_history_bar_week, scheduled, date, taken, scheduled)
        else -> pluralStringResource(R.plurals.usage_history_bar_day, scheduled, date, taken, scheduled)
    }
}

/**
 * The thinnest line the device can draw, so the skipped segment stays legible where its tier sits
 * next to the card's own. A hairline rather than a new dimension token: it is a separator, not a
 * size anything else is measured against.
 */
private val BAR_BORDER = Dp.Hairline

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun UsageChartPreview() {
    PillsnerTheme {
        Surface {
            UsageChart(
                history = UsageHistoryPreviewData.fullWeek(),
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
