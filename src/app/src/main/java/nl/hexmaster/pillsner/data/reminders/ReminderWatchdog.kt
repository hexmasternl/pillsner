package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.PillsnerApplication

/**
 * The net under the alarms (design D3).
 *
 * Every fifteen minutes — the platform's minimum period — this recomputes what the app should be
 * waiting for, puts back any alarm that is no longer armed, and runs an ordinary wake so that
 * anything already due is posted.
 *
 * **It is not how a reminder is delivered**, and the distinction matters: `reminder-scheduling`
 * forbids periodic work from being the delivery mechanism, and it is not one. On a healthy device
 * the alarms fire to the minute and this finds nothing to do. It exists for the case where the
 * platform has already broken its own promise, where the choice is a reminder up to fifteen minutes
 * late or no reminder at all.
 *
 * Nothing here logs a medicine or an amount, and the wake it runs is the same one every other route
 * runs, so it inherits the locked-user guard and the record-when-posted rule unchanged.
 */
class ReminderWatchdog(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PillsnerApplication).container

        // Nothing to watch, so nothing to do — and nothing to query either. A phone with no
        // medicines should not pay for this worker at all.
        val producesDoses = runCatching {
            container.medicationRepository.observeAll().first()
                .any { it.isActive && it.schedules.isNotEmpty() }
        }.getOrDefault(false)
        if (!producesDoses) return Result.success()

        Log.d(TAG, "Watchdog checking the alarms")
        // A wake ends by reconciling the alarm set, and reconciling re-arms everything it wants.
        // Since AlarmManager will not say what it currently holds, re-arming is the only thing that
        // can repair an alarm the platform dropped — so one ordinary wake is the whole repair.
        runCatching { container.reminderCoordinator.onWake(WakeReason.WATCHDOG) }
            .onFailure { return Result.retry() }

        return Result.success()
    }

    companion object {
        private const val TAG = "Reminders"
        private const val WORK_NAME = "reminder-watchdog"

        /**
         * Starts the watchdog, or leaves the running one exactly as it is.
         *
         * `KEEP` rather than `UPDATE`: restarting the period on every app start would mean a phone
         * the user opens often never completes an interval.
         */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWatchdog>(PERIOD_MINUTES, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** The platform's own minimum; asking for less is silently rounded up to it. */
        const val PERIOD_MINUTES = 15L
    }
}
