package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * A group header inside the Medicines list (docs/design-system.md section 3.2): `headlineSmall`,
 * announced as a heading. It is drawn on the `surface` colour so it stays readable when it sticks
 * to the top of the list while tiles scroll underneath it.
 */
@Composable
fun MedicinesSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.sm)
            .semantics { heading() },
    )
}

@PreviewLightDark
@Composable
private fun MedicinesSectionHeaderPreview() {
    PillsnerTheme {
        Surface {
            MedicinesSectionHeader(title = "Active", modifier = Modifier.padding(Spacing.lg))
        }
    }
}
