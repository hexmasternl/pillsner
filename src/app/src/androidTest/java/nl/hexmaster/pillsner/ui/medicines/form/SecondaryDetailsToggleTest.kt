package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "More details" / "Less details" toggle on the Medicine details screen, on its own: what it
 * announces, how big it is, and what it shows or hides (spec: medicine-details, "Secondary details
 * toggle" and "Details screen accessibility").
 */
@RunWith(AndroidJUnit4::class)
class SecondaryDetailsToggleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun collapsedToggleIsATouchTargetThatAnnouncesItsLabelAndState() {
        showForm(detailsState(expanded = false))

        composeRule.onNodeWithTag(MedicationFormTestTags.SECONDARY_DETAILS_TOGGLE)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(hasText("More details"))
            .assert(hasStateDescription("Collapsed"))
            .assertWidthIsAtLeast(Sizes.minTouchTarget)
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
        composeRule.onAllNodesWithTag(MedicationFormTestTags.USED_SINCE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MedicationFormTestTags.ACTIVE_SWITCH).assertCountEquals(0)
    }

    @Test
    fun expandedToggleAnnouncesLessDetailsAndShowsTheFields() {
        showForm(detailsState(expanded = true))

        composeRule.onNodeWithTag(MedicationFormTestTags.SECONDARY_DETAILS_TOGGLE)
            .assert(hasText("Less details"))
            .assert(hasStateDescription("Expanded"))
        composeRule.onNodeWithTag(MedicationFormTestTags.USED_SINCE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.USE_UNTIL).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.PRESCRIBER).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.ACTIVE_SWITCH).assertIsDisplayed()
    }

    @Test
    fun tappingTheToggleRaisesTheEvent() {
        var toggled = 0
        showForm(detailsState(expanded = false), onToggle = { toggled++ })

        composeRule.onNodeWithTag(MedicationFormTestTags.SECONDARY_DETAILS_TOGGLE).performClick()

        assertEquals(1, toggled)
    }

    @Test
    fun addModeHasNoToggleAndShowsEveryField() {
        showForm(MedicationFormUiState(name = "Metoprolol", usedSince = LocalDate.of(2026, 9, 13)))

        composeRule.onAllNodesWithTag(MedicationFormTestTags.SECONDARY_DETAILS_TOGGLE).assertCountEquals(0)
        composeRule.onNodeWithTag(MedicationFormTestTags.USED_SINCE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.USE_UNTIL).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.PRESCRIBER).assertIsDisplayed()
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun detailsState(expanded: Boolean) = MedicationFormUiState(
        mode = MedicationFormMode.Edit(MedicationId(1)),
        name = "Metoprolol",
        doseText = "40",
        doseUnit = DoseUnit.MILLIGRAM,
        usedSince = LocalDate.of(2026, 9, 13),
        secondaryDetailsExpanded = expanded,
    )

    private fun showForm(uiState: MedicationFormUiState, onToggle: () -> Unit = {}) {
        composeRule.setContent {
            PillsnerTheme {
                MedicationFormScreen(
                    uiState = uiState,
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
                    onSecondaryDetailsToggled = onToggle,
                )
            }
        }
    }
}
