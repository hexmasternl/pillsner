package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.intake.RecordIntake
import nl.hexmaster.pillsner.domain.intake.SnoozeDose
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.repositoryWith
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.today
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec: dose-records missed rule, and medicine-reminders snooze bound. */
class MissedAndSnoozeTest {

    @Test
    fun `a dose is missed once the next dose of the same medicine is due`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)), dose(2, at(hour = 20)))
        val clock = MutableTestClock(at(hour = 20), amsterdam)
        val markMissed = MarkMissedDoses(doses, clock)

        val lapsed = markMissed()

        assertEquals(listOf(1L), lapsed.map { it.id.value })
        assertEquals(IntakeOutcome.MISSED, doses.get(lapsed.single().id)?.intake?.outcome)
    }

    @Test
    fun `a dose with no next dose is missed 24 hours later, not before`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)))
        val clock = MutableTestClock(at(today.plusDays(1), 7, 59), amsterdam)
        val markMissed = MarkMissedDoses(doses, clock)

        assertEquals(emptyList<Long>(), markMissed().map { it.id.value })

        clock.advance(Duration.ofMinutes(1))

        assertEquals(listOf(1L), markMissed().map { it.id.value })
    }

    @Test
    fun `a skipped dose stays skipped`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)))
        val clock = MutableTestClock(at(hour = 9), amsterdam)
        RecordIntake(doses, clock)(dose(1, at(hour = 8)).id, IntakeOutcome.SKIPPED)

        clock.advance(Duration.ofDays(2))
        MarkMissedDoses(doses, clock)()

        assertEquals(IntakeOutcome.SKIPPED, doses.all().single().intake?.outcome)
    }

    @Test
    fun `a snooze lasts fifteen minutes`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)))
        val clock = MutableTestClock(at(hour = 8), amsterdam)
        val snooze = SnoozeDose(doses, MarkMissedDoses(doses, clock), clock)

        val until = snooze(dose(1, at(hour = 8)).id)

        assertEquals(at(hour = 8, minute = 15), until)
    }

    @Test
    fun `a snooze never carries a dose past the next dose of the same medicine`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)), dose(2, at(hour = 8, minute = 5)))
        val clock = MutableTestClock(at(hour = 8), amsterdam)
        val snooze = SnoozeDose(doses, MarkMissedDoses(doses, clock), clock)

        val until = snooze(dose(1, at(hour = 8)).id)

        assertEquals(at(hour = 8, minute = 5), until)
    }

    @Test
    fun `a snoozed dose still lapses at its bound`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)), dose(2, at(hour = 8, minute = 5)))
        val clock = MutableTestClock(at(hour = 8), amsterdam)
        SnoozeDose(doses, MarkMissedDoses(doses, clock), clock)(dose(1, at(hour = 8)).id)

        clock.setTo(at(hour = 8, minute = 5))
        val lapsed = MarkMissedDoses(doses, clock)()

        assertEquals(listOf(1L), lapsed.map { it.id.value })
        assertNull(doses.get(lapsed.single().id)?.snoozedUntil)
    }

    @Test
    fun `an answered dose cannot be snoozed`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8)))
        val clock = MutableTestClock(at(hour = 8), amsterdam)
        RecordIntake(doses, clock)(dose(1, at(hour = 8)).id, IntakeOutcome.TAKEN)

        val until = SnoozeDose(doses, MarkMissedDoses(doses, clock), clock)(dose(1, at(hour = 8)).id)

        assertNull(until)
    }

    @Test
    fun `taking a dose records the moment it was taken and clears its snooze`() = runBlocking {
        val doses = repositoryWith(dose(1, at(hour = 8), snoozedUntil = at(hour = 8, minute = 15)))
        val clock = MutableTestClock(at(hour = 8, minute = 7), amsterdam)

        RecordIntake(doses, clock)(dose(1, at(hour = 8)).id, IntakeOutcome.TAKEN)

        val stored = doses.all().single()
        assertEquals(IntakeOutcome.TAKEN, stored.intake?.outcome)
        assertEquals(at(hour = 8, minute = 7), stored.intake?.recordedAt)
        assertNull(stored.snoozedUntil)
    }
}
