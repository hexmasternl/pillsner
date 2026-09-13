package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import java.time.LocalTime
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
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec: reminder-scheduling next wake, and which doses are due. */
class ComputeNextWakeTest {

    private val clock = MutableTestClock(at(hour = 7), amsterdam)
    private val medications = InMemoryMedicationRepository()

    private val scheduled = medication(
        usedSince = today,
        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))),
    )

    @Test
    fun `with nothing at all there is nothing to wake up for`() = runBlocking {
        val nextWake = computeWith(InMemoryDoseRepository())

        assertNull(nextWake())
    }

    @Test
    fun `with a medicine but no doses the app still wakes for the daily refresh`() = runBlocking {
        medications.replaceAll(listOf(scheduled))

        val next = computeWith(InMemoryDoseRepository())()

        assertEquals(at(today.plusDays(1), 0, 5), next)
    }

    @Test
    fun `the next un-reminded dose is the next wake`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

        assertEquals(at(hour = 8), computeWith(doses)())
    }

    @Test
    fun `a snooze that ends sooner wins`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        val doses = InMemoryDoseRepository(
            listOf(
                dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), snoozedUntil = at(hour = 8, minute = 15)),
                dose(2, at(hour = 20)),
            ),
        )
        clock.setTo(at(hour = 8, minute = 1))

        assertEquals(at(hour = 8, minute = 15), computeWith(doses)())
    }

    @Test
    fun `a dose that lapses sooner than the next dose is the next wake`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        // One dose reminded about at 08:00; the next is at 20:00, so the first lapses then.
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8)), dose(2, at(hour = 20))),
        )
        clock.setTo(at(hour = 9))

        assertEquals(at(hour = 20), computeWith(doses)())
    }

    @Test
    fun `a dose already reminded about is not a wake candidate in its own right`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))))
        clock.setTo(at(hour = 9))

        // Only its lapse moment remains, 24 hours after it was due.
        assertEquals(at(today.plusDays(1), 8), computeWith(doses)())
    }

    @Test
    fun `moving the clock forward past a dose leaves the lapse as the next wake`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 12))

        assertEquals(at(today.plusDays(1), 8), computeWith(doses)())
    }

    @Test
    fun `moving the clock backward puts the dose back in the future`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 12))
        computeWith(doses)()

        clock.setTo(at(hour = 6))

        assertEquals(at(hour = 8), computeWith(doses)())
    }

    // --- Due doses --------------------------------------------------------------------------

    @Test
    fun `a dose whose moment has come and that has not been announced is due`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 8))

        assertEquals(listOf(1L), DueDoses(doses, clock)().map { it.id.value })
    }

    @Test
    fun `a dose in the future is not due`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 7, minute = 59))

        assertEquals(emptyList<Long>(), DueDoses(doses, clock)().map { it.id.value })
    }

    @Test
    fun `a dose already announced is not due again`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))))
        clock.setTo(at(hour = 9))

        assertEquals(emptyList<Long>(), DueDoses(doses, clock)().map { it.id.value })
    }

    @Test
    fun `a dose whose snooze has run out is due again`() = runBlocking {
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), snoozedUntil = at(hour = 8, minute = 15))),
        )
        clock.setTo(at(hour = 8, minute = 15))

        assertEquals(listOf(1L), DueDoses(doses, clock)().map { it.id.value })
    }

    @Test
    fun `a dose still inside its snooze is not due`() = runBlocking {
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), snoozedUntil = at(hour = 8, minute = 15))),
        )
        clock.setTo(at(hour = 8, minute = 10))

        assertEquals(emptyList<Long>(), DueDoses(doses, clock)().map { it.id.value })
    }

    @Test
    fun `the daily refresh moves to the next day once it has passed`() = runBlocking {
        medications.replaceAll(listOf(scheduled))
        clock.setTo(at(today.plusDays(1), 0, 5).plus(Duration.ofMinutes(1)))

        assertEquals(at(today.plusDays(2), 0, 5), computeWith(InMemoryDoseRepository())())
    }

    private fun computeWith(doses: InMemoryDoseRepository) =
        ComputeNextWake(doses, medications, MarkMissedDoses(doses, clock), clock)
}
