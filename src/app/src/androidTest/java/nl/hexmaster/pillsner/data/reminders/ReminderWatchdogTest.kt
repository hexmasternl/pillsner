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
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.scheduling.ComputeWakeSchedule
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator
import nl.hexmaster.pillsner.domain.scheduling.DueDoses
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.RefreshPlannedDoses
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the watchdog run actually does (spec: reminder-delivery-resilience, "A watchdog repairs a
 * broken alarm chain").
 *
 * The worker itself is a thin wrapper: it decides whether there is anything to watch and then runs
 * an ordinary wake. So what is worth testing is that wake — that it re-arms what should be armed,
 * posts what is already due, and costs nothing at all on a phone with no medicines.
 */
@RunWith(AndroidJUnit4::class)
class ReminderWatchdogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val today: LocalDate = LocalDate.of(2026, 9, 14)

    /** Ten past nine, so the nine o'clock dose is due and the ten o'clock one is not. */
    private val now: Instant = at(9, 10)
    private val clock: Clock = Clock.fixed(now, zone)

    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)
    private val store = ArmedAlarmStore(context)

    /**
     * One dose a day, not an hourly one: a wake refreshes the whole two-day planning window, so an
     * hourly medicine would fill it with dozens of past doses and bury what each test is about.
     */
    private fun dailyAt(hour: Int) = Medication(
        id = MedicationId(1),
        name = "Ibuprofen",
        defaultDose = mg40,
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.SELF,
        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(hour, 0)))),
        isActive = true,
    )

    @Before
    fun clearTheRecord() = runBlocking { store.clear() }

    @After
    fun leaveNothingBehind() = runBlocking { store.clear() }

    @Test
    fun withTheAlarmsIntact_itArmsExactlyTheSameSetAgain() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(9, 0))))
        val scheduler = WatchingScheduler()
        val coordinator = coordinator(doses, scheduler)

        coordinator.onWake(WakeReason.ALARM)
        val healthy = store.armed()
        scheduler.reset()

        coordinator.onWake(WakeReason.WATCHDOG)

        assertEquals("A healthy phone comes out of the watchdog unchanged", healthy, store.armed())
        assertTrue("Nothing wanted is cancelled", scheduler.cancelledMoments.isEmpty())
        assertEquals("Every wanted alarm is armed again", healthy, scheduler.armedMoments)
    }

    @Test
    fun withADroppedAlarm_theDueDoseIsStillPostedAndTheAlarmsPutBack() = runBlocking {
        // The nine o'clock alarm never fired, so the dose sits past its moment, unannounced —
        // which is exactly the shape of the failure this change exists to fix.
        val doses = InMemoryDoseRepository(listOf(dose(1, at(9, 0))))
        val notifier = RecordingNotifier()
        val scheduler = WatchingScheduler()

        coordinator(doses, scheduler, notifier).onWake(WakeReason.WATCHDOG)

        assertEquals("The dose the dropped alarm lost is announced", 1, notifier.shown.size)
        assertTrue("And the alarms are armed again", scheduler.armedMoments.isNotEmpty())
    }

    @Test
    fun withNoAlarmsAtAll_theyAreArmedFromScratch() = runBlocking {
        // An evening medicine and a morning clock, so nothing is due and the armed set is exactly
        // the two doses the planning window holds — no repeats, nothing already posted.
        val scheduler = WatchingScheduler()

        // Nothing recorded and nothing armed: a phone whose alarms the platform threw away.
        coordinator(InMemoryDoseRepository(), scheduler, medications = InMemoryMedicationRepository(listOf(dailyAt(20))))
            .onWake(WakeReason.WATCHDOG)

        assertEquals(
            setOf(
                WakeMoment(at(20, 0), WakeKind.REMINDER),
                WakeMoment(at(20, 0).plus(Duration.ofDays(1)), WakeKind.REMINDER),
            ),
            scheduler.armedMoments.filterTo(mutableSetOf()) { it.kind == WakeKind.REMINDER },
        )
    }

    @Test
    fun withNoMedicinesTheWatchdogHasNothingToWatch() = runBlocking {
        val scheduler = WatchingScheduler()

        coordinator(InMemoryDoseRepository(), scheduler, medications = InMemoryMedicationRepository())
            .onWake(WakeReason.WATCHDOG)

        assertTrue("A phone with no medicines should not pay for this", scheduler.armedMoments.isEmpty())
        assertTrue(store.armed().isEmpty())
    }

    // --- Fixtures ---------------------------------------------------------------------------

    private fun at(hour: Int, minute: Int = 0): Instant =
        ZonedDateTime.of(today, LocalTime.of(hour, minute), zone).toInstant()

    private fun dose(id: Long, scheduledAt: Instant) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = scheduledAt,
    )

    private fun coordinator(
        doses: InMemoryDoseRepository,
        scheduler: ReminderAlarmScheduler,
        notifier: ReminderNotifier = RecordingNotifier(),
        medications: MedicationRepository = InMemoryMedicationRepository(listOf(dailyAt(9))),
    ): ReminderCoordinator {
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
        )
    }

    /** A notifier that records rather than filling the device's shade. */
    private inner class RecordingNotifier : ReminderNotifier(context) {
        val shown = mutableListOf<Dose>()

        override fun show(dose: Dose, dueCount: Int): Boolean {
            shown += dose
            return true
        }

        override fun cancel(dose: Dose, remainingDue: Int) = Unit
        override fun cancel(id: DoseId, remainingDue: Int) = Unit
    }

    /** A scheduler that records the two calls that would otherwise reach `AlarmManager`. */
    private inner class WatchingScheduler : ReminderAlarmScheduler(context, store) {
        val armedMoments = mutableSetOf<WakeMoment>()
        val cancelledMoments = mutableSetOf<WakeMoment>()

        override fun setAlarm(moment: WakeMoment, exact: Boolean) {
            armedMoments += moment
        }

        override fun cancelAlarm(moment: WakeMoment) {
            cancelledMoments += moment
        }

        fun reset() {
            armedMoments.clear()
            cancelledMoments.clear()
        }
    }
}
