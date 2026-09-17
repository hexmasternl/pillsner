package nl.hexmaster.pillsner.ui.medicines.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
    fun theUsageChartAxisShowsTheBusiestBucketsCountAndZero() {
        showScreen(doses = threeTakenOfFourThisWeek())

        // Every bucket in this fixture holds at most one dose, so the busiest is 1.
        composeRule.onNodeWithTag(UsageChartTestTags.AXIS_TOP).assertTextEquals("1 dose")
        composeRule.onNodeWithTag(UsageChartTestTags.AXIS_BOTTOM).assertTextEquals("0 doses")
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
    fun theTimingAccuracyCardShowsTheAverageAndTheCaption() {
        showScreen(doses = threeTakenOfFourThisWeek())

        composeRule.onNodeWithTag(MedicineHistoryTestTags.TIMING_HEADER)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineHistoryTestTags.TIMING_AVERAGE)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("On average, how far off schedule a dose was taken. Less is better.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun everyTimingBarNamesItsAverageAndTheDosesItCovers() {
        showScreen(doses = threeTakenOfFourThisWeek())

        // Monday 14 September holds one taken dose, recorded exactly on time.
        composeRule.onNode(
            hasContentDescription("${today.format(longDate)}, 0 minutes off schedule on average, over 1 dose taken"),
        ).performScrollTo().assertIsDisplayed()
        // Tuesday 8 September, the first day of the window, has no taken dose.
        composeRule.onNode(
            hasContentDescription("${today.minusDays(6).format(longDate)}, no dose taken"),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aWeeklyTimingBarNamesTheWeekAndHowManyDosesItAverages() {
        showScreen(doses = threeTakenOfFourThisWeek() + twoTakenLastMonth())

        composeRule.onNodeWithTag(period(UsagePeriod.THREE_MONTHS)).performClick()

        // The week opening Monday 7 September holds two taken doses (10 and 11 September); the
        // 12 September dose in the same week was missed and contributes nothing here.
        composeRule.onNode(
            hasContentDescription(
                "Week of ${today.minusDays(7).format(longDate)}, 0 minutes off schedule on average, over 2 doses taken",
            ),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theTimingAccuracyChartAxisShowsTheBusiestBucketsAverageAndZero() {
        showScreen(doses = listOf(doseTakenLate(1, at(today), minutesLate = 22)))

        composeRule.onNodeWithTag(TimeDeviationChartTestTags.AXIS_TOP)
            .performScrollTo()
            .assertTextEquals("22 minutes")
        composeRule.onNodeWithTag(TimeDeviationChartTestTags.AXIS_BOTTOM)
            .performScrollTo()
            .assertTextEquals("0 minutes")
    }

    @Test
    fun theTimingAccuracyCardIsAbsentWhenNothingWasTaken() {
        showScreen(doses = listOf(dose(1, at(today.minusDays(2)), IntakeOutcome.MISSED)))

        composeRule.onAllNodesWithTag(MedicineHistoryTestTags.TIMING_HEADER).assertCountEquals(0)
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

    private fun dose(id: Long, at: Instant, outcome: IntakeOutcome) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Metoprolol",
        amount = mg40,
        scheduledAt = at,
        intake = Intake(outcome, at),
    )

    private fun doseTakenLate(id: Long, scheduledAt: Instant, minutesLate: Long) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Metoprolol",
        amount = mg40,
        scheduledAt = scheduledAt,
        intake = Intake(IntakeOutcome.TAKEN, scheduledAt.plusSeconds(minutesLate * 60)),
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
