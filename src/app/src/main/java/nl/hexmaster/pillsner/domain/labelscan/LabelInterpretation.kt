package nl.hexmaster.pillsner.domain.labelscan

import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule

/**
 * What a medicine label said, as far as the interpretation could tell (medicine-label-photo-prefill
 * design D4). Every optional field that is null means "leave the form field as it is"; nothing here
 * is ever a guess.
 *
 * @property name the medicine name, or null when no line qualified.
 * @property defaultDose the strength on the label (such as 50 mg), or the count from the
 *   instruction line (such as 1 tablet) when the label carries no strength, or null.
 * @property schedules the prescribed dose as schedules; empty when no frequency and amount were
 *   found together.
 * @property usedSince the most recent past date on the label within a year, otherwise today.
 * @property useUntil an explicit end date or the end of a stated course, or null.
 * @property rawText the recognised lines joined with newlines, for the "Show text" sheet only.
 */
data class LabelInterpretation(
    val name: String? = null,
    val defaultDose: Quantity? = null,
    val schedules: List<Schedule> = emptyList(),
    val usedSince: LocalDate,
    val useUntil: LocalDate? = null,
    val rawText: String = "",
) {
    /** True when the label yielded nothing worth putting on the form: no name, dose or schedule. */
    val isEmpty: Boolean get() = name == null && defaultDose == null && schedules.isEmpty()

    /** Debug only. Never log this: it is the user's medical data. */
    override fun toString(): String =
        "LabelInterpretation(name=${if (name == null) "absent" else "present"}, " +
            "defaultDose=${if (defaultDose == null) "absent" else "present"}, schedules=${schedules.size})"

    companion object {
        /** The interpretation of a label that said nothing usable, starting today. */
        fun empty(today: LocalDate) = LabelInterpretation(usedSince = today)
    }
}
