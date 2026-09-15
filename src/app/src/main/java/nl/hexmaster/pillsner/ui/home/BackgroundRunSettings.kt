package nl.hexmaster.pillsner.ui.home

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Asks once to be left out of battery optimisation, and keeps the answer up to date (design D6).
 *
 * The exemption is requested, never required: every path through the app works without it, and the
 * Home banner is what the user sees when it is absent. It is asked for at the first moment there is
 * something to protect — an upcoming dose — rather than at first launch, where a system dialog
 * about battery would arrive before the user has said what they take.
 *
 * The platform has no callback for the exemption and the dialog's result never reaches the app, so
 * the answer is re-read on every resume, exactly as the notification permission is.
 *
 * @param shouldRequest whether the dialog has yet to be shown for the first time.
 * @param onExemptionChanged called with whether Pillsner is currently exempt.
 * @param onRequested called once the dialog has actually been shown.
 */
@Composable
fun BatteryOptimisationEffect(
    shouldRequest: Boolean,
    onExemptionChanged: (Boolean) -> Unit,
    onRequested: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, shouldRequest) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val exempt = context.isIgnoringBatteryOptimisations()
            onExemptionChanged(exempt)
            if (!exempt && shouldRequest) {
                onRequested()
                context.requestBatteryExemption()
            }
        }
    }
}

/** Whether the platform will let Pillsner run when its alarm goes off. */
fun Context.isIgnoringBatteryOptimisations(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

/**
 * The system dialog that grants the exemption in one tap.
 *
 * Lint flags this permission because most apps have no business holding it. Pillsner's core
 * function is exact-time alarms and it has no other way to deliver them, which is the listed
 * acceptable use; the declaration text for Play is recorded in the change's proposal.
 */
@SuppressLint("BatteryLife")
private fun Context.requestBatteryExemption() {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.fromParts("package", packageName, null))
    if (resolves(intent)) start(intent) else start(backgroundRunIntent())
}

/**
 * Where to send a user whose phone is stopping Pillsner from running in the background.
 *
 * On most phones that is the platform's own battery-optimisation list. Samsung, Xiaomi, Oppo,
 * OnePlus, Huawei, Vivo and their relatives ship a second power manager on top of it, with its own
 * screen, and being exempt in the platform's list means nothing until the app is allowed there too.
 * Those screens are undocumented and move between versions, so each one is offered only while it
 * still resolves — which needs the `<queries>` entry in the manifest — and the platform setting is
 * always the fallback. The table is a convenience; nothing depends on it.
 */
internal fun Context.backgroundRunIntent(): Intent =
    vendorAutoStartIntents().firstOrNull { resolves(it) }
        ?: Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

private fun vendorAutoStartIntents(): List<Intent> = when (Build.MANUFACTURER.lowercase()) {
    "xiaomi", "redmi", "poco" -> listOf(
        component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    )

    "oppo", "realme" -> listOf(
        component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
    )

    "vivo", "iqoo" -> listOf(
        component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    )

    "huawei", "honor" -> listOf(
        component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    )

    "oneplus" -> listOf(
        component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
    )

    "samsung" -> listOf(
        component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
    )

    else -> emptyList()
}

private fun component(packageName: String, className: String): Intent =
    Intent().setComponent(ComponentName(packageName, className))

private fun Context.start(intent: Intent) {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** True when something on this device will actually open [intent]. */
private fun Context.resolves(intent: Intent): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L)) != null
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, 0) != null
    }
