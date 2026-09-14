package nl.hexmaster.pillsner.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalContext

/**
 * Asks once for permission to show notifications, and keeps the answer up to date.
 *
 * Android 13 and later need the permission; before that it comes with the install. The dialog is
 * put in front of the user only the first time, because a reminder app that asks again on every
 * launch is a nuisance; from then on the Home banner says reminders cannot be delivered and points
 * at the system setting.
 *
 * The answer is re-read on every resume, so the banner disappears the moment the user comes back
 * from settings having turned notifications on.
 *
 * @param shouldRequest whether the dialog has yet to be shown for the first time.
 * @param onPermissionChanged called with whether the app may post notifications right now.
 * @param onRequested called once the dialog has actually been shown.
 */
@Composable
fun NotificationPermissionEffect(
    shouldRequest: Boolean,
    onPermissionChanged: (Boolean) -> Unit,
    onRequested: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionChanged,
    )

    LaunchedEffect(lifecycleOwner, shouldRequest) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val granted = context.hasNotificationPermission()
            onPermissionChanged(granted)
            // A missing permission can only happen from Android 13, which is where the dialog is.
            if (!granted && shouldRequest) {
                onRequested()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/** Whether the app may post notifications right now. */
fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Opens the page of system settings that fixes whichever problem the banner is reporting.
 *
 * The exact-alarm page exists only from Android 12; before that exact alarms need no permission at
 * all, so the banner never sends anyone there.
 */
fun Context.openReminderSettings(notificationsAllowed: Boolean) {
    val exactAlarmSettingsExist = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val intent = if (!notificationsAllowed || !exactAlarmSettingsExist) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.fromParts("package", packageName, null))
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
