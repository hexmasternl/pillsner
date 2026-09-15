package nl.hexmaster.pillsner.data.reset

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-reset, what a reset erases and what it leaves behind (design D2). */
@RunWith(AndroidJUnit4::class)
class RoomAppDataEraserTest {

    private lateinit var database: PillsnerDatabase
    private lateinit var doses: RoomDoseRepository
    private lateinit var medications: RoomMedicationRepository
    private lateinit var eraser: RoomAppDataEraser

    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val morning: Instant = Instant.parse("2026-09-14T06:00:00Z")
    private val evening: Instant = Instant.parse("2026-09-14T18:00:00Z")
    private val night: Instant = Instant.parse("2026-09-14T21:00:00Z")
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PillsnerDatabase::class.java).build()
        doses = RoomDoseRepository(database.doseDao())
        medications = RoomMedicationRepository(database.medicationDao())
        eraser = RoomAppDataEraser(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun erasing_emptiesMedicinesSchedulesAndTheWholeHistory() = runBlocking {
        val id = seedEverything()

        eraser.eraseAll()

        assertTrue("No medicines", medications.observeAll().first().isEmpty())
        assertTrue("No doses, answered or not", doses.pending().isEmpty())
        assertEquals("And no history either", 0, rowsIn("doses"))
        assertEquals(0, rowsIn("medications"))
        assertEquals("Schedules go with their medicine", 0, rowsIn("schedules"))
        assertEquals(null, doses.earliestScheduledAt(id))
    }

    @Test
    fun erasing_leavesTheSchemaWhereItIs() = runBlocking {
        seedEverything()
        val before = database.openHelper.readableDatabase.version

        eraser.eraseAll()

        // A reset empties the app; it is not a downgrade, an upgrade or a reinstall.
        assertEquals(PillsnerDatabase.VERSION, before)
        assertEquals(before, database.openHelper.readableDatabase.version)
    }

    @Test
    fun erasing_leavesTheDatabaseOpenAndUsable() = runBlocking {
        seedEverything()

        eraser.eraseAll()
        val id = medications.add(newMedication())

        // The app stays configured — it is simply empty — so the very next medicine must store.
        assertEquals(listOf("Ibuprofen"), medications.observeAll().first().map { it.name })
        assertEquals(id, medications.observeAll().first().single().id)
    }

    @Test
    fun erasing_reachesCollectorsThatWereAlreadySubscribed() = runBlocking {
        seedEverything()
        assertTrue(medications.observeAll().first().isNotEmpty())
        assertTrue(doses.observePending().first().isNotEmpty())

        eraser.eraseAll()

        // Home and Medicines fall into their empty states without being told, which is what lets
        // the user stay on Settings (design D9).
        assertTrue(medications.observeAll().first().isEmpty())
        assertTrue(doses.observePending().first().isEmpty())
    }

    /** One medicine with a schedule, and four doses covering every outcome a history can hold. */
    private suspend fun seedEverything(): MedicationId {
        val id = medications.add(newMedication())
        doses.insertPlanned(
            listOf(
                PlannedDose(id, "Ibuprofen", mg40, morning),
                PlannedDose(id, "Ibuprofen", mg40, evening),
                PlannedDose(id, "Ibuprofen", mg40, night),
                PlannedDose(id, "Ibuprofen", mg40, night.plusSeconds(3600)),
            ),
        )
        val all = doses.pending()
        doses.recordIntake(all[0].id, IntakeOutcome.TAKEN, morning)
        doses.recordIntake(all[1].id, IntakeOutcome.SKIPPED, evening)
        doses.recordIntake(all[2].id, IntakeOutcome.MISSED, night)
        return id
    }

    private fun newMedication() = NewMedication(
        name = "Ibuprofen",
        defaultDose = mg40,
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.GENERAL_PRACTITIONER,
        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))),
    )

    private fun rowsIn(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
