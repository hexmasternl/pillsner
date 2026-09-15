package nl.hexmaster.pillsner.ui.dose

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.intake.DoseTiming
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.home.DayLabel
import nl.hexmaster.pillsner.ui.home.FormattedDoseTime
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: dose-detail, what the screen shows and what it lets the user do. */
@RunWith(AndroidJUnit4::class)
class DoseDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val amount = Quantity.of("40", DoseUnit.MILLIGRAM)

    @Test
    fun aPendingDose_showsItsFourControls() {
        setScreen(answerable(DoseTiming.ON_TIME))

        composeRule.onNodeWithTag(DoseDetailTestTags.TIME).assertIsDisplayed()
        composeRule.onNodeWithTag(DoseDetailTestTags.NAME).assertIsDisplayed()
        listOf(
            DoseDetailTestTags.TAKEN,
            DoseDetailTestTags.SNOOZE,
            DoseDetailTestTags.SKIP,
            DoseDetailTestTags.CLOSE,
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed().assertHasClickAction()
        }
        composeRule.onNodeWithText("I took it").assertIsDisplayed()
        composeRule.onNodeWithText("Not yet").assertIsDisplayed()
        composeRule.onNodeWithText("Not going to").assertIsDisplayed()
    }

    @Test
    fun eachButtonReportsItsOwnAnswer() {
        val answers = mutableListOf<DoseAnswer>()
        setScreen(answerable(DoseTiming.ON_TIME), onAnswer = { answers += it })

        composeRule.onNodeWithTag(DoseDetailTestTags.TAKEN).performClick()
        composeRule.onNodeWithTag(DoseDetailTestTags.SNOOZE).performClick()
        composeRule.onNodeWithTag(DoseDetailTestTags.SKIP).performClick()

        assertEquals(listOf(DoseAnswer.TAKEN, DoseAnswer.SNOOZE, DoseAnswer.SKIP), answers)
    }

    @Test
    fun onTime_showsNoWarning() {
        setScreen(answerable(DoseTiming.ON_TIME))

        composeRule.onAllNodesWithTag(DoseTimingBannerTestTags.BANNER).assertCountEquals(0)
    }

    @Test
    fun early_saysSoAndStillLetsTheDoseBeRecorded() {
        setScreen(answerable(DoseTiming.EARLY))

        composeRule.onNodeWithTag(DoseTimingBannerTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("This dose is not due yet. You can still record it.").assertIsDisplayed()
        assertEveryAnswerIsLive()
    }

    @Test
    fun late_saysSoAndStillLetsTheDoseBeRecorded() {
        setScreen(answerable(DoseTiming.LATE, IntakeStatus.Overdue))

        composeRule.onNodeWithTag(DoseTimingBannerTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("This dose is overdue. You can still record it.").assertIsDisplayed()
        assertEveryAnswerIsLive()
    }

    @Test
    fun anAnsweredDose_showsTheOutcomeAndNoAnswers() {
        setScreen(
            DoseDetailUiState.Settled(
                medicationName = "Ibuprofen",
                amount = amount,
                time = FormattedDoseTime("08:00", null),
                status = IntakeStatus.Skipped,
                outcome = IntakeOutcome.SKIPPED,
                recordedAt = Instant.parse("2026-09-14T06:05:00Z"),
            ),
        )

        composeRule.onNodeWithTag(DoseDetailTestTags.OUTCOME).assertIsDisplayed()
        composeRule.onNodeWithText("You chose not to take this dose").assertIsDisplayed()
        listOf(DoseDetailTestTags.TAKEN, DoseDetailTestTags.SNOOZE, DoseDetailTestTags.SKIP).forEach { tag ->
            composeRule.onAllNodesWithTag(tag).assertCountEquals(0)
        }
        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).assertIsDisplayed()
    }

    @Test
    fun aWithdrawnDose_showsTheEmptyStateAndOnlyClose() {
        setScreen(DoseDetailUiState.Gone)

        composeRule.onNodeWithTag(DoseDetailTestTags.GONE).assertIsDisplayed()
        composeRule.onNodeWithText("This dose is no longer scheduled").assertIsDisplayed()
        composeRule.onAllNodesWithTag(DoseDetailTestTags.TAKEN).assertCountEquals(0)
        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).assertIsDisplayed()
    }

    @Test
    fun theBackArrowAndCloseBothLeave() {
        var closes = 0
        setScreen(answerable(DoseTiming.ON_TIME), onClose = { closes++ })

        composeRule.onNodeWithTag(DoseDetailTestTags.BACK).performClick()
        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).performClick()

        assertEquals(2, closes)
    }

    @Test
    fun atDoubleFontScale_everythingStillRendersAndCloseStaysReachable() {
        // The content scrolls and Close does not, so the primary way out is on screen whatever the
        // font scale. Rendering at all is the assertion: a clipped layout throws here.
        setScreen(answerable(DoseTiming.LATE, IntakeStatus.Overdue), fontScale = 2f)

        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).assertIsDisplayed()
        composeRule.onNodeWithTag(DoseDetailTestTags.NAME).assertIsDisplayed()
    }

    private fun assertEveryAnswerIsLive() {
        listOf(DoseDetailTestTags.TAKEN, DoseDetailTestTags.SNOOZE, DoseDetailTestTags.SKIP).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsEnabled()
        }
    }

    private fun answerable(timing: DoseTiming, status: IntakeStatus = IntakeStatus.Due) =
        DoseDetailUiState.Answerable(
            medicationName = "Ibuprofen",
            amount = amount,
            time = FormattedDoseTime("08:00", DayLabel.Tomorrow),
            status = status,
            timing = timing,
        )

    private fun setScreen(
        state: DoseDetailUiState,
        onAnswer: (DoseAnswer) -> Unit = {},
        onClose: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density.density,
                    fontScale,
                ),
            ) {
                PillsnerTheme {
                    Surface {
                        DoseDetailScreen(uiState = state, onAnswer = onAnswer, onClose = onClose)
                    }
                }
            }
        }
    }
}
