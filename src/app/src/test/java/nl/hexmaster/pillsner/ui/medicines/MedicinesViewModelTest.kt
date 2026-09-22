package nl.hexmaster.pillsner.ui.medicines

import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.model.TestFixtures.everyDay
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Partitioning, ordering and the tile's schedule lines (spec: medicine-overview). */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicinesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = InMemoryMedicationRepository()
    private val stockBatchRepository = InMemoryStockBatchRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with no medicines`() {
        val viewModel = MedicinesViewModel(repository, stockBatchRepository, Locale.UK)

        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertEquals(emptyList<MedicineTileState>(), state.active)
        assertEquals(emptyList<MedicineTileState>(), state.inactive)
    }

    @Test
    fun `empty repository yields empty sections`() = runTest(dispatcher) {
        val viewModel = collecting()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(emptyList<MedicineTileState>(), state.active)
        assertEquals(emptyList<MedicineTileState>(), state.inactive)
    }

    @Test
    fun `a mixed emission is partitioned on the active flag`() = runTest(dispatcher) {
        repository.replaceAll(
            listOf(
                medication(1, "Ibuprofen"),
                medication(2, "Methotrexate", isActive = false),
                medication(3, "Amoxicillin"),
            ),
        )
        val viewModel = collecting()

        val state = viewModel.uiState.value

        assertEquals(listOf("Amoxicillin", "Ibuprofen"), state.active.map { it.name })
        assertEquals(listOf("Methotrexate"), state.inactive.map { it.name })
    }

    @Test
    fun `names sort case-insensitively`() = runTest(dispatcher) {
        repository.replaceAll(
            listOf(
                medication(1, "paracetamol"),
                medication(2, "Ibuprofen"),
                medication(3, "Amoxicillin"),
            ),
        )
        val viewModel = collecting()

        assertEquals(
            listOf("Amoxicillin", "Ibuprofen", "paracetamol"),
            viewModel.uiState.value.active.map { it.name },
        )
    }

    @Test
    fun `an inactive name that sorts first still lands in the inactive section`() = runTest(dispatcher) {
        repository.replaceAll(
            listOf(medication(1, "Zolpidem"), medication(2, "Amoxicillin", isActive = false)),
        )
        val viewModel = collecting()

        val state = viewModel.uiState.value

        assertEquals(listOf("Zolpidem"), state.active.map { it.name })
        assertEquals(listOf("Amoxicillin"), state.inactive.map { it.name })
    }

    @Test
    fun `flipping the active flag moves the medicine to the other section`() = runTest(dispatcher) {
        repository.replaceAll(listOf(medication(1, "Ibuprofen")))
        val viewModel = collecting()
        assertEquals(listOf("Ibuprofen"), viewModel.uiState.value.active.map { it.name })

        repository.update(MedicationId(1)) { it.copy(isActive = false) }

        val state = viewModel.uiState.value
        assertEquals(emptyList<MedicineTileState>(), state.active)
        assertEquals(listOf("Ibuprofen"), state.inactive.map { it.name })
    }

    @Test
    fun `a medicine without schedules has one as-needed line at its default dose`() = runTest(dispatcher) {
        repository.replaceAll(listOf(medication(schedules = emptyList())))
        val viewModel = collecting()

        val line = viewModel.uiState.value.active.single().schedules.single()

        assertEquals(ScheduleSummary.AsNeeded, line.summary)
        assertEquals(mg40, line.amount)
    }

    @Test
    fun `a medicine with two schedules has one line each, in order`() = runTest(dispatcher) {
        repository.replaceAll(
            listOf(medication(schedules = listOf(everyDay(8), everyDay(9, 21)))),
        )
        val viewModel = collecting()

        assertEquals(
            listOf(ScheduleSummary.TimesPerDay(1), ScheduleSummary.TimesPerDay(2)),
            viewModel.uiState.value.active.single().schedules.map { it.summary },
        )
    }

    @Test
    fun `onSetActive asks the repository to stop the medicine`() = runTest(dispatcher) {
        repository.replaceAll(listOf(medication(1, "Ibuprofen")))
        val viewModel = collecting()

        viewModel.onSetActive(MedicationId(1), isActive = false)

        assertEquals(false, repository.observeAll().first().single().isActive)
    }

    @Test
    fun `a stopped medicine lands at its alphabetical place in the inactive section`() = runTest(dispatcher) {
        repository.replaceAll(
            listOf(
                medication(1, "Ibuprofen"),
                medication(2, "Amoxicillin", isActive = false),
                medication(3, "Zolpidem", isActive = false),
            ),
        )
        val viewModel = collecting()

        viewModel.onSetActive(MedicationId(1), isActive = false)

        assertEquals(
            listOf("Amoxicillin", "Ibuprofen", "Zolpidem"),
            viewModel.uiState.value.inactive.map { it.name },
        )
        assertEquals(emptyList<MedicineTileState>(), viewModel.uiState.value.active)
    }

    @Test
    fun `a failing repository reports it once and leaves the screen as it was`() = runTest(dispatcher) {
        val failing = FailingRepository(medication(1, "Ibuprofen"))
        val viewModel = MedicinesViewModel(failing, stockBatchRepository, Locale.UK)
        backgroundScope.launch { viewModel.uiState.collect {} }
        val effects = mutableListOf<MedicinesEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }

        viewModel.onSetActive(MedicationId(1), isActive = false)

        assertEquals(listOf(MedicinesEffect.UpdateFailed), effects)
        assertEquals(listOf("Ibuprofen"), viewModel.uiState.value.active.map { it.name })
    }

    private fun TestScope.collecting(): MedicinesViewModel {
        val viewModel = MedicinesViewModel(repository, stockBatchRepository, Locale.UK)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    /** Refuses every write, so the failure path can be exercised. */
    private class FailingRepository(vararg medications: Medication) : MedicationRepository {
        private val list = medications.toList()
        override fun observeAll(): Flow<List<Medication>> = flowOf(list)
        override suspend fun get(id: MedicationId): Medication? = null
        override suspend fun update(medication: Medication): Unit = error("no database")
        override suspend fun add(medication: NewMedication): MedicationId = error("no database")
        override suspend fun setActive(id: MedicationId, isActive: Boolean): Unit = error("no database")
        override suspend fun setLowStockAcknowledgement(id: MedicationId, value: LowStockAcknowledgement?): Unit =
            error("no database")
    }
}
