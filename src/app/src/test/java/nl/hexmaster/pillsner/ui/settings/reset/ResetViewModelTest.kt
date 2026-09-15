package nl.hexmaster.pillsner.ui.settings.reset

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.domain.reset.EraseAllData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec: app-reset, the confirmation contract (design D8). */
@OptIn(ExperimentalCoroutinesApi::class)
class ResetViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var erases = 0

    private val eraseAllData = EraseAllData(
        eraser = { erases++ },
        teardown = { },
        history = { },
        refresh = { },
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `confirming while the box is unticked erases nothing`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        viewModel.onResetTapped()

        viewModel.onConfirmed()
        advanceUntilIdle()

        assertEquals("The tick is the gate, not a suggestion", 0, erases)
        assertTrue("And the dialog stays open", viewModel.uiState.value.dialogVisible)
    }

    @Test
    fun `ticking then confirming erases once`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        viewModel.onResetTapped()
        viewModel.onConfirmationToggled(true)

        viewModel.onConfirmed()
        advanceUntilIdle()

        assertEquals(1, erases)
    }

    @Test
    fun `a second confirm while the first is running erases nothing more`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        viewModel.onResetTapped()
        viewModel.onConfirmationToggled(true)

        viewModel.onConfirmed()
        // Not yet idle: the erase is in flight, which is exactly when a double tap lands.
        viewModel.onConfirmed()
        advanceUntilIdle()

        assertEquals("A double tap erases once", 1, erases)
    }

    @Test
    fun `dismissing clears the tick, so reopening starts unticked`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        viewModel.onResetTapped()
        viewModel.onConfirmationToggled(true)

        viewModel.onDismiss()

        assertFalse(viewModel.uiState.value.dialogVisible)
        assertFalse(viewModel.uiState.value.confirmationAccepted)
        assertEquals("Cancel erases nothing", 0, erases)

        viewModel.onResetTapped()
        assertFalse(viewModel.uiState.value.confirmationAccepted)
        assertFalse(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun `an erase closes the dialog and leaves it unticked for next time`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        viewModel.onResetTapped()
        viewModel.onConfirmationToggled(true)

        viewModel.onConfirmed()
        advanceUntilIdle()

        assertEquals(ResetUiState(), viewModel.uiState.value)
    }

    @Test
    fun `Erased is emitted exactly once per erase`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        val effects = mutableListOf<ResetEffect>()
        // Unconfined, so the collector is subscribed before anything is emitted; with the standard
        // dispatcher it would not start until the scheduler ran it, which is after the send.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects += it }
        }
        advanceUntilIdle()

        viewModel.onResetTapped()
        viewModel.onConfirmationToggled(true)
        viewModel.onConfirmed()
        advanceUntilIdle()

        assertEquals(listOf(ResetEffect.Erased), effects)
    }

    @Test
    fun `the effect is not replayed to a later collector`() = runTest(dispatcher) {
        val viewModel = ResetViewModel(eraseAllData)
        viewModel.onResetTapped()
        viewModel.onConfirmationToggled(true)
        viewModel.onConfirmed()
        advanceUntilIdle()

        // A channel, so the one waiting message is delivered to the screen that comes back — and
        // delivered once, never again after that.
        assertEquals(ResetEffect.Erased, viewModel.effects.first())

        val later = mutableListOf<ResetEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { later += it }
        }
        advanceUntilIdle()

        assertTrue("A configuration change must not read as a second reset", later.isEmpty())
    }
}
