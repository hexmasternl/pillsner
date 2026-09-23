package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.model.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: medicine-stock-tracking, batch expiry classification and the live stock picture. */
class StockStateTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")

    @Test
    fun `a date in the past is PAST`() {
        assertEquals(BatchExpiryState.PAST, batchExpiryState(today.minusDays(1), today))
    }

    @Test
    fun `today itself is APPROACHING, not PAST`() {
        assertEquals(BatchExpiryState.APPROACHING, batchExpiryState(today, today))
    }

    @Test
    fun `exactly 30 days away is APPROACHING`() {
        assertEquals(BatchExpiryState.APPROACHING, batchExpiryState(today.plusDays(30), today))
    }

    @Test
    fun `31 days away is NONE`() {
        assertEquals(BatchExpiryState.NONE, batchExpiryState(today.plusDays(31), today))
    }

    private fun batch(
        remaining: String,
        expiry: LocalDate,
        strengthPerUnit: String = "1",
        id: Long = 1,
        unit: DoseUnit = DoseUnit.TABLET,
    ) = StockBatch(
        id = StockBatchId(id),
        medicationId = MedicationId(1),
        remaining = BigDecimal(remaining),
        unit = unit,
        strengthPerUnit = BigDecimal(strengthPerUnit),
        expiryDate = expiry,
        addedAt = Instant.EPOCH,
    )

    @Test
    fun `low stock when remaining is less than the projected week`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.oneTablet,
            usedSince = today,
            schedules = listOf(Schedule.EveryNDays(TestFixtures.oneTablet, 1, listOf(TestFixtures.time(8)))),
        )
        // Projected usage over 7 days is 7 tablets; only 3 remain.
        val batches = listOf(batch("3", today.plusDays(60)))

        val state = stockState(batches, medication, today, zone)

        assertTrue(state.isLow)
    }

    @Test
    fun `sufficient stock is not low`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.oneTablet,
            usedSince = today,
            schedules = listOf(Schedule.EveryNDays(TestFixtures.oneTablet, 1, listOf(TestFixtures.time(8)))),
        )
        val batches = listOf(batch("30", today.plusDays(60)))

        val state = stockState(batches, medication, today, zone)

        assertFalse(state.isLow)
    }

    @Test
    fun `an as-needed medicine is exempt from the low-stock check regardless of remaining stock`() {
        val medication = TestFixtures.medication(schedules = emptyList())
        val batches = listOf(batch("1", today.plusDays(60)))

        assertFalse(stockState(batches, medication, today, zone).isLow)
    }

    @Test
    fun `nearest expiry ignores an exhausted batch`() {
        val medication = TestFixtures.medication(schedules = emptyList())
        val batches = listOf(
            batch("0", today), // exhausted, expiring today - must not count
            batch("5", today.plusDays(60)),
        )

        assertEquals(BatchExpiryState.NONE, stockState(batches, medication, today, zone).nearestExpiry)
    }

    // --- Stock unit conversion (strength) ---------------------------------------------------

    @Test
    fun `remaining stock is converted to the dose unit before comparing`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.mg40,
            usedSince = today,
            // 70 mg/day * 7 days = 490 mg projected weekly usage.
            schedules = listOf(Schedule.EveryNDays(TestFixtures.mg40.copy(value = BigDecimal("70")), 1, listOf(TestFixtures.time(8)))),
        )
        // 100 tablets worth 5 mg each = 500 mg, which covers the 490 mg projection.
        val batches = listOf(batch("100", today.plusDays(60), strengthPerUnit = "5"))

        assertFalse("500 mg of stock covers a 490 mg projection", stockState(batches, medication, today, zone).isLow)
    }

    @Test
    fun `a mix of units across batches still totals correctly`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.mg40,
            usedSince = today,
            schedules = listOf(Schedule.EveryNDays(TestFixtures.mg40, 1, listOf(TestFixtures.time(8)))),
        )
        // 7 tablets of 20 mg (140 mg) plus 200 mg already in the dose's own unit: 340 mg total,
        // against a 280 mg (40 mg * 7 days) weekly projection.
        val batches = listOf(
            batch("7", today.plusDays(60), strengthPerUnit = "20"),
            batch("200", today.plusDays(90), strengthPerUnit = "1", id = 2, unit = DoseUnit.MILLIGRAM),
        )

        assertFalse("340 mg of stock covers a 280 mg projection", stockState(batches, medication, today, zone).isLow)
    }

    // --- Whole-pill units ------------------------------------------------------------------

    private val fortyMgDaily = TestFixtures.medication(
        defaultDose = TestFixtures.mg40,
        usedSince = today,
        schedules = listOf(TestFixtures.everyDay(8)),
    )

    @Test
    fun `whole-pill rounding makes stock low sooner`() {
        // 6 tablets of 500 mg is 3,000 mg, far more than 7 x 40 mg = 280 mg, but each dose uses a
        // whole tablet, so the week needs 7 of them.
        val batches = listOf(batch("6", today.plusDays(60), strengthPerUnit = "500"))

        assertTrue(stockState(batches, fortyMgDaily, today, zone).isLow)
    }

    @Test
    fun `enough whole tablets for the week is not low`() {
        val batches = listOf(batch("7", today.plusDays(60), strengthPerUnit = "500"))

        assertFalse(stockState(batches, fortyMgDaily, today, zone).isLow)
    }

    @Test
    fun `a legacy fractional tablet batch with less than one tablet has no nearest expiry`() {
        val medication = TestFixtures.medication(schedules = emptyList())
        val batches = listOf(batch("0.92", today))

        assertEquals(BatchExpiryState.NONE, stockState(batches, medication, today, zone).nearestExpiry)
    }
}
