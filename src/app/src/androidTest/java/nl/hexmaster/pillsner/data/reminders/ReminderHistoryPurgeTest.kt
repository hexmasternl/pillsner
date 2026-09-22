package nl.hexmaster.pillsner.data.reminders

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.PurgeExpiredDoseHistory
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.TrustedNow
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import nl.hexmaster.pillsner.domain.scheduling.WakeSchedule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The dose-history purge step of the wake cycle (spec: dose-history-retention).
 *
 * Coverage the earlier, abandoned attempt at this feature (PR #41) was missing: a first wake with
 * no persisted trusted-clock sample, a later wake that actually purges, a purge failure that must
 * not block the rest of the wake, and a wake that times out while the purge is running still being
 * retried rather than falsely reported complete.
 *
 * Also covers the gap PR #50's review found in this PR's own first draft: a genuinely first-ever
 * observation, with no prior sample *and* no existing dose history, must still skip the purge
 * (nothing to clamp a tampered seed against), but one with existing dose history to draw a floor
 * from must clamp a tampered seed and be trusted immediately, rather than only "eventually" via a
 * small elapsed-time delta added on top of an already-wrong baseline.
 */
@RunWith(AndroidJUnit4::class)
class ReminderHistoryPurgeTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val today: LocalDate = LocalDate.of(2026, 9, 20)
    private val now: Instant = ZonedDateTime.of(today, LocalTime.of(9, 0), zone).toInstant()
    private val store = TrustedClockStore(context)

    @After
    fun tearDown() = runBlocking { store.clear() }

    private fun oldAnsweredDose(id: Long, scheduledAt: Instant, plannedAt: Instant = scheduledAt) = Dose(
        id = DoseId(id),
        medicationId = null,
        medicationName = "Ibuprofen",
        amount = Quantity.of("40", DoseUnit.MILLIGRAM),
        scheduledAt = scheduledAt,
        plannedAt = plannedAt,
        intake = Intake(IntakeOutcome.TAKEN, scheduledAt.plusSeconds(60)),
    )

    @Test
    fun aFirstWakeWithNoDoseHistoryAtAll_seedsTheClockAndSkipsThePurgeButStillCompletes() = runBlocking {
        // Nothing stored yet at all, so there is neither a prior sample nor any dose history to
        // draw a floor from -- the one case where skipping the purge is genuinely the only safe
        // option, and also genuinely harmless: there is nothing yet a wrong seed could delete.
        val doses = InMemoryDoseRepository()
        val scheduler = RecordingScheduler()

        coordinator(doses, scheduler).onWake(WakeReason.APP_START)

        assertTrue("Nothing was validated yet, so nothing should have been purged", doses.all().isEmpty())
        assertNotNull("The guard should have seeded a sample for next time", store.read())
    }

    @Test
    fun aFirstWakeWithExistingDoseHistory_clampsATamperedSeedAndPurgesImmediately() = runBlocking {
        // Reproduces the gap PR #50's review found: a device whose wall clock is already tampered
        // far into the future on the very first observation this install ever makes (no prior
        // trusted-clock sample), but which already has real dose history from before the tamper.
        // That history is exactly the independent evidence the fix clamps the seed against, so the
        // purge must run correctly on this very first wake rather than only "eventually".
        val recentReal = now.minusSeconds(3_600)
        val old = now.minusSeconds(400L * 86_400)
        val doses = InMemoryDoseRepository(
            listOf(oldAnsweredDose(1, old, plannedAt = old), oldAnsweredDose(2, now, plannedAt = recentReal)),
        )
        val tamperedGuard = TrustedClockGuard(
            store,
            wallClockMillis = { TAMPERED_FUTURE_MILLIS },
            elapsedRealtimeMillis = { android.os.SystemClock.elapsedRealtime() },
        )

        coordinator(doses, RecordingScheduler(), trustedClockGuard = tamperedGuard).onWake(WakeReason.APP_START)

        assertEquals(
            "The old dose is purged immediately, clamped by the recent dose's own plannedAt floor",
            listOf(now),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun aLaterWakeWithAValidatedTrustedNow_purgesExpiredHistory() = runBlocking {
        seedValidatedSample()
        val old = now.minusSeconds(400L * 86_400)
        val recent = now.minusSeconds(86_400)
        val doses = InMemoryDoseRepository(listOf(oldAnsweredDose(1, old), oldAnsweredDose(2, recent)))

        coordinator(doses, RecordingScheduler()).onWake(WakeReason.APP_START)

        assertEquals(listOf(recent), doses.all().map { it.scheduledAt })
    }

    @Test
    fun aPurgeFailure_doesNotBlockReminderWorkOrAlarmReconciliation() = runBlocking {
        seedValidatedSample()
        val doses = ThrowingHistoryDoseRepository(InMemoryDoseRepository())
        val medications = InMemoryMedicationRepository()
        val scheduler = RecordingScheduler()

        // Should not throw, and should still reconcile alarms normally (an empty schedule here,
        // since there is nothing due and no medication) rather than leaving them untouched.
        coordinator(doses, scheduler, medications).onWake(WakeReason.APP_START)

        assertTrue("A purge failure must not leave stale alarms behind either", scheduler.schedule.isEmpty())
    }

    @Test
    fun aWakeThatTimesOutDuringThePurge_isRetriedRatherThanReportedComplete() = runBlocking {
        seedValidatedSample()
        val doses = SlowHistoryDoseRepository(InMemoryDoseRepository())
        val scheduler = RecordingScheduler()
        val clock = Clock.fixed(now, zone)

        coordinator(doses, scheduler, clock = clock).onWake(WakeReason.ALARM, timeoutMillis = 50)

        assertEquals(
            "A timed-out wake arms a short retry instead of the ordinary schedule",
            setOf(WakeMoment(clock.instant().plus(ReminderCoordinator.RETRY_DELAY), WakeKind.REMINDER)),
            scheduler.schedule,
        )
    }

    private suspend fun seedValidatedSample() {
        // Ten real minutes behind now, in both clocks, so the next observation advances normally
        // and validates.
        val tenMinutes = 10 * 60_000L
        store.write(
            TrustedNow.Sample(
                trustedNowMillis = System.currentTimeMillis() - tenMinutes,
                anchorElapsedRealtimeMillis = android.os.SystemClock.elapsedRealtime() - tenMinutes,
            ),
        )
    }

    private fun coordinator(
        doses: DoseRepository,
        scheduler: ReminderAlarmScheduler,
        medications: InMemoryMedicationRepository = InMemoryMedicationRepository(),
        clock: Clock = Clock.fixed(now, zone),
        trustedClockGuard: TrustedClockGuard = TrustedClockGuard(store),
    ): ReminderCoordinator {
        val markMissed = MarkMissedDoses(doses, clock)
        return ReminderCoordinator(
            medicationRepository = medications,
            doseRepository = doses,
            refreshPlannedDoses = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock),
            markMissedDoses = markMissed,
            dueDoses = DueDoses(clock),
            computeWakeSchedule = ComputeWakeSchedule(clock),
            notifier = ReminderNotifier(context),
            scheduler = scheduler,
            clock = clock,
            trustedClockGuard = trustedClockGuard,
            purgeExpiredDoseHistory = PurgeExpiredDoseHistory(clock),
        )
    }

    /** A scheduler that records the set it was asked for instead of waking the device. */
    private inner class RecordingScheduler : ReminderAlarmScheduler(context) {
        var schedule: WakeSchedule = emptySet()

        override suspend fun reconcile(schedule: WakeSchedule) {
            this.schedule = schedule
        }
    }

    /** A [DoseRepository] whose purge query always fails, to prove a purge failure costs nothing. */
    private class ThrowingHistoryDoseRepository(
        private val delegate: DoseRepository,
    ) : DoseRepository by delegate {
        override suspend fun deleteHistoryBefore(cutoff: Instant): Int = error("purge boom")
    }

    /** A [DoseRepository] whose purge query outlasts a short wake timeout, on purpose. */
    private class SlowHistoryDoseRepository(
        private val delegate: DoseRepository,
    ) : DoseRepository by delegate {
        override suspend fun deleteHistoryBefore(cutoff: Instant): Int {
            delay(500)
            return delegate.deleteHistoryBefore(cutoff)
        }
    }

    private companion object {
        /** A wall-clock reading no legitimate elapsed-time delta could ever justify. */
        const val TAMPERED_FUTURE_MILLIS = 100_000_000_000_000L
    }
}
