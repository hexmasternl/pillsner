package nl.hexmaster.pillsner.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: medicine-stock-tracking, "Whole-pill units". */
class StockBatchTest {

    private fun batch(remaining: String, unit: DoseUnit, strengthPerUnit: String = "1") = StockBatch(
        id = StockBatchId(1),
        medicationId = MedicationId(1),
        remaining = BigDecimal(remaining),
        unit = unit,
        strengthPerUnit = BigDecimal(strengthPerUnit),
        expiryDate = LocalDate.of(2027, 1, 1),
        addedAt = Instant.EPOCH,
    )

    @Test
    fun `only tablets and capsules are whole-pill units`() {
        val wholePill = DoseUnit.entries.filter { it.isWholePill }

        assertEquals(listOf(DoseUnit.TABLET, DoseUnit.CAPSULE), wholePill)
        assertFalse(DoseUnit.MILLILITRE.isWholePill)
        assertTrue(DoseUnit.CAPSULE.isWholePill)
    }

    @Test
    fun `a legacy fractional tablet count reads as the whole pills left`() {
        val legacy = batch("49.92", DoseUnit.TABLET, strengthPerUnit = "500")

        assertEquals(BigDecimal("49"), legacy.usableRemaining)
        assertEquals(BigDecimal("24500"), legacy.remainingInDoseUnits)
    }

    @Test
    fun `a continuous unit keeps its exact remaining amount`() {
        val liquid = batch("150.5", DoseUnit.MILLILITRE)

        assertEquals(BigDecimal("150.5"), liquid.usableRemaining)
    }
}
