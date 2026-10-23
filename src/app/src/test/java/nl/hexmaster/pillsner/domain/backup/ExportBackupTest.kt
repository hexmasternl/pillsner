package nl.hexmaster.pillsner.domain.backup

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.model.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: data-backup-restore, what an export carries and how it refers to itself. */
class ExportBackupTest {

    private val morning = Instant.parse("2026-09-14T06:00:00Z")

    private fun storeHolding(dataset: StoredDataset) = object : BackupDataStore {
        override suspend fun readAll() = dataset
        override suspend fun replaceAll(snapshot: BackupSnapshot) = error("An export never writes")
    }

    private fun counter(): () -> String {
        var next = 0
        return { "m${next++}" }
    }

    @Test
    fun `an empty dataset exports as an empty snapshot`() = runBlocking {
        val export = ExportBackup(storeHolding(StoredDataset(emptyList(), emptyList(), emptyList())))

        assertEquals(BackupSnapshot.EMPTY, export())
    }

    @Test
    fun `stock and doses point at their medication by export-local id, never by Room id`() = runBlocking {
        val dataset = StoredDataset(
            medications = listOf(TestFixtures.medication(id = 41), TestFixtures.medication(id = 7, name = "Vitamin D")),
            stockBatches = listOf(
                StockBatch(StockBatchId(1), MedicationId(7), BigDecimal("30"), DoseUnit.TABLET, BigDecimal.ONE, LocalDate.of(2027, 1, 1), morning),
            ),
            doses = listOf(
                Dose(DoseId(3), MedicationId(41), "Ibuprofen", TestFixtures.mg40, morning, intake = Intake(IntakeOutcome.TAKEN, morning)),
                Dose(DoseId(4), MedicationId(7), "Vitamin D", TestFixtures.oneTablet, morning),
            ),
        )

        val snapshot = ExportBackup(storeHolding(dataset), counter())()

        assertEquals(listOf("m0", "m1"), snapshot.medications.map { it.backupId })
        assertEquals("Vitamin D's batch follows Vitamin D", "m1", snapshot.stockBatches.single().medicationBackupId)
        assertEquals(listOf("m0", "m1"), snapshot.doses.map { it.medicationBackupId })
    }

    @Test
    fun `a dose whose medicine was already gone exports with no reference`() = runBlocking {
        val orphan = Dose(DoseId(9), null, "Paracetamol", TestFixtures.mg40, morning, intake = Intake(IntakeOutcome.TAKEN, morning))

        val snapshot = ExportBackup(storeHolding(StoredDataset(emptyList(), emptyList(), listOf(orphan))))()

        assertNull(snapshot.doses.single().medicationBackupId)
        assertEquals("The record itself is kept", "Paracetamol", snapshot.doses.single().medicationName)
    }

    @Test
    fun `every field of a dose travels, including the reminder bookkeeping`() = runBlocking {
        val dose = Dose(
            id = DoseId(1),
            medicationId = MedicationId(1),
            medicationName = "Ibuprofen",
            amount = TestFixtures.mg40,
            scheduledAt = morning,
            plannedAt = morning.minusSeconds(60),
            intake = null,
            snoozedUntil = morning.plusSeconds(600),
            firstRemindedAt = morning,
            lastRemindedAt = morning.plusSeconds(300),
            reminderCount = 2,
        )

        val exported = ExportBackup(storeHolding(StoredDataset(listOf(TestFixtures.medication()), emptyList(), listOf(dose))))()
            .doses.single()

        assertEquals(dose.plannedAt, exported.plannedAt)
        assertEquals(dose.snoozedUntil, exported.snoozedUntil)
        assertEquals(dose.firstRemindedAt, exported.firstRemindedAt)
        assertEquals(dose.lastRemindedAt, exported.lastRemindedAt)
        assertEquals(dose.reminderCount, exported.reminderCount)
    }

    @Test
    fun `two exports of the same data use fresh ids each time`() = runBlocking {
        val store = storeHolding(StoredDataset(listOf(TestFixtures.medication()), emptyList(), emptyList()))

        val first = ExportBackup(store)().medications.single().backupId
        val second = ExportBackup(store)().medications.single().backupId

        assertTrue("A UUID per export, never the Room id", first != second && first != "1")
    }
}
