package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Duration
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
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import nl.hexmaster.pillsner.domain.scheduling.WakeSchedule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The three ways a reminder used to be lost without a trace, and the fix for each
 * (spec: reminder-scheduling).
 *
 * A dose recorded as reminded when nothing was shown; a wake that ran out of time turning a due
 * dose into a missed one; and a wake running before the user has unlocked the phone, where nothing
 * it needs can be read. Nothing here touches the device's own database or its notification shade:
 * the repositories are in memory, and the notifier and the scheduler both report what they were
 * asked to do instead of doing it.
 */
@RunWith(AndroidJUnit4::class)
class ReminderRecoveryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val today: LocalDate = LocalDate.of(2026, 9, 14)

    /** Ten past nine, so the nine o'clock dose of an hourly medicine is already due. */
    private val now: Instant = at(today, 9, 10)
    private val clock: Clock = Clock.fixed(now, zone)

    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    /** The real device-protected record, which is what a locked boot reads. */
    private val store = ArmedAlarmStore(context)

    @Before
    fun clearTheRecord() = runBlocking { store.clear() }

    @After
    fun leaveNothingBehind() = runBlocking { store.clear() }

    private val hourly = Medication(
        id = MedicationId(1),
        name = "Ibuprofen",
        defaultDose = mg40,
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.SELF,
        schedules = listOf(Schedule.EveryNHours(mg40, 1, LocalTime.of(0, 0))),
        isActive = true,
    )

    // --- A dose is only recorded as reminded when it was announced (design D1) ---------------

    @Test
    fun aDoseThatCouldNotBeAnnounced_staysUnReminded() = runBlocking {
        val doses = InMemoryDoseRepository()
        val notifier = RecordingNotifier(posts = false)

        coordinator(doses, notifier = notifier).onWake(WakeReason.ALARM)

        val dose = doses.all().first { it.scheduledAt == at(today, 9, 0) }
        assertNull("A dose nothing announced must stay announceable", dose.firstRemindedAt)
    }

    @Test
    fun aDoseThatWasAnnounced_isRecordedAsReminded() = runBlocking {
        val doses = InMemoryDoseRepository()

        coordinator(doses, notifier = RecordingNotifier(posts = true)).onWake(WakeReason.ALARM)

        val dose = doses.all().first { it.scheduledAt == at(today, 9, 0) }
        assertNotNull("A dose the user was told about is reminded", dose.firstRemindedAt)
    }

    @Test
    fun aDoseThatCouldNotBeAnnounced_isStillDueOnTheNextWake() = runBlocking {
        val doses = InMemoryDoseRepository()
        coordinator(doses, notifier = RecordingNotifier(posts = false)).onWake(WakeReason.ALARM)

        // The user grants the permission, and the very next wake announces the dose.
        val notifier = RecordingNotifier(posts = true)
        coordinator(doses, notifier = notifier).onWake(WakeReason.ALARM)

        assertTrue(
            "The dose should have been announced once it could be",
            notifier.shown.any { it.scheduledAt == at(today, 9, 0) },
        )
    }

    @Test
    fun aDoseThatCouldNotBeAnnounced_keepsItsSnooze() = runBlocking {
        val snoozed = dose(1, at(today, 8, 0), firstRemindedAt = at(today, 8, 0), snoozedUntil = at(today, 9, 0))
        val doses = InMemoryDoseRepository(listOf(snoozed))

        coordinator(doses, notifier = RecordingNotifier(posts = false)).onWake(WakeReason.ALARM)

        // Clearing the snooze as well would take the dose out of the due check, and the user would
        // never hear about it again.
        assertEquals(at(today, 9, 0), doses.all().first { it.id == DoseId(1) }.snoozedUntil)
    }

    @Test
    fun anUndeliverableDose_stillLapsesAsMissed() = runBlocking {
        val old = dose(1, at(today.minusDays(2), 9, 0))
        val doses = InMemoryDoseRepository(listOf(old))

        coordinator(doses, notifier = RecordingNotifier(posts = false)).onWake(WakeReason.ALARM)

        assertEquals(
            "The ordinary lapse rule still applies to a dose nothing could announce",
            IntakeOutcome.MISSED,
            doses.all().first { it.id == DoseId(1) }.intake?.outcome,
        )
    }

    // --- A wake that does not complete is retried (design D2) -------------------------------

    @Test
    fun aWakeThatRunsOutOfTime_armsTheRetryAndNotTheLapseMoment() = runBlocking {
        val doses = SlowDoseRepository(InMemoryDoseRepository(listOf(dose(1, at(today, 9, 0)))))
        val scheduler = RecordingScheduler()

        coordinator(doses, scheduler = scheduler).onWake(WakeReason.ALARM)

        assertEquals("A timed-out wake tries again shortly", setOf(now.plus(RETRY)), scheduler.moments)
        assertNull("Nothing was announced, so nothing is reminded", doses.all().first().firstRemindedAt)
    }

    @Test
    fun aRetryThatCompletes_clearsTheCount() = runBlocking {
        val doses = SlowDoseRepository(InMemoryDoseRepository(listOf(dose(1, at(today, 9, 0)))))
        val scheduler = RecordingScheduler()
        val coordinator = coordinator(doses, scheduler = scheduler)

        coordinator.onWake(WakeReason.ALARM)
        doses.isSlow = false
        coordinator.onWake(WakeReason.ALARM)

        assertNotEquals(
            "A wake that completed arms the ordinary alarm set, not another retry",
            setOf(now.plus(RETRY)),
            scheduler.moments,
        )
        assertTrue(scheduler.moments.isNotEmpty())
    }

    @Test
    fun retriesAreBoundedAndStillLeaveAnAlarmSet() = runBlocking {
        val doses = SlowDoseRepository(InMemoryDoseRepository(listOf(dose(1, at(today, 9, 0)))))
        val scheduler = RecordingScheduler()
        val coordinator = coordinator(doses, scheduler = scheduler)

        // The first wake and its three retries all run out of time.
        repeat(4) { coordinator.onWake(WakeReason.ALARM) }

        assertNotEquals(
            "A fourth consecutive timeout means something a fifth attempt will not fix",
            setOf(now.plus(RETRY)),
            scheduler.moments,
        )
        assertTrue("An alarm is set on every path, this one included", scheduler.moments.isNotEmpty())
    }

    @Test
    fun aStepThatThrows_isRetriedLikeATimeout() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(today, 9, 0))))
        val scheduler = RecordingScheduler()

        coordinator(doses, notifier = ThrowingNotifier(), scheduler = scheduler)
            .onWake(WakeReason.ALARM)

        // A wake that threw has not announced what was due any more than one that ran out of
        // time has, and every step of it is idempotent, so it is tried again the same way.
        assertEquals("A wake that threw tries again shortly", setOf(now.plus(RETRY)), scheduler.moments)
        assertNull("Nothing was announced, so nothing is reminded", doses.all().first().firstRemindedAt)
    }

    // --- The wake cycle does not run while the user is locked (design D4) -------------------

    @Test
    fun aLockedWake_postsNothingMarksNothingMissedAndStillLeavesAnAlarmSet() = runBlocking {
        // The alarm that just fired is still in the record: nothing takes it out until a reconcile.
        store.replace(setOf(WakeMoment(at(today, 9, 0), WakeKind.REMINDER)))
        val lapsed = dose(1, at(today.minusDays(2), 9, 0))
        val doses = InMemoryDoseRepository(listOf(lapsed))
        val notifier = RecordingNotifier(posts = true)
        val scheduler = RecordingScheduler()

        coordinator(doses, notifier, scheduler, unlocked = false).onWake(WakeReason.ALARM)

        assertEquals("Nothing may be announced before the phone is unlocked", 0, notifier.shown.size)
        assertNull(
            "A dose cannot be settled against a database that cannot be read",
            doses.all().first().intake,
        )
        assertEquals(
            "The alarm goes back a few minutes out, so the app tries again",
            setOf(now.plus(ReminderAlarmScheduler.LOCKED_RETRY)),
            scheduler.moments,
        )
    }

    @Test
    fun aLockedWake_keepsARecordedMomentThatIsStillFurtherOut() = runBlocking {
        val later = now.plus(Duration.ofMinutes(30))
        store.replace(setOf(WakeMoment(later, WakeKind.REMINDER)))
        val scheduler = RecordingScheduler()

        coordinator(InMemoryDoseRepository(), scheduler = scheduler, unlocked = false)
            .onWake(WakeReason.LOCKED_BOOT)

        assertEquals(setOf(later), scheduler.moments)
    }

    @Test
    fun aLockedWake_putsBackEveryRecordedAlarm() = runBlocking {
        val evening = now.plus(Duration.ofHours(11))
        store.replace(
            setOf(
                WakeMoment(at(today, 9, 0), WakeKind.REMINDER),
                WakeMoment(evening, WakeKind.REMINDER),
            ),
        )
        val scheduler = RecordingScheduler()

        coordinator(InMemoryDoseRepository(), scheduler = scheduler, unlocked = false)
            .onWake(WakeReason.LOCKED_BOOT)

        // Both, not only the earliest: one alarm per moment is what stops a dropped alarm taking
        // every later reminder with it (design D2, D8).
        assertEquals(setOf(now.plus(ReminderAlarmScheduler.LOCKED_RETRY), evening), scheduler.moments)
    }

    @Test
    fun anUnlockedWake_behavesAsBefore() = runBlocking {
        val doses = InMemoryDoseRepository()
        val notifier = RecordingNotifier(posts = true)

        coordinator(doses, notifier, unlocked = true).onWake(WakeReason.ALARM)

        assertTrue("A phone that is unlocked reminds as it always did", notifier.shown.isNotEmpty())
    }

    // --- Putting the alarms back on a locked boot (design D3, D8) --------------------------

    @Test
    fun aRecordedMomentThePhoneSleptThrough_isPutBackAFewMinutesOut() = runBlocking {
        store.replace(setOf(WakeMoment(at(today, 8, 0), WakeKind.REMINDER)))
        val scheduler = RecordingScheduler()

        scheduler.rearmStoredAlarms(now.plus(ReminderAlarmScheduler.LOCKED_RETRY))

        assertEquals(setOf(now.plus(ReminderAlarmScheduler.LOCKED_RETRY)), scheduler.moments)
    }

    @Test
    fun aRecordedMomentStillAhead_isPutBackExactlyWhereItWas() = runBlocking {
        store.replace(setOf(WakeMoment(at(today, 20, 0), WakeKind.REMINDER)))
        val scheduler = RecordingScheduler()

        scheduler.rearmStoredAlarms(now.plus(ReminderAlarmScheduler.LOCKED_RETRY))

        assertEquals(setOf(at(today, 20, 0)), scheduler.moments)
    }

    @Test
    fun aRecordedMomentKeepsItsKind_soTheRightAlarmTierIsUsed() = runBlocking {
        val housekeeping = WakeMoment(at(today, 20, 0), WakeKind.HOUSEKEEPING)
        store.replace(setOf(housekeeping))
        val scheduler = RecordingScheduler()

        scheduler.rearmStoredAlarms(now.plus(ReminderAlarmScheduler.LOCKED_RETRY))

        assertEquals(setOf(housekeeping), scheduler.schedule)
    }

    @Test
    fun withNothingRecorded_aLockedBootArmsNothing() = runBlocking {
        store.clear()
        val scheduler = RecordingScheduler()

        scheduler.rearmStoredAlarms(now.plus(ReminderAlarmScheduler.LOCKED_RETRY))

        assertNull("There was no alarm to put back", scheduler.schedule)
    }

    // --- Fixtures ---------------------------------------------------------------------------

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant()

    private fun dose(
        id: Long,
        scheduledAt: Instant,
        firstRemindedAt: Instant? = null,
        snoozedUntil: Instant? = null,
    ) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = scheduledAt,
        firstRemindedAt = firstRemindedAt,
        snoozedUntil = snoozedUntil,
    )

    private fun coordinator(
        doses: DoseRepository,
        notifier: ReminderNotifier = RecordingNotifier(posts = true),
        scheduler: ReminderAlarmScheduler = RecordingScheduler(),
        unlocked: Boolean = true,
    ): ReminderCoordinator {
        val medications = InMemoryMedicationRepository(listOf(hourly))
        val markMissed = MarkMissedDoses(doses, clock)
        return ReminderCoordinator(
            medicationRepository = medications,
            doseRepository = doses,
            refreshPlannedDoses = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock),
            markMissedDoses = markMissed,
            dueDoses = DueDoses(doses, markMissed, clock),
            computeWakeSchedule = ComputeWakeSchedule(doses, medications, markMissed, clock),
            notifier = notifier,
            scheduler = scheduler,
            clock = clock,
            unlockState = { unlocked },
            // Short enough that a stalled read times out in milliseconds rather than in the nine
            // seconds a broadcast receiver is really given.
            wakeTimeoutMillis = WAKE_TIMEOUT_MILLIS,
        )
    }

    /** A notifier that records what it was asked to show, and whether it claims to have shown it. */
    private inner class RecordingNotifier(private val posts: Boolean) : ReminderNotifier(context) {
        val shown = mutableListOf<Dose>()

        override fun show(dose: Dose, dueCount: Int): Boolean {
            if (posts) shown += dose
            return posts
        }

        override fun cancel(dose: Dose, remainingDue: Int) = Unit
        override fun cancel(id: DoseId, remainingDue: Int) = Unit
    }

    /** A notifier that fails the way an unexpected defect would, part-way through the wake. */
    private inner class ThrowingNotifier : ReminderNotifier(context) {
        override fun show(dose: Dose, dueCount: Int): Boolean = error("no notification manager")
        override fun cancel(dose: Dose, remainingDue: Int) = Unit
        override fun cancel(id: DoseId, remainingDue: Int) = Unit
    }

    /**
     * A scheduler that records the set it was asked for instead of waking the device.
     *
     * `rearmStoredAlarms` is deliberately not overridden: it reads the real device-protected store,
     * which is what a locked boot reads, and lands back here through [reconcile].
     */
    private inner class RecordingScheduler : ReminderAlarmScheduler(context) {
        var schedule: WakeSchedule? = null

        override suspend fun reconcile(schedule: WakeSchedule) {
            this.schedule = schedule
        }

        /** The moments armed, or the empty set when reconcile was never reached. */
        val moments: Set<Instant> get() = schedule.orEmpty().mapTo(mutableSetOf()) { it.at }
    }

    /**
     * A repository whose snapshot read is slower than the wake is allowed to take, which is what a
     * cold start that has to open Room and run its migrations looks like.
     */
    private class SlowDoseRepository(
        private val delegate: InMemoryDoseRepository,
    ) : DoseRepository by delegate {
        var isSlow = true

        override suspend fun pending(): List<Dose> {
            if (isSlow) delay(STALL_MILLIS)
            return delegate.pending()
        }

        fun all(): List<Dose> = delegate.all()
    }

    private companion object {
        val RETRY: Duration = ReminderCoordinator.RETRY_DELAY
        const val WAKE_TIMEOUT_MILLIS = 50L
        const val STALL_MILLIS = 400L
    }
}
