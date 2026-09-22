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
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
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
            elapsedRealtimeMillis = { FIXED_ELAPSED_REALTIME_MILLIS },
        )

        coordinator(doses, RecordingScheduler(), trustedClockGuard = tamperedGuard).onWake(WakeReason.APP_START)

        assertEquals(
            "The old dose is purged immediately, clamped by the recent dose's own plannedAt floor",
            listOf(now),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun aRefreshsOwnNewInsertsCannotBeUsedAsTheFloorForTheSameWakesPurge() = runBlocking {
        // Reproduces a second gap PR #50's review found: runDoseHistoryPurge() used to read
        // latestKnownMoment() itself, after RefreshPlannedDoses had already run earlier in the same
        // wake -- and that refresh inserts newly generated doses with plannedAt = clock.instant(),
        // the very clock this whole guard exists to distrust. On a first observation with an
        // active schedule and a tampered clock, those brand-new rows would become the "independent"
        // floor, which is not independent of anything: it is the same wake poisoning its own guard.
        val tamperedInstant = now.plusSeconds(2L * 365 * 86_400) // ~2 tampered years ahead
        val tamperedClock = Clock.fixed(tamperedInstant, zone)
        val activeSchedule = Medication(
            id = MedicationId(1),
            name = "Ibuprofen",
            defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
            usedSince = LocalDate.of(2020, 1, 1),
            useUntil = null,
            prescribedBy = Prescriber.SELF,
            schedules = listOf(Schedule.EveryNDays(Quantity.of("40", DoseUnit.MILLIGRAM), 1, listOf(LocalTime.of(9, 0)))),
            isActive = true,
        )
        val medications = InMemoryMedicationRepository(listOf(activeSchedule))
        val old = now.minusSeconds(400L * 86_400)
        val doses = InMemoryDoseRepository(listOf(oldAnsweredDose(1, old, plannedAt = old)))
        val tamperedGuard = TrustedClockGuard(
            store,
            wallClockMillis = { tamperedInstant.toEpochMilli() },
            elapsedRealtimeMillis = { FIXED_ELAPSED_REALTIME_MILLIS },
        )

        coordinator(doses, RecordingScheduler(), medications, tamperedClock, tamperedGuard)
            .onWake(WakeReason.APP_START)

        // RefreshPlannedDoses will have inserted new pending doses for "today"/"tomorrow" as the
        // tampered clock sees them, using that same tampered clock as their plannedAt. If those
        // rows were allowed to feed the guard's floor, this genuinely old, real dose would have
        // been purged too.
        assertTrue(
            "A dose this same wake just inserted must never become evidence for its own purge",
            doses.all().any { it.id == DoseId(1) },
        )

        // The active schedule means this wake may have posted a real reminder; take it down so it
        // does not linger past this test.
        doses.all().forEach { ReminderNotifier(context).cancel(it) }
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
        // Ten minutes behind the test's own fixed `now`, in both clocks -- not the real machine
        // clock (correction found in PR review: seeding from System.currentTimeMillis() while the
        // coordinator's own clock is fixed at a 2026 date is a ticking time bomb, since the two
        // drift further apart every day this test suite keeps existing) -- so the next observation
        // advances normally and validates to exactly `now`, deterministically, forever.
        val tenMinutes = 10 * 60_000L
        store.write(
            TrustedNow.Sample(
                trustedNowMillis = now.toEpochMilli() - tenMinutes,
                anchorElapsedRealtimeMillis = FIXED_ELAPSED_REALTIME_MILLIS - tenMinutes,
            ),
        )
    }

    private fun coordinator(
        doses: DoseRepository,
        scheduler: ReminderAlarmScheduler,
        medications: InMemoryMedicationRepository = InMemoryMedicationRepository(),
        clock: Clock = Clock.fixed(now, zone),
        trustedClockGuard: TrustedClockGuard = TrustedClockGuard(
            store,
            wallClockMillis = { now.toEpochMilli() },
            elapsedRealtimeMillis = { FIXED_ELAPSED_REALTIME_MILLIS },
        ),
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

        /** An arbitrary, fixed boot-clock reading, paired with the fixed `now` throughout. */
        const val FIXED_ELAPSED_REALTIME_MILLIS = 10_000_000L
    }
}
