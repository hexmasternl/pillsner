package nl.hexmaster.pillsner.ui.medicines

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.LabelScanResult
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes

/** Test tags for semantics tests of the label-scan capture flow. */
object LabelScanTestTags {
    const val SCAN_FAB = "medicines_scan_fab"
}

/**
 * The "Scan medicine label" button and the capture flow it starts (medicine-label-scan spec,
 * design D4, D5): request the camera permission only at tap time, capture or pick a photo,
 * recognize it on-device, then hand the (possibly empty) result to [onScanResult].
 *
 * Deliberately not the whole [MedicinesScreen]: this is the one composable that owns Android
 * launchers and file I/O, so the screen itself stays a plain state-in, events-out composable.
 *
 * @param legalAccepted whether the current legal documents are accepted; a scan always leads to the
 *   same add-mode form the plain add button does, so it is gated the same way (medicine-add "Add
 *   medicine form fields").
 * @param onLegalRequired called instead of starting any capture when [legalAccepted] is false.
 * @param onScanLabel recognizes a decoded photo; the view model's `scanLabel` (or a fake in tests).
 * @param onScanResult called once recognition finishes, successfully or not, with the best-effort
 *   result. The caller decides what to do with it (navigate to a fresh Add medicine form).
 */
@Composable
fun LabelScanFab(
    legalAccepted: Boolean,
    onLegalRequired: () -> Unit,
    onScanLabel: suspend (Bitmap, Int) -> LabelScanResult,
    onScanResult: (LabelScanResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Gesture/IO state, not domain state: none of it survives process death, which is fine since a
    // half-finished scan simply has to be started again.
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    // A cancelled camera or picker call MUST NOT reach here at all (spec, "Cancelling leaves the
    // Medicines screen untouched"): no navigation, no message, nothing. Only an actual photo, or a
    // genuine decode/recognition failure on one, ever produces a result.
    suspend fun finish(uri: Uri) {
        isScanning = true
        val result = runCatching {
            decodeUprightBitmap(context, uri)?.let { (bitmap, rotation) -> onScanLabel(bitmap, rotation) }
        }.getOrNull() ?: LabelScanResult()
        isScanning = false
        onScanResult(result)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = pendingCapture
        pendingCapture = null
        if (!captured || file == null) {
            // Cancelled: nothing to recognize, and the spec forbids any navigation or message here.
            file?.delete()
            return@rememberLauncherForActivityResult
        }
        val uri = context.captureUri(file)
        scope.launch {
            // Deleted on every outcome, including a decode failure inside `finish` (task 3.2, and
            // medicine-label-scan spec "Photo is discarded after recognition"): this is the one
            // place the temporary file exists, and it never survives past this call.
            try {
                finish(uri)
            } finally {
                file.delete()
            }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // uri is null exactly when the user cancelled the picker without choosing a photo.
        if (uri != null) scope.launch { finish(uri) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera(context) { pendingCapture = it }.let(cameraLauncher::launch)
        } else {
            pickerLauncher.launchPicker()
        }
    }

    val description = if (isScanning) {
        stringResource(R.string.medicines_scan_in_progress)
    } else {
        stringResource(R.string.medicines_scan_content_description)
    }

    FloatingActionButton(
        onClick = {
            if (isScanning) return@FloatingActionButton
            if (!legalAccepted) {
                onLegalRequired()
                return@FloatingActionButton
            }
            when (decideLabelScanAction(hasCamera(context), context.hasCameraPermission())) {
                LabelScanAction.LAUNCH_CAMERA ->
                    launchCamera(context) { pendingCapture = it }.let(cameraLauncher::launch)
                LabelScanAction.REQUEST_PERMISSION -> permissionLauncher.launch(Manifest.permission.CAMERA)
                LabelScanAction.LAUNCH_PICKER -> pickerLauncher.launchPicker()
            }
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .testTag(LabelScanTestTags.SCAN_FAB)
            .semantics { contentDescription = description },
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                modifier = Modifier.size(Sizes.iconDefault),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
        }
    }
}

private fun androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>.launchPicker() {
    launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
}

/** Whether the app may use the camera right now. */
private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Whether the device has any camera at all (design D4); a phone with none always uses the picker. */
private fun hasCamera(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

/** What tapping the scan button should do, decided from state alone so it needs no Android launcher. */
internal enum class LabelScanAction { LAUNCH_CAMERA, REQUEST_PERMISSION, LAUNCH_PICKER }

/**
 * Pure decision at the heart of design D4: request permission only when a camera exists and
 * permission has not been granted yet; a device with no camera always falls back to the picker,
 * whatever the (irrelevant) permission state is.
 */
internal fun decideLabelScanAction(hasCamera: Boolean, hasCameraPermission: Boolean): LabelScanAction = when {
    !hasCamera -> LabelScanAction.LAUNCH_PICKER
    hasCameraPermission -> LabelScanAction.LAUNCH_CAMERA
    else -> LabelScanAction.REQUEST_PERMISSION
}

/** Creates a fresh temporary capture file and returns the [FileProvider] `Uri` the camera writes to. */
private fun launchCamera(context: Context, onFileCreated: (File) -> Unit): Uri {
    val dir = File(context.cacheDir, "label_scan").apply { mkdirs() }
    val file = File.createTempFile("label_scan_", ".jpg", dir)
    onFileCreated(file)
    return context.captureUri(file)
}

private fun Context.captureUri(file: File): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

/**
 * Decodes [uri] to an upright [Bitmap], reading its EXIF orientation first so a sideways camera
 * capture is recognized the right way up (design D5). Returns null when the photo cannot be
 * decoded at all, which the caller treats exactly like "nothing recognized".
 */
private fun decodeUprightBitmap(context: Context, uri: Uri): Pair<Bitmap, Int>? {
    val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { input ->
        ExifInterface(input).rotationDegrees
    } ?: 0
    val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
    } ?: return null
    return bitmap to rotationDegrees
}

@PreviewLightDark
@Composable
private fun LabelScanFabPreview() {
    PillsnerTheme {
        Surface {
            LabelScanFab(
                legalAccepted = true,
                onLegalRequired = {},
                onScanLabel = { _, _ -> LabelScanResult() },
                onScanResult = {},
            )
        }
    }
}
