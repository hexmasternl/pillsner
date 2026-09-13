package nl.hexmaster.pillsner.ui.medicines

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Resource selection for every summary shape (spec: Human-readable schedule description).
 * Needs a real `Context`, so it runs on a device; the collapsing rules themselves are unit-tested.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDescriptionFormatterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val formatter = ScheduleDescriptionFormatter(context, Locale.UK)

    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)
    private val oneTablet = Quantity.of("1", DoseUnit.TABLET)
    private val twoTablets = Quantity.of("2", DoseUnit.TABLET)
    private val ml25 = Quantity.of("2.5", DoseUnit.MILLILITRE)
    private val ml5 = Quantity.of("5", DoseUnit.MILLILITRE)
    private val mg20 = Quantity.of("20", DoseUnit.MILLIGRAM)

    @Test
    fun onceADay() {
        assertEquals("40 mg once a day", formatter.describe(ScheduleSummary.TimesPerDay(1), mg40))
    }

    @Test
    fun twiceADay() {
        assertEquals("1 tablet twice a day", formatter.describe(ScheduleSummary.TimesPerDay(2), oneTablet))
    }

    @Test
    fun threeTimesADay() {
        assertEquals(
            "2 tablets 3 times a day",
            formatter.describe(ScheduleSummary.TimesPerDay(3), twoTablets),
        )
    }

    @Test
    fun aDecimalAmount() {
        assertEquals("2.5 ml once a day", formatter.describe(ScheduleSummary.TimesPerDay(1), ml25))
    }

    @Test
    fun onceADayOnThreeDays() {
        val summary = ScheduleSummary.TimesPerDayOnDays(
            count = 1,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        assertEquals("40 mg once a day on Mon, Wed, Fri", formatter.describe(summary, mg40))
    }

    @Test
    fun twiceADayOnWeekdays() {
        val summary = ScheduleSummary.TimesPerDayOnDays(
            count = 2,
            days = ScheduleDescriptionFormatter.WORKING_WEEK,
        )

        assertEquals("40 mg twice a day on weekdays", formatter.describe(summary, mg40))
    }

    @Test
    fun threeTimesADayOnTwoDays() {
        val summary = ScheduleSummary.TimesPerDayOnDays(
            count = 3,
            days = setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        )

        assertEquals("40 mg 3 times a day on Mon, Thu", formatter.describe(summary, mg40))
    }

    @Test
    fun daysAreListedInTheLocaleOwnWeekOrder() {
        val weekend = ScheduleSummary.TimesPerDayOnDays(
            count = 1,
            days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )

        // The United Kingdom starts its week on Monday, so the weekend reads Saturday first.
        assertEquals("20 mg once a day on Sat, Sun", formatter.describe(weekend, mg20))
        // The United States starts on Sunday, and the same schedule reads the other way round.
        assertEquals(
            "20 mg once a day on Sun, Sat",
            ScheduleDescriptionFormatter(context, Locale.US).describe(weekend, mg20),
        )
    }

    @Test
    fun onceEveryOtherDay() {
        assertEquals(
            "40 mg once every other day",
            formatter.describe(ScheduleSummary.TimesEveryOtherDay(1), mg40),
        )
    }

    @Test
    fun twiceEveryOtherDay() {
        assertEquals(
            "40 mg twice every other day",
            formatter.describe(ScheduleSummary.TimesEveryOtherDay(2), mg40),
        )
    }

    @Test
    fun onceEveryThreeDays() {
        assertEquals(
            "40 mg once every 3 days",
            formatter.describe(ScheduleSummary.TimesEveryNDays(1, 3), mg40),
        )
    }

    @Test
    fun twiceEveryThreeDays() {
        assertEquals(
            "40 mg twice every 3 days",
            formatter.describe(ScheduleSummary.TimesEveryNDays(2, 3), mg40),
        )
    }

    @Test
    fun everyEightHours() {
        assertEquals("5 ml every 8 hours", formatter.describe(ScheduleSummary.EveryNHours(8), ml5))
    }

    @Test
    fun asNeeded() {
        assertEquals("40 mg as needed", formatter.describe(ScheduleSummary.AsNeeded, mg40))
    }
}
