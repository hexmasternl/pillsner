package nl.hexmaster.pillsner.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 *
 * Every moment armed here is mirrored into [ArmedAlarmStore], so a reboot can put the alarm back
 * before the user has unlocked the phone (reminder-delivery-after-reboot design D3).
 */
open class ReminderAlarmScheduler(
    context: Context,
    private val armedAlarmStore: ArmedAlarmStore = ArmedAlarmStore(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

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
        scope.launch { armedAlarmStore.set(at) }
    }

    /** Removes the pending alarm; there is nothing left to wake up for. */
    open fun cancel() {
        alarmManager.cancel(wakeIntent())
        scope.launch { armedAlarmStore.clear() }
    }

    /** The moment this app last armed an alarm for, readable before the user unlocks the phone. */
    open suspend fun armedAt(): Instant? = armedAlarmStore.armedAt()

    /**
     * Puts back the alarm recorded before the reboot, without reading anything the user's
     * credentials protect (design D3).
     *
     * A moment that has already passed — the phone was off across it — is armed a few minutes out
     * instead, which is the soonest this process can usefully try again while still locked.
     */
    open suspend fun rearmStoredAlarm(now: Instant) {
        val stored = armedAt() ?: return
        scheduleAt(maxOf(stored, now.plus(LOCKED_RETRY)))
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

    companion object {
        private const val REQUEST_CODE = 1
        private const val INEXACT_WINDOW_MILLIS = 10L * 60L * 1000L

        /** How far out an alarm is put when the app cannot do anything with it yet. */
        val LOCKED_RETRY: Duration = Duration.ofMinutes(5)
    }
}
