package nl.hexmaster.pillsner.ui.dose

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.intake.DoseTiming
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.home.DayLabel
import nl.hexmaster.pillsner.ui.home.FormattedDoseTime
import nl.hexmaster.pillsner.ui.insets.EdgeToEdgeHost
import nl.hexmaster.pillsner.ui.insets.InsetPreset
import nl.hexmaster.pillsner.ui.insets.assertInsideSafeDrawingArea
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: app-theme, "Content stays clear of system bars, cutouts and the keyboard". The answers
 * that confirm a dose are exactly what a navigation bar or a cutout must never hide.
 */
@RunWith(AndroidJUnit4::class)
class DoseDetailInsetsTest {

    @get:Rule
    val host = EdgeToEdgeHost(ComponentActivity::class.java)

    private val composeRule get() = host.composeRule

    @Test
    fun threeButtonNavigation_everyAnswerAndCloseIsClearOfTheBarAndTappable() {
        val answers = mutableListOf<DoseAnswer>()
        var closes = 0
        showScreen(InsetPreset.THREE_BUTTON, onAnswer = { answers += it }, onClose = { closes++ })

        ANSWER_TAGS.forEach { tag ->
            composeRule.onNodeWithTag(tag).assertInsideSafeDrawingArea(host).performClick()
        }
        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).assertInsideSafeDrawingArea(host).performClick()

        assertEquals(listOf(DoseAnswer.TAKEN, DoseAnswer.SNOOZE, DoseAnswer.SKIP), answers)
        assertEquals(1, closes)
    }

    @Test
    fun largestFont_everyAnswerAndCloseIsClearOfTheBar() {
        showScreen(InsetPreset.THREE_BUTTON, fontScale = LARGEST_FONT_SCALE)

        assertAnswersAndCloseInsideSafeArea()
    }

    @Test
    fun landscapeWithCutout_everyAnswerAndCloseIsClearOfTheCutoutAndTheBar() {
        showScreen(InsetPreset.LANDSCAPE_CUTOUT)

        assertAnswersAndCloseInsideSafeArea()
    }

    /** The answers scroll and Close does not, so each answer is scrolled into view first. */
    private fun assertAnswersAndCloseInsideSafeArea() {
        ANSWER_TAGS.forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().assertInsideSafeDrawingArea(host)
        }
        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).assertInsideSafeDrawingArea(host)
    }

    private fun showScreen(
        preset: InsetPreset,
        fontScale: Float = 1f,
        onAnswer: (DoseAnswer) -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        host.show(preset) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                PillsnerTheme {
                    Surface {
                        DoseDetailScreen(
                            // Late shows the timing banner too: the tallest an answerable dose gets.
                            uiState = DoseDetailUiState.Answerable(
                                medicationName = "Ibuprofen",
                                amount = Quantity.of("40", DoseUnit.MILLIGRAM),
                                time = FormattedDoseTime("08:00", DayLabel.Tomorrow),
                                status = IntakeStatus.Overdue,
                                timing = DoseTiming.LATE,
                            ),
                            onAnswer = onAnswer,
                            onClose = onClose,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        /** The largest system font scale on current Android (design D5). */
        const val LARGEST_FONT_SCALE = 2f

        val ANSWER_TAGS = listOf(DoseDetailTestTags.TAKEN, DoseDetailTestTags.SNOOZE, DoseDetailTestTags.SKIP)
    }
}
