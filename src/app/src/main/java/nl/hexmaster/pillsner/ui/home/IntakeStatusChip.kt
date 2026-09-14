package nl.hexmaster.pillsner.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.intakeStatusColors

/** The user-facing label for an [IntakeStatus], from string resources. */
@Composable
fun IntakeStatus.label(): String = stringResource(
    when (this) {
        IntakeStatus.Due -> R.string.dose_status_due
        IntakeStatus.Taken -> R.string.dose_status_taken
        IntakeStatus.Snoozed -> R.string.dose_status_snoozed
        IntakeStatus.Skipped -> R.string.dose_status_skipped
        IntakeStatus.Overdue -> R.string.dose_status_overdue
        IntakeStatus.Missed -> R.string.dose_status_missed
    },
)

/**
 * Non-interactive status chip (docs/design-system.md section 8.3): pill shape, icon plus label so the
 * state never relies on colour alone, colours from section 2.3, `stateDescription` for TalkBack.
 */
@Composable
fun IntakeStatusChip(
    status: IntakeStatus,
    modifier: Modifier = Modifier,
) {
    val colors = intakeStatusColors(status)
    val label = status.label()
    Surface(
        modifier = modifier
            .height(Sizes.statusChipHeight)
            .semantics { stateDescription = label },
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.onContainer,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(colors.icon),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconChip),
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@PreviewLightDark
@Composable
private fun IntakeStatusChipPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                IntakeStatus.entries.forEach { IntakeStatusChip(it) }
            }
        }
    }
}
