package nl.hexmaster.pillsner.domain.intake

import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec: dose-detail, answering a dose; and medicine-reminders, the three answers.
 *
 * One use case is what keeps the notification and the detail screen from drifting apart, so these
 * tests state what each answer records and that the effects which must follow an answer always do.
 */
class AnswerDoseTest {

    private val clock = MutableTestClock(at(hour = 8, minute = 5), amsterdam)
    private val answered = mutableListOf<Dose>()

    @Test
    fun `I took it records it taken, at the moment the user said so`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

        answerWith(doses)(DoseId(1), DoseAnswer.TAKEN)

        val stored = checkNotNull(doses.get(DoseId(1)))
        assertEquals(IntakeOutcome.TAKEN, stored.intake?.outcome)
        assertEquals(at(hour = 8, minute = 5), stored.intake?.recordedAt)
    }

    @Test
    fun `Not going to records it skipped, which is never a missed dose`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

        answerWith(doses)(DoseId(1), DoseAnswer.SKIP)

        assertEquals(IntakeOutcome.SKIPPED, doses.get(DoseId(1))?.intake?.outcome)
    }

    @Test
    fun `Not yet postpones without settling anything`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

        answerWith(doses)(DoseId(1), DoseAnswer.SNOOZE)

        val stored = checkNotNull(doses.get(DoseId(1)))
        assertNull("A snooze is not an outcome", stored.intake)
        assertEquals(at(hour = 8, minute = 20), stored.snoozedUntil)
    }

    @Test
    fun `every answer runs the effects that have to follow one`() = runBlocking {
        DoseAnswer.entries.forEach { answer ->
            answered.clear()
            val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

            answerWith(doses)(DoseId(1), answer)

            assertEquals("$answer should have run them exactly once", 1, answered.size)
            assertEquals(DoseId(1), answered.single().id)
        }
    }

    @Test
    fun `the dose handed to the effects is the one as it was before the answer`() = runBlocking {
        // The notifier identifies what to take down from it, so it must be the dose the
        // notification was built from, not the answered one.
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))

        answerWith(doses)(DoseId(1), DoseAnswer.TAKEN)

        assertNull(answered.single().intake)
        assertNotNull("But the answer itself is written", doses.get(DoseId(1))?.intake)
    }

    @Test
    fun `a dose someone has already answered is left exactly as it is`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        answerWith(doses)(DoseId(1), DoseAnswer.SKIP)
        answered.clear()

        // The same dose answered again, from the shade while the screen was open.
        answerWith(doses)(DoseId(1), DoseAnswer.TAKEN)

        assertEquals("The first answer wins", IntakeOutcome.SKIPPED, doses.get(DoseId(1))?.intake?.outcome)
        assertTrue("And nothing follows a no-op", answered.isEmpty())
    }

    @Test
    fun `a dose that is not there changes nothing and does not throw`() = runBlocking {
        val doses = InMemoryDoseRepository()

        answerWith(doses)(DoseId(404), DoseAnswer.TAKEN)

        assertTrue(answered.isEmpty())
        assertTrue(doses.all().isEmpty())
    }

    private fun answerWith(doses: InMemoryDoseRepository) = AnswerDose(
        doseRepository = doses,
        recordIntake = RecordIntake(doses, clock),
        snoozeDose = SnoozeDose(doses, MarkMissedDoses(doses, clock), clock),
        onAnswered = { answered += it },
    )
}
