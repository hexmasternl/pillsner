package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the schedules section of the form. */
object ScheduleRowTestTags {
    const val ROW = "schedule_row"
    const val REMOVE = "schedule_row_remove"
}

/**
 * One schedule in the form's list: its description, tappable to edit, with a remove button.
 *
 * @param description the already-formatted line, for example "40 mg every 12 hours".
 */
@Composable
fun ScheduleRow(
    description: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(description, style = MaterialTheme.typography.bodyLarge) },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                    .testTag(ScheduleRowTestTags.REMOVE),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.schedule_remove_content_description),
                    modifier = Modifier.size(Sizes.iconDefault),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag(ScheduleRowTestTags.ROW),
    )
}

@PreviewLightDark
@Composable
private fun ScheduleRowPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg)) {
                ScheduleRow(description = "40 mg every 12 hours", onEdit = {}, onRemove = {})
                ScheduleRow(description = "20 mg once a day on Sat, Sun", onEdit = {}, onRemove = {})
            }
        }
    }
}
