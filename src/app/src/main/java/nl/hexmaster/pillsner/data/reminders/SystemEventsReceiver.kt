package nl.hexmaster.pillsner.data.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> WakeReason.BOOT
            Intent.ACTION_MY_PACKAGE_REPLACED -> WakeReason.APP_UPDATED
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> WakeReason.TIME_CHANGED
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> WakeReason.PERMISSION_CHANGED
            else -> return
        }

        val container = (context.applicationContext as PillsnerApplication).container
        if (reason == WakeReason.PERMISSION_CHANGED) container.reminderAlarmScheduler.refreshExactness()

        // Null when the receiver is driven directly rather than by a real broadcast, which is how
        // the instrumented tests exercise it.
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.reminderCoordinator.onWake(reason)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
