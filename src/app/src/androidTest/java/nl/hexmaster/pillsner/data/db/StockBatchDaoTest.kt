package nl.hexmaster.pillsner.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.stock.RoomStockBatchRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.BatchRemainingUpdate
import nl.hexmaster.pillsner.domain.stock.AddStockBatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: medicine-stock-tracking and medication-persistence version 6, against the real database. */
@RunWith(AndroidJUnit4::class)
class StockBatchDaoTest {

    private lateinit var database: PillsnerDatabase
    private lateinit var medications: RoomMedicationRepository
    private lateinit var batches: RoomStockBatchRepository

    private val today: LocalDate = LocalDate.of(2026, 9, 13)
    private val addedAt: Instant = Instant.parse("2026-09-13T08:15:30.123Z")

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PillsnerDatabase::class.java).build()
        medications = RoomMedicationRepository(database.medicationDao())
        batches = RoomStockBatchRepository(database.stockBatchDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aBatch_roundTripsEveryField() = runBlocking {
        val id = addMedication()

        batches.addBatch(id, Quantity.of("20.5", DoseUnit.TABLET), BigDecimal("12.5"), LocalDate.of(2027, 2, 28), addedAt)

        val stored = batches.batches(id).single()
        assertEquals(id, stored.medicationId)
        assertEquals(BigDecimal("20.5"), stored.remaining)
        assertEquals(DoseUnit.TABLET, stored.unit)
        assertEquals(BigDecimal("12.5"), stored.strengthPerUnit)
        assertEquals(LocalDate.of(2027, 2, 28), stored.expiryDate)
        assertEquals(addedAt, stored.addedAt)
    }

    @Test
    fun aBatchWithAStrengthOfZero_isRejectedBeforeAnyRowIsWritten() = runBlocking {
        val id = addMedication()

        val result = runCatching {
            batches.addBatch(id, Quantity.of("20", DoseUnit.TABLET), BigDecimal.ZERO, LocalDate.of(2027, 1, 1), addedAt)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue("Nothing was stored that a later read would choke on", batches.batches(id).isEmpty())
    }

    @Test
    fun batches_comeBackSoonestExpiryFirst_thenInAddOrder() = runBlocking {
        val id = addMedication()
        batches.addBatch(id, Quantity.of("1", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 6, 1), addedAt)
        batches.addBatch(id, Quantity.of("2", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1), addedAt.plusSeconds(60))
        batches.addBatch(id, Quantity.of("3", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1), addedAt)

        assertEquals(
            listOf(BigDecimal("3"), BigDecimal("2"), BigDecimal("1")),
            batches.batches(id).map { it.remaining },
        )
    }

    @Test
    fun applyingConsumption_writesOnlyTheNamedBatches_andKeepsAZeroBatch() = runBlocking {
        val id = addMedication()
        batches.addBatch(id, Quantity.of("1", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1), addedAt)
        batches.addBatch(id, Quantity.of("30", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 6, 1), addedAt)
        val (sooner, later) = batches.batches(id)

        batches.applyConsumption(listOf(BatchRemainingUpdate(sooner.id, BigDecimal.ZERO)))

        val stored = batches.batches(id)
        assertEquals("An exhausted batch is kept", 2, stored.size)
        assertEquals(0, BigDecimal.ZERO.compareTo(stored.first { it.id == sooner.id }.remaining))
        assertEquals(later.remaining, stored.first { it.id == later.id }.remaining)
    }

    @Test
    fun theStream_reEmitsWhenABatchIsAdded() = runBlocking {
        val id = addMedication()
        assertTrue(batches.observeBatches(id).first().isEmpty())

        batches.addBatch(id, Quantity.of("5", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1), addedAt)

        assertEquals(1, batches.observeBatches(id).first().size)
    }

    @Test
    fun removingABatch_removesOnlyThatOne() = runBlocking {
        val id = addMedication()
        batches.addBatch(id, Quantity.of("1", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1), addedAt)
        batches.addBatch(id, Quantity.of("2", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 6, 1), addedAt)
        val first = batches.batches(id).first()

        batches.removeBatch(first.id)

        assertEquals(listOf(BigDecimal("2")), batches.batches(id).map { it.remaining })
    }

    @Test
    fun deletingTheMedication_cascadesToItsBatches() = runBlocking {
        // As in MedicationDaoTest: nothing in production deletes a medicine, so the cascade is
        // exercised through raw SQL.
        val id = addMedication()
        batches.addBatch(id, Quantity.of("1", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1), addedAt)

        database.openHelper.writableDatabase.execSQL("DELETE FROM medications WHERE id = ${id.value}")

        assertTrue(batches.batches(id).isEmpty())
    }

    @Test
    fun addingStock_writesTheBatchAndClearsTheAcknowledgementTogether() = runBlocking {
        val id = addMedication()
        medications.setLowStockAcknowledgement(id, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)
        val addStockBatch = AddStockBatch(batches, medications, RoomTransactionRunner(database))

        addStockBatch(id, Quantity.of("30", DoseUnit.MILLIGRAM), BigDecimal.ONE, LocalDate.of(2027, 1, 1))

        assertEquals(1, batches.batches(id).size)
        assertNull(medications.get(id)?.lowStockAcknowledgement)
    }

    private suspend fun addMedication(): MedicationId = medications.add(
        NewMedication(
            name = "Ibuprofen",
            defaultDose = Quantity.of("400", DoseUnit.MILLIGRAM),
            usedSince = today,
            useUntil = null,
            prescribedBy = Prescriber.GENERAL_PRACTITIONER,
            schedules = emptyList(),
        ),
    )
}
