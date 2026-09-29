package nl.hexmaster.pillsner.ui.medicines.labelscan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.data.labelscan.FrameCropper
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the scanning screen. */
object LabelScanTestTags {
    const val TITLE = "label_scan_title"
    const val CANCEL = "label_scan_cancel"
    const val VIEWFINDER = "label_scan_viewfinder"
    const val GUIDE = "label_scan_guide"
    const val INSTRUCTION = "label_scan_instruction"
    const val SHUTTER = "label_scan_shutter"
    const val TORCH = "label_scan_torch"
}

/**
 * The live scanning screen (medicine-label-photo-prefill design D3): the camera preview with a
 * framing guide over it, one line of instruction, and the shutter, torch and cancel controls.
 *
 * The preview is a slot, so the screen renders in previews and tests without a camera. The
 * instruction is a live region, which is how a screen reader hears the opening instruction, the
 * hint changes and "Label read"; the shutter is how a screen reader user, who cannot see the
 * guide, completes a scan on their own terms.
 *
 * @param viewfinder draws the camera preview into the given modifier; empty by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelScanScreen(
    uiState: LabelScanUiState,
    onShutter: () -> Unit,
    onTorchToggled: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    guide: FrameCropper.Guide = FrameCropper.Guide.DEFAULT,
    onViewfinderSizeChanged: (IntSize) -> Unit = {},
    viewfinder: @Composable (Modifier) -> Unit = {},
) {
    // System back leaves like Cancel does: without a result, and with the recogniser stopped.
    BackHandler(onBack = onCancel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.label_scan_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(LabelScanTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                            .testTag(LabelScanTestTags.CANCEL),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    // The camera binder shapes its viewport to this size, so the preview and the
                    // analysis stream show the same region behind the guide.
                    .onSizeChanged(onViewfinderSizeChanged)
                    .testTag(LabelScanTestTags.VIEWFINDER),
            ) {
                viewfinder(Modifier.fillMaxSize())
                FramingGuide(guide = guide, modifier = Modifier.fillMaxSize())
            }

            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    Text(
                        text = stringResource(uiState.instructionRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .widthIn(max = Spacing.contentMaxWidth)
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag(LabelScanTestTags.INSTRUCTION),
                    )
                    Controls(uiState = uiState, onShutter = onShutter, onTorchToggled = onTorchToggled)
                }
            }
        }
    }
}

/**
 * Shutter in the middle, torch to its left when the camera has one, an equal blank to its right
 * so the shutter stays centred. The shutter is the screen's primary action, so it is
 * `Sizes.primaryActionHeight` tall and sits in the bottom third (design system 8.4, 10).
 */
@Composable
private fun Controls(
    uiState: LabelScanUiState,
    onShutter: () -> Unit,
    onTorchToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shutterEnabled = uiState.isReady && !uiState.failed && !uiState.finished

    Row(
        modifier
            .widthIn(max = Spacing.contentMaxWidth)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shutterModifier = Modifier
            .weight(1f)
            .heightIn(min = Sizes.primaryActionHeight)
            .testTag(LabelScanTestTags.SHUTTER)

        if (uiState.torchAvailable) {
            IconButton(
                onClick = onTorchToggled,
                modifier = Modifier
                    .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                    .testTag(LabelScanTestTags.TORCH),
            ) {
                Icon(
                    painter = painterResource(if (uiState.torchOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on),
                    contentDescription = stringResource(
                        if (uiState.torchOn) R.string.label_scan_torch_off else R.string.label_scan_torch_on,
                    ),
                    modifier = Modifier.size(Sizes.iconDefault),
                )
            }
        } else {
            Spacer(Modifier.width(Sizes.minTouchTarget))
        }

        // Tonal at first: the scan is meant to finish on its own. Filled once the hint says to use
        // it, which is the "gains emphasis" of design D3.
        if (uiState.hint == ScanHint.TRY_SHUTTER) {
            Button(onClick = onShutter, enabled = shutterEnabled, modifier = shutterModifier) {
                Text(stringResource(R.string.label_scan_shutter))
            }
        } else {
            FilledTonalButton(onClick = onShutter, enabled = shutterEnabled, modifier = shutterModifier) {
                Text(stringResource(R.string.label_scan_shutter))
            }
        }

        Spacer(Modifier.width(Sizes.minTouchTarget))
    }
}

/** The guide rectangle the recogniser reads, drawn where the cropper will cut. */
@Composable
private fun FramingGuide(guide: FrameCropper.Guide, modifier: Modifier = Modifier) {
    val colour = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.label_scan_instruction)
    Canvas(
        modifier
            .semantics { contentDescription = description }
            .testTag(LabelScanTestTags.GUIDE),
    ) {
        val left = size.width * guide.left
        val top = size.height * guide.top
        drawRoundRect(
            color = colour,
            topLeft = Offset(left, top),
            size = Size(size.width * (guide.right - guide.left), size.height * (guide.bottom - guide.top)),
            cornerRadius = CornerRadius(Spacing.lg.toPx()),
            style = Stroke(width = Sizes.stateStripeWidth.toPx()),
        )
    }
}

private fun LabelScanUiState.instructionRes(): Int = when {
    failed -> R.string.label_scan_failed
    finished -> R.string.label_scan_read
    !isReady -> R.string.label_scan_preparing
    hint == ScanHint.HOLD_STEADY -> R.string.label_scan_instruction
    hint == ScanHint.ADJUST -> R.string.label_scan_hint_adjust
    else -> R.string.label_scan_hint_shutter
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun LabelScanScreenPreview() {
    PillsnerTheme {
        LabelScanScreen(
            uiState = LabelScanUiState(isReady = true, torchAvailable = true),
            onShutter = {},
            onTorchToggled = {},
            onCancel = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LabelScanScreenShutterEmphasisPreview() {
    PillsnerTheme {
        LabelScanScreen(
            uiState = LabelScanUiState(isReady = true, hint = ScanHint.TRY_SHUTTER, hasReading = true),
            onShutter = {},
            onTorchToggled = {},
            onCancel = {},
        )
    }
}
