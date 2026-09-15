package nl.hexmaster.pillsner.ui.settings.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the Theme section. */
object ThemeSectionTestTags {
    const val DROPDOWN = "settings_theme_dropdown"

    /** One option in the open menu; append the `AppTheme` name. */
    const val OPTION_PREFIX = "settings_theme_option_"
}

/**
 * Which colour scheme Pillsner renders in: follow the phone, or hold it to light or dark.
 *
 * The same dropdown as the Language section above it, so two adjacent single-choice settings read
 * the same way (design D5). Unlike Language there is no restart notice: a scheme is an argument to
 * `PillsnerTheme`, so the choice repaints this very screen (design D3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSection(
    state: ThemeSectionState,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(R.string.settings_theme_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.selected.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_theme_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag(ThemeSectionTestTags.DROPDOWN),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                            onThemeSelected(option)
                            expanded = false
                        },
                        modifier = Modifier.testTag(ThemeSectionTestTags.OPTION_PREFIX + option.name),
                    )
                }
            }
        }
    }
}

/** What the dropdown calls this option, in the app's language. */
@Composable
private fun AppTheme.label(): String = stringResource(
    when (this) {
        AppTheme.SYSTEM -> R.string.settings_theme_system_default
        AppTheme.LIGHT -> R.string.settings_theme_light
        AppTheme.DARK -> R.string.settings_theme_dark
    },
)

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ThemeSectionPreview() {
    PillsnerTheme {
        Surface {
            Column(
                Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                ThemeSection(ThemeSectionState(), onThemeSelected = {})
                ThemeSection(ThemeSectionState(selected = AppTheme.DARK), onThemeSelected = {})
            }
        }
    }
}
