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
        unit: DoseUnit = DoseUnit.TABLET,
    ) = StockBatch(
        id = StockBatchId(id),
        medicationId = medication,
        remaining = BigDecimal(remaining),
        unit = unit,
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
        val batches = listOf(batch(1, "300", "2027-01-01", strengthPerUnit = "1", unit = DoseUnit.MILLILITRE))

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
            batch(2, "30", "2027-06-01", strengthPerUnit = "1", unit = DoseUnit.MILLIGRAM),
        )

        val result = consumeFefo(batches, BigDecimal("20"))

        assertEquals("The sooner batch is exhausted (its one 10 mg tablet)", BigDecimal.ZERO, result.updates.first { it.batchId == StockBatchId(1) }.remaining)
        assertEquals("The remaining 10 mg owed is drawn from the later batch", BigDecimal("20"), result.updates.first { it.batchId == StockBatchId(2) }.remaining)
        assertEquals(BigDecimal("20"), result.consumed)
    }

    // --- Whole-pill units --------------------------------------------------------------------

    @Test
    fun `a dose smaller than one tablet uses the whole tablet`() {
        // Issue #67: 40 mg against a batch of 50 tablets of 500 mg.
        val batches = listOf(batch(1, "50", "2027-01-01", strengthPerUnit = "500"))

        val result = consumeFefo(batches, BigDecimal("40"))

        assertEquals(BigDecimal("49"), result.updates.single().remaining)
        assertEquals("The whole tablet's worth is consumed", BigDecimal("500"), result.consumed)
        assertEquals(BigDecimal.ZERO, result.shortfall)
    }

    @Test
    fun `a dose that does not divide evenly rounds up to whole tablets`() {
        // 25 mg against 20 mg tablets is 1.25 tablets, rounded up to 2.
        val batches = listOf(batch(1, "100", "2027-01-01", strengthPerUnit = "20"))

        val result = consumeFefo(batches, BigDecimal("25"))

        assertEquals(BigDecimal("98"), result.updates.single().remaining)
    }

    @Test
    fun `a fractional tablet dose uses the whole tablet`() {
        val batches = listOf(batch(1, "30", "2027-01-01"))

        val result = consumeFefo(batches, BigDecimal("0.5"))

        assertEquals(BigDecimal("29"), result.updates.single().remaining)
    }

    @Test
    fun `a capsule batch rounds up the same way`() {
        val batches = listOf(batch(1, "10", "2027-01-01", strengthPerUnit = "250", unit = DoseUnit.CAPSULE))

        val result = consumeFefo(batches, BigDecimal("100"))

        assertEquals(BigDecimal("9"), result.updates.single().remaining)
    }

    @Test
    fun `rounding up applies to what is still owed per batch`() {
        // 25 mg against 20 mg tablets: the sooner batch's one tablet gives 20 mg, and the 5 mg still
        // owed takes one whole tablet from the later batch.
        val batches = listOf(
            batch(1, "1", "2027-01-01", strengthPerUnit = "20"),
            batch(2, "10", "2027-06-01", strengthPerUnit = "20"),
        )

        val result = consumeFefo(batches, BigDecimal("25"))

        assertEquals(BigDecimal.ZERO, result.updates.first { it.batchId == StockBatchId(1) }.remaining)
        assertEquals(BigDecimal("9"), result.updates.first { it.batchId == StockBatchId(2) }.remaining)
    }

    @Test
    fun `a legacy fractional tablet count is normalised on the next draw`() {
        val batches = listOf(batch(1, "49.92", "2027-01-01"))

        val result = consumeFefo(batches, BigDecimal.ONE)

        assertEquals(BigDecimal("48"), result.updates.single().remaining)
    }

    @Test
    fun `a tablet batch with less than one whole tablet is skipped`() {
        val batches = listOf(
            batch(1, "0.92", "2027-01-01"),
            batch(2, "5", "2027-06-01"),
        )

        val result = consumeFefo(batches, BigDecimal.ONE)

        assertTrue(result.updates.none { it.batchId == StockBatchId(1) })
        assertEquals(BigDecimal("4"), result.updates.single().remaining)
    }

    // --- Exact-fit preference ----------------------------------------------------------------

    @Test
    fun `an exact-fit batch is preferred over wasting a pill`() {
        val larger = batch(1, "50", "2027-01-01", strengthPerUnit = "500")
        val exact = batch(2, "30", "2027-06-01", strengthPerUnit = "40")

        val result = consumeFefo(listOf(larger, exact), BigDecimal("40"))

        assertEquals(listOf(BatchRemainingUpdate(StockBatchId(2), BigDecimal("29"))), result.updates)
        assertEquals("The pill actually taken is reported", exact, result.firstDrawn)
    }

    @Test
    fun `no exact fit falls back to first-expiry-first-out`() {
        val batches = listOf(
            batch(1, "50", "2027-01-01", strengthPerUnit = "500"),
            batch(2, "30", "2027-06-01", strengthPerUnit = "300"),
        )

        val result = consumeFefo(batches, BigDecimal("40"))

        assertEquals(listOf(BatchRemainingUpdate(StockBatchId(1), BigDecimal("49"))), result.updates)
    }

    @Test
    fun `an exact-fit batch without enough stock is not preferred`() {
        val batches = listOf(
            batch(1, "50", "2027-01-01", strengthPerUnit = "500"),
            batch(2, "1", "2027-06-01", strengthPerUnit = "40"),
        )

        val result = consumeFefo(batches, BigDecimal("80"))

        assertEquals(listOf(BatchRemainingUpdate(StockBatchId(1), BigDecimal("49"))), result.updates)
    }

    @Test
    fun `a continuous batch counts as an exact fit`() {
        val batches = listOf(
            batch(1, "50", "2027-01-01", strengthPerUnit = "500"),
            batch(2, "1000", "2027-06-01", unit = DoseUnit.MILLIGRAM),
        )

        val result = consumeFefo(batches, BigDecimal("40"))

        assertEquals(listOf(BatchRemainingUpdate(StockBatchId(2), BigDecimal("960"))), result.updates)
    }

    @Test
    fun `exact fit is not used when expiry order breaks no pill`() {
        // The sooner 20 mg tablets divide the 40 mg dose evenly, so the later 40 mg tablets are
        // left alone even though they would also fit.
        val batches = listOf(
            batch(1, "10", "2027-01-01", strengthPerUnit = "20"),
            batch(2, "10", "2027-06-01", strengthPerUnit = "40"),
        )

        val result = consumeFefo(batches, BigDecimal("40"))

        assertEquals(listOf(BatchRemainingUpdate(StockBatchId(1), BigDecimal("8"))), result.updates)
    }

    @Test
    fun `the shortfall reports what the batches could not cover`() {
        val batches = listOf(batch(1, "1", "2027-01-01", strengthPerUnit = "20"))

        val result = consumeFefo(batches, BigDecimal("50"))

        assertEquals(BigDecimal("30"), result.shortfall)
    }

    // --- Continuous units keep exact conversion ------------------------------------------------

    @Test
    fun `a continuous conversion that does not terminate never deducts more than the dose`() {
        // 2 mg against a 3 mg-per-ml liquid is 0.666666… ml; rounding up would take 2.000001 mg.
        val batches = listOf(batch(1, "10", "2027-01-01", strengthPerUnit = "3", unit = DoseUnit.MILLILITRE))

        val result = consumeFefo(batches, BigDecimal("2"))

        assertEquals(BigDecimal("9.333334"), result.updates.single().remaining)
        assertTrue("Consumed ${result.consumed} must not exceed the 2 mg dose", result.consumed <= BigDecimal("2"))
    }

    @Test
    fun `a rounding remainder does not spill into the next batch`() {
        val batches = listOf(
            batch(1, "10", "2027-01-01", strengthPerUnit = "3", unit = DoseUnit.MILLILITRE),
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
