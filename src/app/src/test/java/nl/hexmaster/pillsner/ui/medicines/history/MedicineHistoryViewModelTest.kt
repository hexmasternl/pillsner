package nl.hexmaster.pillsner.ui.medicines.history

import androidx.lifecycle.SavedStateHandle
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.history.SummariseTimeDeviation
import nl.hexmaster.pillsner.domain.history.SummariseUsageHistory
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.UsagePeriod
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Choosing a period, and keeping that choice (spec: medicine-usage-history).
 *
 * Rotation keeps the view model itself, so the interesting case is the one process death leaves
 * behind: a brand new view model built from the same `SavedStateHandle` has to come back on the
 * period the user had chosen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicineHistoryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val clock = MutableTestClock(
        ZonedDateTime.of(today, LocalTime.NOON, amsterdam).toInstant(),
        amsterdam,
    )

    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)
    private val medications = InMemoryMedicationRepository()
    private var storedId: MedicationId? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the week period is what the screen opens on`() = runTest {
        val viewModel = viewModel(SavedStateHandle(), dosesLastMonth())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(UsagePeriod.WEEK, state.period)
        assertEquals("Metoprolol", state.medicineName)
        // Two doses in the last seven days, one of them taken.
        assertEquals(2, state.history?.scheduled)
        assertEquals(1, state.history?.taken)
        // The one taken dose in the last seven days was recorded at exactly its scheduled moment.
        assertEquals(0, state.timeDeviation?.averageMinutes)
    }

    @Test
    fun `choosing a longer period recomputes the figures`() = runTest {
        val viewModel = viewModel(SavedStateHandle(), dosesLastMonth())
        viewModel.uiState.first { !it.isLoading }

        viewModel.onPeriodSelected(UsagePeriod.MONTH)

        val state = viewModel.uiState.first { it.period == UsagePeriod.MONTH }
        assertEquals(4, state.history?.scheduled)
        assertEquals(3, state.history?.taken)
        assertEquals(0, state.timeDeviation?.averageMinutes)
    }

    @Test
    fun `no taken dose in the period leaves timing accuracy empty`() = runTest {
        val id = storedMedicationId()
        val onlyMissed = listOf(
            Dose(
                id = DoseId(1),
                medicationId = id,
                medicationName = "Metoprolol",
                amount = mg40,
                scheduledAt = day(3),
                intake = Intake(IntakeOutcome.MISSED, day(3)),
            ),
        )

        val viewModel = viewModel(SavedStateHandle(), onlyMissed)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(true, state.timeDeviation?.isEmpty)
    }

    @Test
    fun `the chosen period comes back after process death`() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle, dosesLastMonth()).onPeriodSelected(UsagePeriod.THREE_MONTHS)

        // A second view model on the same handle is what the process is restored with.
        val restored = viewModel(handle, dosesLastMonth())

        assertEquals(UsagePeriod.THREE_MONTHS, restored.uiState.first { !it.isLoading }.period)
    }

    @Test
    fun `a medicine that cannot be read closes the screen`() = runTest {
        val handle = SavedStateHandle(mapOf("medicationId" to 404L))
        val viewModel = MedicineHistoryViewModel(
            medicationRepository = medications,
            doseRepository = InMemoryDoseRepository(),
            summarise = SummariseUsageHistory(clock) { DayOfWeek.MONDAY },
            summariseTimeDeviation = SummariseTimeDeviation(clock) { DayOfWeek.MONDAY },
            savedStateHandle = handle,
            clock = clock,
        )

        assertEquals(MedicineHistoryEffect.OpenFailed, viewModel.effects.first())
    }

    // --- Helpers ---------------------------------------------------------------------------

    /** Four doses spread over the last month, two of them inside the last week. */
    private suspend fun dosesLastMonth(): List<Dose> {
        val id = storedMedicationId()
        var nextId = 1L
        return listOf(
            day(20) to IntakeOutcome.TAKEN,
            day(12) to IntakeOutcome.TAKEN,
            day(3) to IntakeOutcome.TAKEN,
            day(1) to IntakeOutcome.MISSED,
        ).map { (at, outcome) ->
            Dose(
                id = DoseId(nextId++),
                medicationId = id,
                medicationName = "Metoprolol",
                amount = mg40,
                scheduledAt = at,
                intake = Intake(outcome, at),
            )
        }
    }

    /** Eight in the morning, [daysAgo] days before today. */
    private fun day(daysAgo: Long): Instant =
        ZonedDateTime.of(today.minusDays(daysAgo), LocalTime.of(8, 0), amsterdam).toInstant()

    private suspend fun storedMedicationId(): MedicationId {
        storedId?.let { return it }
        val id = medications.add(
            NewMedication(
                name = "Metoprolol",
                defaultDose = mg40,
                usedSince = today.minusMonths(6),
                useUntil = null,
                prescribedBy = Prescriber.GENERAL_PRACTITIONER,
                schedules = emptyList(),
            ),
        )
        storedId = id
        return id
    }

    private suspend fun viewModel(handle: SavedStateHandle, doses: List<Dose>): MedicineHistoryViewModel {
        val id = storedMedicationId()
        handle["medicationId"] = id.value
        return MedicineHistoryViewModel(
            medicationRepository = medications,
            doseRepository = InMemoryDoseRepository(doses),
            summarise = SummariseUsageHistory(clock) { DayOfWeek.MONDAY },
            summariseTimeDeviation = SummariseTimeDeviation(clock) { DayOfWeek.MONDAY },
            savedStateHandle = handle,
            clock = clock,
        )
    }
}
