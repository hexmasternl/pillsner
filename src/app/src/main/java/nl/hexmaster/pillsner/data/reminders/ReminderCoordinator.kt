package nl.hexmaster.pillsner.data.reminders

import android.util.Log
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.scheduling.ComputeNextWake
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.data.wear.DoseSyncPublisher
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses

/** Why the app woke up. Only ever logged, never shown. */
enum class WakeReason {
    APP_START,
    APP_UPDATED,
    ALARM,
    BOOT,
    TIME_CHANGED,
    ACTION,
    MEDICATIONS_CHANGED,
    PERMISSION_CHANGED,
}

/**
 * Everything that has to happen when the app wakes (design D5).
 *
 * One entry point, one order: settle what has lapsed, bring the planning window up to date, show
 * what is due, then work out when to wake next. The last step runs in a `finally`, so even a
 * failure earlier on can never leave the app without an alarm and the user without reminders.
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
) {

    // One wake at a time: an alarm and an answer from a notification can arrive in the same second.
    private val lock = Mutex()

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
     */
    suspend fun onWake(reason: WakeReason) {
        lock.withLock {
            try {
                withTimeout(WAKE_TIMEOUT_MILLIS) {
                    markMissedDoses().forEach { notifier.cancel(it) }

                    refreshPlannedDoses()

                    val due = dueDoses()
                    due.forEach { dose ->
                        notifier.show(dose, due.size)
                        if (dose.firstRemindedAt == null) {
                            doseRepository.setFirstReminded(dose.id, clock.instant())
                        }
                        if (dose.snoozedUntil != null) {
                            doseRepository.setSnooze(dose.id, null)
                        }
                    }
                }
            } catch (error: Exception) {
                // The next alarm is still set below, so one bad wake is always recoverable.
                Log.d(TAG, "Wake for $reason did not complete: ${error::class.simpleName}")
            } finally {
                rescheduleNextWake()
                // The wake has just settled what is still to be taken, so this is the moment the
                // watch should hear about it. It is also every app start, which is when a new
                // language takes effect, so the watch follows the phone without its own trigger.
                doseSyncPublisher?.publishNow()
            }
        }
    }

    private suspend fun rescheduleNextWake() {
        val next = runCatching { computeNextWake() }.getOrNull()
        if (next == null) scheduler.cancel() else scheduler.scheduleAt(next)
    }

    private companion object {
        const val TAG = "Reminders"

        /** Under the ten seconds a broadcast receiver is allowed, with a second to spare. */
        const val WAKE_TIMEOUT_MILLIS = 9_000L
    }
}
