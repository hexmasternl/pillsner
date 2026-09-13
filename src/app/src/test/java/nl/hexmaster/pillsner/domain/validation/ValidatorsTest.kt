package nl.hexmaster.pillsner.domain.validation

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The form and editor rules, checked without Android (design D8). */
class ValidatorsTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 13)
    private val eight: LocalTime = LocalTime.of(8, 0)

    // --- Form -----------------------------------------------------------------------------

    @Test
    fun aFilledInForm_isValid() {
        val result = MedicationFormValidator.validate("Ibuprofen", "40", BigDecimal("40"), today, null)

        assertTrue(result.isValid)
    }

    @Test
    fun aBlankName_isReported() {
        val result = MedicationFormValidator.validate("  ", "40", BigDecimal("40"), today, null)

        assertEquals(MedicationFieldError.NAME_REQUIRED, result.name)
    }

    @Test
    fun anEmptyDose_isReported() {
        val result = MedicationFormValidator.validate("Ibuprofen", "", null, today, null)

        assertEquals(MedicationFieldError.DOSE_REQUIRED, result.defaultDose)
    }

    @Test
    fun aDoseThatIsNotANumber_isReported() {
        val result = MedicationFormValidator.validate("Ibuprofen", "abc", null, today, null)

        assertEquals(MedicationFieldError.DOSE_NOT_A_NUMBER, result.defaultDose)
    }

    @Test
    fun aZeroDose_isReported() {
        val result = MedicationFormValidator.validate("Ibuprofen", "0", BigDecimal.ZERO, today, null)

        assertEquals(MedicationFieldError.DOSE_NOT_POSITIVE, result.defaultDose)
    }

    @Test
    fun anEndDateBeforeTheStart_isReported() {
        val result = MedicationFormValidator.validate(
            name = "Ibuprofen",
            doseText = "40",
            doseValue = BigDecimal("40"),
            usedSince = today,
            useUntil = today.minusDays(5),
        )

        assertEquals(MedicationFieldError.USE_UNTIL_BEFORE_USED_SINCE, result.useUntil)
    }

    // --- Schedule editor ------------------------------------------------------------------

    @Test
    fun anEveryNDaysScheduleWithATime_isValid() {
        val errors = ScheduleDraftValidator.validate(
            pattern = SchedulePattern.EVERY_N_DAYS,
            amountText = "40",
            amountValue = BigDecimal("40"),
            times = listOf(eight),
            days = emptySet(),
        )

        assertEquals(emptyList<ScheduleDraftError>(), errors)
    }

    @Test
    fun anEveryNDaysScheduleWithoutTimes_reportsTimesRequired() {
        val errors = ScheduleDraftValidator.validate(
            pattern = SchedulePattern.EVERY_N_DAYS,
            amountText = "40",
            amountValue = BigDecimal("40"),
            times = emptyList(),
            days = emptySet(),
        )

        assertEquals(listOf(ScheduleDraftError.TIMES_REQUIRED), errors)
    }

    @Test
    fun aWeekdaysScheduleWithoutDays_reportsDaysRequired() {
        val errors = ScheduleDraftValidator.validate(
            pattern = SchedulePattern.ON_WEEKDAYS,
            amountText = "40",
            amountValue = BigDecimal("40"),
            times = listOf(eight),
            days = emptySet(),
        )

        assertEquals(listOf(ScheduleDraftError.DAYS_REQUIRED), errors)
    }

    @Test
    fun aWeekdaysScheduleOnAllSevenDays_isRejected() {
        val errors = ScheduleDraftValidator.validate(
            pattern = SchedulePattern.ON_WEEKDAYS,
            amountText = "40",
            amountValue = BigDecimal("40"),
            times = listOf(eight),
            days = DayOfWeek.entries.toSet(),
        )

        assertEquals(listOf(ScheduleDraftError.ALL_SEVEN_DAYS), errors)
    }

    @Test
    fun theAmountIsCheckedBeforeThePattern() {
        val errors = ScheduleDraftValidator.validate(
            pattern = SchedulePattern.EVERY_N_DAYS,
            amountText = "",
            amountValue = null,
            times = emptyList(),
            days = emptySet(),
        )

        assertEquals(
            listOf(ScheduleDraftError.AMOUNT_REQUIRED, ScheduleDraftError.TIMES_REQUIRED),
            errors,
        )
    }

    @Test
    fun anEveryNHoursScheduleAlwaysHasEnoughToSave() {
        val errors = ScheduleDraftValidator.validate(
            pattern = SchedulePattern.EVERY_N_HOURS,
            amountText = "40",
            amountValue = BigDecimal("40"),
            times = emptyList(),
            days = emptySet(),
        )

        assertEquals(emptyList<ScheduleDraftError>(), errors)
    }
}
