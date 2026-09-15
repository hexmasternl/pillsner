package nl.hexmaster.pillsner.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.hexmaster.pillsner.MainActivity
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import nl.hexmaster.pillsner.domain.scheduling.WakeSchedule

/**
 * The app's alarms: one per moment it has to act on (design D1, D2).
 *
 * This replaces the single alarm the app used to keep. One alarm for the earliest moment made the
 * schedule a chain, because the only place the next alarm was armed was inside the wake the current
 * one triggered; a deferred or dropped alarm therefore stopped every reminder after it. With one
 * alarm per moment, losing one costs one reminder.
 *
 * Reminder alarms go on [AlarmManager.setAlarmClock], the platform's alarm-clock tier. It is the
 * only tier exempt from Doze, from app-standby quotas *and* from the vendor power managers that
 * Samsung, Xiaomi, Oppo, OnePlus and Huawei ship — they dare not suppress it, because that would
 * break the clock app. The price is an alarm icon in the status bar, which is honest: the user has
 * set an alarm. Housekeeping stays on an allow-while-idle alarm, where a few minutes of drift costs
 * nothing and an icon would be noise.
 *
 * Exact alarms are what a medication reminder needs; when the platform will not grant them the app
 * falls back to a ten-minute window and [isExact] tells the Home screen to say so. That is a
 * degraded mode, not a design choice.
 *
 * Every moment armed here is mirrored into [ArmedAlarmStore], so a reboot can put the alarms back
 * before the phone is unlocked, and so the watchdog knows what it is looking for.
 */
open class ReminderAlarmScheduler(
    context: Context,
    private val armedAlarmStore: ArmedAlarmStore = ArmedAlarmStore(context),
) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    private val _isExact = MutableStateFlow(canScheduleExact())

    /** Whether reminders will fire at the minute, or within a ten-minute window. */
    val isExact: StateFlow<Boolean> = _isExact.asStateFlow()

    // The one alarm this class replaced is not removed by a reboot or by an update; the first
    // reconcile after the new version runs is what has to take it down.
    @Volatile
    private var legacyAlarmCancelled = false

    /**
     * Makes the armed alarms match [schedule]: cancels the ones no longer wanted, sets the ones
     * that are.
     *
     * Every wanted alarm is armed on every call rather than only the ones the record does not
     * mention. `AlarmManager` will not say what it currently holds, so the record is the only
     * account the app has of it, and a platform that dropped an alarm left the record untouched —
     * re-arming an alarm that is already set moves nothing, and is the only thing that can repair
     * one that is not. This is what makes the watchdog's job a plain call to this method.
     */
    open suspend fun reconcile(schedule: WakeSchedule) {
        val exact = canScheduleExact()
        _isExact.value = exact

        cancelLegacyAlarm()
        (armedAlarmStore.armed() - schedule).forEach { cancelAlarm(it) }
        schedule.forEach { setAlarm(it, exact) }
        armedAlarmStore.replace(schedule)
    }

    /** Removes every pending alarm; there is nothing left to wake up for. */
    open suspend fun cancel() = reconcile(emptySet())

    /** The alarms this app last armed, readable before the user unlocks the phone. */
    open suspend fun armed(): WakeSchedule = armedAlarmStore.armed()

    /**
     * Puts back the alarms recorded before this process could read anything (design D3).
     *
     * Used on a locked boot, where the medicines cannot be read at all, and by a wake that finds
     * the phone still locked. A moment the phone slept through is armed [atLeast] instead, which is
     * the soonest this process can usefully try again; the full wake at the first unlock is what
     * settles everything properly.
     */
    open suspend fun rearmStoredAlarms(atLeast: Instant) {
        val armed = armedAlarmStore.armed()
        if (armed.isEmpty()) return
        reconcile(armed.mapTo(mutableSetOf()) { WakeMoment(maxOf(it.at, atLeast), it.kind) })
    }

    /** Re-reads whether the platform currently allows exact alarms. */
    fun refreshExactness() {
        _isExact.value = canScheduleExact()
    }

    /**
     * Arms one alarm, at the tier its kind calls for.
     *
     * Open, and its counterpart below with it, so a test can watch what [reconcile] arms and
     * cancels. `AlarmManager` will not say what it holds, so there is nothing else to observe.
     */
    internal open fun setAlarm(moment: WakeMoment, exact: Boolean) {
        val operation = alarmIntent(moment)
        val at = moment.at.toEpochMilli()
        when {
            !exact -> alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, INEXACT_WINDOW_MILLIS, operation)
            moment.kind == WakeKind.REMINDER ->
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showAlarm()), operation)
            else -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    /** Takes one alarm down. */
    internal open fun cancelAlarm(moment: WakeMoment) {
        alarmManager.cancel(alarmIntent(moment))
    }

    private fun cancelLegacyAlarm() {
        if (legacyAlarmCancelled) return
        legacyAlarmCancelled = true
        alarmManager.cancel(wakeIntent(LEGACY_REQUEST_CODE))
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmIntent(moment: WakeMoment): PendingIntent = wakeIntent(requestCode(moment))

    private fun wakeIntent(requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        requestCode,
        Intent(appContext, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Where the user lands from the system's own next-alarm slot. */
    private fun showAlarm(): PendingIntent = PendingIntent.getActivity(
        appContext,
        SHOW_ALARM_REQUEST_CODE,
        Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val INEXACT_WINDOW_MILLIS = 10L * 60L * 1000L
        private const val SHOW_ALARM_REQUEST_CODE = 3

        /** The request code the single alarm used, kept only so it can be taken down. */
        private const val LEGACY_REQUEST_CODE = 1

        /** How far out an alarm is put when the app cannot do anything with it yet. */
        val LOCKED_RETRY: Duration = Duration.ofMinutes(5)

        /**
         * An alarm's identity is its moment and kind, not the dose behind it.
         *
         * Derived from the moment rather than the dose id so that the same request code can be
         * rebuilt from [ArmedAlarmStore] alone, which holds no dose ids and never will (design D8).
         * A locked boot therefore re-arms the very alarms a later reconcile can cancel, instead of
         * leaving duplicates behind. Two doses in the same second share one alarm, which is
         * correct: one wake posts both.
         */
        internal fun requestCode(moment: WakeMoment): Int =
            31 * moment.at.epochSecond.hashCode() + moment.kind.ordinal
    }
}
