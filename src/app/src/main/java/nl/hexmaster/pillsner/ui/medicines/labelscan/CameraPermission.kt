package nl.hexmaster.pillsner.ui.medicines.labelscan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * The camera permission, asked for in context and never at install, app start or on opening the
 * form (medicine-label-photo-prefill design D2). Follows the notification-permission precedent in
 * `ui/home/NotificationPermissionRequest.kt`: the view model decides when to ask, this answers
 * the platform questions and owns the launcher.
 */
object CameraPermission {

    const val PERMISSION: String = Manifest.permission.CAMERA

    /** Whether the app may open the camera right now. */
    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Whether the device has any camera at all; without one "Scan with camera" is not offered. */
    fun deviceHasCamera(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    /**
     * Whether the system will still show its prompt. False both before the first request and after
     * the user has chosen not to be asked again; [isPermanentlyDenied] tells the two apart by being
     * evaluated right after a denial.
     */
    fun shouldShowSystemRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)

    /** Right after a denial: the system prompt will not appear again, so only settings can help. */
    fun isPermanentlyDenied(activity: Activity): Boolean =
        !isGranted(activity) && !shouldShowSystemRationale(activity)

    /** The app's own page in system settings, where a permanently denied permission is re-enabled. */
    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * A launcher for the camera permission prompt.
 *
 * @param onResult called with whether it was granted and, when it was not, whether the system will
 *   refuse to ask again, so the caller can offer the settings page instead of a prompt that never
 *   comes.
 * @return a function that shows the system prompt.
 */
@Composable
fun rememberCameraPermissionRequest(onResult: (granted: Boolean, permanentlyDenied: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context.findActivity()
        currentOnResult(granted, !granted && activity != null && CameraPermission.isPermanentlyDenied(activity))
    }
    return { launcher.launch(CameraPermission.PERMISSION) }
}

/** The activity behind a Compose context, or null when there is none (previews, tests). */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
