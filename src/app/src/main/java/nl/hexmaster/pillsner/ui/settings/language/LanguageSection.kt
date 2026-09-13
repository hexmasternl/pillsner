package nl.hexmaster.pillsner.ui.settings.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the Language section. */
object LanguageSectionTestTags {
    const val DROPDOWN = "settings_language_dropdown"
    const val RESTART_NOTICE = "settings_language_restart_notice"

    /** One option in the open menu; append the `AppLanguage` name. */
    const val OPTION_PREFIX = "settings_language_option_"
}

/**
 * Which language Pillsner is read in.
 *
 * Every language is offered under its own name — English, Nederlands — so someone who cannot read
 * the language the app is currently in can still find theirs.
 *
 * A choice is saved at once but applies at the next start, so a notice appears underneath until
 * then. It is blue, not red: a pending restart is information, and docs/design-system.md section
 * 2.4 keeps red for things that are actually wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSection(
    state: LanguageSectionState,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val nativeNames = stringArrayResource(R.array.language_names)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(R.string.settings_language_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.selected.label(nativeNames),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_language_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag(LanguageSectionTestTags.DROPDOWN),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label(nativeNames)) },
                        onClick = {
                            onLanguageSelected(option)
                            expanded = false
                        },
                        modifier = Modifier.testTag(LanguageSectionTestTags.OPTION_PREFIX + option.name),
                    )
                }
            }
        }

        if (state.restartRequired) {
            RestartNotice()
        }
    }
}

/** Says that the chosen language is not the one on screen yet. */
@Composable
private fun RestartNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Announced when it appears, so a screen-reader user is not left waiting for a change
            // that is not coming until the next start.
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(LanguageSectionTestTags.RESTART_NOTICE),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
            Text(
                text = stringResource(R.string.settings_language_restart_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * What the dropdown calls this option: "System default" in the app's language, and every real
 * language in its own.
 */
@Composable
private fun AppLanguage.label(nativeNames: Array<String>): String =
    if (this == AppLanguage.SYSTEM) {
        stringResource(R.string.settings_language_system_default)
    } else {
        nativeNames.getOrElse(ordinal - 1) { name }
    }

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun LanguageSectionPreview() {
    PillsnerTheme {
        Surface {
            Column(
                Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                LanguageSection(LanguageSectionState(), onLanguageSelected = {})
                LanguageSection(
                    LanguageSectionState(selected = AppLanguage.DUTCH, restartRequired = true),
                    onLanguageSelected = {},
                )
            }
        }
    }
}
