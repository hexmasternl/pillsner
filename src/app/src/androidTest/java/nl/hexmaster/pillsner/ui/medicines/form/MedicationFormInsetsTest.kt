package nl.hexmaster.pillsner.ui.medicines.form

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.pillsner.ui.insets.EdgeToEdgeHost
import nl.hexmaster.pillsner.ui.insets.InsetPreset
import nl.hexmaster.pillsner.ui.insets.assertBottomGapToSafeAreaAtMost
import nl.hexmaster.pillsner.ui.insets.assertInsideSafeDrawingArea
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: app-theme, "Content stays clear of system bars, cutouts and the keyboard": the form's Save
 * stays above the keyboard, right on top of it, and clear of the navigation bar at any font size.
 */
@RunWith(AndroidJUnit4::class)
class MedicationFormInsetsTest {

    @get:Rule
    val host = EdgeToEdgeHost(ComponentActivity::class.java)

    private val composeRule get() = host.composeRule

    @Test
    fun keyboardOpen_saveSitsRightAboveTheKeyboard() {
        showForm(InsetPreset.KEYBOARD_OPEN)

        // The dose amount is the lowest text field of the Add medicine form.
        composeRule.onNodeWithTag(DOSE_AMOUNT).performScrollTo().requestFocus()
        composeRule.onNodeWithTag(DOSE_AMOUNT).assertInsideSafeDrawingArea(host)

        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE)
            .assertInsideSafeDrawingArea(host)
            .assertBottomGapToSafeAreaAtMost(host, Spacing.lg)
    }

    @Test
    fun largestFont_titleBackAndSaveAreClearOfBothBars() {
        showForm(InsetPreset.THREE_BUTTON, fontScale = 2f)

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).assertInsideSafeDrawingArea(host)
        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertInsideSafeDrawingArea(host)

        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE)
            .assertInsideSafeDrawingArea(host)
            .assertBottomGapToSafeAreaAtMost(host, Spacing.lg)
    }

    private fun showForm(preset: InsetPreset, fontScale: Float = 1f) {
        host.show(preset) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                PillsnerTheme {
                    MedicationFormScreen(
                        uiState = MedicationFormUiState(name = "Metoprolol", usedSince = LocalDate.of(2026, 9, 13)),
                        onNameChange = {},
                        onDoseTextChange = {},
                        onDoseUnitChange = {},
                        onUsedSinceChange = {},
                        onUseUntilChange = {},
                        onPrescriberChange = {},
                        onActiveChanged = {},
                        onAddSchedule = {},
                        onEditSchedule = {},
                        onRemoveSchedule = {},
                        onSave = {},
                        onBack = {},
                        onDiscard = {},
                        onKeepEditing = {},
                        snackbarHostState = remember { SnackbarHostState() },
                    )
                }
            }
        }
    }

    private companion object {
        const val DOSE_AMOUNT = MedicationFormTestTags.DOSE_PREFIX + QuantityFieldTestTags.AMOUNT
    }
}
