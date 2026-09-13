package nl.hexmaster.pillsner.ui.medicines.form

import androidx.lifecycle.SavedStateHandle
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.validation.MedicationFieldError
import nl.hexmaster.pillsner.domain.validation.ScheduleDraftError
import nl.hexmaster.pillsner.domain.validation.SchedulePattern
import nl.hexmaster.pillsner.ui.medicines.AmountParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The one draft both add-medicine screens edit (spec: medicine-add, schedule-editor). */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicationFormViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 9, 13)
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

    // --- Form -----------------------------------------------------------------------------

    @Test
    fun `the form opens empty, starting today, prescribed by a general practitioner`() {
        val state = viewModel().uiState.value

        assertEquals("", state.name)
        assertEquals("", state.doseText)
        assertEquals(today, state.usedSince)
        assertNull(state.useUntil)
        assertEquals(Prescriber.GENERAL_PRACTITIONER, state.prescribedBy)
        assertEquals(emptyList<ScheduleRowState>(), state.schedules)
        assertFalse(state.showErrors)
    }

    @Test
    fun `saving an empty form reports the missing name and dose and stores nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.save()

        val state = viewModel.uiState.value
        assertTrue(state.showErrors)
        assertEquals(MedicationFieldError.NAME_REQUIRED, state.nameError)
        assertEquals(MedicationFieldError.DOSE_REQUIRED, state.doseError)
        assertEquals(emptyList<Medication>(), repository.snapshot())
    }

    @Test
    fun `a dose that is not a number is reported`() {
        val viewModel = viewModel()

        viewModel.onDoseTextChange("abc")

        assertEquals(MedicationFieldError.DOSE_NOT_A_NUMBER, viewModel.uiState.value.doseError)
    }

    @Test
    fun `an end date before the start is reported`() {
        val viewModel = viewModel()

        viewModel.onUseUntilChange(today.minusDays(1))

        assertEquals(
            MedicationFieldError.USE_UNTIL_BEFORE_USED_SINCE,
            viewModel.uiState.value.useUntilError,
        )
    }

    @Test
    fun `use until can be cleared again`() {
        val viewModel = viewModel()
        viewModel.onUseUntilChange(today.plusDays(30))

        viewModel.onUseUntilChange(null)

        assertNull(viewModel.uiState.value.useUntil)
    }

    @Test
    fun `saving a valid form stores the medicine and reports it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = mutableListOf<MedicationFormEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        fillValidForm(viewModel)

        viewModel.save()

        assertEquals(listOf(MedicationFormEffect.Saved), effects)
        val stored = repository.snapshot().single()
        assertEquals("Metoprolol", stored.name)
        assertEquals(Quantity.of("40", DoseUnit.MILLIGRAM), stored.defaultDose)
        assertTrue(stored.isActive)
    }

    @Test
    fun `the name is trimmed before it is stored`() = runTest(dispatcher) {
        val viewModel = viewModel()
        fillValidForm(viewModel, name = "  Metoprolol  ")

        viewModel.save()

        assertEquals("Metoprolol", repository.snapshot().single().name)
    }

    @Test
    fun `a failing save keeps the form and reports the failure`() = runTest(dispatcher) {
        val viewModel = viewModel(repository = FailingRepository)
        val effects = mutableListOf<MedicationFormEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        fillValidForm(viewModel)

        viewModel.save()

        assertEquals(listOf(MedicationFormEffect.SaveFailed), effects)
        assertEquals("Metoprolol", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `back on an untouched form leaves straight away`() {
        val viewModel = viewModel()

        assertTrue(viewModel.onBackRequested())
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `back on a touched form asks first`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Metoprolol")

        assertFalse(viewModel.onBackRequested())
        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `keeping editing closes the dialog and keeps the draft`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Metoprolol")
        viewModel.onBackRequested()

        viewModel.onDiscardDialogDismissed()

        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertEquals("Metoprolol", viewModel.uiState.value.name)
    }

    // --- Schedule editor ------------------------------------------------------------------

    @Test
    fun `a new schedule starts from the medicine's default dose`() {
        val viewModel = viewModel()
        viewModel.onDoseTextChange("40")
        viewModel.onDoseUnitChange(DoseUnit.MILLIGRAM)

        viewModel.openSchedule(null)

        val state = viewModel.editorState.value
        assertTrue(state.isNew)
        assertEquals("40", state.amountText)
        assertEquals(DoseUnit.MILLIGRAM, state.amountUnit)
    }

    @Test
    fun `switching pattern keeps the amount and the times`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onAmountTextChange("40")
        viewModel.onTimeAdded(LocalTime.of(8, 0))
        viewModel.onTimeAdded(LocalTime.of(20, 0))

        viewModel.onPatternChange(SchedulePattern.ON_WEEKDAYS)

        val state = viewModel.editorState.value
        assertEquals("40", state.amountText)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), state.times)
        assertEquals(emptySet<DayOfWeek>(), state.days)
    }

    @Test
    fun `a duplicate time is refused and reported`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onTimeAdded(LocalTime.of(8, 0))

        viewModel.onTimeAdded(LocalTime.of(8, 0))

        assertEquals(listOf(LocalTime.of(8, 0)), viewModel.editorState.value.times)
        assertTrue(viewModel.editorState.value.duplicateTimeRejected)
    }

    @Test
    fun `times are kept in ascending order`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onTimeAdded(LocalTime.of(20, 0))
        viewModel.onTimeAdded(LocalTime.of(8, 0))

        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), viewModel.editorState.value.times)
    }

    @Test
    fun `every twelve hours from eight shows both dose times and previews as every 12 hours`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onAmountTextChange("40")
        viewModel.onPatternChange(SchedulePattern.EVERY_N_HOURS)
        viewModel.onIntervalHoursChange(12)
        viewModel.onFirstDoseAtChange(LocalTime.of(8, 0))

        val state = viewModel.editorState.value
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), state.dailyDoseTimes)
        assertEquals(ScheduleSummary.EveryNHours(12), state.preview)
    }

    @Test
    fun `an empty times list shows the times-required message instead of a preview`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onAmountTextChange("40")

        val state = viewModel.editorState.value
        assertEquals(ScheduleDraftError.TIMES_REQUIRED, state.firstError)
        assertNull(state.preview)
    }

    @Test
    fun `all seven days cannot be committed`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onAmountTextChange("40")
        viewModel.onPatternChange(SchedulePattern.ON_WEEKDAYS)
        DayOfWeek.entries.forEach(viewModel::onDayToggled)
        viewModel.onTimeAdded(LocalTime.of(8, 0))

        assertFalse(viewModel.commitSchedule())
        assertTrue(viewModel.editorState.value.showsAllSevenDaysHint)
        assertEquals(emptyList<ScheduleRowState>(), viewModel.uiState.value.schedules)
    }

    @Test
    fun `committing a new schedule appends it to the form`() {
        val viewModel = viewModel()
        viewModel.openSchedule(null)
        viewModel.onAmountTextChange("40")
        viewModel.onPatternChange(SchedulePattern.EVERY_N_HOURS)
        viewModel.onIntervalHoursChange(12)

        assertTrue(viewModel.commitSchedule())

        val row = viewModel.uiState.value.schedules.single()
        assertEquals(0, row.index)
        assertEquals(ScheduleSummary.EveryNHours(12), row.summary)
        assertEquals(Quantity.of("40", DoseUnit.MILLIGRAM), row.amount)
    }

    @Test
    fun `committing an edited schedule replaces it without adding a row`() {
        val viewModel = viewModel()
        addEveryTwelveHours(viewModel, amount = "40")
        addEveryTwelveHours(viewModel, amount = "10")

        viewModel.openSchedule(0)
        viewModel.onAmountTextChange("20")
        assertTrue(viewModel.commitSchedule())

        val rows = viewModel.uiState.value.schedules
        assertEquals(2, rows.size)
        assertEquals(Quantity.of("20", DoseUnit.MILLIGRAM), rows[0].amount)
        assertEquals(Quantity.of("10", DoseUnit.MILLIGRAM), rows[1].amount)
    }

    @Test
    fun `opening an existing schedule fills the editor from it`() {
        val viewModel = viewModel()
        addEveryTwelveHours(viewModel, amount = "40")

        viewModel.openSchedule(0)

        val state = viewModel.editorState.value
        assertFalse(state.isNew)
        assertEquals("40", state.amountText)
        assertEquals(SchedulePattern.EVERY_N_HOURS, state.pattern)
        assertEquals(12, state.intervalHours)
    }

    @Test
    fun `removing a schedule keeps the order of the rest`() {
        val viewModel = viewModel()
        addEveryTwelveHours(viewModel, amount = "40")
        addEveryTwelveHours(viewModel, amount = "10")
        addEveryTwelveHours(viewModel, amount = "5")

        viewModel.removeSchedule(1)

        assertEquals(
            listOf(Quantity.of("40", DoseUnit.MILLIGRAM), Quantity.of("5", DoseUnit.MILLIGRAM)),
            viewModel.uiState.value.schedules.map { it.amount },
        )
    }

    @Test
    fun `saving stores every schedule in order`() = runTest(dispatcher) {
        val viewModel = viewModel()
        fillValidForm(viewModel)
        addEveryTwelveHours(viewModel, amount = "40")
        addEveryTwelveHours(viewModel, amount = "10")

        viewModel.save()

        val stored = repository.snapshot().single().schedules
        assertEquals(2, stored.size)
        assertEquals(Quantity.of("40", DoseUnit.MILLIGRAM), stored[0].amount)
        assertEquals(Quantity.of("10", DoseUnit.MILLIGRAM), stored[1].amount)
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun viewModel(
        repository: MedicationRepository = this.repository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = MedicationFormViewModel(
        repository = repository,
        savedStateHandle = savedStateHandle,
        amountParser = AmountParser(Locale.UK),
        clock = clock,
    )

    /** The handle navigation would build for the details route. */
    private fun editHandle(id: MedicationId) = SavedStateHandle(mapOf("medicationId" to id.value))

    private fun editing(id: MedicationId) = viewModel(savedStateHandle = editHandle(id))

    private fun metoprolol() = NewMedication(
        name = "Metoprolol",
        defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.SPECIALIST,
        schedules = listOf(
            Schedule.EveryNHours(Quantity.of("40", DoseUnit.MILLIGRAM), 12, LocalTime.of(8, 0)),
            Schedule.EveryNDays(Quantity.of("20", DoseUnit.MILLIGRAM), 1, listOf(LocalTime.of(9, 0))),
        ),
    )

    private fun fillValidForm(viewModel: MedicationFormViewModel, name: String = "Metoprolol") {
        viewModel.onNameChange(name)
        viewModel.onDoseTextChange("40")
        viewModel.onDoseUnitChange(DoseUnit.MILLIGRAM)
    }

    private fun addEveryTwelveHours(viewModel: MedicationFormViewModel, amount: String) {
        viewModel.openSchedule(null)
        viewModel.onAmountTextChange(amount)
        viewModel.onAmountUnitChange(DoseUnit.MILLIGRAM)
        viewModel.onPatternChange(SchedulePattern.EVERY_N_HOURS)
        viewModel.onIntervalHoursChange(12)
        viewModel.commitSchedule()
    }

    private fun MedicationRepository.snapshot(): List<Medication> =
        runBlocking { observeAll().first() }

    /** Every save fails, so the failure path can be exercised. */
    private object FailingRepository : MedicationRepository {
        override fun observeAll(): Flow<List<Medication>> = flowOf(emptyList())
        override suspend fun get(id: MedicationId): Medication? = null
        override suspend fun update(medication: Medication): Unit = error("no database")
        override suspend fun add(medication: NewMedication): MedicationId =
            throw IllegalStateException("no database")

        override suspend fun setActive(id: MedicationId, isActive: Boolean): Unit =
            throw IllegalStateException("no database")
    }
}
