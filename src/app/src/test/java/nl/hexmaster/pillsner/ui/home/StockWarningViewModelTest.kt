package nl.hexmaster.pillsner.ui.home

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockWarningQueue
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.model.TestFixtures
import nl.hexmaster.pillsner.domain.stock.EvaluateStockWarning
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec: medicine-stock-tracking, "Combined warning presentation". */
@OptIn(ExperimentalCoroutinesApi::class)
class StockWarningViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = MutableTestClock(Instant.parse("2026-09-11T10:00:00Z"))
    private val medication = TestFixtures.medication(id = 1L)
    private val medications = InMemoryMedicationRepository().apply { upsert(medication) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `nothing pending shows no warning`() = runTest(dispatcher) {
        val viewModel = collecting(InMemoryStockWarningQueue(), batchesExpiringOn(LocalDate.of(2026, 9, 20)))

        assertNull(viewModel.warning.value)
    }

    @Test
    fun `a deferred warning names the expiry of the batch the take drew from, even once it is empty`() =
        runTest(dispatcher) {
            // The batch the take drew from is used up; what is left is far from expiry.
            val batches = InMemoryStockBatchRepository(
                listOf(
                    stockBatch(1, remaining = "0", expiry = LocalDate.of(2026, 9, 20)),
                    stockBatch(2, remaining = "400", expiry = LocalDate.of(2027, 6, 1)),
                ),
            )
            val queue = InMemoryStockWarningQueue(mapOf(medication.id to LocalDate.of(2026, 9, 20)))

            val viewModel = collecting(queue, batches)

            assertEquals(BatchExpiryState.APPROACHING, viewModel.warning.value?.expiryState)
        }

    @Test
    fun `OK dismisses the warning and leaves the acknowledgement unset`() = runTest(dispatcher) {
        val queue = InMemoryStockWarningQueue(mapOf(medication.id to LocalDate.of(2026, 9, 20)))
        val viewModel = collecting(queue, batchesExpiringOn(LocalDate.of(2026, 9, 20)))

        viewModel.onAcknowledged(medication.id)

        assertNull(viewModel.warning.value)
        assertTrue(queue.observePending().first().isEmpty())
        assertNull(medications.get(medication.id)?.lowStockAcknowledgement)
    }

    @Test
    fun `I ordered new dismisses the warning and records the acknowledgement`() = runTest(dispatcher) {
        val queue = InMemoryStockWarningQueue(mapOf(medication.id to LocalDate.of(2026, 9, 20)))
        val viewModel = collecting(queue, batchesExpiringOn(LocalDate.of(2026, 9, 20)))

        viewModel.onOrderedNew(medication.id)

        assertTrue(queue.observePending().first().isEmpty())
        assertEquals(LowStockAcknowledgement.ACKNOWLEDGED_ORDERED, medications.get(medication.id)?.lowStockAcknowledgement)
    }

    private fun TestScope.collecting(
        queue: InMemoryStockWarningQueue,
        batches: InMemoryStockBatchRepository,
    ): StockWarningViewModel {
        val viewModel = StockWarningViewModel(
            stockWarningQueue = queue,
            evaluateStockWarning = EvaluateStockWarning(medications, batches, clock = clock),
            medicationRepository = medications,
        )
        backgroundScope.launch { viewModel.warning.collect {} }
        return viewModel
    }

    private fun batchesExpiringOn(expiry: LocalDate) =
        InMemoryStockBatchRepository(listOf(stockBatch(1, remaining = "400", expiry = expiry)))

    private fun stockBatch(id: Long, remaining: String, expiry: LocalDate) = StockBatch(
        id = StockBatchId(id),
        medicationId = MedicationId(1),
        remaining = BigDecimal(remaining),
        unit = DoseUnit.MILLIGRAM,
        strengthPerUnit = BigDecimal.ONE,
        expiryDate = expiry,
        addedAt = Instant.EPOCH,
    )
}
