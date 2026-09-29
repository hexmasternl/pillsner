package nl.hexmaster.pillsner.ui.medicines.labelscan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the label-scan parts of the Add medicine form. */
object LabelScanFormTestTags {
    const val SCAN_BUTTON = "medication_form_scan_label"
    const val OPTION_CAMERA = "medication_form_scan_option_camera"
    const val OPTION_PHOTO = "medication_form_scan_option_photo"
    const val RATIONALE = "medication_form_camera_rationale"
    const val RATIONALE_CONTINUE = "medication_form_camera_rationale_continue"
    const val RATIONALE_NOT_NOW = "medication_form_camera_rationale_not_now"
    const val READING = "medication_form_scan_reading"
    const val READING_CANCEL = "medication_form_scan_reading_cancel"
    const val REPLACE = "medication_form_scan_replace"
    const val REPLACE_CONFIRM = "medication_form_scan_replace_confirm"
    const val REPLACE_KEEP = "medication_form_scan_replace_keep"
    const val BANNER = "medication_form_scan_banner"
    const val BANNER_SHOW_TEXT = "medication_form_scan_banner_show_text"
    const val BANNER_DISMISS = "medication_form_scan_banner_dismiss"
    const val TEXT_SHEET = "medication_form_scan_text"
}

/**
 * "Scan a label", above the name field in add mode (medicine-label-photo-prefill design D5). An
 * outlined button: a neutral alternative to typing, not the form's positive action (design system
 * 8.4). The icon repeats the label, so it has no description of its own.
 */
@Composable
fun ScanLabelButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTouchTarget)
            .testTag(LabelScanFormTestTags.SCAN_BUTTON),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null,
            modifier = Modifier.size(Sizes.iconDefault),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.label_scan_action))
    }
}

/**
 * The two ways to scan (design D2, design system 8.12). "Scan with camera" is offered only when the
 * device has one; "Choose a photo" needs no permission and is always there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOptionsSheet(
    cameraAvailable: Boolean,
    onScanWithCamera: () -> Unit,
    onChoosePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(bottom = Spacing.xl)) {
            Text(
                text = stringResource(R.string.label_scan_action),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = Spacing.screenEdge)
                    .padding(bottom = Spacing.sm)
                    .semantics { heading() },
            )
            if (cameraAvailable) {
                SheetOption(
                    label = stringResource(R.string.label_scan_option_camera),
                    icon = R.drawable.ic_camera,
                    onClick = onScanWithCamera,
                    testTag = LabelScanFormTestTags.OPTION_CAMERA,
                )
            }
            SheetOption(
                label = stringResource(R.string.label_scan_option_photo),
                icon = R.drawable.ic_image,
                onClick = onChoosePhoto,
                testTag = LabelScanFormTestTags.OPTION_PHOTO,
            )
        }
    }
}

@Composable
private fun SheetOption(label: String, icon: Int, onClick: () -> Unit, testTag: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTouchTarget)
            .clickable(onClick = onClick)
            .testTag(testTag),
    )
}

/**
 * The in-app rationale before the system camera prompt (design D2): why the camera, and what is
 * not done with it. "Not now" leaves the form exactly as it was.
 */
@Composable
fun CameraRationaleDialog(onContinue: () -> Unit, onNotNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotNow,
        modifier = Modifier.testTag(LabelScanFormTestTags.RATIONALE),
        title = {
            Text(stringResource(R.string.label_scan_rationale_title), style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Text(stringResource(R.string.label_scan_rationale_body), style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            TextButton(onClick = onContinue, modifier = Modifier.testTag(LabelScanFormTestTags.RATIONALE_CONTINUE)) {
                Text(stringResource(R.string.label_scan_rationale_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow, modifier = Modifier.testTag(LabelScanFormTestTags.RATIONALE_NOT_NOW)) {
                Text(stringResource(R.string.label_scan_rationale_not_now))
            }
        },
    )
}

/** The modal "Reading the photo" state with its one way out (design D5). */
@Composable
fun ReadingPhotoDialog(onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        modifier = Modifier.testTag(LabelScanFormTestTags.READING),
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.label_scan_reading_photo), style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(LabelScanFormTestTags.READING_CANCEL)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Asks before a scan overwrites what the user has typed (design D5). Only Replace applies it. */
@Composable
fun ReplaceDraftDialog(onReplace: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        modifier = Modifier.testTag(LabelScanFormTestTags.REPLACE),
        title = {
            Text(stringResource(R.string.label_scan_replace_title), style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Text(stringResource(R.string.label_scan_replace_body), style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            TextButton(onClick = onReplace, modifier = Modifier.testTag(LabelScanFormTestTags.REPLACE_CONFIRM)) {
                Text(stringResource(R.string.label_scan_replace_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep, modifier = Modifier.testTag(LabelScanFormTestTags.REPLACE_KEEP)) {
                Text(stringResource(R.string.label_scan_replace_keep))
            }
        },
    )
}

/**
 * "Filled in from your label scan. Check every field before saving." at the top of the form after
 * a scan is applied (design D5). Built like the attention banner (design system 8.9) but on
 * `secondaryContainer`: red is reserved for danger (2.4), and a pre-filled form is a thing to
 * check, not a failure. Stays until dismissed or the form closes.
 */
@Composable
fun ScanReviewBanner(onShowText: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(LabelScanFormTestTags.BANNER),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.label_scan_banner_text), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(
                        onClick = onShowText,
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                        modifier = Modifier
                            .heightIn(min = Sizes.minTouchTarget)
                            .testTag(LabelScanFormTestTags.BANNER_SHOW_TEXT),
                    ) {
                        Text(stringResource(R.string.label_scan_banner_show_text))
                    }
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                        modifier = Modifier
                            .heightIn(min = Sizes.minTouchTarget)
                            .testTag(LabelScanFormTestTags.BANNER_DISMISS),
                    ) {
                        Text(stringResource(R.string.label_scan_banner_dismiss))
                    }
                }
            }
        }
    }
}

/** The recognised text, selectable, and nothing else (design D5, design system 8.12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognisedTextSheet(text: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag(LabelScanFormTestTags.TEXT_SHEET),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenEdge)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.label_scan_text_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            SelectionContainer {
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ScanFormPartsPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ScanReviewBanner(onShowText = {}, onDismiss = {})
                ScanLabelButton(onClick = {})
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CameraRationaleDialogPreview() {
    PillsnerTheme {
        CameraRationaleDialog(onContinue = {}, onNotNow = {})
    }
}

@PreviewLightDark
@Composable
private fun ReplaceDraftDialogPreview() {
    PillsnerTheme {
        ReplaceDraftDialog(onReplace = {}, onKeep = {})
    }
}
