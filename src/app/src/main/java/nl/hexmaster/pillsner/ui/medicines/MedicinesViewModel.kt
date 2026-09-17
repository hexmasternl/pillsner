package nl.hexmaster.pillsner.ui.medicines

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.model.LabelScanResult
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.model.summarize
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * Turns the unordered medication stream into the two ordered sections the Medicines screen shows
 * (design D4). Partitioning and ordering are presentation concerns, so they live here and not in
 * the repository.
 *
 * Also fronts the "Scan medicine label" shortcut (medicine-add-label-scan design D3): the screen
 * hands this view model a decoded photo and gets back a best-effort prefill to navigate with. That
 * is the only thing this view model does with a scan; deciding what to prefill or how to validate
 * it is the Add medicine form's job, unchanged from a typed value.
 *
 * @param locale the locale whose collation orders the names; injectable so tests are deterministic.
 * @param recognizeLabel recognizes and parses a photographed label; injectable so a UI test can
 *   substitute a fixed result without touching ML Kit (`AppContainer` wires the real
 *   `ScanMedicineLabel` use case here).
 */
class MedicinesViewModel(
    private val repository: MedicationRepository,
    locale: Locale = Locale.getDefault(),
    private val recognizeLabel: suspend (Bitmap, Int) -> LabelScanResult = { _, _ -> LabelScanResult() },
) : ViewModel() {

    private val _effects = Channel<MedicinesEffect>(Channel.BUFFERED)

    /** One-shot messages for the screen, such as a failed update. */
    val effects: Flow<MedicinesEffect> = _effects.receiveAsFlow()

    private val collator: Collator = Collator.getInstance(locale).apply {
        // Ignore case and accents so "paracetamol" sorts with "Paracetamol".
        strength = Collator.SECONDARY
    }

    private val byName: Comparator<MedicineTileState> = Comparator { left, right ->
        collator.compare(left.name, right.name)
    }

    val uiState: StateFlow<MedicinesUiState> = repository
        .observeAll()
        .map { medications ->
            val (active, inactive) = medications.map { it.toTileState() }.partition { it.isActive }
            MedicinesUiState(
                active = active.sortedWith(byName),
                inactive = inactive.sortedWith(byName),
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = MedicinesUiState(),
        )

    /**
     * Starts or stops the medication with [id].
     *
     * Deliberately not optimistic: Room re-emits within milliseconds, so the tile moves fast enough
     * on its own, and a second source of truth would need a rollback path for the failure case.
     */
    fun onSetActive(id: MedicationId, isActive: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setActive(id, isActive) }.onFailure { error ->
                // The exception type only: anything more could carry the medicine name.
                Log.d(TAG, "Could not set medication ${id.value} active=$isActive: ${error::class.simpleName}")
                _effects.send(MedicinesEffect.UpdateFailed)
            }
        }
    }

    /**
     * Recognizes text from [bitmap] and turns it into a best-effort [LabelScanResult].
     *
     * A plain suspend function, not routed through [effects]: the caller needs the value itself to
     * build the `navigate` call into a fresh add-medicine form, so this is not a fire-and-forget
     * event the way [MedicinesEffect] is. Never throws: a recognition failure comes back as a
     * [LabelScanResult] with every field null, exactly like "nothing recognized" does.
     *
     * @param rotationDegrees the clockwise rotation, in multiples of 90, needed to make [bitmap]
     *   upright; the caller owns EXIF correction before decoding.
     */
    suspend fun scanLabel(bitmap: Bitmap, rotationDegrees: Int = 0): LabelScanResult =
        recognizeLabel(bitmap, rotationDegrees)

    private fun Medication.toTileState() = MedicineTileState(
        id = id,
        name = name,
        schedules = toScheduleLines(),
        isActive = isActive,
    )

    /** One line per schedule; a medicine with none reads as needed, at its default dose. */
    private fun Medication.toScheduleLines(): List<ScheduleLine> =
        if (schedules.isEmpty()) {
            listOf(ScheduleLine(ScheduleSummary.AsNeeded, defaultDose))
        } else {
            schedules.map { ScheduleLine(it.summarize(), it.amount) }
        }

    private companion object {
        const val TAG = "Medicines"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
