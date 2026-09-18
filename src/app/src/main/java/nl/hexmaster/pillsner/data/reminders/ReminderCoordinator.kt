package nl.hexmaster.pillsner.data.reminders

import android.os.SystemClock
import android.util.Log
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import nl.hexmaster.pillsner.data.wear.DoseSyncPublisher
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.ReminderOutcomeUpdate
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.PendingSnapshot
import nl.hexmaster.pillsner.domain.scheduling.PurgeExpiredDoseHistory
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.TrustedNow
import nl.hexmaster.pillsner.domain.scheduling.TrustedNowResult
import nl.hexmaster.pillsner.domain.scheduling.buildPendingSnapshot
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
    // Null in tests that do not care what was recorded; the real one lives in AppContainer.
    private val deliveryLog: ReminderDeliveryLog? = null,
    // Null in a build or test that has not wired dose history retention; the purge step is then a
    // no-op (dose-history-retention design D1).
    private val trustedClockStore: TrustedClockStore? = null,
    private val purgeExpiredDoseHistory: PurgeExpiredDoseHistory? = null,
    // Overridden only by tests that need to simulate a reboot or a span of elapsed time without
    // waiting for it; the real default reads the platform's own boot clock.
    private val bootElapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) {

    // One wake at a time: an alarm and an answer from a notification can arrive in the same second.
    private val lock = Mutex()

    /**
     * How many wakes in a row have timed out (design D2).
     *
     * An exception is not a timeout (`reminder-scheduling` "A wake that does not complete is
     * retried"): a step throwing still lets the wake settle what it could and reconcile the alarm
     * set normally, so only running out of the time budget counts here.
     *
     * In memory on purpose: a retry sequence lives inside one episode of trouble, and a fresh
     * process is a fresh attempt. Erring towards delivering the reminder is the right direction.
     */
    private var consecutiveFailures = 0

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
     * The whole wake cycle, bounded by [timeoutMillis] so it never runs past the window the platform
     * gives whoever called it: about ten seconds in a broadcast receiver, minutes in the foreground
     * service. The budget is the caller's to state, because the same wake serves both and a wake
     * cancelled at nine seconds inside a service that had three minutes is a reminder thrown away.
     *
     * **Before the first unlock after a reboot this does almost nothing** (design D4). The
     * medicines, the doses and the settings all live in credential-encrypted storage and cannot be
     * read yet, so the locked branch re-arms the alarm and returns. Anything a later change adds to
     * the wake belongs inside [wake], below that guard, never above it.
     */
    suspend fun onWake(reason: WakeReason, timeoutMillis: Long = wakeTimeoutMillis) {
        lock.withLock {
            if (!unlockState.isUnlocked()) {
                Log.d(TAG, "Wake for $reason deferred: the user has not unlocked yet")
                deliveryLog?.record(DeliveryEvent.WAKE_DEFERRED, reason.name)
                rearmWhileLocked()
                return@withLock
            }

            // The retry is an ordinary alarm, so only the coordinator knows it armed one. Saying so
            // in the log is what makes a run of timeouts recognisable rather than a run of alarms.
            val wakeReason =
                if (reason == WakeReason.ALARM && consecutiveFailures > 0) WakeReason.RETRY else reason
            deliveryLog?.record(DeliveryEvent.WAKE, wakeReason.name)

            when (val outcome = runWakeBody(wakeReason, timeoutMillis)) {
                is WakeOutcome.Completed -> {
                    consecutiveFailures = 0
                    reconcileAlarms(outcome.result)
                }
                // A wake that threw is not a timeout (`reminder-scheduling` "An exception is not a
                // timeout"): every step is idempotent, so the alarm set is still reconciled
                // normally from what is stored rather than treated as a reason to retry.
WakeOutcome.Failed -> {
    consecutiveFailures = 0
    reconcileAlarms()
}
                // A wake that ran out of its time budget has not announced what was due, so it is
                // retried shortly instead of being treated as complete.
                WakeOutcome.TimedOut -> armRetryOrGiveUp()
            }

            // The wake has just settled what is still to be taken, so this is the moment the
            // watch should hear about it. It is also every app start, which is when a new
            // language takes effect, so the watch follows the phone without its own trigger.
            doseSyncPublisher?.publishNow()
        }
    }

    /** What a completed wake found, carried forward to [reconcileAlarms] without re-querying it. */
    private class WakeResult(val snapshot: PendingSnapshot, val medications: List<Medication>)

    /** What became of one run of the wake body. Each outcome leaves a different alarm behind. */
    private sealed class WakeOutcome {
        data class Completed(val result: WakeResult) : WakeOutcome()
        data object TimedOut : WakeOutcome()
        data object Failed : WakeOutcome()
    }

    private suspend fun runWakeBody(reason: WakeReason, timeoutMillis: Long): WakeOutcome = try {
        WakeOutcome.Completed(withTimeout(timeoutMillis) { wake(reason) })
    } catch (timeout: TimeoutCancellationException) {
        Log.d(TAG, "Wake for $reason ran out of time")
        deliveryLog?.record(DeliveryEvent.TIMED_OUT, reason.name)
        WakeOutcome.TimedOut
    } catch (error: Exception) {
        Log.d(TAG, "Wake for $reason did not complete: ${error::class.simpleName}")
        deliveryLog?.record(DeliveryEvent.FAILED, "${reason.name} ${error::class.simpleName}")
        WakeOutcome.Failed
    }

    private suspend fun wake(reason: WakeReason): WakeResult {
        val lapsed = markMissedDoses()
        lapsed.forEach { dose ->
            notifier.cancel(dose)
            deliveryLog?.record(
                if (dose.wasMissedInSilence) DeliveryEvent.LAPSED_UNANNOUNCED else DeliveryEvent.LAPSED,
                dose.id.value.toString(),
            )
        }
        recordSilentlyMissedReminders(lapsed)

        // Only a change the user made may withdraw a dose they have already been reminded about:
        // they have just said they no longer take it then. A clock or time-zone move must leave
        // such a dose exactly where it is.
        val refreshResult = refreshPlannedDoses(
            afterUserEdit = reason == WakeReason.MEDICATIONS_CHANGED,
        )
        // A withdrawn dose no longer exists, so a notification for it would offer answers that
        // resolve to nothing. Cancelling one that was never shown is a no-op, so there is no need
        // to ask first.
        refreshResult.withdrawn.forEach { notifier.cancel(it) }

        // Built once, after the refresh has settled what is pending, so it reflects every insert
        // and withdrawal the refresh just made; shared by dueDoses below and, once updated to
        // reflect the postings just below, by computeWakeSchedule in reconcileAlarms
        // (reminder-wake-cycle-db-efficiency design D1).
        val snapshot = buildPendingSnapshot(doseRepository, markMissedDoses)
        val due = dueDoses(snapshot)
        val now = clock.instant()
        val outcomeUpdates = mutableListOf<ReminderOutcomeUpdate>()
        val updatedDoses = snapshot.doses.associateByTo(LinkedHashMap()) { it.id }
        due.forEach { dose ->
            // A dose counts as reminded only once it has actually been announced (design D1). When
            // the post did not happen, its snooze stays where it is too: clearing it would take the
            // dose out of the due check as well, and the user would never hear about it.
            if (!notifier.show(dose, due.size)) {
                deliveryLog?.record(DeliveryEvent.POST_REFUSED, dose.id.value.toString())
                return@forEach
            }
            deliveryLog?.record(DeliveryEvent.POSTED, dose.id.value.toString())
            // A dose falling due starts the repeat sequence and a snooze running out restarts it;
            // only a posting that neither of those explains is the repeat rule asking again
            // (design D5).
            val countsAsRepeat = dose.firstRemindedAt != null && dose.snoozedUntil == null
            val clearsSnooze = dose.snoozedUntil != null
            outcomeUpdates += ReminderOutcomeUpdate(dose.id, now, countsAsRepeat, clearsSnooze)

            // Mirrors exactly what applyReminderOutcomes is about to write, so the snapshot handed
            // to computeWakeSchedule below sees this dose's post-reminder state rather than the
            // moment before it was announced.
            var updated = dose.copy(
                firstRemindedAt = dose.firstRemindedAt ?: now,
                lastRemindedAt = now,
                reminderCount = dose.reminderCount + if (countsAsRepeat) 1 else 0,
            )
            if (clearsSnooze) {
                updated = updated.copy(snoozedUntil = null, reminderCount = 0)
            }
            updatedDoses[dose.id] = updated
        }
        if (outcomeUpdates.isNotEmpty()) {
            doseRepository.applyReminderOutcomes(outcomeUpdates)
        }

        // Housekeeping only, and never on the path a due reminder's timely posting depends on, so
        // it runs last and never lets a failure of its own affect anything above.
        runDoseHistoryPurge()

        return WakeResult(snapshot.copy(doses = updatedDoses.values.toList()), refreshResult.medications)
    }

    /**
     * The dose history retention purge, guarded by the trusted-now high-water mark rather than the
     * raw wall clock (dose-history-retention design D1, D3).
     *
     * Deliberately tolerant of its own failure, unlike the steps above: purging old history is
     * never allowed to cost the user a reminder, so any exception here is logged and left for the
     * next wake to try again, exactly as an ordinary day with nothing to purge would look.
     */
    private suspend fun runDoseHistoryPurge() {
        val store = trustedClockStore ?: return
        val purge = purgeExpiredDoseHistory ?: return
        try {
            val previous = store.read()
            when (val result = TrustedNow(previous, clock.instant(), bootElapsedRealtimeMillis())) {
                is TrustedNowResult.Reseed -> store.write(result.newSample)
                is TrustedNowResult.Advanced -> {
                    store.write(result.newSample)
                    val purged = purge(result.trustedNow)
                    if (purged > 0) Log.d(TAG, "Dose history purge removed $purged dose(s)")
                }
            }
        } catch (error: Exception) {
            Log.d(TAG, "Dose history purge did not complete: ${error::class.simpleName}")
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
     * A wake that did not complete knows nothing reliable about what is due, so handing that world
     * to [ComputeWakeSchedule] alone would arm the dose's *lapse* moment and record it missed having
     * never been announced (design D2). Try again shortly instead — and once trying again has
     * stopped helping, fall back to the ordinary schedule, so an alarm is still set on this path
     * too. That schedule now carries its own bounded retry for a due dose that was never announced,
     * so even the give-up path does not leave such a dose to its lapse.
     *
     * The foreground service the wake now runs in makes a timeout rare. It does not make it
     * impossible — a service can be stopped too — so this stays as the backstop behind it.
     */
    private suspend fun armRetryOrGiveUp() {
        if (consecutiveFailures >= MAX_RETRIES) {
            consecutiveFailures = 0
            deliveryLog?.record(DeliveryEvent.GAVE_UP)
            reconcileAlarms()
            return
        }
        consecutiveFailures++
        deliveryLog?.record(DeliveryEvent.RETRY_ARMED)
        scheduler.reconcile(setOf(WakeMoment(clock.instant().plus(RETRY_DELAY), WakeKind.REMINDER)))
    }

    /**
     * Brings the armed alarms back in line with what the app now has to wake for.
     *
     * [wakeResult] is what a wake that just completed found, reused here instead of read again. A
     * wake that gave up after repeated failures has no such result to reuse — [armRetryOrGiveUp]
     * calls this with none, and a fresh snapshot and medication list are read for it, exactly as
     * this whole method always did before the wake-cycle snapshot was introduced.
     *
     * When even working out the schedule fails, the alarms already armed are left exactly as they
     * are. They are the app's last good answer, and a reconcile against a schedule that could not
     * be read would cancel every one of them over what may be a passing failure.
     */
    private suspend fun reconcileAlarms(wakeResult: WakeResult? = null) {
        val schedule = runCatching {
            val snapshot = wakeResult?.snapshot ?: buildPendingSnapshot(doseRepository, markMissedDoses)
            val medications = wakeResult?.medications ?: medicationRepository.observeAll().first()
            computeWakeSchedule(snapshot, medications)
        }.getOrNull()
        if (schedule == null) {
            deliveryLog?.record(DeliveryEvent.ALARMS_LEFT_AS_IS)
            return
        }
        scheduler.reconcile(schedule)
        deliveryLog?.record(
            DeliveryEvent.ALARMS_ARMED,
            "${schedule.size} next=${schedule.minOfOrNull { it.at } ?: "none"}",
        )
    }

    companion object {
        private const val TAG = "Reminders"

        /**
         * The budget when the wake runs inside a broadcast receiver: under the ten seconds the
         * platform allows one, with a second to spare. This is the default, because it is the one
         * a caller that has not thought about its budget had better get.
         */
        const val WAKE_TIMEOUT_MILLIS = 9_000L

        /**
         * The budget when the wake runs inside [ReminderWakeService]. A short foreground service is
         * allowed about three minutes; a minute is enough for the slowest cold start a phone under
         * memory pressure produces, and cancelling a wake that would have finished at eleven seconds
         * was the one way the service itself could lose a reminder.
         */
        const val SERVICE_WAKE_TIMEOUT_MILLIS = 60_000L

        /**
         * Long enough for the cold start that caused the timeout to have finished, short enough
         * that the reminder is still worth delivering.
         */
        val RETRY_DELAY: Duration = Duration.ofMinutes(2)

        /** A fourth consecutive failure means something a fifth attempt will not fix. */
        const val MAX_RETRIES = 3
    }
}
