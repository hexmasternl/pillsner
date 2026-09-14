package nl.hexmaster.pillsner.ui.medicines.history

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.UsageBucket
import nl.hexmaster.pillsner.domain.model.UsageHistory
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.intakeStatusColors

/**
 * The four things that can become of a dose, in the order the breakdown and the chart both use
 * (app-medicine-usage-history design D5, D6). Each borrows an intake state's colour and icon from
 * docs/design-system.md section 2.3 rather than introducing a hue of its own.
 *
 * A dose that has passed but has not lapsed yet is still due, which is why [UNANSWERED] wears the
 * Due colours: it is neither a success nor a failure.
 */
enum class UsageCategory(val status: IntakeStatus, @StringRes val labelRes: Int) {
    TAKEN(IntakeStatus.Taken, R.string.dose_status_taken),
    SKIPPED(IntakeStatus.Skipped, R.string.dose_status_skipped),
    UNANSWERED(IntakeStatus.Due, R.string.usage_status_unanswered),
    MISSED(IntakeStatus.Missed, R.string.dose_status_missed),
    ;

    /** How many doses of this category the period holds. */
    fun countIn(history: UsageHistory): Int = when (this) {
        TAKEN -> history.taken
        SKIPPED -> history.skipped
        UNANSWERED -> history.unanswered
        MISSED -> history.missed
    }

    /** How many doses of this category one bucket of the chart holds. */
    fun countIn(bucket: UsageBucket): Int = when (this) {
        TAKEN -> bucket.taken
        SKIPPED -> bucket.skipped
        UNANSWERED -> bucket.unanswered
        MISSED -> bucket.missed
    }
}

/** Test tags for the breakdown, so semantics tests can reach one legend row. */
object UsageBreakdownTestTags {
    const val BAR = "usage_breakdown_bar"
    const val ROW_PREFIX = "usage_breakdown_row_"
}

/**
 * The period's outcomes as one proportional bar and a legend beneath it (design D6 item 4).
 *
 * The bar is decoration: it repeats what the legend says in words, so it is hidden from screen
 * readers rather than announced twice. Every legend row carries an icon and a written label as
 * well as its colour, so no state rests on colour alone.
 */
@Composable
fun UsageBreakdown(history: UsageHistory, modifier: Modifier = Modifier) {
    val present = UsageCategory.entries.filter { it.countIn(history) > 0 }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Sizes.usageBreakdownBarHeight)
                .clip(CircleShape)
                .testTag(UsageBreakdownTestTags.BAR)
                .clearAndSetSemantics { },
        ) {
            present.forEach { category ->
                Spacer(
                    Modifier
                        .weight(category.countIn(history).toFloat())
                        .fillMaxHeight()
                        .background(intakeStatusColors(category.status).container),
                )
            }
        }

        present.forEach { category ->
            UsageLegendRow(category = category, count = category.countIn(history))
        }
    }
}

/** One legend line: the category's icon in its own colour, its label, and how many doses it holds. */
@Composable
private fun UsageLegendRow(category: UsageCategory, count: Int, modifier: Modifier = Modifier) {
    val colors = intakeStatusColors(category.status)
    val label = stringResource(category.labelRes)
    val description = stringResource(R.string.usage_legend_row_description, label, count)

    Row(
        modifier
            .fillMaxWidth()
            .testTag(UsageBreakdownTestTags.ROW_PREFIX + category.name)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Surface(
            shape = CircleShape,
            color = colors.container,
            contentColor = colors.onContainer,
        ) {
            Icon(
                painter = painterResource(colors.icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(Spacing.xs)
                    .size(Sizes.iconChip),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun UsageBreakdownPreview() {
    PillsnerTheme {
        Surface {
            UsageBreakdown(
                history = UsageHistoryPreviewData.fullWeek(),
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
