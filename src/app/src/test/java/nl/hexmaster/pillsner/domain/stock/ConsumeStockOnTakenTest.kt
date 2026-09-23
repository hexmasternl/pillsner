package nl.hexmaster.pillsner.domain.stock

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.InMemoryTransactionRunner
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockWarningQueue
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec: medicine-stock-tracking — consumption on a taken dose, the low-stock warning and its
 * acknowledgement, the expiry-at-use warning, and adding stock clearing a standing suppression.
 */
class ConsumeStockOnTakenTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-14T08:00:00Z"), ZoneId.of("Europe/Amsterdam"))
    private val medications = InMemoryMedicationRepository()
    private val batches = InMemoryStockBatchRepository()
    private val queue = InMemoryStockWarningQueue()
    private val evaluate = EvaluateStockWarning(medications, batches, ProjectWeeklyUsage(), clock)
    private val consume = ConsumeStockOnTaken(batches, medications, queue, evaluate)
    private val addStockBatch = AddStockBatch(batches, medications, InMemoryTransactionRunner(), clock)

    private val scheduledMedication = TestFixtures.medication(
        defaultDose = TestFixtures.oneTablet,
        usedSince = java.time.LocalDate.of(2026, 9, 14),
        schedules = listOf(Schedule.EveryNDays(TestFixtures.oneTablet, 1, listOf(TestFixtures.time(8)))),
    )

    @Test
    fun `a medicine with no stock batches is left untouched`() = runBlocking {
        medications.upsert(scheduledMedication)

        consume(scheduledMedication.id, TestFixtures.oneTablet)

        assertTrue(batches.batches(scheduledMedication.id).isEmpty())
        assertTrue(queue.observePending().first().isEmpty())
    }

    @Test
    fun `consumption deducts from stock and low stock enqueues a warning`() = runBlocking {
        medications.upsert(scheduledMedication)
        // Weekly usage is 7 tablets; 8 remaining, after taking one leaves 7 - still not below 7.
        addStockBatch(scheduledMedication.id, Quantity.of("8", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))

        consume(scheduledMedication.id, TestFixtures.oneTablet)

        assertEquals(java.math.BigDecimal("7"), batches.batches(scheduledMedication.id).single().remaining)
        assertTrue("7 remaining still covers the projected week", queue.observePending().first().isEmpty())

        // Taking one more drops remaining to 6, now below the 7-tablet weekly projection.
        consume(scheduledMedication.id, TestFixtures.oneTablet)

        assertEquals(setOf(scheduledMedication.id), queue.observePending().first().keys)
        val warning = evaluate(scheduledMedication.id)
        assertEquals(true, warning?.lowStock)
    }

    @Test
    fun `OK leaves the acknowledgement unset so the next low-stock take warns again`() = runBlocking {
        medications.upsert(scheduledMedication)
        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))

        consume(scheduledMedication.id, TestFixtures.oneTablet)
        assertEquals(setOf(scheduledMedication.id), queue.observePending().first().keys)

        // "OK": the warning is dismissed but the acknowledgement is left unset.
        queue.clear(setOf(scheduledMedication.id))
        assertNull(medications.get(scheduledMedication.id)?.lowStockAcknowledgement)

        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))
        consume(scheduledMedication.id, TestFixtures.oneTablet)

        assertEquals("Still low, and unacknowledged, so it warns again", setOf(scheduledMedication.id), queue.observePending().first().keys)
    }

    @Test
    fun `I ordered new suppresses the warning until new stock is added`() = runBlocking {
        medications.upsert(scheduledMedication)
        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))
        consume(scheduledMedication.id, TestFixtures.oneTablet)
        queue.clear(setOf(scheduledMedication.id))

        medications.setLowStockAcknowledgement(scheduledMedication.id, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)
        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))
        // Adding stock always clears the suppression per AddStockBatch, so re-set it to isolate
        // what this test is about: a take while it is set does not enqueue a warning.
        medications.setLowStockAcknowledgement(scheduledMedication.id, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)

        consume(scheduledMedication.id, TestFixtures.oneTablet)

        assertTrue("Suppressed: no warning enqueued", queue.observePending().first().isEmpty())
    }

    @Test
    fun `adding stock always clears a standing acknowledgement`() = runBlocking {
        medications.upsert(scheduledMedication)
        medications.setLowStockAcknowledgement(scheduledMedication.id, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)

        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))

        assertNull(medications.get(scheduledMedication.id)?.lowStockAcknowledgement)
    }

    @Test
    fun `a batch with a strength of zero or less is rejected and nothing is written`() = runBlocking {
        medications.upsert(scheduledMedication)
        medications.setLowStockAcknowledgement(scheduledMedication.id, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)

        listOf("0", "-20").forEach { strength ->
            val result = runCatching {
                addStockBatch(
                    scheduledMedication.id,
                    Quantity.of("20", DoseUnit.MILLIGRAM),
                    java.math.BigDecimal(strength),
                    java.time.LocalDate.of(2027, 1, 1),
                )
            }
            assertTrue("Strength $strength must be rejected", result.exceptionOrNull() is IllegalArgumentException)
        }

        assertTrue(batches.batches(scheduledMedication.id).isEmpty())
        assertEquals(
            "A rejected batch does not clear the acknowledgement either",
            LowStockAcknowledgement.ACKNOWLEDGED_ORDERED,
            medications.get(scheduledMedication.id)?.lowStockAcknowledgement,
        )
    }

    @Test
    fun `a fractional number of tablets is rejected at the domain boundary`() = runBlocking {
        medications.upsert(scheduledMedication)

        val result = runCatching {
            addStockBatch(
                scheduledMedication.id,
                Quantity.of("20.5", DoseUnit.TABLET),
                java.math.BigDecimal("20"),
                java.time.LocalDate.of(2027, 1, 1),
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(batches.batches(scheduledMedication.id).isEmpty())
    }

    @Test
    fun `a strength passed for a batch in the medicine's own unit is ignored, not rejected`() = runBlocking {
        medications.upsert(scheduledMedication)

        addStockBatch(scheduledMedication.id, Quantity.of("20", DoseUnit.TABLET), java.math.BigDecimal.ZERO, java.time.LocalDate.of(2027, 1, 1))

        assertEquals(java.math.BigDecimal.ONE, batches.batches(scheduledMedication.id).single().strengthPerUnit)
    }

    @Test
    fun `expiry-at-use enqueues even when the low-stock warning is suppressed`() = runBlocking {
        medications.upsert(scheduledMedication)
        // Low stock (well under the 7-tablet weekly projection) and expiring in 10 days.
        addStockBatch(scheduledMedication.id, Quantity.of("2", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2026, 9, 24))
        medications.setLowStockAcknowledgement(scheduledMedication.id, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)

        consume(scheduledMedication.id, TestFixtures.oneTablet)

        assertEquals(setOf(scheduledMedication.id), queue.observePending().first().keys)
        val warning = evaluate(scheduledMedication.id)
        assertEquals("Low stock is real but suppressed", false, warning?.lowStock)
        assertEquals(nl.hexmaster.pillsner.domain.model.BatchExpiryState.APPROACHING, warning?.expiryState)
    }

    @Test
    fun `exhausting an expiring batch still warns about the batch the dose came from`() = runBlocking {
        medications.upsert(scheduledMedication)
        // One tablet left in a batch expiring in 10 days, plenty in a batch far from expiry.
        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2026, 9, 24))
        addStockBatch(scheduledMedication.id, Quantity.of("30", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 6, 1))

        consume(scheduledMedication.id, TestFixtures.oneTablet)

        val pending = queue.observePending().first()
        assertEquals(mapOf(scheduledMedication.id to java.time.LocalDate.of(2026, 9, 24)), pending)
        val warning = evaluate(scheduledMedication.id, pending.getValue(scheduledMedication.id))
        assertEquals(nl.hexmaster.pillsner.domain.model.BatchExpiryState.APPROACHING, warning?.expiryState)
        assertEquals("30 tablets cover the week", false, warning?.lowStock)
    }

    @Test
    fun `a dose whose unit does not match the medicine's stock unit is not consumed`() = runBlocking {
        medications.upsert(scheduledMedication)
        addStockBatch(scheduledMedication.id, Quantity.of("10", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))

        consume(scheduledMedication.id, Quantity.of("1", DoseUnit.MILLIGRAM))

        assertEquals(java.math.BigDecimal("10"), batches.batches(scheduledMedication.id).single().remaining)
    }

    // --- Stock unit conversion (strength) ---------------------------------------------------

    @Test
    fun `a dose in mg deducts tablets from a batch with a strength`() = runBlocking {
        val mgMedication = TestFixtures.medication(
            id = 2,
            defaultDose = TestFixtures.mg40,
            usedSince = java.time.LocalDate.of(2026, 9, 14),
            schedules = listOf(Schedule.EveryNDays(TestFixtures.mg40, 1, listOf(TestFixtures.time(8)))),
        )
        medications.upsert(mgMedication)
        // 20 tablets, each worth 20 mg.
        addStockBatch(
            mgMedication.id,
            Quantity.of("20", DoseUnit.TABLET),
            java.math.BigDecimal("20"),
            java.time.LocalDate.of(2027, 1, 1),
        )

        consume(mgMedication.id, TestFixtures.mg40)

        // 40 mg / 20 mg-per-tablet = 2 tablets deducted.
        assertEquals(java.math.BigDecimal("18"), batches.batches(mgMedication.id).single().remaining)
    }

    @Test
    fun `a dose in ml deducts directly from stock already in ml`() = runBlocking {
        val mlMedication = TestFixtures.medication(
            id = 3,
            defaultDose = Quantity.of("15", DoseUnit.MILLILITRE),
            usedSince = java.time.LocalDate.of(2026, 9, 14),
            schedules = listOf(
                Schedule.EveryNDays(Quantity.of("15", DoseUnit.MILLILITRE), 1, listOf(TestFixtures.time(8))),
            ),
        )
        medications.upsert(mlMedication)
        addStockBatch(
            mlMedication.id,
            Quantity.of("300", DoseUnit.MILLILITRE),
            java.math.BigDecimal.ONE,
            java.time.LocalDate.of(2027, 1, 1),
        )

        consume(mlMedication.id, Quantity.of("15", DoseUnit.MILLILITRE))

        assertEquals(java.math.BigDecimal("285"), batches.batches(mlMedication.id).single().remaining)
    }

    // --- Removing a stock batch -------------------------------------------------------------

    @Test
    fun `removing a batch takes it out of the repository`() = runBlocking {
        medications.upsert(scheduledMedication)
        addStockBatch(scheduledMedication.id, Quantity.of("8", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))
        val batchId = batches.batches(scheduledMedication.id).single().id

        batches.removeBatch(batchId)

        assertTrue(batches.batches(scheduledMedication.id).isEmpty())
    }

    @Test
    fun `removing a medicine's last batch takes it out of stock tracking`() = runBlocking {
        medications.upsert(scheduledMedication)
        // Well under the 7-tablet weekly projection, so it would otherwise warn.
        addStockBatch(scheduledMedication.id, Quantity.of("1", DoseUnit.TABLET), java.math.BigDecimal.ONE, java.time.LocalDate.of(2027, 1, 1))
        val batchId = batches.batches(scheduledMedication.id).single().id

        batches.removeBatch(batchId)

        assertTrue("No batches left to consume from", batches.batches(scheduledMedication.id).isEmpty())
        assertEquals("A medicine with no batches is exempt from stock warnings", null, evaluate(scheduledMedication.id))

        // Taking a dose with no stock batches at all must not fail or resurrect a batch.
        consume(scheduledMedication.id, TestFixtures.oneTablet)
        assertTrue(batches.batches(scheduledMedication.id).isEmpty())
        assertTrue(queue.observePending().first().isEmpty())
    }
}
