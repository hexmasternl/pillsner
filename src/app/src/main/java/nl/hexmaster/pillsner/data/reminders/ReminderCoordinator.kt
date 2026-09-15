package nl.hexmaster.pillsner.data.reminders

import android.util.Log
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import nl.hexmaster.pillsner.data.wear.DoseSyncPublisher
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.silentlyMissedReminderAmong
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment

/** Why the app woke up. Only ever logged, never shown. */
enum class WakeReason {
    APP_START,
    APP_UPDATED,
    ALARM,
    BOOT,

    /** The device booted but the user has not unlocked it yet. */
    LOCKED_BOOT,

    /** The first unlock after a reboot, which is the first moment the database can be read. */
    USER_UNLOCKED,

    /** A second attempt at a wake that ran out of time (design D2). */
    RETRY,
    TIME_CHANGED,
    ACTION,
    MEDICATIONS_CHANGED,
    PERMISSION_CHANGED,

    /** The periodic check that the alarms the app expects are still armed (design D3). */
    WATCHDOG,
}

/**
 * Everything that has to happen when the app wakes (design D5).
 *
 * One entry point, one order: settle what has lapsed, bring the planning window up to date, show
 * what is due, then work out when to wake next. Every path through [onWake] ends with an alarm set,
 * so one bad wake can never leave the user without reminders.
 *
 * Nothing here logs a medicine's name or amount: dose ids only, at debug level.
 */
class ReminderCoordinator(
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val refreshPlannedDoses: RefreshPlannedDoses,
    private val markMissedDoses: MarkMissedDoses,
    private val dueDoses: DueDoses,
    private val computeWakeSchedule: ComputeWakeSchedule,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderAlarmScheduler,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    // Null when this build has no watch to talk to, which is the ordinary case on a phone
    // without Play services (app-wearable-support design D3).
    private val doseSyncPublisher: DoseSyncPublisher? = null,
    // True always on a device with no secure lock screen, where the locked branch never runs.
    private val unlockState: UserUnlockState = UserUnlockState { true },
    // Overridden only by the tests of the retry, which would otherwise wait nine real seconds.
    private val wakeTimeoutMillis: Long = WAKE_TIMEOUT_MILLIS,
    // Nothing to record on a build that has not wired the preferences yet, which is every test
    // that does not care about the banner.
    private val silentlyMissedReminders: SilentlyMissedReminders = SilentlyMissedReminders {},
) {

    // One wake at a time: an alarm and an answer from a notification can arrive in the same second.
    private val lock = Mutex()

    /**
     * How many wakes in a row have run out of time (design D2).
     *
     * In memory on purpose: a retry sequence lives inside one episode of trouble, and a fresh
     * process is a fresh attempt. Erring towards delivering the reminder is the right direction.
     */
    private var consecutiveTimeouts = 0

    /** Starts reacting to medicines being added, changed or deactivated. */
    fun start() {
        doseSyncPublisher?.start(scope)
        scope.launch {
            medicationRepository.observeAll().collect { onWake(WakeReason.MEDICATIONS_CHANGED) }
        }
    }

    /** Runs a wake on the coordinator's own scope, for callers that cannot suspend. */
    fun requestWake(reason: WakeReason) {
        scope.launch { onWake(reason) }
    }

    /**
     * The whole wake cycle. Bounded by [WAKE_TIMEOUT_MILLIS] so a broadcast receiver never runs
     * past the window the platform gives it.
     *
     * **Before the first unlock after a reboot this does almost nothing** (design D4). The
     * medicines, the doses and the settings all live in credential-encrypted storage and cannot be
     * read yet, so the locked branch re-arms the alarm and returns. Anything a later change adds to
     * the wake belongs inside [wake], below that guard, never above it.
     */
    suspend fun onWake(reason: WakeReason) {
        lock.withLock {
            if (!unlockState.isUnlocked()) {
                Log.d(TAG, "Wake for $reason deferred: the user has not unlocked yet")
                rearmWhileLocked()
                return@withLock
            }

            // The retry is an ordinary alarm, so only the coordinator knows it armed one. Saying so
            // in the log is what makes a run of timeouts recognisable rather than a run of alarms.
            val wakeReason =
                if (reason == WakeReason.ALARM && consecutiveTimeouts > 0) WakeReason.RETRY else reason

            when (runWakeBody(wakeReason)) {
                WakeOutcome.COMPLETED -> {
                    consecutiveTimeouts = 0
                    reconcileAlarms()
                }
                // A step throwing is not a reason to run the whole wake again: the steps that did
                // run have done their work, and the alarm set is computed from what is stored.
                WakeOutcome.FAILED -> reconcileAlarms()
                WakeOutcome.TIMED_OUT -> armRetryOrGiveUp()
            }

            // The wake has just settled what is still to be taken, so this is the moment the
            // watch should hear about it. It is also every app start, which is when a new
            // language takes effect, so the watch follows the phone without its own trigger.
            doseSyncPublisher?.publishNow()
        }
    }

    /** What became of one run of the wake body. Each outcome leaves a different alarm behind. */
    private enum class WakeOutcome { COMPLETED, TIMED_OUT, FAILED }

    private suspend fun runWakeBody(reason: WakeReason): WakeOutcome = try {
        withTimeout(wakeTimeoutMillis) { wake(reason) }
        WakeOutcome.COMPLETED
    } catch (timeout: TimeoutCancellationException) {
        Log.d(TAG, "Wake for $reason ran out of time")
        WakeOutcome.TIMED_OUT
    } catch (error: Exception) {
        Log.d(TAG, "Wake for $reason did not complete: ${error::class.simpleName}")
        WakeOutcome.FAILED
    }

    private suspend fun wake(reason: WakeReason) {
        val lapsed = markMissedDoses()
        lapsed.forEach { notifier.cancel(it) }
        recordSilentlyMissedReminders(lapsed)

        // Only a change the user made may withdraw a dose they have already been reminded about:
        // they have just said they no longer take it then. A clock or time-zone move must leave
        // such a dose exactly where it is.
        val withdrawn = refreshPlannedDoses(
            afterUserEdit = reason == WakeReason.MEDICATIONS_CHANGED,
        )
        // A withdrawn dose no longer exists, so a notification for it would offer answers that
        // resolve to nothing. Cancelling one that was never shown is a no-op, so there is no need
        // to ask first.
        withdrawn.forEach { notifier.cancel(it) }

        val due = dueDoses()
        val now = clock.instant()
        due.forEach { dose ->
            // A dose counts as reminded only once it has actually been announced (design D1). When
            // the post did not happen, its snooze stays where it is too: clearing it would take the
            // dose out of the due check as well, and the user would never hear about it.
            if (!notifier.show(dose, due.size)) return@forEach
            // A dose falling due starts the repeat sequence and a snooze running out restarts it;
            // only a posting that neither of those explains is the repeat rule asking again
            // (design D5).
            val countsAsRepeat = dose.firstRemindedAt != null && dose.snoozedUntil == null
            doseRepository.recordReminded(dose.id, now, countsAsRepeat)
            if (dose.snoozedUntil != null) {
                doseRepository.setSnooze(dose.id, null)
            }
        }
    }

    /**
     * Records the evidence the Home banner is raised by (design D2).
     *
     * The rule itself is [silentlyMissedReminderAmong], in the domain, where it can be read without
     * a device. The one thing it cannot work out for itself is whether a notification could have
     * been posted at all; [ReminderNotifier] is what already knows that, so it is what is asked.
     */
    private suspend fun recordSilentlyMissedReminders(lapsed: List<Dose>) {
        if (lapsed.none { it.wasMissedInSilence }) return
        val at = silentlyMissedReminderAmong(lapsed, notifier.notificationsAllowed()) ?: return
        Log.d(TAG, "A dose lapsed with no reminder ever having been posted for it")
        silentlyMissedReminders.record(at)
    }

    /**
     * The only thing the app can do before the first unlock: leave its alarms behind (design D4).
     *
     * An alarm has just fired that nothing can act on, so everything the app was waiting for goes
     * back, never sooner than a few minutes out. The full wake at the first unlock is what settles
     * the rest.
     */
    private suspend fun rearmWhileLocked() {
        scheduler.rearmStoredAlarms(clock.instant().plus(ReminderAlarmScheduler.LOCKED_RETRY))
    }

    /**
     * A wake that ran out of time knows nothing about what is due, so handing that world to
     * [ComputeWakeSchedule] would arm the dose's *lapse* moment and record it missed having never
     * been announced (design D2). Try again shortly instead — and once trying again has stopped
     * helping, fall back to the ordinary schedule, so an alarm is still set on this path too.
     *
     * The foreground service the wake now runs in makes a timeout rare. It does not make it
     * impossible — a service can be stopped too — so this stays as the backstop behind it.
     */
    private suspend fun armRetryOrGiveUp() {
        if (consecutiveTimeouts >= MAX_RETRIES) {
            consecutiveTimeouts = 0
            reconcileAlarms()
            return
        }
        consecutiveTimeouts++
        scheduler.reconcile(setOf(WakeMoment(clock.instant().plus(RETRY_DELAY), WakeKind.REMINDER)))
    }

    /**
     * Brings the armed alarms back in line with what the app now has to wake for.
     *
     * When even working out the schedule fails, the alarms already armed are left exactly as they
     * are. They are the app's last good answer, and a reconcile against a schedule that could not
     * be read would cancel every one of them over what may be a passing failure.
     */
    private suspend fun reconcileAlarms() {
        val schedule = runCatching { computeWakeSchedule() }.getOrNull() ?: return
        scheduler.reconcile(schedule)
    }

    companion object {
        private const val TAG = "Reminders"

        /** Under the ten seconds a broadcast receiver is allowed, with a second to spare. */
        const val WAKE_TIMEOUT_MILLIS = 9_000L

        /**
         * Long enough for the cold start that caused the timeout to have finished, short enough
         * that the reminder is still worth delivering.
         */
        val RETRY_DELAY: Duration = Duration.ofMinutes(2)

        /** A fourth consecutive timeout means something a fifth attempt will not fix. */
        const val MAX_RETRIES = 3
    }
}
