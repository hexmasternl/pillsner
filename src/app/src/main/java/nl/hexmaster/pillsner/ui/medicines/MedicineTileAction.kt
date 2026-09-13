package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tag for the action behind a swiped tile. */
object MedicineTileActionTestTags {
    const val ACTION = "medicine_tile_action"
}

/**
 * The button a swipe uncovers: Deactivate on a medicine the user is taking, Activate on one they
 * have stopped.
 *
 * Neither is red. Stopping a medicine is reversible with the opposite swipe and takes nothing away
 * — the record, the schedules and the dose history all stay — so it is not one of the dangers
 * docs/design-system.md section 2.4 reserves the error colour for. Deactivate is blue, which the
 * design system uses for information; Activate is green, which means go.
 *
 * The label wraps rather than truncating, and the action grows with it, so nothing clips at the
 * largest font scale.
 */
@Composable
fun MedicineTileAction(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        if (isActive) R.string.medicines_action_deactivate else R.string.medicines_action_activate,
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = Sizes.minTouchTarget)
            .widthIn(min = MinimumActionWidth)
            .testTag(MedicineTileActionTestTags.ACTION),
        shape = MaterialTheme.shapes.large,
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (isActive) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    painter = painterResource(
                        if (isActive) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconDefault),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicineTileActionPreview() {
    PillsnerTheme {
        Surface {
            Column(
                Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                MedicineTileAction(
                    isActive = true,
                    onClick = {},
                    modifier = Modifier.heightIn(min = Sizes.tileMinHeight),
                )
                MedicineTileAction(
                    isActive = false,
                    onClick = {},
                    modifier = Modifier.heightIn(min = Sizes.tileMinHeight),
                )
            }
        }
    }
}
