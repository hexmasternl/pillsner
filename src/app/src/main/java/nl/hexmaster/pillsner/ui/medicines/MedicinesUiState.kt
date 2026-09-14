package nl.hexmaster.pillsner.ui.medicines

import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary

/**
 * What the Medicines screen shows (design D4).
 *
 * @property active active medicines, ordered by name.
 * @property inactive inactive medicines, ordered by name.
 * @property isLoading true until the first list has arrived, so the empty state does not flash
 *   before real data is known.
 */
data class MedicinesUiState(
    val active: List<MedicineTileState> = emptyList(),
    val inactive: List<MedicineTileState> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * One medicine as the overview needs it. Holds [ScheduleSummary]s rather than text so the wording
 * stays in the UI layer, where string resources live and a locale change re-renders correctly.
 *
 * @property schedules one line per schedule, in the medicine's own order. A medicine with no
 *   schedules has exactly one line: as needed, with the medicine's default dose.
 */
data class MedicineTileState(
    val id: MedicationId,
    val name: String,
    val schedules: List<ScheduleLine>,
    val isActive: Boolean,
)

/** One schedule of a medicine, as the tile describes it: how much, how often. */
data class ScheduleLine(
    val summary: ScheduleSummary,
    val amount: Quantity,
)
