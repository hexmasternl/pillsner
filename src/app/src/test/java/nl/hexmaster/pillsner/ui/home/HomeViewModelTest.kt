package nl.hexmaster.pillsner.ui.home

import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockBatchRepository
import nl.hexmaster.pillsner.data.stock.InMemoryStockWarningQueue
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.model.TestFixtures
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository
import nl.hexmaster.pillsner.domain.stock.EvaluateStockWarning
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val now: Instant = Instant.parse("2026-09-11T10:00:00Z")
    private val clock = MutableTestClock(now)
    private val repository = FakeUpcomingDosesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with no doses`() = runTest {
        val viewModel = HomeViewModel(repository, clock = clock)

        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertEquals(emptyList<UpcomingDose>(), state.upcomingDoses)
    }

    @Test
    fun `empty repository yields empty state`() = runTest(dispatcher) {
        val viewModel = collecting()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(emptyList<UpcomingDose>(), state.upcomingDoses)
    }

    @Test
    fun `three doses yield three tiles in scheduled order`() = runTest(dispatcher) {
        val doses = dosesAtHours(6, 2, 4)
        repository.emit(doses)
        val viewModel = collecting()

        val shown = viewModel.uiState.value.upcomingDoses

        assertEquals(3, shown.size)
        assertEquals(doses.sortedBy { it.scheduledAt }, shown)
    }

    @Test
    fun `eight doses yield the five earliest`() = runTest(dispatcher) {
        val doses = dosesAtHours(8, 3, 5, 1, 7, 2, 6, 4)
        repository.emit(doses)
        val viewModel = collecting()

        val shown = viewModel.uiState.value.upcomingDoses

        assertEquals(5, shown.size)
        assertEquals(doses.sortedBy { it.scheduledAt }.take(5), shown)
    }

    @Test
    fun `an overdue unanswered dose comes first`() = runTest(dispatcher) {
        val overdue = dose(id = 9, hoursFromNow = -2).copy(isOverdue = true)
        repository.emit(dosesAtHours(3, 1) + overdue)
        val viewModel = collecting()

        assertEquals(overdue, viewModel.uiState.value.upcomingDoses.first())
    }

    @Test
    fun `an answered dose disappears when the repository stops emitting it`() = runTest(dispatcher) {
        repository.emit(dosesAtHours(1, 2))
        val viewModel = collecting()
        assertEquals(2, viewModel.uiState.value.upcomingDoses.size)

        repository.emit(dosesAtHours(2))

        assertEquals(1, viewModel.uiState.value.upcomingDoses.size)
    }

    @Test
    fun `view model asks the repository for at most five doses`() = runTest(dispatcher) {
        collecting()

        assertEquals(listOf(HomeViewModel.MAX_UPCOMING_DOSES), repository.requestedLimits)
    }

    @Test
    fun `reminders are reported as unreliable when notifications are off`() = runTest(dispatcher) {
        val viewModel = collecting()

        viewModel.onNotificationPermissionChecked(false)

        assertTrue(viewModel.uiState.value.remindersAreUnreliable)
        assertFalse(viewModel.uiState.value.notificationsAllowed)
    }

    @Test
    fun `reminders are reported as unreliable when alarms are inexact`() = runTest(dispatcher) {
        val exactness = MutableStateFlow(false)
        val viewModel = HomeViewModel(repository, exactness, clock)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.value.remindersAreUnreliable)

        exactness.value = true

        assertFalse(viewModel.uiState.value.remindersAreUnreliable)
    }

    @Test
    fun `reminders are reported as unreliable once a reminder has been missed`() {
        val state = HomeUiState(isLoading = false, reminderWasMissed = true, now = now)

        assertTrue(state.remindersAreUnreliable)
        assertEquals(ReminderProblem.SILENTLY_MISSED_REMINDER, state.reminderProblem)
    }

    @Test
    fun `not being exempt from battery optimisation is no longer a problem in itself`() {
        // The banner is raised by evidence now: nothing has been missed, so there is nothing to
        // say, however the phone is configured (design D2).
        val state = HomeUiState(isLoading = false, now = now)

        assertFalse(state.remindersAreUnreliable)
        assertEquals(null, state.reminderProblem)
    }

    @Test
    fun `the banner reports the most severe problem and only that one`() {
        val everythingWrong = HomeUiState(
            isLoading = false,
            notificationsAllowed = false,
            alarmsAreExact = false,
            reminderWasMissed = true,
            now = now,
        )

        // Without the permission there is no reminder at all, so it is reported first. Fixing each
        // problem reveals the next, which is what makes one banner enough.
        assertEquals(ReminderProblem.NOTIFICATIONS_DENIED, everythingWrong.reminderProblem)

        val notificationsFixed = everythingWrong.copy(notificationsAllowed = true)
        assertEquals(ReminderProblem.SILENTLY_MISSED_REMINDER, notificationsFixed.reminderProblem)

        val acknowledged = notificationsFixed.copy(reminderWasMissed = false)
        assertEquals(ReminderProblem.INEXACT_ALARMS, acknowledged.reminderProblem)

        val nothingWrong = acknowledged.copy(alarmsAreExact = true)
        assertEquals(null, nothingWrong.reminderProblem)
        assertFalse(nothingWrong.remindersAreUnreliable)
    }

    @Test
    fun `a dose in the future is due, a passed one overdue, a postponed one snoozed`() {
        val future = dose(id = 1, hoursFromNow = 2)
        val passed = dose(id = 2, hoursFromNow = -1)
        val snoozed = dose(id = 3, hoursFromNow = -1).copy(snoozedUntil = now.plus(Duration.ofMinutes(15)))

        assertEquals(IntakeStatus.Due, future.status(now))
        assertEquals(IntakeStatus.Overdue, passed.status(now))
        assertEquals(IntakeStatus.Snoozed, snoozed.status(now))
    }

    @Test
    fun `a renamed medicine reaches the tile without the screen being reopened`() = runTest(dispatcher) {
        repository.emit(listOf(dose(id = 1, hoursFromNow = 2)))
        val viewModel = collecting()
        assertEquals("Medicine 1", viewModel.uiState.value.upcomingDoses.single().medicationName)

        repository.emit(listOf(dose(id = 1, hoursFromNow = 2).copy(medicationName = "Ibuprofen 400")))

        assertEquals("Ibuprofen 400", viewModel.uiState.value.upcomingDoses.single().medicationName)
    }

    @Test
    fun `a deferred warning names the expiry of the batch the take drew from, even once it is empty`() =
        runTest(dispatcher) {
            val medication = TestFixtures.medication(id = 1L)
            val medications = InMemoryMedicationRepository().apply { upsert(medication) }
            // The batch the take drew from is used up; what is left is far from expiry.
            val batches = InMemoryStockBatchRepository(
                listOf(
                    stockBatch(1, remaining = "0", expiry = LocalDate.of(2026, 9, 20)),
                    stockBatch(2, remaining = "400", expiry = LocalDate.of(2027, 6, 1)),
                ),
            )
            val queue = InMemoryStockWarningQueue(mapOf(medication.id to LocalDate.of(2026, 9, 20)))
            val viewModel = HomeViewModel(
                repository,
                clock = clock,
                stockWarningQueue = queue,
                evaluateStockWarning = EvaluateStockWarning(medications, batches, clock = clock),
                medicationRepository = medications,
            )
            backgroundScope.launch { viewModel.uiState.collect {} }

            assertEquals(BatchExpiryState.APPROACHING, viewModel.uiState.value.stockWarning?.expiryState)
        }

    private fun stockBatch(id: Long, remaining: String, expiry: LocalDate) = StockBatch(
        id = StockBatchId(id),
        medicationId = MedicationId(1),
        remaining = BigDecimal(remaining),
        unit = DoseUnit.MILLIGRAM,
        strengthPerUnit = BigDecimal.ONE,
        expiryDate = expiry,
        addedAt = Instant.EPOCH,
    )

    private fun kotlinx.coroutines.test.TestScope.collecting(): HomeViewModel {
        val viewModel = HomeViewModel(repository, clock = clock)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun dosesAtHours(vararg hoursFromNow: Int): List<UpcomingDose> =
        hoursFromNow.mapIndexed { index, hours -> dose(index.toLong(), hours) }

    private fun dose(id: Long, hoursFromNow: Int) = UpcomingDose(
        doseId = DoseId(id),
        medicationName = "Medicine $id",
        amount = Quantity.of("1", DoseUnit.TABLET),
        scheduledAt = now.plus(Duration.ofHours(hoursFromNow.toLong())),
    )

    /** Emits whatever the test hands it, ignoring the limit on purpose so the view model's cap is exercised. */
    private class FakeUpcomingDosesRepository : UpcomingDosesRepository {
        private val doses = MutableStateFlow<List<UpcomingDose>>(emptyList())
        val requestedLimits = mutableListOf<Int>()

        fun emit(list: List<UpcomingDose>) {
            doses.value = list
        }

        override fun observeUpcoming(limit: Int): Flow<List<UpcomingDose>> {
            requestedLimits += limit
            return doses.map { it }
        }
    }
}
