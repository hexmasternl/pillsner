package nl.hexmaster.pillsner.ui.medicines.labelscan

import androidx.lifecycle.ViewModelStore
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.labelscan.FrameRecogniser
import nl.hexmaster.pillsner.data.labelscan.GreyFrame
import nl.hexmaster.pillsner.domain.labelscan.InterpretLabelText
import nl.hexmaster.pillsner.domain.labelscan.RecognisedLine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The live scan session (design D3): acceptance, shutter, hints, and the recogniser's lifetime. */
@OptIn(ExperimentalCoroutinesApi::class)
class LabelScanViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 9, 29)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val recogniser = FakeRecogniser()
    private val frame = GreyFrame(ByteArray(4), 2, 2)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the recogniser makes the screen ready`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isReady)
        assertEquals(1, recogniser.openCalls)
    }

    @Test
    fun `a recogniser that cannot open marks the scan failed`() = runTest(dispatcher) {
        recogniser.failOpen = true
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.isReady)
    }

    @Test
    fun `the shutter before any read returns an empty interpretation`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onShutter()

        val finished = effects.single() as LabelScanEffect.Finished
        assertTrue(finished.interpretation.isEmpty)
        assertEquals(today, finished.interpretation.usedSince)
        assertFalse(finished.accepted)
        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun `two consecutive good frames accept the later one`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        recogniser.responses += goodLabel()
        recogniser.responses += goodLabel()

        viewModel.analyse(frame)
        assertTrue(effects.isEmpty())
        viewModel.analyse(frame)

        val finished = effects.single() as LabelScanEffect.Finished
        assertTrue(finished.accepted)
        assertEquals("ZORVALEX", finished.interpretation.name)
        assertTrue(viewModel.uiState.value.hasReading)
    }

    @Test
    fun `a misread between two good frames keeps the scan going`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        recogniser.responses += goodLabel()
        recogniser.responses += goodLabel(name = "ZORVALEK")
        recogniser.responses += goodLabel()

        repeat(3) { viewModel.analyse(frame) }

        assertTrue(effects.isEmpty())
        assertFalse(viewModel.uiState.value.finished)
    }

    @Test
    fun `a camera stop between two good frames breaks the streak`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        recogniser.responses += goodLabel()
        recogniser.responses += goodLabel()
        recogniser.responses += goodLabel()

        viewModel.analyse(frame)
        viewModel.onCameraStopped()
        viewModel.analyse(frame)
        assertTrue("the first frame after the pause must not pair with one from before it", effects.isEmpty())
        viewModel.analyse(frame)

        assertEquals(1, effects.size)
    }

    @Test
    fun `a frame that was being read when the camera stopped is discarded`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        recogniser.responses += goodLabel()
        recogniser.responses += goodLabel()
        recogniser.responses += goodLabel()

        // The stop lands while the first frame is inside the recogniser: that frame is old news.
        recogniser.duringRecognise = { viewModel.onCameraStopped() }
        viewModel.analyse(frame)
        recogniser.duringRecognise = {}
        assertFalse("a frame from the stopped session must not count as a reading", viewModel.uiState.value.hasReading)

        viewModel.analyse(frame)
        assertTrue("the first frame after the stop has nothing to pair with", effects.isEmpty())
        viewModel.analyse(frame)

        assertEquals(1, effects.size)
    }

    @Test
    fun `a cancel that lands while the engine is opening leaves it closed`() = runTest(dispatcher) {
        val io = StandardTestDispatcher(dispatcher.scheduler)
        val viewModel = viewModel(io = io)

        viewModel.onCancel()
        advanceUntilIdle()

        assertEquals(1, recogniser.openCalls)
        assertTrue(recogniser.closed)
        assertFalse(viewModel.uiState.value.isReady)
    }

    @Test
    fun `the shutter with a partial read returns that partial read`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        recogniser.responses += listOf(RecognisedLine("Zorvalex", 80f))

        viewModel.analyse(frame)
        viewModel.onShutter()

        val finished = effects.single() as LabelScanEffect.Finished
        assertEquals("Zorvalex", finished.interpretation.name)
        assertNull(finished.interpretation.defaultDose)
        assertFalse(finished.accepted)
    }

    @Test
    fun `frames after the scan finished are not recognised`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        viewModel.onShutter()
        recogniser.responses += goodLabel()

        viewModel.analyse(frame)

        assertEquals(1, effects.size)
        assertEquals(1, recogniser.responses.size)
    }

    @Test
    fun `frames before the recogniser is open are skipped`() = runTest(dispatcher) {
        // The open runs on a dispatcher that only moves when told to, so the first frame arrives first.
        val viewModel = viewModel(io = StandardTestDispatcher(dispatcher.scheduler))
        recogniser.responses += goodLabel()

        viewModel.analyse(frame)

        assertEquals(1, recogniser.responses.size)
        assertFalse(viewModel.uiState.value.hasReading)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun `the hint changes after eight seconds and again after twenty`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(ScanHint.HOLD_STEADY, viewModel.uiState.value.hint)

        advanceTimeBy(LabelScanViewModel.ADJUST_HINT_AFTER_MILLIS + 1)
        assertEquals(ScanHint.ADJUST, viewModel.uiState.value.hint)

        advanceTimeBy(LabelScanViewModel.SHUTTER_HINT_AFTER_MILLIS - LabelScanViewModel.ADJUST_HINT_AFTER_MILLIS)
        assertEquals(ScanHint.TRY_SHUTTER, viewModel.uiState.value.hint)
    }

    @Test
    fun `cancelling stops the recogniser and the shutter no longer reports anything`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onCancel()
        viewModel.onShutter()

        assertTrue(recogniser.stopped)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `the torch follows the camera's flash unit`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val torchStates = mutableListOf<Boolean>()

        viewModel.onCameraBound(hasFlashUnit = true) { torchStates += it }
        assertTrue(viewModel.uiState.value.torchAvailable)
        viewModel.onTorchToggled()

        assertEquals(listOf(true), torchStates)
        assertTrue(viewModel.uiState.value.torchOn)
    }

    @Test
    fun `a camera that cannot be bound marks the scan failed`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onCameraFailed()

        assertTrue(viewModel.uiState.value.failed)
    }

    @Test
    fun `clearing the view model closes the recogniser`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val store = ViewModelStore()
        store.put("scan", viewModel)

        store.clear()
        assertTrue(viewModel.analysisExecutor.awaitTermination(2, TimeUnit.SECONDS))

        assertTrue(recogniser.stopped)
        assertTrue(recogniser.closed)
    }

    private fun viewModel(io: CoroutineDispatcher = dispatcher) = LabelScanViewModel(
        recogniser = recogniser,
        interpret = InterpretLabelText(),
        clock = clock,
        ioDispatcher = io,
    )

    private fun TestScope.collect(viewModel: LabelScanViewModel): List<LabelScanEffect> {
        val effects = mutableListOf<LabelScanEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        return effects
    }

    private fun goodLabel(name: String = "ZORVALEX") = listOf(
        RecognisedLine("$name 50 MG TABLETS", 90f),
        RecognisedLine("Take 1 tablet twice daily", 85f),
    )

    /** A recogniser that answers each frame from a queue and never touches native code. */
    private class FakeRecogniser : FrameRecogniser {
        var openCalls = 0
        var failOpen = false
        var stopped = false
        var closed = false
        val responses = ArrayDeque<List<RecognisedLine>>()

        override var isOpen: Boolean = false
            private set

        override fun open() {
            openCalls++
            if (failOpen) throw IllegalStateException("cannot open")
            isOpen = true
        }

        /** Runs in the middle of a recognition, standing in for the main thread acting meanwhile. */
        var duringRecognise: () -> Unit = {}

        override fun recognise(frame: GreyFrame): List<RecognisedLine> {
            duringRecognise()
            return responses.removeFirstOrNull() ?: emptyList()
        }

        override fun stop() {
            stopped = true
        }

        override fun close() {
            closed = true
            isOpen = false
        }
    }
}
