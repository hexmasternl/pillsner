package nl.hexmaster.pillsner.domain.validation

import java.math.BigDecimal
import java.time.LocalDate

/** What is wrong with one field of the add-medicine form. The UI maps these to string resources. */
enum class MedicationFieldError {
    NAME_REQUIRED,
    DOSE_REQUIRED,
    DOSE_NOT_A_NUMBER,
    DOSE_NOT_POSITIVE,

    /** The default dose unit was changed while stock batches, whose strengths depend on it, exist. */
    DOSE_UNIT_LOCKED_BY_STOCK,
    USE_UNTIL_BEFORE_USED_SINCE,

    /** A stock quantity in tablets or capsules was not a whole number (`medicine-stock-tracking`). */
    STOCK_NOT_WHOLE_PILLS,
}

/** Every field error the form currently has, keyed by the field it belongs to. */
data class MedicationValidation(
    val name: MedicationFieldError? = null,
    val defaultDose: MedicationFieldError? = null,
    val useUntil: MedicationFieldError? = null,
) {
    val isValid: Boolean get() = name == null && defaultDose == null && useUntil == null
}

/**
 * The rules of the add-medicine form, in plain Kotlin so they are unit-tested without Android and
 * cannot drift from the `Medication` constructor's own checks (design D8).
 *
 * The amount arrives as the already-parsed decimal the locale-aware field produced, or null when
 * the text could not be parsed, so parsing stays in the UI where the locale is known.
 */
object MedicationFormValidator {

    fun validate(
        name: String,
        doseText: String,
        doseValue: BigDecimal?,
        usedSince: LocalDate,
        useUntil: LocalDate?,
    ): MedicationValidation = MedicationValidation(
        name = if (name.isBlank()) MedicationFieldError.NAME_REQUIRED else null,
        defaultDose = validateAmount(doseText, doseValue),
        useUntil = if (useUntil != null && useUntil.isBefore(usedSince)) {
            MedicationFieldError.USE_UNTIL_BEFORE_USED_SINCE
        } else {
            null
        },
    )

    /** Shared with the schedule editor, which applies the same rules to a schedule's amount. */
    fun validateAmount(text: String, value: BigDecimal?): MedicationFieldError? = when {
        text.isBlank() -> MedicationFieldError.DOSE_REQUIRED
        value == null -> MedicationFieldError.DOSE_NOT_A_NUMBER
        value <= BigDecimal.ZERO -> MedicationFieldError.DOSE_NOT_POSITIVE
        else -> null
    }
}
