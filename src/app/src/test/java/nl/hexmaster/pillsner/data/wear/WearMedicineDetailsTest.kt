package nl.hexmaster.pillsner.data.wear

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec: what the phone tells the watch about the medicine behind a dose, for the watch's read-only
 * details screen. The wording comes from the phone's formatters, which a unit test has no resources
 * for, so they are stood in for here; what is tested is which medicine facts are sent.
 */
class WearMedicineDetailsTest {

    private val id = MedicationId(1)
    private val medication = Medication(
        id = id,
        name = "Ibuprofen",
        defaultDose = Quantity(BigDecimal("400"), DoseUnit.MILLIGRAM),
        usedSince = LocalDate.of(2026, 9, 1),
        useUntil = null,
        prescribedBy = Prescriber.GENERAL_PRACTITIONER,
        schedules = listOf(
            Schedule.EveryNDays(
                amount = Quantity(BigDecimal("400"), DoseUnit.MILLIGRAM),
                intervalDays = 1,
                times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            ),
        ),
        isActive = true,
    )

    private val medications = InMemoryMedicationRepository(listOf(medication))
    private val stock = InMemoryStockBatchRepository()

    private fun details(
        medications: InMemoryMedicationRepository = this.medications,
        stock: InMemoryStockBatchRepository = this.stock,
    ) = WearMedicineDetails(
        medicationRepository = medications,
        stockBatchRepository = stock,
        amountText = { "${it.value.toPlainString()} ${it.unit.name.lowercase()}" },
        stockText = { value, unit -> "${value.toPlainString()} ${unit.name.lowercase()}" },
        scheduleText = { summary, amount ->
            "${amount.value.toPlainString()} ${amount.unit.name.lowercase()} $summary"
        },
    )

    private fun batch(remaining: String, strengthPerUnit: String = "1") = StockBatch(
        id = StockBatchId(1),
        medicationId = id,
        remaining = BigDecimal(remaining),
        unit = DoseUnit.TABLET,
        strengthPerUnit = BigDecimal(strengthPerUnit),
        expiryDate = LocalDate.of(2027, 1, 1),
        addedAt = Instant.parse("2026-09-01T08:00:00Z"),
    )

    @Test
    fun `the default dose and the schedules are sent`() = runTest {
        val sent = details().forMedication(id)

        assertEquals("400 milligram", sent?.defaultDoseText)
        assertEquals(
            listOf("400 milligram ${ScheduleSummary.TimesPerDay(2)}"),
            sent?.scheduleLines,
        )
    }

    @Test
    fun `a medicine taken as needed says so rather than sending no schedule at all`() = runTest {
        val asNeeded = InMemoryMedicationRepository(listOf(medication.copy(schedules = emptyList())))

        val sent = details(medications = asNeeded).forMedication(id)

        assertEquals(listOf("400 milligram ${ScheduleSummary.AsNeeded}"), sent?.scheduleLines)
    }

    @Test
    fun `a medicine that records no stock has no stock line`() = runTest {
        assertNull(details().forMedication(id)?.stockText)
    }

    @Test
    fun `stock is totalled in the medicine's own dose unit`() = runTest {
        // Twelve tablets of 400 mg each, counted the way the medicine is dosed.
        val withStock = InMemoryStockBatchRepository(listOf(batch("12", strengthPerUnit = "400")))

        assertEquals("4800 milligram", details(stock = withStock).forMedication(id)?.stockText)
    }

    @Test
    fun `stock that has run out is still a stock line, reading zero`() = runTest {
        val empty = InMemoryStockBatchRepository(listOf(batch("0")))

        assertEquals("0 milligram", details(stock = empty).forMedication(id)?.stockText)
    }

    @Test
    fun `a medicine that no longer exists has nothing to tell`() = runTest {
        assertNull(details().forMedication(MedicationId(99)))
    }
}
