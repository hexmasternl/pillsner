package nl.hexmaster.pillsner.ui.dose

import androidx.lifecycle.SavedStateHandle
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.InMemoryTransactionRunner
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockWarningQueue
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.intake.AnswerDose
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.intake.DoseTiming
import nl.hexmaster.pillsner.domain.intake.RecordIntake
import nl.hexmaster.pillsner.domain.intake.SnoozeDose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses
import nl.hexmaster.pillsner.domain.stock.ConsumeStockOnTaken
import nl.hexmaster.pillsner.domain.stock.EvaluateStockWarning
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import nl.hexmaster.pillsner.ui.home.UpcomingDoseTimeFormatter
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec: dose-detail, what the screen shows and what each answer does. */
@OptIn(ExperimentalCoroutinesApi::class)
class DoseDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Two hours before the dose, so it starts out early and the boundary is an hour away. */
    private val clock = MutableTestClock(at(hour = 6), amsterdam)
    private val answered = mutableListOf<DoseId>()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a pending dose is answerable, with its timing`() = runTest(dispatcher) {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        val viewModel = viewModelFor(doses)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DoseDetailUiState.Answerable)
        state as DoseDetailUiState.Answerable
        assertEquals("Ibuprofen", state.medicationName)
        assertEquals(DoseTiming.EARLY, state.timing)
        assertEquals(IntakeStatus.Due, state.status)
    }

    @Test
    fun `an answered dose is settled, with the outcome and the moment it was recorded`() = runTest(dispatcher) {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        doses.recordIntake(DoseId(1), IntakeOutcome.TAKEN, at(hour = 7, minute = 58))
        val viewModel = viewModelFor(doses)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DoseDetailUiState.Settled)
        state as DoseDetailUiState.Settled
        assertEquals(IntakeOutcome.TAKEN, state.outcome)
        assertEquals(at(hour = 7, minute = 58), state.recordedAt)
        assertEquals(IntakeStatus.Taken, state.status)
    }

    @Test
    fun `a dose withdrawn while the screen is open is gone`() = runTest(dispatcher) {
        val doses = InMemoryDoseRepository()
        val viewModel = viewModelFor(doses)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(DoseDetailUiState.Gone, viewModel.uiState.value)
    }

    @Test
    fun `an answer recorded elsewhere moves the screen from answerable to settled`() = runTest(dispatcher) {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        val viewModel = viewModelFor(doses)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DoseDetailUiState.Answerable)

        // The user answers from the notification shade while this screen is in front of them.
        doses.recordIntake(DoseId(1), IntakeOutcome.SKIPPED, at(hour = 6, minute = 30))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DoseDetailUiState.Settled)
        assertEquals(IntakeOutcome.SKIPPED, (state as DoseDetailUiState.Settled).outcome)
    }

    @Test
    fun `each answer is recorded as that answer, and then the screen closes`() = runTest(dispatcher) {
        val recorded = DoseAnswer.entries.associateWith { answer ->
            val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
            val viewModel = viewModelFor(doses)

            viewModel.onAnswer(answer)
            advanceUntilIdle()

            assertEquals("$answer should close the screen", DoseDetailEffect.Close, viewModel.effects.first())
            checkNotNull(doses.get(DoseId(1)))
        }

        assertEquals(IntakeOutcome.TAKEN, recorded.getValue(DoseAnswer.TAKEN).intake?.outcome)
        assertEquals(IntakeOutcome.SKIPPED, recorded.getValue(DoseAnswer.SKIP).intake?.outcome)
        val snoozed = recorded.getValue(DoseAnswer.SNOOZE)
        assertEquals("A snooze is not an outcome", null, snoozed.intake)
        assertEquals(at(hour = 6, minute = 15), snoozed.snoozedUntil)
        // The effects that must follow an answer ran once for each of the three.
        assertEquals(DoseAnswer.entries.size, answered.size)
    }

    @Test
    fun `the timing changes on its own as the clock crosses the boundary`() = runTest(dispatcher) {
        val doses = InMemoryDoseRepository(listOf(dose(1, at(hour = 8))))
        val viewModel = viewModelFor(doses)
        backgroundScope.launch { viewModel.uiState.collect {} }
        // runCurrent, not advanceUntilIdle: idling the scheduler would run the waiting boundary
        // tick straight away and the test would never see the wait it exists to prove.
        runCurrent()
        assertEquals(DoseTiming.EARLY, (viewModel.uiState.value as DoseDetailUiState.Answerable).timing)

        // An hour of virtual time passes with nothing happening to the dose at all. Exactly an hour
        // before is still early, so the clock lands just the other side of the boundary.
        clock.setTo(at(hour = 7, minute = 1))
        advanceTimeBy(ONE_HOUR_MILLIS + 1)
        runCurrent()

        assertEquals(DoseTiming.ON_TIME, (viewModel.uiState.value as DoseDetailUiState.Answerable).timing)
    }

    private fun viewModelFor(doses: InMemoryDoseRepository) = DoseDetailViewModel(
        doseRepository = doses,
        // The real use case, so what the screen does is what the notification does.
        answerDose = AnswerDose(
            doseRepository = doses,
            recordIntake = RecordIntake(doses, clock),
            snoozeDose = SnoozeDose(doses, MarkMissedDoses(doses, clock), clock),
            // No medication or stock batch is registered for these fixture doses, so this is a
            // no-op; stock behaviour has its own test suite.
            consumeStockOnTaken = run {
                val medications = InMemoryMedicationRepository()
                val batches = InMemoryStockBatchRepository()
                ConsumeStockOnTaken(
                    stockBatchRepository = batches,
                    medicationRepository = medications,
                    stockWarningQueue = InMemoryStockWarningQueue(),
                    evaluateStockWarning = EvaluateStockWarning(medications, batches, clock = clock),
                )
            },
            transactionRunner = InMemoryTransactionRunner(),
            onAnswered = { answered += it.id },
        ),
        savedStateHandle = SavedStateHandle(mapOf(DoseDetailViewModel.DOSE_ID_ARG to 1L)),
        clock = clock,
        timeFormatter = UpcomingDoseTimeFormatter(ZoneOffset.UTC, Locale.UK, clock),
    )

    private companion object {
        const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    }
}
