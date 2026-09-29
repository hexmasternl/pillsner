package nl.hexmaster.pillsner.ui.medicines.labelscan

import android.util.Log
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.data.labelscan.FrameCropper
import nl.hexmaster.pillsner.data.labelscan.FrameRecogniser
import nl.hexmaster.pillsner.data.labelscan.GreyFrame
import nl.hexmaster.pillsner.domain.labelscan.InterpretLabelText
import nl.hexmaster.pillsner.domain.labelscan.LabelInterpretation
import nl.hexmaster.pillsner.domain.labelscan.ScanAcceptance

/** What the instruction line under the viewfinder says (design D3, "Guidance"). */
enum class ScanHint {
    /** From the start: hold the label inside the frame. */
    HOLD_STEADY,

    /** After eight seconds without acceptance: move closer, add light or hold still. */
    ADJUST,

    /** After twenty seconds: point at the shutter, which also gains emphasis. */
    TRY_SHUTTER,
}

/** What the scanning screen shows. Nothing here is the recognised text. */
data class LabelScanUiState(
    /** The recogniser is open and frames are being read. */
    val isReady: Boolean = false,
    /** The recogniser could not start; only Cancel is useful. */
    val failed: Boolean = false,
    val torchAvailable: Boolean = false,
    val torchOn: Boolean = false,
    val hint: ScanHint = ScanHint.HOLD_STEADY,
    /** At least one frame yielded something, so the shutter has something to return. */
    val hasReading: Boolean = false,
    /** The scan has finished and the screen is about to close. */
    val finished: Boolean = false,
)

/** Happens once; the navigation layer acts on it. */
sealed interface LabelScanEffect {
    /**
     * The scan ended with an interpretation to hand to the form: accepted automatically, or taken
     * by the shutter (then possibly empty).
     */
    data class Finished(val interpretation: LabelInterpretation, val accepted: Boolean) : LabelScanEffect
}

/**
 * Runs one live scan session (medicine-label-photo-prefill design D3): opens the recogniser,
 * reads every frame the camera hands over on [analysisExecutor], applies the two-frame acceptance
 * rule, and closes the recogniser when the screen goes.
 *
 * The camera itself is bound by the screen to its own lifecycle (so backgrounding releases it);
 * this view model only receives frames and the preview's surface requests. Nothing here logs what
 * was read: the log lines are "opened", "finished after N frames" and "cancelled".
 *
 * @param recogniser the app's one recogniser; opened here, closed in [onCleared].
 * @param ioDispatcher where the blocking [FrameRecogniser.open] runs.
 */
class LabelScanViewModel(
    private val recogniser: FrameRecogniser,
    private val interpret: InterpretLabelText,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val guide: FrameCropper.Guide = FrameCropper.Guide.DEFAULT,
) : ViewModel() {

    /** One thread for analysis: frames are recognised one at a time and dropped while busy. */
    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val acceptance = ScanAcceptance()

    @Volatile
    private var latest: LabelInterpretation? = null

    /**
     * Guards every path that ends the scan. Acceptance runs on the analysis thread and the shutter,
     * Cancel and clearing on the main thread; under this lock exactly one of them wins, so a result
     * is never emitted twice or after a cancel.
     */
    private val terminalLock = Any()

    @Volatile
    private var finished = false

    private var setTorch: ((Boolean) -> Unit)? = null

    private val _uiState = MutableStateFlow(LabelScanUiState())
    val uiState: StateFlow<LabelScanUiState> = _uiState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)

    /** The preview's current surface request, for `CameraXViewfinder`. */
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _effects = Channel<LabelScanEffect>(Channel.BUFFERED)
    val effects: Flow<LabelScanEffect> = _effects.receiveAsFlow()

    init {
        Log.d(TAG, "Scan opened")
        viewModelScope.launch(ioDispatcher) {
            try {
                recogniser.open()
                // A cancel that landed while open() was blocking may have run onCleared()'s close()
                // against a not-yet-installed engine; the one just installed must not stay resident.
                if (finished) {
                    recogniser.close()
                    return@launch
                }
                _uiState.update { it.copy(isReady = true) }
            } catch (failure: IllegalStateException) {
                Log.d(TAG, "Recogniser failed to open")
                _uiState.update { it.copy(failed = true) }
            }
        }
        viewModelScope.launch {
            delay(ADJUST_HINT_AFTER_MILLIS)
            if (!finished) _uiState.update { it.copy(hint = ScanHint.ADJUST) }
            delay(SHUTTER_HINT_AFTER_MILLIS - ADJUST_HINT_AFTER_MILLIS)
            if (!finished) _uiState.update { it.copy(hint = ScanHint.TRY_SHUTTER) }
        }
    }

    /** The preview use case's surface provider hands its requests here. */
    fun onSurfaceRequest(request: SurfaceRequest) {
        _surfaceRequest.value = request
    }

    /**
     * Reads one upright, cropped frame. Runs on [analysisExecutor]; the screen's analyzer closes
     * the `ImageProxy` afterwards, so the next frame can arrive.
     */
    fun analyse(frame: GreyFrame) {
        if (finished || !recogniser.isOpen) return
        val lines = recogniser.recognise(frame)
        if (finished) return
        val interpretation = interpret(lines, LocalDate.now(clock))
        latest = interpretation
        if (!interpretation.isEmpty) _uiState.update { it.copy(hasReading = true) }
        acceptance.offer(interpretation)?.let { accepted -> finish(accepted, accepted = true) }
    }

    /**
     * The camera session stopped (the app went to the background) and will restart with the
     * lifecycle. The streak is forgotten: a good frame from before the pause must not pair with the
     * first frame after it to satisfy the two-consecutive-frames rule.
     */
    fun onCameraStopped() {
        latest = null
        acceptance.reset()
    }

    /** The camera is bound: remember how to drive the torch, and whether there is one. */
    fun onCameraBound(hasFlashUnit: Boolean, setTorch: (Boolean) -> Unit) {
        this.setTorch = setTorch
        _uiState.update { it.copy(torchAvailable = hasFlashUnit, torchOn = false) }
    }

    /** The camera could not be bound at all; the screen says so and only Cancel is useful. */
    fun onCameraFailed() {
        _uiState.update { it.copy(failed = true) }
    }

    fun onTorchToggled() {
        val on = !_uiState.value.torchOn
        setTorch?.invoke(on)
        _uiState.update { it.copy(torchOn = on) }
    }

    /** Takes the most recent frame's interpretation, good or not; empty when nothing was read yet. */
    fun onShutter() {
        finish(latest ?: LabelInterpretation.empty(LocalDate.now(clock)), accepted = false)
    }

    /** The user leaves without a result; stops a frame in progress so the screen closes at once. */
    fun onCancel() {
        if (!claimFinish()) return
        recogniser.stop()
        Log.d(TAG, "Scan cancelled")
    }

    private fun finish(interpretation: LabelInterpretation, accepted: Boolean) {
        if (!claimFinish()) return
        _uiState.update { it.copy(finished = true) }
        _effects.trySend(LabelScanEffect.Finished(interpretation, accepted))
        Log.d(TAG, "Scan finished after ${acceptance.framesSeen} frames, accepted=$accepted")
    }

    /** Atomically ends the scan; true for the one caller that got there first. */
    private fun claimFinish(): Boolean = synchronized(terminalLock) {
        if (finished) return false
        finished = true
        true
    }

    override fun onCleared() {
        claimFinish()
        recogniser.stop()
        // Queued behind any frame still being recognised on the same thread, so the engine is
        // never released under a running recognition.
        analysisExecutor.execute { recogniser.close() }
        analysisExecutor.shutdown()
    }

    companion object {
        private const val TAG = "LabelScan"

        /** Design D3: after eight seconds the hint changes. */
        const val ADJUST_HINT_AFTER_MILLIS = 8_000L

        /** Design D3: after twenty seconds the shutter gains emphasis. */
        const val SHUTTER_HINT_AFTER_MILLIS = 20_000L
    }
}
