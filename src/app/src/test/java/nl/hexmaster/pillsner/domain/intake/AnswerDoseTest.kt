package nl.hexmaster.pillsner.domain.intake

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.InMemoryTransactionRunner
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockWarningQueue
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.model.TestFixtures
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import nl.hexmaster.pillsner.domain.stock.ConsumeStockOnTaken
import nl.hexmaster.pillsner.domain.stock.EvaluateStockWarning
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
    fun `the same dose answered taken twice at once deducts its stock once`() = runBlocking {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        val stock = StockFixture(doses)
        val batchesBefore = stock.remaining()

        // Two surfaces, the notification and the detail screen, answering in the same instant.
        withContext(Dispatchers.Default) {
            List(2) { async { stock.answer(DoseId(1), DoseAnswer.TAKEN) } }.awaitAll()
        }

        assertEquals("One 40 mg dose, deducted once", batchesBefore - BigDecimal("40"), stock.remaining())
    }

    @Test
    fun `different doses answered at once each deduct from the same stock`() = runBlocking {
        val doses = InMemoryDoseRepository((1L..20L).map { dose(it, at(hour = 8)) })
        val stock = StockFixture(doses)
        val batchesBefore = stock.remaining()

        withContext(Dispatchers.Default) {
            (1L..20L).map { async { stock.answer(DoseId(it), DoseAnswer.TAKEN) } }.awaitAll()
        }

        assertEquals("No deduction overwrites another", batchesBefore - BigDecimal("800"), stock.remaining())
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
        // No medication or stock batch is registered for these fixture doses, so consumption is a
        // no-op in every test here; stock behaviour has its own test suite.
        consumeStockOnTaken = run {
            val medications = InMemoryMedicationRepository()
            val batches = InMemoryStockBatchRepository()
            ConsumeStockOnTaken(
                stockBatchRepository = batches,
                medicationRepository = medications,
                stockWarningQueue = InMemoryStockWarningQueue(),
                evaluateStockWarning = EvaluateStockWarning(medications, batches, clock = clock),
            )
        },
        transactionRunner = InMemoryTransactionRunner(),
        onAnswered = { answered += it },
    )

    /**
     * The fixture doses' medicine (id 1, 40 mg a dose) with 1000 mg of stock, answered through the
     * real use case.
     */
    private inner class StockFixture(private val doses: InMemoryDoseRepository) {
        private val medication = TestFixtures.medication(id = 1L, defaultDose = TestFixtures.mg40)
        private val medications = InMemoryMedicationRepository().apply { upsert(medication) }
        private val batches = InMemoryStockBatchRepository(
            listOf(
                StockBatch(
                    id = StockBatchId(1),
                    medicationId = medication.id,
                    remaining = BigDecimal("1000"),
                    unit = DoseUnit.MILLIGRAM,
                    strengthPerUnit = BigDecimal.ONE,
                    expiryDate = LocalDate.of(2030, 1, 1),
                    addedAt = Instant.EPOCH,
                ),
            ),
        )
        private val answerDose = AnswerDose(
            doseRepository = doses,
            recordIntake = RecordIntake(doses, clock),
            snoozeDose = SnoozeDose(doses, MarkMissedDoses(doses, clock), clock),
            consumeStockOnTaken = ConsumeStockOnTaken(
                stockBatchRepository = batches,
                medicationRepository = medications,
                stockWarningQueue = InMemoryStockWarningQueue(),
                evaluateStockWarning = EvaluateStockWarning(medications, batches, clock = clock),
            ),
            transactionRunner = InMemoryTransactionRunner(),
            onAnswered = {},
        )

        suspend fun answer(id: DoseId, answer: DoseAnswer) = answerDose(id, answer)

        fun remaining(): BigDecimal = runBlocking { batches.batches(medication.id).single().remaining }
    }
}
