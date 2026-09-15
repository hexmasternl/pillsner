package nl.hexmaster.pillsner.domain.scheduling

import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec: medicine-reminders, a reminder repeating until it is answered (design D5).
 *
 * The lapse moment used here is 24 hours after the dose, which is what [MarkMissedDoses] gives a
 * dose with no successor, so nothing in these tests is near it except the test that says so.
 */
class ReminderRepeatsTest {

    private val clock = MutableTestClock(at(hour = 8), amsterdam)
    private val lapseAt = at(hour = 8).plusSeconds(24 * 60 * 60)

    @Test
    fun `a dose never announced is not repeated`() {
        assertNull(ReminderRepeats.nextRepeatAt(dose(1, at(hour = 8)), lapseAt))
    }

    @Test
    fun `the first repeat is a quarter of an hour after the dose was announced`() {
        val announced = dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))

        assertEquals(at(hour = 8, minute = 15), ReminderRepeats.nextRepeatAt(announced, lapseAt))
    }

    @Test
    fun `each repeat counts from the last posting, not the first`() {
        val asked = dose(
            id = 1,
            scheduledAt = at(hour = 8),
            firstRemindedAt = at(hour = 8),
            lastRemindedAt = at(hour = 8, minute = 30),
            reminderCount = 2,
        )

        assertEquals(at(hour = 8, minute = 45), ReminderRepeats.nextRepeatAt(asked, lapseAt))
    }

    @Test
    fun `the app goes quiet after four repeats`() {
        val spent = dose(
            id = 1,
            scheduledAt = at(hour = 8),
            firstRemindedAt = at(hour = 8),
            lastRemindedAt = at(hour = 9),
            reminderCount = ReminderRepeats.MAX,
        )

        assertNull(ReminderRepeats.nextRepeatAt(spent, lapseAt))
    }

    @Test
    fun `a repeat is never posted at or after the dose lapses`() {
        val announced = dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))
        // The successor is due five minutes on, so this dose lapses before its first repeat.
        val lapsesSoon = at(hour = 8, minute = 5)

        assertNull(ReminderRepeats.nextRepeatAt(announced, lapsesSoon))
    }

    @Test
    fun `a snoozed dose waits for its snooze rather than its repeat`() {
        val snoozed = dose(
            id = 1,
            scheduledAt = at(hour = 8),
            firstRemindedAt = at(hour = 8),
            snoozedUntil = at(hour = 8, minute = 15),
        )

        assertNull(ReminderRepeats.nextRepeatAt(snoozed, lapseAt))
    }

    @Test
    fun `any answer ends the sequence`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8), firstRemindedAt = at(hour = 8))))
        val id = doses.pending().first().id

        doses.recordIntake(id, IntakeOutcome.SKIPPED, at(hour = 8, minute = 2))

        assertNull(ReminderRepeats.nextRepeatAt(checkNotNull(doses.get(id)), lapseAt))
    }

    @Test
    fun `a snooze puts the count back to the start`() = runBlocking {
        val asked = dose(
            id = 1,
            scheduledAt = at(hour = 8),
            firstRemindedAt = at(hour = 8),
            lastRemindedAt = at(hour = 8, minute = 45),
            reminderCount = 3,
        )
        val doses = InMemoryDoseRepository(listOf(asked))

        doses.setSnooze(asked.id, at(hour = 9))

        assertEquals(0, checkNotNull(doses.get(asked.id)).reminderCount)
    }

    @Test
    fun `a posting that is not a repeat leaves the count where it is`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        val id = doses.pending().first().id

        doses.recordReminded(id, clock.instant(), countsAsRepeat = false)

        val announced = checkNotNull(doses.get(id))
        assertEquals(0, announced.reminderCount)
        assertEquals(at(hour = 8), announced.firstRemindedAt)
        assertEquals(at(hour = 8), announced.lastRemindedAt)
    }

    @Test
    fun `the first posting is never rewritten by a later one`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        val id = doses.pending().first().id

        doses.recordReminded(id, at(hour = 8), countsAsRepeat = false)
        doses.recordReminded(id, at(hour = 8, minute = 15), countsAsRepeat = true)

        val repeated = checkNotNull(doses.get(id))
        assertEquals(at(hour = 8), repeated.firstRemindedAt)
        assertEquals(at(hour = 8, minute = 15), repeated.lastRemindedAt)
        assertEquals(1, repeated.reminderCount)
    }
}
