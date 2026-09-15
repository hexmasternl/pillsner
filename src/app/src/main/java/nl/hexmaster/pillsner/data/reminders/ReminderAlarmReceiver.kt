package nl.hexmaster.pillsner.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.PillsnerApplication

/** One of the app's alarms going off: hand straight to the wake service (design D2, D4). */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        handOffWake(context, WakeReason.ALARM)
    }
}

/**
 * Starts the wake, in the foreground service where the platform allows it.
 *
 * The service is the whole point of D4: ten seconds of receiver budget is not enough for a cold
 * start that has to open the database. When the platform refuses the service — which it should not,
 * for an exact alarm or a boot broadcast, but which is its call — the work falls back into the
 * receiver's own budget, where the wake's retry is the backstop behind it.
 *
 * Before the first unlock after a reboot the service is skipped deliberately. A foreground service
 * has to show a notification, that notification needs its channel, and notification channels live
 * in credential-encrypted storage that cannot be read yet — so starting it there would fail on the
 * one path that has no user to see it fail. The locked wake is a couple of reads from
 * device-protected storage and re-arming the alarms, which fits inside the receiver's own budget
 * with room to spare (reminder-delivery-after-reboot design D4).
 */
internal fun BroadcastReceiver.handOffWake(context: Context, reason: WakeReason) {
    if (context.isUnlocked()) {
        if (ReminderWakeService.startWake(context, reason)) return
        context.deliveryLog().record(DeliveryEvent.SERVICE_REFUSED, reason.name)
    }

    // Null when the receiver is driven directly rather than by a real broadcast, which is how the
    // instrumented tests exercise it.
    val pendingResult: BroadcastReceiver.PendingResult? = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            context.reminderCoordinator().onWake(reason)
        } finally {
            pendingResult?.finish()
        }
    }
}

/**
 * The container a receiver works through. A receiver may be the first thing that starts the
 * process, which is why building the container has to stay cheap (design D10).
 */
internal fun Context.reminderCoordinator(): ReminderCoordinator =
    (applicationContext as PillsnerApplication).container.reminderCoordinator

/** The delivery log, so a receiver can say when the platform would not start the service. */
internal fun Context.deliveryLog(): ReminderDeliveryLog =
    (applicationContext as PillsnerApplication).container.reminderDeliveryLog

/** Whether the user has unlocked the phone since it booted; false in the direct-boot window. */
internal fun Context.isUnlocked(): Boolean =
    (applicationContext as PillsnerApplication).container.userUnlockState.isUnlocked()
