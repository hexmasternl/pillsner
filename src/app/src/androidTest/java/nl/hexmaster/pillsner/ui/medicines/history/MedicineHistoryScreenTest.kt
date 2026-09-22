package nl.hexmaster.pillsner.ui.medicines.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.NumberFormat
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import nl.hexmaster.pillsner.domain.history.SummariseTimeDeviation
import nl.hexmaster.pillsner.domain.history.SummariseUsageHistory
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.UsagePeriod
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the usage history screen shows for a known record (spec: medicine-usage-history).
 *
 * The figures come from the real summariser over a fixed set of doses and a fixed clock, so the
 * screen is tested against the same arithmetic the app uses rather than against a hand-made state.
 * Percentages and dates are compared against the same formatters the screen uses, so the test says
 * the same thing in every locale the device may be in.
 */
@RunWith(AndroidJUnit4::class)
class MedicineHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")

    /** Monday 14 September 2026, lunchtime: a dose due this evening has not happened yet. */
    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val clock: Clock =
        Clock.fixed(ZonedDateTime.of(today, LocalTime.NOON, amsterdam).toInstant(), amsterdam)
    private val summarise = SummariseUsageHistory(clock) { DayOfWeek.MONDAY }
    private val summariseTimeDeviation = SummariseTimeDeviation(clock) { DayOfWeek.MONDAY }
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    private val locale: Locale = Locale.getDefault()
    private val percent = NumberFormat.getPercentInstance(locale)
    private val longDate: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)

    @Test
    fun theScreenShowsTheMedicineTheTitleTheSegmentsAndTheFigures() {
        showScreen(doses = threeTakenOfFourThisWeek())

        composeRule.onNodeWithTag(MedicineHistoryTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineHistoryTestTags.MEDICINE_NAME).assertIsDisplayed()
        composeRule.onNodeWithText("Metoprolol").assertIsDisplayed()
        composeRule.onNodeWithTag(period(UsagePeriod.WEEK)).assertIsSelected()
        composeRule.onNodeWithTag(period(UsagePeriod.MONTH)).assertIsDisplayed()
        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).assertIsDisplayed()

        // Four scheduled, three taken: 75 %.
        composeRule.onNodeWithText(percent.format(0.75)).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineHistoryTestTags.SCHEDULED).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineHistoryTestTags.TAKEN).assertIsDisplayed()
        composeRule.onNodeWithText("Scheduled").assertIsDisplayed()
        // "Taken" twice on purpose: the count above, and the legend row for the same category.
        composeRule.onAllNodesWithText("Taken").assertCountEquals(2)
    }

    @Test
    fun choosingThreeMonthsRecomputesTheFiguresAndTheChart() {
        showScreen(doses = threeTakenOfFourThisWeek() + twoTakenLastMonth())

        composeRule.onNodeWithText("By day").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).performClick()

        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).assertIsSelected()
        composeRule.onNodeWithText("By week").performScrollTo().assertIsDisplayed()
        // Six scheduled, five taken: 83 %.
        composeRule.onNodeWithText(percent.format(5.0 / 6.0)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theEmptyStateAppearsAndWideningThePeriodReplacesItWithTheFigures() {
        showScreen(doses = twoTakenLastMonth())

        composeRule.onNodeWithTag(MedicineHistoryTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("Nothing recorded for this period").assertIsDisplayed()
        composeRule.onAllNodesWithTag(MedicineHistoryTestTags.ADHERENCE).assertCountEquals(0)

        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).performClick()

        composeRule.onAllNodesWithTag(MedicineHistoryTestTags.EMPTY).assertCountEquals(0)
        composeRule.onNodeWithTag(MedicineHistoryTestTags.ADHERENCE).assertIsDisplayed()
    }

    @Test
    fun theRecordsStartNoteAppearsWhenTheMedicineIsYoungerThanThePeriod() {
        val doses = threeTakenOfFourThisWeek()

        showScreen(doses = doses, earliestRecordedAt = doses.minOf { it.scheduledAt })

        composeRule.onNodeWithTag(MedicineHistoryTestTags.RECORDS_START)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun theRecordsStartNoteIsAbsentWhenTheRecordsReachBackFurther() {
        showScreen(
            doses = threeTakenOfFourThisWeek(),
            earliestRecordedAt = at(today.minusMonths(6)),
        )

        composeRule.onAllNodesWithTag(MedicineHistoryTestTags.RECORDS_START).assertCountEquals(0)
    }

    @Test
    fun everyBarNamesItsBucketAndItsCounts() {
        showScreen(doses = threeTakenOfFourThisWeek())

        // Monday 14 September holds one dose, taken.
        composeRule.onNode(hasContentDescription("${today.format(longDate)}, 1 of 1 dose taken"))
            .assertIsDisplayed()
        // Tuesday 8 September, the first day of the window, holds none.
        composeRule.onNode(
            hasContentDescription("${today.minusDays(6).format(longDate)}, no doses scheduled"),
        ).assertIsDisplayed()
    }

    @Test
    fun aWeeklyBarNamesTheWeekItStartsOn() {
        showScreen(doses = threeTakenOfFourThisWeek())

        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).performClick()

        // Thursday, Friday and Saturday all sit in the week that opened on Monday 7 September:
        // two of those three were taken.
        composeRule.onNode(
            hasContentDescription("Week of ${today.minusDays(7).format(longDate)}, 2 of 3 doses taken"),
        ).assertIsDisplayed()
    }

    @Test
    fun theLegendLeavesOutTheCategoriesThePeriodHasNoneOf() {
        showScreen(doses = threeTakenOfFourThisWeek())

        // Three taken and one missed, no skipped and nothing still unanswered.
        composeRule.onNodeWithTag(row(UsageCategory.TAKEN)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(row(UsageCategory.MISSED)).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag(row(UsageCategory.SKIPPED)).assertCountEquals(0)
        composeRule.onAllNodesWithTag(row(UsageCategory.UNANSWERED)).assertCountEquals(0)
    }

    @Test
    fun theTimingAccuracyChartShowsTheOverallFigureAndItsCaption() {
        showScreen(doses = mixedTimingThisWeek())

        // (0 + 10 + 6) minutes over three taken doses averages to 5.
        composeRule.onNodeWithTag(MedicineHistoryTestTags.TIMING_CHART_HEADER)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineHistoryTestTags.TIMING_OVERALL)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("5 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Lower is better").assertIsDisplayed()
    }

    @Test
    fun theTimingAccuracyChartIsOmittedWhenNothingWasTaken() {
        showScreen(doses = oneSkippedThisWeek())

        // A skipped dose means the period is not empty, but nothing was taken to time.
        composeRule.onNodeWithTag(MedicineHistoryTestTags.ADHERENCE).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MedicineHistoryTestTags.TIMING_CHART_HEADER).assertCountEquals(0)
    }

    @Test
    fun aTimingBarNamesItsBucketTheAverageAndTheDoseCount() {
        showScreen(doses = mixedTimingThisWeek())

        // Tuesday 8 September (today minus 6) holds no taken dose.
        composeRule.onNode(
            hasContentDescription("${today.minusDays(6).format(longDate)}, no doses recorded taken"),
        ).assertIsDisplayed()
        // today minus 3 was taken ten minutes late: one dose backs its average.
        composeRule.onNode(
            hasContentDescription(
                "${today.minusDays(3).format(longDate)}, 10 minutes off schedule on average, over 1 dose",
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun choosingThreeMonthsBucketsTheTimingChartByWeekWhileAMonthStaysDaily() {
        showScreen(doses = mixedTimingThisWeek())

        composeRule.onNodeWithTag(period(UsagePeriod.MONTH)).performClick()
        composeRule.onNodeWithText("Timing accuracy, by day").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).performClick()
        composeRule.onNodeWithText("Timing accuracy, by week").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bothChartsShowAValueAxisStatingItsUnit() {
        showScreen(doses = mixedTimingThisWeek())

        // Outcome chart: the busiest day holds one scheduled dose.
        composeRule.onAllNodesWithText("1 dose").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("0 doses").onFirst().assertIsDisplayed()
        // Timing chart: the busiest bucket averaged ten minutes off schedule.
        composeRule.onNodeWithText("10 minutes").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("0 minutes").onFirst().performScrollTo().assertIsDisplayed()
    }

    // --- Fixtures -------------------------------------------------------------------------

    /** Four doses this week: three taken, one missed. The first day of the window holds none. */
    private fun threeTakenOfFourThisWeek(): List<Dose> = listOf(
        dose(1, at(today.minusDays(4)), IntakeOutcome.TAKEN),
        dose(2, at(today.minusDays(3)), IntakeOutcome.TAKEN),
        dose(3, at(today.minusDays(2)), IntakeOutcome.MISSED),
        dose(4, at(today), IntakeOutcome.TAKEN),
    )

    /** Two more taken doses, over a month back, outside the week window. */
    private fun twoTakenLastMonth(): List<Dose> = listOf(
        dose(5, at(today.minusDays(40)), IntakeOutcome.TAKEN),
        dose(6, at(today.minusDays(30)), IntakeOutcome.TAKEN),
    )

    /**
     * Three taken doses this week with known deviations - on time, ten minutes late, six minutes
     * early - and one missed dose that contributes no timing data. Overall average: (0 + 10 + 6) /
     * 3, rounded to 5 minutes; the busiest single bucket is the ten-minute one.
     */
    private fun mixedTimingThisWeek(): List<Dose> = listOf(
        takenWithDeviation(1, at(today.minusDays(4)), deviationMinutes = 0),
        takenWithDeviation(2, at(today.minusDays(3)), deviationMinutes = 10),
        dose(3, at(today.minusDays(2)), IntakeOutcome.MISSED),
        takenWithDeviation(4, at(today), deviationMinutes = -6),
    )

    /** One skipped dose this week: the period is not empty, but nothing was taken. */
    private fun oneSkippedThisWeek(): List<Dose> = listOf(
        dose(1, at(today.minusDays(2)), IntakeOutcome.SKIPPED),
    )

    private fun dose(id: Long, at: Instant, outcome: IntakeOutcome) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Metoprolol",
        amount = mg40,
        scheduledAt = at,
        intake = Intake(outcome, at),
    )

    /** A taken dose recorded [deviationMinutes] after its scheduled moment; negative is early. */
    private fun takenWithDeviation(id: Long, scheduledAt: Instant, deviationMinutes: Long) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Metoprolol",
        amount = mg40,
        scheduledAt = scheduledAt,
        intake = Intake(IntakeOutcome.TAKEN, scheduledAt.plusSeconds(deviationMinutes * 60)),
    )

    private fun at(date: LocalDate): Instant =
        ZonedDateTime.of(date, LocalTime.of(8, 0), amsterdam).toInstant()

    private fun period(period: UsagePeriod) = MedicineHistoryTestTags.PERIOD_PREFIX + period.name

    private fun row(category: UsageCategory) = UsageBreakdownTestTags.ROW_PREFIX + category.name

    /**
     * The screen with its period held here, so tapping a segment recomputes the record the same way
     * the view model does: one window per selection, summarised from the stored doses.
     */
    private fun showScreen(doses: List<Dose>, earliestRecordedAt: Instant? = null) {
        composeRule.setContent {
            Screen(doses = doses, earliestRecordedAt = earliestRecordedAt)
        }
    }

    @Composable
    private fun Screen(doses: List<Dose>, earliestRecordedAt: Instant?) {
        var period by remember { mutableStateOf(UsagePeriod.WEEK) }

        PillsnerTheme {
            MedicineHistoryScreen(
                uiState = MedicineHistoryUiState(
                    medicineName = "Metoprolol",
                    period = period,
                    history = summarise(period, doses, earliestRecordedAt),
                    timeDeviation = summariseTimeDeviation(period, doses),
                    isLoading = false,
                ),
                onPeriodSelected = { period = it },
                onBack = {},
            )
        }
    }
}
