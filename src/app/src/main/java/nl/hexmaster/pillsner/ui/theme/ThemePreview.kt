package nl.hexmaster.pillsner.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark

/** Sanity preview of the theme: both palettes, the two anchor text styles, a filled button and every status. */
@PreviewLightDark
@Composable
private fun ThemePreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("Today", style = MaterialTheme.typography.displayLarge)
                Text("Take 40 mg of your medicine 'Ibuprofen', on 08:00", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = {}) { Text("I took it") }
                IntakeStatus.entries.forEach { status -> StatusSwatch(status) }
            }
        }
    }
}

@Composable
private fun StatusSwatch(status: IntakeStatus) {
    val colors = intakeStatusColors(status)
    Surface(
        modifier = Modifier.height(Sizes.statusChipHeight),
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.onContainer,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(painterResource(colors.icon), contentDescription = null, modifier = Modifier.size(Sizes.iconChip))
            Text(status.name, style = MaterialTheme.typography.labelMedium)
        }
    }
}
