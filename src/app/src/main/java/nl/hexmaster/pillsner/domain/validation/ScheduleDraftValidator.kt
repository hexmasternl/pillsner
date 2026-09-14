package nl.hexmaster.pillsner.domain.validation

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.Schedule

/** The three timing patterns the schedule editor offers. */
enum class SchedulePattern { EVERY_N_DAYS, ON_WEEKDAYS, EVERY_N_HOURS }

/** What stops the schedule under construction from being saved. The UI maps these to resources. */
enum class ScheduleDraftError {
    AMOUNT_REQUIRED,
    AMOUNT_NOT_A_NUMBER,
    AMOUNT_NOT_POSITIVE,
    TIMES_REQUIRED,
    DAYS_REQUIRED,
    ALL_SEVEN_DAYS,
}

/**
 * The rules of the schedule editor, in plain Kotlin so they are unit-tested without Android
 * (design D8). The first error is what the preview area shows, so order matters: the amount is
 * checked before the pattern's own inputs.
 */
object ScheduleDraftValidator {

    /** Every problem with the draft, most important first; empty when it can be saved. */
    fun validate(
        pattern: SchedulePattern,
        amountText: String,
        amountValue: BigDecimal?,
        times: List<LocalTime>,
        days: Set<DayOfWeek>,
    ): List<ScheduleDraftError> = buildList {
        when (MedicationFormValidator.validateAmount(amountText, amountValue)) {
            MedicationFieldError.DOSE_REQUIRED -> add(ScheduleDraftError.AMOUNT_REQUIRED)
            MedicationFieldError.DOSE_NOT_A_NUMBER -> add(ScheduleDraftError.AMOUNT_NOT_A_NUMBER)
            MedicationFieldError.DOSE_NOT_POSITIVE -> add(ScheduleDraftError.AMOUNT_NOT_POSITIVE)
            else -> Unit
        }

        when (pattern) {
            SchedulePattern.EVERY_N_DAYS -> if (times.isEmpty()) add(ScheduleDraftError.TIMES_REQUIRED)

            SchedulePattern.ON_WEEKDAYS -> {
                when {
                    days.isEmpty() -> add(ScheduleDraftError.DAYS_REQUIRED)
                    days.size == Schedule.DAYS_IN_WEEK -> add(ScheduleDraftError.ALL_SEVEN_DAYS)
                }
                if (times.isEmpty()) add(ScheduleDraftError.TIMES_REQUIRED)
            }

            // The first dose time and the interval always hold a value, so nothing else can fail.
            SchedulePattern.EVERY_N_HOURS -> Unit
        }
    }
}
