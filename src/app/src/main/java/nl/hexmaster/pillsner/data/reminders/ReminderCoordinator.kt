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
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.scheduling.ComputeNextWake
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses

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
    private val computeNextWake: ComputeNextWake,
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
                    rescheduleNextWake()
                }
                // A step throwing is not a reason to run the whole wake again: the steps that did
                // run have done their work, and the next alarm is computed from what is stored.
                WakeOutcome.FAILED -> rescheduleNextWake()
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
        markMissedDoses().forEach { notifier.cancel(it) }

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
        due.forEach { dose ->
            // A dose counts as reminded only once it has actually been announced (design D1). When
            // the post did not happen, its snooze stays where it is too: clearing it would take the
            // dose out of the due check as well, and the user would never hear about it.
            if (!notifier.show(dose, due.size)) return@forEach
            if (dose.firstRemindedAt == null) {
                doseRepository.setFirstReminded(dose.id, clock.instant())
            }
            if (dose.snoozedUntil != null) {
                doseRepository.setSnooze(dose.id, null)
            }
        }
    }

    /**
     * The only thing the app can do before the first unlock: leave an alarm behind (design D4).
     *
     * An alarm has just fired that nothing can act on, so it goes back a few minutes out. The
     * recorded moment wins when it is still further away than that — the phone may simply have
     * booted, in which case the real work is still ahead.
     */
    private suspend fun rearmWhileLocked() {
        val soonest = clock.instant().plus(ReminderAlarmScheduler.LOCKED_RETRY)
        val stored = scheduler.armedAt()
        scheduler.scheduleAt(if (stored != null && stored.isAfter(soonest)) stored else soonest)
    }

    /**
     * A wake that ran out of time knows nothing about what is due, so handing that world to
     * [ComputeNextWake] would arm the dose's *lapse* moment and record it missed having never been
     * announced (design D2). Try again shortly instead — and once trying again has stopped helping,
     * fall back to the ordinary schedule, so an alarm is still set on this path too.
     */
    private suspend fun armRetryOrGiveUp() {
        if (consecutiveTimeouts >= MAX_RETRIES) {
            consecutiveTimeouts = 0
            rescheduleNextWake()
            return
        }
        consecutiveTimeouts++
        scheduler.scheduleAt(clock.instant().plus(RETRY_DELAY))
    }

    private suspend fun rescheduleNextWake() {
        val next = runCatching { computeNextWake() }.getOrNull()
        if (next == null) scheduler.cancel() else scheduler.scheduleAt(next)
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
