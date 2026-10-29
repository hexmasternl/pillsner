package nl.hexmaster.pillsner.ui.medicines.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.ui.insets.EdgeToEdgeHost
import nl.hexmaster.pillsner.ui.insets.InsetPreset
import nl.hexmaster.pillsner.ui.insets.assertBottomGapToSafeAreaAtMost
import nl.hexmaster.pillsner.ui.insets.assertInsideSafeDrawingArea
import nl.hexmaster.pillsner.ui.medicines.form.QuantityFieldTestTags
import nl.hexmaster.pillsner.ui.medicines.form.ScheduleEditorUiState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: app-theme, "Content stays clear of system bars, cutouts and the keyboard": the schedule
 * editor's Done behaves like the form's Save.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleEditorInsetsTest {

    @get:Rule
    val host = EdgeToEdgeHost(ComponentActivity::class.java)

    private val composeRule get() = host.composeRule

    @Test
    fun keyboardOpen_doneSitsRightAboveTheKeyboard() {
        showEditor(InsetPreset.KEYBOARD_OPEN)

        // The amount is the editor's only text field.
        composeRule.onNodeWithTag(AMOUNT).performScrollTo().requestFocus()
        composeRule.onNodeWithTag(AMOUNT).assertInsideSafeDrawingArea(host)

        composeRule.onNodeWithTag(ScheduleEditorTestTags.DONE)
            .assertInsideSafeDrawingArea(host)
            .assertBottomGapToSafeAreaAtMost(host, Spacing.lg)
    }

    @Test
    fun largestFont_titleBackAndDoneAreClearOfBothBars() {
        showEditor(InsetPreset.THREE_BUTTON, fontScale = 2f)

        composeRule.onNodeWithTag(ScheduleEditorTestTags.BACK).assertInsideSafeDrawingArea(host)
        composeRule.onNodeWithTag(ScheduleEditorTestTags.TITLE).assertInsideSafeDrawingArea(host)

        composeRule.onNodeWithTag(ScheduleEditorTestTags.DONE)
            .assertInsideSafeDrawingArea(host)
            .assertBottomGapToSafeAreaAtMost(host, Spacing.lg)
    }

    private fun showEditor(preset: InsetPreset, fontScale: Float = 1f) {
        host.show(preset) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                PillsnerTheme {
                    ScheduleEditorScreen(
                        uiState = ScheduleEditorUiState(amountText = "40", amountUnit = DoseUnit.MILLIGRAM),
                        onAmountTextChange = {},
                        onAmountUnitChange = {},
                        onPatternChange = {},
                        onIntervalDaysChange = {},
                        onIntervalHoursChange = {},
                        onFirstDoseAtChange = {},
                        onDayToggled = {},
                        onTimeAdded = {},
                        onTimeRemoved = {},
                        onDone = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    private companion object {
        const val AMOUNT = ScheduleEditorTestTags.AMOUNT_PREFIX + QuantityFieldTestTags.AMOUNT
    }
}
