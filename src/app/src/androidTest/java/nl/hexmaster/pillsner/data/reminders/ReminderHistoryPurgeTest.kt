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

    private fun oldAnsweredDose(id: Long, scheduledAt: Instant) = Dose(
        id = DoseId(id),
        medicationId = null,
        medicationName = "Ibuprofen",
        amount = Quantity.of("40", DoseUnit.MILLIGRAM),
        scheduledAt = scheduledAt,
        intake = Intake(IntakeOutcome.TAKEN, scheduledAt.plusSeconds(60)),
    )

    @Test
    fun aFirstWakeWithNoPriorSample_seedsTheClockAndSkipsThePurgeButStillCompletes() = runBlocking {
        val old = now.minusSeconds(400L * 86_400)
        val doses = InMemoryDoseRepository(listOf(oldAnsweredDose(1, old)))
        val scheduler = RecordingScheduler()

        coordinator(doses, scheduler).onWake(WakeReason.APP_START)

        assertEquals("Nothing was validated yet, so nothing should have been purged", 1, doses.all().size)
        assertNotNull("The guard should have seeded a sample for next time", store.read())
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
            trustedClockGuard = TrustedClockGuard(store),
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
}
