package nl.hexmaster.pillsner.domain.scheduling

import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: medicine-reminders, which doses should have a reminder showing right now. */
class DueDosesTest {

    private val clock = MutableTestClock(at(hour = 7), amsterdam)

    @Test
    fun `a dose whose moment has come and that has not been announced is due`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 8))

        assertEquals(listOf(1L), dueWith(doses))
    }

    @Test
    fun `a dose in the future is not due`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 7, minute = 59))

        assertEquals(emptyList<Long>(), dueWith(doses))
    }

    @Test
    fun `a dose announced a moment ago is not due again yet`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))))
        clock.setTo(at(hour = 8, minute = 14))

        assertEquals(emptyList<Long>(), dueWith(doses))
    }

    @Test
    fun `an unanswered dose is due again a quarter of an hour after it was announced`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))))
        clock.setTo(at(hour = 8, minute = 15))

        assertEquals(listOf(1L), dueWith(doses))
    }

    @Test
    fun `a dose whose snooze has run out is due again`() = runBlocking {
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), snoozedUntil = at(hour = 8, minute = 15))),
        )
        clock.setTo(at(hour = 8, minute = 15))

        assertEquals(listOf(1L), dueWith(doses))
    }

    @Test
    fun `a dose still inside its snooze is not due`() = runBlocking {
        val doses = InMemoryDoseRepository(
            listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8), snoozedUntil = at(hour = 8, minute = 15))),
        )
        clock.setTo(at(hour = 8, minute = 10))

        assertEquals(emptyList<Long>(), dueWith(doses))
    }

    @Test
    fun `an answered dose is never due`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        clock.setTo(at(hour = 8, minute = 30))
        doses.recordIntake(
            id = doses.pending().first().id,
            outcome = nl.hexmaster.pillsner.domain.model.IntakeOutcome.TAKEN,
            at = at(hour = 8, minute = 5),
        )

        assertEquals(emptyList<Long>(), dueWith(doses))
    }

    private suspend fun dueWith(doses: InMemoryDoseRepository): List<Long> {
        val snapshot = buildPendingSnapshot(doses, MarkMissedDoses(doses, clock))
        return DueDoses(clock)(snapshot).map { it.id.value }
    }
}
