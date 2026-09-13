package nl.hexmaster.pillsner.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's one alarm (design D5).
 *
 * There is deliberately a single alarm for "the next thing that has to happen" rather than one per
 * dose: Android throttles how often an app may wake an idle device, and one alarm means every
 * change — a new medicine, an answer, a time zone move — is handled by recomputing one moment.
 *
 * Exact alarms are what a medication reminder needs; when the platform will not grant them the app
 * falls back to a ten-minute window and [isExact] tells the Home screen to say so. That is a
 * degraded mode, not a design choice.
 */
open class ReminderAlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    private val _isExact = MutableStateFlow(canScheduleExact())

    /** Whether reminders will fire at the minute, or within a ten-minute window. */
    val isExact: StateFlow<Boolean> = _isExact.asStateFlow()

    /** Replaces the pending alarm with one at [at]. */
    open fun scheduleAt(at: Instant) {
        val exact = canScheduleExact()
        _isExact.value = exact
        val intent = wakeIntent()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                at.toEpochMilli(),
                INEXACT_WINDOW_MILLIS,
                intent,
            )
        }
    }

    /** Removes the pending alarm; there is nothing left to wake up for. */
    open fun cancel() {
        alarmManager.cancel(wakeIntent())
    }

    /** Re-reads whether the platform currently allows exact alarms. */
    fun refreshExactness() {
        _isExact.value = canScheduleExact()
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** One fixed request code, so setting a new alarm always replaces the previous one. */
    private fun wakeIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        REQUEST_CODE,
        Intent(appContext, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 1
        const val INEXACT_WINDOW_MILLIS = 10L * 60L * 1000L
    }
}
