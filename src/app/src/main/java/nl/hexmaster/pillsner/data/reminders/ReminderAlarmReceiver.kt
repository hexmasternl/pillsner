package nl.hexmaster.pillsner.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.PillsnerApplication

/** The app's one alarm going off: hand straight to the coordinator (design D5). */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val coordinator = context.reminderCoordinator()
        // Null when the receiver is driven directly rather than by a real broadcast, which is how
        // the instrumented tests exercise it.
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                coordinator.onWake(WakeReason.ALARM)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}

/**
 * The container a receiver works through. A receiver may be the first thing that starts the
 * process, which is why building the container has to stay cheap (design D10).
 */
internal fun Context.reminderCoordinator(): ReminderCoordinator =
    (applicationContext as PillsnerApplication).container.reminderCoordinator
