package nl.hexmaster.pillsner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.Instant
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.ui.medicines.rememberQuantityFormatter
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.TabularNumbers
import nl.hexmaster.pillsner.ui.theme.intakeStatusColors
import nl.hexmaster.pillsner.ui.theme.tileContainerColor

/**
 * One planned dose on the welcome screen (docs/design-system.md section 8.1).
 *
 * Informational only in this change: no tap target, no actions. The whole card is one merged
 * semantics node so TalkBack reads "Ibuprofen, 1 tablet, Due at 08:00 Tomorrow" as one item. The
 * stripe, the icon, the chip and the spoken state all change together with [status], so the state
 * never rests on colour alone.
 */
@Composable
fun DoseTile(
    dose: UpcomingDose,
    time: FormattedDoseTime,
    modifier: Modifier = Modifier,
    status: IntakeStatus = IntakeStatus.Due,
) {
    val statusColors = intakeStatusColors(status)
    val statusLabel = status.label()
    val amount = rememberQuantityFormatter().format(dose.amount)
    val dayLabel = time.day?.text()
    val description = if (dayLabel == null) {
        stringResource(R.string.dose_tile_description, dose.medicationName, amount, statusLabel, time.time)
    } else {
        stringResource(
            R.string.dose_tile_description_with_day,
            dose.medicationName,
            amount,
            statusLabel,
            time.time,
            dayLabel,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.tileMinHeight)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tileContainerColor()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .width(Sizes.stateStripeWidth)
                    .fillMaxHeight()
                    .background(statusColors.container),
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    painter = painterResource(statusColors.icon),
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconDefault),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(dose.medicationName, style = MaterialTheme.typography.titleMedium)
                    Text(amount, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(Spacing.xs))
                    IntakeStatusChip(status)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        time.time,
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularNumbers),
                        textAlign = TextAlign.End,
                    )
                    if (dayLabel != null) {
                        Text(
                            dayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

/** Resolves a [DayLabel] to display text; only Tomorrow needs a string resource. */
@Composable
private fun DayLabel.text(): String = when (this) {
    DayLabel.Tomorrow -> stringResource(R.string.day_tomorrow)
    is DayLabel.Text -> value
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseTilePreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                DoseTile(
                    dose = UpcomingDose(
                        DoseId(1),
                        "Ibuprofen",
                        Quantity.of("1", DoseUnit.TABLET),
                        Instant.EPOCH,
                    ),
                    time = FormattedDoseTime("20:00", null),
                )
                DoseTile(
                    dose = UpcomingDose(
                        DoseId(2),
                        "Amoxicillin",
                        Quantity.of("500", DoseUnit.MILLIGRAM),
                        Instant.EPOCH,
                        isOverdue = true,
                    ),
                    time = FormattedDoseTime("08:00", null),
                    status = IntakeStatus.Overdue,
                )
                DoseTile(
                    dose = UpcomingDose(
                        DoseId(3),
                        "Metoprolol",
                        Quantity.of("40", DoseUnit.MILLIGRAM),
                        Instant.EPOCH,
                        snoozedUntil = Instant.EPOCH,
                    ),
                    time = FormattedDoseTime("08:00", DayLabel.Tomorrow),
                    status = IntakeStatus.Snoozed,
                )
            }
        }
    }
}
