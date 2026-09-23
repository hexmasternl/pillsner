package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.time.Instant
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.repository.BatchRemainingUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: medicine-stock-tracking, "First-expiry-first-out consumption". */
class FefoConsumptionTest {

    private val medication = MedicationId(1)

    private fun batch(
        id: Long,
        remaining: String,
        expiry: String,
        addedAt: Instant = Instant.EPOCH,
        strengthPerUnit: String = "1",
    ) = StockBatch(
        id = StockBatchId(id),
        medicationId = medication,
        remaining = BigDecimal(remaining),
        unit = DoseUnit.TABLET,
        strengthPerUnit = BigDecimal(strengthPerUnit),
        expiryDate = java.time.LocalDate.parse(expiry),
        addedAt = addedAt,
    )

    @Test
    fun `single batch covers the dose`() {
        val batches = listOf(batch(1, "30", "2027-01-01"))

        val result = consumeFefo(batches, BigDecimal.ONE)

        assertEquals(listOf(BatchRemainingUpdate(StockBatchId(1), BigDecimal("29"))), result.updates)
        assertEquals(BigDecimal.ONE, result.consumed)
    }

    @Test
    fun `consumption spans two batches, soonest expiry first`() {
        val batches = listOf(
            batch(1, "1", "2027-01-01"),
            batch(2, "30", "2027-06-01"),
        )

        val result = consumeFefo(batches, BigDecimal("2"))

        // Batch 1 expires soonest, so it is drawn from first (exhausted to 0); the remaining unit
        // spills into batch 2, the only other batch.
        assertEquals(BigDecimal.ZERO, result.updates.first { it.batchId == StockBatchId(1) }.remaining)
        assertEquals(BigDecimal("29"), result.updates.first { it.batchId == StockBatchId(2) }.remaining)
        assertEquals(BigDecimal("2"), result.consumed)
    }

    @Test
    fun `exact exhaustion leaves a batch at zero, not removed`() {
        val batches = listOf(batch(1, "1", "2027-01-01"))

        val result = consumeFefo(batches, BigDecimal.ONE)

        assertEquals(BigDecimal.ZERO, result.updates.single().remaining)
    }

    @Test
    fun `insufficient total stock floors at zero rather than going negative`() {
        val batches = listOf(batch(1, "1", "2027-01-01"))

        val result = consumeFefo(batches, BigDecimal("2"))

        assertEquals(BigDecimal.ZERO, result.updates.single().remaining)
        assertEquals("Only what was actually available is reported consumed", BigDecimal.ONE, result.consumed)
    }

    @Test
    fun `a tie in expiry date is broken by added-at`() {
        val batches = listOf(
            batch(1, "5", "2027-01-01", addedAt = Instant.ofEpochMilli(2_000)),
            batch(2, "5", "2027-01-01", addedAt = Instant.ofEpochMilli(1_000)),
        )

        val result = consumeFefo(batches, BigDecimal("5"))

        assertEquals("The batch added first is drawn from first", BigDecimal.ZERO, result.updates.single().remaining)
        assertEquals(StockBatchId(2), result.updates.single().batchId)
    }

    @Test
    fun `a batch already at zero is skipped`() {
        val batches = listOf(
            batch(1, "0", "2027-01-01"),
            batch(2, "5", "2027-06-01"),
        )

        val result = consumeFefo(batches, BigDecimal("1"))

        assertTrue("The zero batch is never touched", result.updates.none { it.batchId == StockBatchId(1) })
        assertEquals(BigDecimal("4"), result.updates.single { it.batchId == StockBatchId(2) }.remaining)
    }

    // --- Stock unit conversion (strength) ---------------------------------------------------

    @Test
    fun `a dose amount is converted to tablets via the batch's strength`() {
        // 40 mg dose against a batch of 20 mg tablets deducts exactly 2 tablets.
        val batches = listOf(batch(1, "20", "2027-01-01", strengthPerUnit = "20"))

        val result = consumeFefo(batches, BigDecimal("40"))

        assertEquals(BigDecimal("18"), result.updates.single().remaining)
        assertEquals(BigDecimal("40"), result.consumed)
    }

    @Test
    fun `a strength of 1 is plain subtraction in one shared unit`() {
        val batches = listOf(batch(1, "300", "2027-01-01", strengthPerUnit = "1"))

        val result = consumeFefo(batches, BigDecimal("15"))

        assertEquals(BigDecimal("285"), result.updates.single().remaining)
        assertEquals(BigDecimal("15"), result.consumed)
    }

    @Test
    fun `FEFO conversion applies per batch it draws from`() {
        // A sooner-expiring batch of 10 mg tablets with 1 tablet remaining, and a later-expiring
        // batch already in the dose's own unit (strength 1) with 30 remaining.
        val batches = listOf(
            batch(1, "1", "2027-01-01", strengthPerUnit = "10"),
            batch(2, "30", "2027-06-01", strengthPerUnit = "1"),
        )

        val result = consumeFefo(batches, BigDecimal("20"))

        assertEquals("The sooner batch is exhausted (its one 10 mg tablet)", BigDecimal.ZERO, result.updates.first { it.batchId == StockBatchId(1) }.remaining)
        assertEquals("The remaining 10 mg owed is drawn from the later batch", BigDecimal("20"), result.updates.first { it.batchId == StockBatchId(2) }.remaining)
        assertEquals(BigDecimal("20"), result.consumed)
    }

    @Test
    fun `a dose that does not divide evenly still deducts a fractional unit`() {
        // 25 mg dose against 20 mg tablets deducts 1.25 tablets.
        val batches = listOf(batch(1, "100", "2027-01-01", strengthPerUnit = "20"))

        val result = consumeFefo(batches, BigDecimal("25"))

        assertEquals(BigDecimal("98.75"), result.updates.single().remaining)
    }

    @Test
    fun `a conversion that does not terminate never deducts more than the dose`() {
        // 2 mg against 3 mg tablets is 0.666666… tablets; rounding up would take 2.000001 mg.
        val batches = listOf(batch(1, "10", "2027-01-01", strengthPerUnit = "3"))

        val result = consumeFefo(batches, BigDecimal("2"))

        assertEquals(BigDecimal("9.333334"), result.updates.single().remaining)
        assertTrue("Consumed ${result.consumed} must not exceed the 2 mg dose", result.consumed <= BigDecimal("2"))
    }

    @Test
    fun `a rounding remainder does not spill into the next batch`() {
        val batches = listOf(
            batch(1, "10", "2027-01-01", strengthPerUnit = "3"),
            batch(2, "30", "2027-06-01"),
        )

        val result = consumeFefo(batches, BigDecimal("2"))

        assertTrue("The later batch is not touched", result.updates.none { it.batchId == StockBatchId(2) })
    }

    @Test
    fun `the first drawn batch is reported even when the dose exhausts it`() {
        val soonest = batch(1, "1", "2026-09-20")
        val batches = listOf(soonest, batch(2, "30", "2027-06-01"))

        val result = consumeFefo(batches, BigDecimal("2"))

        assertEquals(soonest, result.firstDrawn)
    }

    @Test
    fun `nothing to draw from reports no first drawn batch`() {
        val result = consumeFefo(listOf(batch(1, "0", "2027-01-01")), BigDecimal.ONE)

        assertNull(result.firstDrawn)
    }
}
