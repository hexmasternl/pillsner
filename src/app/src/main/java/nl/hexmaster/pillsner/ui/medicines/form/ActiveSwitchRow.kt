package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * Whether this medicine is one the user is currently taking.
 *
 * The same flag the swipe on the overview flips, in the one place where the user is looking at
 * everything else about the medicine. Stopping a medicine takes nothing away — the record, its
 * schedules and its dose history all stay — so the row is an ordinary switch and uses no error
 * colour.
 */
@Composable
fun ActiveSwitchRow(
    isActive: Boolean,
    onActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.medication_form_active),
                style = MaterialTheme.typography.titleSmall,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.medication_form_active_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Switch(
                checked = isActive,
                onCheckedChange = onActiveChanged,
                modifier = Modifier.testTag(MedicationFormTestTags.ACTIVE_SWITCH),
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTouchTarget),
    )
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ActiveSwitchRowPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg)) {
                ActiveSwitchRow(isActive = true, onActiveChanged = {})
                ActiveSwitchRow(isActive = false, onActiveChanged = {})
            }
        }
    }
}
