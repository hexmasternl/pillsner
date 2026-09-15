package nl.hexmaster.pillsner.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Where to send a user whose phone is stopping Pillsner from running in the background (design D1,
 * D3).
 *
 * Nothing here asks for anything. The app used to open the battery-optimisation dialog by itself,
 * and no longer does: the exemption buys a reminder nothing it does not already have, and a system
 * prompt the user did not ask for is a poor trade for that. What is left is the route — offered
 * only once a reminder has actually gone missing, behind the Home banner's button.
 *
 * On most phones the destination is the platform's own battery-optimisation list, which needs no
 * permission to open. Samsung, Xiaomi, Oppo, OnePlus, Huawei, Vivo and their relatives ship a
 * second power manager on top of it, with its own screen, and being exempt in the platform's list
 * means nothing until the app is allowed there too. Those screens are undocumented and move between
 * versions, so each one is offered only while it still resolves — which needs the `<queries>` entry
 * in the manifest — and the platform setting is always the fallback. The table is a convenience;
 * nothing depends on it.
 *
 * The choice never consults [isIgnoringBatteryOptimisations]: a user who is already exempt and is
 * still missing reminders is exactly the user the vendor screen is for.
 */
internal fun Context.backgroundRunIntent(): Intent =
    vendorAutoStartIntents().firstOrNull { resolves(it) }
        ?: Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

/**
 * Whether the platform will let Pillsner run freely when its alarm goes off.
 *
 * Reading this is silent and costs nothing, but it decides nothing the user sees: the banner is
 * raised by a reminder that was actually missed, never by this answer (design D2).
 */
fun Context.isIgnoringBatteryOptimisations(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

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

/** True when something on this device will actually open [intent]. */
private fun Context.resolves(intent: Intent): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L)) != null
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, 0) != null
    }
