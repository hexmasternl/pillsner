package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.today
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec: reminder-scheduling, the set of moments the app wakes for.
 *
 * The app used to keep one alarm for the earliest moment, which made the schedule a chain: losing
 * one alarm stopped everything after it. These tests hold the replacement to its promise — every
 * moment gets its own entry, so losing one costs one reminder.
 */
class ComputeWakeScheduleTest {

    private val clock = MutableTestClock(at(hour = 7), amsterdam)
    private val medications = InMemoryMedicationRepository()

    private val scheduled = medication(
        usedSince = today,
        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))),
    )

    @Test
    fun `with nothing at all there is nothing to wake up for`() = runBlocking {
        assertEquals(emptySet<WakeMoment>(), computeWith(InMemoryDoseRepository(), hasDoseHistory = false))
    }

    @Test
    fun `a quiet day still wakes for the daily refresh`() = runBlocking {
        medications.replaceAll(listOf(scheduled))

        assertEquals(
            setOf(WakeMoment(at(today.plusDays(1), 0, 5), WakeKind.HOUSEKEEPING)),
            computeWith(InMemoryDoseRepository(), hasDoseHistory = false),
        )
    }

    @Test
    fun `two doses today are two reminder alarms, not one`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8)), dose(2, at(hour = 20))))

        val schedule = computeWith(doses, hasDoseHistory = true)

        assertEquals(
            listOf(at(hour = 8), at(hour = 20)),
            schedule.filter { it.kind == WakeKind.REMINDER }.map { it.at }.sorted(),
        )
    }

    @Test
    fun `an outstanding snooze is its own moment`() = runBlocking {
        val doses = InMemoryDoseRepository(
            listOf(
                dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), snoozedUntil = at(hour = 8, minute = 15)),
                dose(2, at(hour = 20)),
            ),
        )
        clock.setTo(at(hour = 8, minute = 1))

        val reminders = computeWith(doses, hasDoseHistory = true).filter { it.kind == WakeKind.REMINDER }.map { it.at }

        assertEquals(listOf(at(hour = 8, minute = 15), at(hour = 20)), reminders.sorted())
    }

    @Test
    fun `an unanswered reminder is woken for again a quarter of an hour on`() = runBlocking {
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))),
        )
        clock.setTo(at(hour = 8, minute = 1))

        val reminders = computeWith(doses, hasDoseHistory = true).filter { it.kind == WakeKind.REMINDER }.map { it.at }

        assertEquals(listOf(at(hour = 8, minute = 15)), reminders)
    }

    @Test
    fun `a dose already announced is not woken for at its own moment again`() = runBlocking {
        // Four repeats already spent, so only the lapse moment is left to wake for.
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), reminderCount = ReminderRepeats.MAX)),
        )
        clock.setTo(at(hour = 9))

        assertEquals(
            setOf(WakeMoment(at(today.plusDays(1), 8), WakeKind.HOUSEKEEPING)),
            computeWith(doses, hasDoseHistory = false),
        )
    }

    @Test
    fun `housekeeping is one moment, the earliest of the lapse and the daily refresh`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        // Reminded at 08:00, and the next dose of the same medicine is at 20:00, so it lapses then.
        val doses = InMemoryDoseRepository(
            listOf(
                dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), reminderCount = ReminderRepeats.MAX),
                dose(2, at(hour = 20)),
            ),
        )
        clock.setTo(at(hour = 9))

        val housekeeping = computeWith(doses, hasDoseHistory = true).filter { it.kind == WakeKind.HOUSEKEEPING }

        assertEquals(listOf(WakeMoment(at(hour = 20), WakeKind.HOUSEKEEPING)), housekeeping)
    }

    @Test
    fun `moving the clock backward puts the dose back in the future`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 12))
        computeWith(doses, hasDoseHistory = true)

        clock.setTo(at(hour = 6))

        assertEquals(
            listOf(at(hour = 8)),
            computeWith(doses, hasDoseHistory = true).filter { it.kind == WakeKind.REMINDER }.map { it.at },
        )
    }

    @Test
    fun `the daily refresh moves to the next day once it has passed`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        clock.setTo(at(today.plusDays(1), 0, 5).plus(Duration.ofMinutes(1)))

        assertEquals(
            setOf(WakeMoment(at(today.plusDays(2), 0, 5), WakeKind.HOUSEKEEPING)),
            computeWith(InMemoryDoseRepository(), hasDoseHistory = false),
        )
    }

    @Test
    fun `a due dose that was never announced gets a retry alarm rather than only its lapse`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        clock.setTo(at(hour = 8, minute = 2))
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

        val schedule = computeWith(doses, hasDoseHistory = true)

        // The 08:00 alarm has fired and nothing was posted. Without this the next moment that
        // touches the dose is its lapse, where it is marked missed having never been announced.
        assertEquals(
            listOf(at(hour = 8, minute = 7)),
            schedule.filter { it.kind == WakeKind.REMINDER }.map { it.at },
        )
    }

    @Test
    fun `a due dose that was announced is not retried, the repeat rule has it`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        clock.setTo(at(hour = 8, minute = 2))
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))))

        val schedule = computeWith(doses, hasDoseHistory = true)

        assertEquals(
            listOf(at(hour = 8, minute = 15)),
            schedule.filter { it.kind == WakeKind.REMINDER }.map { it.at },
        )
    }

    @Test
    fun `no active medication and no dose history means no daily refresh`() = runBlocking {
        assertEquals(
            emptySet<WakeMoment>(),
            computeWith(InMemoryDoseRepository(), hasDoseHistory = false),
        )
    }

    @Test
    fun `no active medication but dose history still wakes for the daily refresh`() = runBlocking {
        assertEquals(
            setOf(WakeMoment(at(today.plusDays(1), 0, 5), WakeKind.HOUSEKEEPING)),
            computeWith(InMemoryDoseRepository(), hasDoseHistory = true),
        )
    }

    @Test
    fun `an active scheduled medication wakes for the daily refresh regardless of dose history`() = runBlocking {
        medications.replaceAll(listOf(scheduled))

        assertEquals(
            setOf(WakeMoment(at(today.plusDays(1), 0, 5), WakeKind.HOUSEKEEPING)),
            computeWith(InMemoryDoseRepository(), hasDoseHistory = false),
        )
    }

    private suspend fun computeWith(doses: InMemoryDoseRepository, hasDoseHistory: Boolean): WakeSchedule {
        val snapshot = buildPendingSnapshot(doses, MarkMissedDoses(doses, clock))
        return ComputeWakeSchedule(clock)(snapshot, medications.observeAll().first(), hasDoseHistory)
    }
}
