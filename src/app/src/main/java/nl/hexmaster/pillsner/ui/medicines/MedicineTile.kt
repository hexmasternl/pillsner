package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.DayOfWeek
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.stock.StockState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.tileContainerColor

/**
 * One medicine on the overview (docs/design-system.md section 8.2): the same card as the dose tile
 * without the state stripe, a leading medication icon avatar, the name in `titleMedium` and one
 * schedule description per line below it, in the medicine's own schedule order. A trailing chevron
 * hints that the tile opens the medicine's details when [onClick] is set.
 *
 * An inactive medicine keeps full contrast — alpha is never lowered — and is set apart by a lower
 * surface tier, an `Inactive` chip and a spoken state, so the distinction never rests on colour
 * alone. The whole card is one merged semantics node, so TalkBack reads "Metoprolol, 40 mg every
 * 12 hours, 20 mg once a day on Sat, Sun" as a single item. The avatar and chevron are purely
 * decorative and carry no `contentDescription` of their own.
 *
 * @param descriptions one already-formatted line per entry of [MedicineTileState.schedules].
 */
@Composable
fun MedicineTile(
    state: MedicineTileState,
    descriptions: List<String>,
    modifier: Modifier = Modifier,
    onSetActive: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val inactiveLabel = stringResource(R.string.medicine_inactive_label)
    val separator = stringResource(R.string.list_separator)
    val stockLabels = state.stockState.spokenLabels()
    val spoken = (listOf(state.name) + descriptions + stockLabels).joinToString(separator)
    val activationLabel = stringResource(
        if (state.isActive) R.string.medicines_action_deactivate else R.string.medicines_action_activate,
    )
    // Swiping is invisible to a screen reader, switch access and a keyboard, so the same action is
    // offered here as well. Without it, stopping a medicine would be unreachable for those users.
    val activationActions = onSetActive?.let { set ->
        listOf(CustomAccessibilityAction(activationLabel) { set(!state.isActive); true })
    }
    val openLabel = stringResource(R.string.medicines_tile_open_details)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.tileMinHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                if (!state.isActive) stateDescription = inactiveLabel
                activationActions?.let { customActions = it }
                // Opening the medicine is the default action a screen reader performs on a list
                // item; starting and stopping it stays one menu away.
                if (onClick != null) onClick(label = openLabel) { onClick(); true }
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (state.isActive) {
                tileContainerColor()
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = if (state.isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            MedicineAvatar(isActive = state.isActive)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (!state.isActive) {
                        InactiveChip(
                            label = inactiveLabel,
                            modifier = Modifier.testTag(MedicinesScreenTestTags.INACTIVE_CHIP),
                        )
                    }
                }
                descriptions.forEach { description ->
                    Text(text = description, style = MaterialTheme.typography.bodyMedium)
                }
                state.stockState?.let { StockHeadsUp(it) }
            }
            if (onClick != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    // Decorative: the tile's own click semantics already say "opens details".
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.iconDefault),
                )
            }
        }
    }
}

/**
 * The tile's leading visual anchor: a medication icon inside a tinted circle, filled for an active
 * medicine and outlined for an inactive one. Purely decorative — the tile's merged semantics
 * already speak the active/inactive state.
 */
@Composable
private fun MedicineAvatar(isActive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(Sizes.iconAvatar),
        shape = CircleShape,
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (isActive) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(
                    if (isActive) R.drawable.ic_medication_filled else R.drawable.ic_medication,
                ),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
        }
    }
}

/**
 * The live stock heads-up (`medicine-stock-tracking`'s "Medicine tile stock heads-up" requirement):
 * one chip for low stock, one for the nearest batch's expiry, either, both or neither depending on
 * [stockState]. Never colour alone — each chip carries its own icon and text.
 */
@Composable
private fun StockHeadsUp(stockState: StockState, modifier: Modifier = Modifier) {
    if (!stockState.isLow && stockState.nearestExpiry == BatchExpiryState.NONE) return

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (stockState.isLow) {
            StockChip(
                label = stringResource(R.string.medicine_tile_low_stock),
                icon = R.drawable.ic_info,
                modifier = Modifier.testTag(MedicinesScreenTestTags.LOW_STOCK_CHIP),
            )
        }
        when (stockState.nearestExpiry) {
            BatchExpiryState.APPROACHING -> StockChip(
                label = stringResource(R.string.medicine_tile_expiring_soon),
                icon = R.drawable.ic_info,
                modifier = Modifier.testTag(MedicinesScreenTestTags.EXPIRING_CHIP),
            )
            BatchExpiryState.PAST -> StockChip(
                label = stringResource(R.string.medicine_tile_expired),
                icon = R.drawable.ic_error_filled,
                modifier = Modifier.testTag(MedicinesScreenTestTags.EXPIRED_CHIP),
            )
            BatchExpiryState.NONE -> Unit
        }
    }
}

/**
 * The stock heads-up's own colour (design system 2.4): `error` is reserved for overdue/missed
 * doses, missing permissions, destructive confirmations, stock at zero and validation errors — an
 * expired batch is none of those, so every chip here uses the informational `secondaryContainer`
 * pair and is distinguished from the others by its icon and wording alone, never by hue.
 */
@Composable
private fun StockChip(label: String, icon: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(Sizes.statusChipHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                // Decorative: the chip's own label says everything the icon does.
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconChip),
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The stock state's words, appended to the tile's single combined screen-reader announcement. */
@Composable
private fun StockState?.spokenLabels(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        if (isLow) add(stringResource(R.string.medicine_tile_low_stock))
        when (nearestExpiry) {
            BatchExpiryState.APPROACHING -> add(stringResource(R.string.medicine_tile_expiring_soon))
            BatchExpiryState.PAST -> add(stringResource(R.string.medicine_tile_expired))
            BatchExpiryState.NONE -> Unit
        }
    }
}

/**
 * The `Inactive` marker: the status chip's shape and size (section 8.3) on a neutral surface,
 * because being inactive is not one of the six intake statuses.
 */
@Composable
private fun InactiveChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(Sizes.statusChipHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicineTilePreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                MedicineTile(
                    state = MedicineTileState(
                        id = MedicationId(1),
                        name = "Metoprolol",
                        schedules = listOf(
                            ScheduleLine(
                                ScheduleSummary.EveryNHours(12),
                                Quantity.of("40", DoseUnit.MILLIGRAM),
                            ),
                            ScheduleLine(
                                ScheduleSummary.TimesPerDayOnDays(
                                    1,
                                    setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                                ),
                                Quantity.of("20", DoseUnit.MILLIGRAM),
                            ),
                        ),
                        isActive = true,
                    ),
                    descriptions = listOf("40 mg every 12 hours", "20 mg once a day on Sat, Sun"),
                    onClick = {},
                )
                MedicineTile(
                    state = MedicineTileState(
                        id = MedicationId(2),
                        name = "Methotrexate",
                        schedules = listOf(
                            ScheduleLine(
                                ScheduleSummary.TimesPerDayOnDays(
                                    1,
                                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                                ),
                                Quantity.of("1", DoseUnit.TABLET),
                            ),
                        ),
                        isActive = false,
                    ),
                    descriptions = listOf("1 tablet once a day on Mon, Wed, Fri"),
                )
                MedicineTile(
                    state = MedicineTileState(
                        id = MedicationId(3),
                        name = "Amoxicillin",
                        schedules = listOf(
                            ScheduleLine(ScheduleSummary.EveryNHours(8), Quantity.of("500", DoseUnit.MILLIGRAM)),
                        ),
                        isActive = true,
                        stockState = StockState(isLow = true, nearestExpiry = BatchExpiryState.NONE),
                    ),
                    descriptions = listOf("500 mg every 8 hours"),
                )
                MedicineTile(
                    state = MedicineTileState(
                        id = MedicationId(4),
                        name = "Vitamin D",
                        schedules = listOf(ScheduleLine(ScheduleSummary.AsNeeded, Quantity.of("2", DoseUnit.DROP))),
                        isActive = true,
                        stockState = StockState(isLow = false, nearestExpiry = BatchExpiryState.APPROACHING),
                    ),
                    descriptions = listOf("2 drops as needed"),
                )
                MedicineTile(
                    state = MedicineTileState(
                        id = MedicationId(5),
                        name = "Paracetamol",
                        schedules = listOf(ScheduleLine(ScheduleSummary.AsNeeded, Quantity.of("2", DoseUnit.TABLET))),
                        isActive = true,
                        stockState = StockState(isLow = true, nearestExpiry = BatchExpiryState.PAST),
                    ),
                    descriptions = listOf("2 tablets as needed"),
                )
            }
        }
    }
}
