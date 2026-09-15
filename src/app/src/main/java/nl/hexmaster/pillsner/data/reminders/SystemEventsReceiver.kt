package nl.hexmaster.pillsner.data.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.PillsnerApplication

/**
 * The platform events that invalidate the app's one alarm (design D10).
 *
 * A reboot clears every alarm the app had set; an app update does the same. Setting the clock or
 * moving time zone changes which instant a wall-clock rule means, and the refresh inside the wake
 * cycle is what re-anchors the planned doses to the new clock. Each of these is handled the same
 * way: wake, recompute, reschedule.
 *
 * A reboot is the one case that is not one event but two (reminder-delivery-after-reboot design
 * D5). `LOCKED_BOOT_COMPLETED` arrives while the phone is still locked, when nothing the app owns
 * can be read and the only useful thing to do is put the recorded alarm back. `ACTION_USER_UNLOCKED`
 * arrives at the first unlock and runs the full wake, which catches up on everything the locked
 * stretch accumulated. `BOOT_COMPLETED` stays because on a device with no secure lock screen it is
 * the one that arrives.
 *
 * `RECEIVE_BOOT_COMPLETED`, already declared, covers the locked variant too; no new permission is
 * needed for any of this.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> WakeReason.BOOT
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> WakeReason.LOCKED_BOOT
            Intent.ACTION_USER_UNLOCKED -> WakeReason.USER_UNLOCKED
            Intent.ACTION_MY_PACKAGE_REPLACED -> WakeReason.APP_UPDATED
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> WakeReason.TIME_CHANGED
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> WakeReason.PERMISSION_CHANGED
            else -> return
        }

        val application = context.applicationContext as PillsnerApplication
        val container = application.container
        if (reason == WakeReason.PERMISSION_CHANGED) container.reminderAlarmScheduler.refreshExactness()
        // The process may have been started while the phone was still locked, in which case
        // everything that needs the database is waiting for exactly this moment.
        if (reason == WakeReason.USER_UNLOCKED) application.startWhenUnlocked()

        // Null when the receiver is driven directly rather than by a real broadcast, which is how
        // the instrumented tests exercise it.
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (reason == WakeReason.LOCKED_BOOT) {
                    // No wake and no database: only the moment recorded in device-protected
                    // storage, put back so the phone has an alarm before it is first unlocked.
                    container.reminderAlarmScheduler.rearmStoredAlarm(Clock.systemUTC().instant())
                } else {
                    container.reminderCoordinator.onWake(reason)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
