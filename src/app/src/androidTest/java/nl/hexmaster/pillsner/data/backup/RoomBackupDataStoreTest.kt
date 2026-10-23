package nl.hexmaster.pillsner.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.domain.backup.BackupDose
import nl.hexmaster.pillsner.domain.backup.BackupMedication
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import nl.hexmaster.pillsner.domain.backup.BackupStockBatch
import nl.hexmaster.pillsner.domain.backup.ExportBackup
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: data-backup-restore, the all-or-nothing replace and the export read. */
@RunWith(AndroidJUnit4::class)
class RoomBackupDataStoreTest {

    private lateinit var database: PillsnerDatabase
    private lateinit var store: RoomBackupDataStore

    private val morning: Instant = Instant.parse("2026-09-14T06:00:00Z")
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)
    private val tablet = Quantity.of("1", DoseUnit.TABLET)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PillsnerDatabase::class.java).build()
        store = RoomBackupDataStore(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun anEmptyDatabase_readsAsNothing() = runBlocking {
        val stored = store.readAll()

        assertTrue(stored.medications.isEmpty() && stored.stockBatches.isEmpty() && stored.doses.isEmpty())
    }

    @Test
    fun replacingWithAnEmptySnapshot_emptiesEveryTable() = runBlocking {
        seedSomethingElse()

        store.replaceAll(BackupSnapshot.EMPTY)

        listOf("medications", "schedules", "stock_batches", "doses").forEach { table ->
            assertEquals("$table is empty", 0, rowsIn(table))
        }
    }

    @Test
    fun aSnapshot_survivesReplaceThenExportUnchanged() = runBlocking {
        val original = populated()

        store.replaceAll(original)
        val exported = ExportBackup(store, sequentialIds())()

        // Ids are export-local, so the second export names them afresh. Everything else, and every
        // reference between records, must come back exactly.
        assertEquals(original.relabelled(), exported.relabelled())
    }

    @Test
    fun stockAndDoses_followTheirMedicationToItsNewRoomId() = runBlocking {
        seedSomethingElse()

        store.replaceAll(populated())
        val stored = store.readAll()

        val ibuprofen = stored.medications.single { it.name == "Ibuprofen" }.id
        val vitaminD = stored.medications.single { it.name == "Vitamin D" }.id
        assertEquals(listOf(ibuprofen), stored.stockBatches.filter { it.unit == DoseUnit.TABLET && it.strengthPerUnit.compareTo(BigDecimal("20")) == 0 }.map { it.medicationId })
        assertEquals(vitaminD, stored.stockBatches.single { it.strengthPerUnit.compareTo(BigDecimal.ONE) == 0 }.medicationId)
        assertEquals(3, stored.doses.count { it.medicationId == ibuprofen })
        assertNull("The orphan stays an orphan", stored.doses.single { it.medicationName == "Paracetamol" }.medicationId)
        assertEquals(
            "The acknowledgement travels with the medicine",
            LowStockAcknowledgement.ACKNOWLEDGED_ORDERED,
            stored.medications.single { it.id == ibuprofen }.lowStockAcknowledgement,
        )
    }

    @Test
    fun replacing_leavesNothingFromBefore() = runBlocking {
        seedSomethingElse()

        store.replaceAll(populated())

        val names = store.readAll().medications.map { it.name }.toSet()
        assertEquals(setOf("Ibuprofen", "Vitamin D"), names)
        assertEquals("Only the two imported schedules", 2, rowsIn("schedules"))
        assertEquals(4, rowsIn("doses"))
    }

    @Test
    fun aFailurePartwayThrough_leavesTheDatabaseExactlyAsItWas() = runBlocking {
        seedSomethingElse()
        val before = ExportBackup(store, sequentialIds())()

        // Two doses of one medicine at one moment break the unique dose index, but only at the very
        // last insert: by then every delete and every other insert has already run.
        val broken = populated().let { it.copy(doses = it.doses + it.doses.first()) }
        val failure = runCatching { store.replaceAll(broken) }.exceptionOrNull()

        assertTrue("The failure is reported", failure != null)
        assertEquals(before, ExportBackup(store, sequentialIds())())
    }

    private fun populated() = BackupSnapshot(
        medications = listOf(
            BackupMedication(
                backupId = "x",
                name = "Ibuprofen",
                defaultDose = mg40,
                usedSince = LocalDate.of(2026, 9, 1),
                useUntil = null,
                prescribedBy = Prescriber.SPECIALIST,
                schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))),
                isActive = true,
                lowStockAcknowledgement = LowStockAcknowledgement.ACKNOWLEDGED_ORDERED,
            ),
            BackupMedication(
                backupId = "y",
                name = "Vitamin D",
                defaultDose = tablet,
                usedSince = LocalDate.of(2026, 1, 1),
                useUntil = LocalDate.of(2026, 12, 31),
                prescribedBy = Prescriber.SELF,
                schedules = listOf(Schedule.EveryNHours(tablet, 12, LocalTime.of(7, 0))),
                isActive = false,
                lowStockAcknowledgement = null,
            ),
        ),
        stockBatches = listOf(
            BackupStockBatch("x", BigDecimal("12"), DoseUnit.TABLET, BigDecimal("20"), LocalDate.of(2027, 2, 1), morning),
            BackupStockBatch("y", BigDecimal("30"), DoseUnit.TABLET, BigDecimal.ONE, LocalDate.of(2027, 5, 1), morning),
        ),
        doses = listOf(
            dose("x", morning, Intake(IntakeOutcome.TAKEN, morning.plusSeconds(60))),
            dose("x", morning.plusSeconds(43_200), Intake(IntakeOutcome.SKIPPED, morning.plusSeconds(43_300))),
            dose("x", morning.plusSeconds(86_400), null).copy(firstRemindedAt = morning.plusSeconds(86_400), reminderCount = 1),
            dose(null, morning.minusSeconds(86_400), Intake(IntakeOutcome.MISSED, morning.minusSeconds(80_000)))
                .copy(medicationName = "Paracetamol"),
        ),
    )

    private fun dose(medication: String?, at: Instant, intake: Intake?) = BackupDose(
        medicationBackupId = medication,
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = at,
        plannedAt = at.minusSeconds(3600),
        intake = intake,
        snoozedUntil = null,
        firstRemindedAt = null,
        lastRemindedAt = null,
        reminderCount = 0,
    )

    /** Something unrelated to [populated], so a test can tell the old data from the new. */
    private suspend fun seedSomethingElse() {
        val medications = RoomMedicationRepository(database.medicationDao())
        val doses = RoomDoseRepository(database.doseDao())
        val id = medications.add(
            NewMedication(
                name = "Old medicine",
                defaultDose = mg40,
                usedSince = LocalDate.of(2026, 1, 1),
                useUntil = null,
                prescribedBy = Prescriber.OTHER,
                schedules = listOf(
                    Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(9, 0))),
                    Schedule.EveryNDays(mg40, 2, listOf(LocalTime.of(21, 0))),
                ),
            ),
        )
        doses.insertPlanned(listOf(PlannedDose(id, "Old medicine", mg40, morning)), plannedAt = morning.minusSeconds(60))
    }

    private fun sequentialIds(): () -> String {
        var next = 0
        return { "id${next++}" }
    }

    /** Renames the backup ids by first appearance, so two snapshots compare on content alone. */
    private fun BackupSnapshot.relabelled(): BackupSnapshot {
        val names = medications.mapIndexed { index, medication -> medication.backupId to "m$index" }.toMap()
        return BackupSnapshot(
            medications = medications.map { it.copy(backupId = names.getValue(it.backupId)) },
            stockBatches = stockBatches.map { it.copy(medicationBackupId = names.getValue(it.medicationBackupId)) },
            doses = doses
                .map { it.copy(medicationBackupId = it.medicationBackupId?.let(names::getValue)) }
                .sortedWith(compareBy({ it.scheduledAt }, { it.medicationBackupId })),
        )
    }

    private fun rowsIn(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
