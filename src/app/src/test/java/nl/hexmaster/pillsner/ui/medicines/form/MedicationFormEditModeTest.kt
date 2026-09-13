package nl.hexmaster.pillsner.ui.medicines.form

import androidx.lifecycle.SavedStateHandle
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.ui.medicines.AmountParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The medicine form opened on an existing medicine (spec: medicine-details). */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicationFormEditModeTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today: LocalDate = LocalDate.of(2026, 9, 13)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val repository = InMemoryMedicationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening a medicine fills the form with it`() = runTest(dispatcher) {
        val viewModel = editing(store())

        val state = viewModel.uiState.value
        assertEquals("Metoprolol", state.name)
        assertEquals("40", state.doseText)
        assertEquals(DoseUnit.MILLIGRAM, state.doseUnit)
        assertEquals(Prescriber.SPECIALIST, state.prescribedBy)
        assertEquals(2, state.schedules.size)
        assertTrue(state.isActive)
        assertTrue(state.showsActiveSwitch)
        assertFalse(state.isLoading)
    }

    @Test
    fun `the schedules come back in the medicine's own order`() = runTest(dispatcher) {
        val viewModel = editing(store())

        assertEquals(
            listOf(Quantity.of("40", DoseUnit.MILLIGRAM), Quantity.of("20", DoseUnit.MILLIGRAM)),
            viewModel.uiState.value.schedules.map { it.amount },
        )
    }

    @Test
    fun `an untouched details form does not ask before leaving`() = runTest(dispatcher) {
        val viewModel = editing(store())

        assertTrue(viewModel.onBackRequested())
    }

    @Test
    fun `an edit that is undone by hand leaves the form untouched again`() = runTest(dispatcher) {
        val viewModel = editing(store())

        viewModel.onNameChange("Metoprololl")
        assertFalse(viewModel.onBackRequested())
        viewModel.onDiscardDialogDismissed()

        viewModel.onNameChange("Metoprolol")

        assertTrue(viewModel.onBackRequested())
    }

    @Test
    fun `saving an edit replaces the medicine under the same identifier`() = runTest(dispatcher) {
        val id = store()
        val viewModel = editing(id)
        val effects = mutableListOf<MedicationFormEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }

        viewModel.onNameChange("Metoprolol retard")
        viewModel.onDoseTextChange("20")
        viewModel.save()

        assertEquals(listOf(MedicationFormEffect.Saved), effects)
        val stored = snapshot().single()
        assertEquals(id, stored.id)
        assertEquals("Metoprolol retard", stored.name)
        assertEquals(Quantity.of("20", DoseUnit.MILLIGRAM), stored.defaultDose)
    }

    @Test
    fun `the active switch is written with the rest of the medicine`() = runTest(dispatcher) {
        val viewModel = editing(store())

        viewModel.onActiveChanged(false)
        viewModel.save()

        assertFalse(snapshot().single().isActive)
    }

    @Test
    fun `removing a schedule leaves only the other one`() = runTest(dispatcher) {
        val viewModel = editing(store())

        viewModel.removeSchedule(0)
        viewModel.save()

        val stored = snapshot().single()
        assertEquals(1, stored.schedules.size)
        assertEquals(Quantity.of("20", DoseUnit.MILLIGRAM), stored.schedules.single().amount)
    }

    @Test
    fun `a failing update keeps the form and reports it`() = runTest(dispatcher) {
        val id = store()
        val viewModel = MedicationFormViewModel(
            repository = RefusingRepository(repository),
            savedStateHandle = editHandle(id),
            amountParser = AmountParser(Locale.UK),
            clock = clock,
        )
        val effects = mutableListOf<MedicationFormEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }

        viewModel.onNameChange("Metoprolol retard")
        viewModel.save()

        assertEquals(listOf(MedicationFormEffect.SaveFailed), effects)
        assertEquals("Metoprolol retard", viewModel.uiState.value.name)
        assertEquals("Metoprolol", snapshot().single().name)
    }

    @Test
    fun `a medicine that cannot be found reports that the form could not open`() = runTest(dispatcher) {
        val viewModel = editing(MedicationId(404))
        val effects = mutableListOf<MedicationFormEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }

        // The effect is buffered, so a collector that subscribes afterwards still receives it.
        assertEquals(listOf(MedicationFormEffect.OpenFailed), effects)
    }

    @Test
    fun `a restored draft is not overwritten by the stored medicine`() = runTest(dispatcher) {
        val handle = editHandle(store())
        val first = viewModel(handle)
        first.onNameChange("Half typed")

        val restored = viewModel(handle)

        assertEquals("Half typed", restored.uiState.value.name)
        assertFalse("A restored half-edited form is still touched", restored.onBackRequested())
    }

    @Test
    fun `add mode shows no active switch and still stores a new medicine`() = runTest(dispatcher) {
        val viewModel = viewModel(SavedStateHandle())
        assertFalse(viewModel.uiState.value.showsActiveSwitch)

        viewModel.onNameChange("Ibuprofen")
        viewModel.onDoseTextChange("400")
        viewModel.save()

        assertEquals(listOf("Ibuprofen"), snapshot().map { it.name })
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun store(): MedicationId = runBlocking {
        repository.add(
            NewMedication(
                name = "Metoprolol",
                defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
                usedSince = today,
                useUntil = null,
                prescribedBy = Prescriber.SPECIALIST,
                schedules = listOf(
                    Schedule.EveryNHours(Quantity.of("40", DoseUnit.MILLIGRAM), 12, LocalTime.of(8, 0)),
                    Schedule.EveryNDays(
                        Quantity.of("20", DoseUnit.MILLIGRAM),
                        1,
                        listOf(LocalTime.of(9, 0)),
                    ),
                ),
            ),
        )
    }

    private fun snapshot(): List<Medication> = runBlocking { repository.observeAll().first() }

    /** The handle navigation builds for the details route. */
    private fun editHandle(id: MedicationId) = SavedStateHandle(mapOf("medicationId" to id.value))

    private fun editing(id: MedicationId) = viewModel(editHandle(id))

    private fun viewModel(handle: SavedStateHandle) = MedicationFormViewModel(
        repository = repository,
        savedStateHandle = handle,
        amountParser = AmountParser(Locale.UK),
        clock = clock,
    )

    /** Reads through, refuses to write, so the failure path can be exercised. */
    private class RefusingRepository(private val delegate: MedicationRepository) : MedicationRepository {
        override fun observeAll(): Flow<List<Medication>> = delegate.observeAll()
        override suspend fun get(id: MedicationId): Medication? = delegate.get(id)
        override suspend fun add(medication: NewMedication): MedicationId = error("no database")
        override suspend fun update(medication: Medication): Unit = error("no database")
        override suspend fun setActive(id: MedicationId, isActive: Boolean): Unit = error("no database")
    }
}
