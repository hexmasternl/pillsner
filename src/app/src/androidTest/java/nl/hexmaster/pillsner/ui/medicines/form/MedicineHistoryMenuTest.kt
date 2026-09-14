package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The overflow action on the medicine form's top bar, and the one item behind it
 * (spec: medicine-details, medicine-usage-history).
 */
@RunWith(AndroidJUnit4::class)
class MedicineHistoryMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addModeHasNoOverflowAction() {
        showForm(MedicationFormUiState(name = "Metoprolol"))

        composeRule.onAllNodesWithTag(MedicationFormTestTags.OVERFLOW).assertCountEquals(0)
    }

    @Test
    fun editModeHasAnOverflowActionWithExactlyOneItem() {
        showForm(detailsState())

        composeRule.onNodeWithTag(MedicationFormTestTags.OVERFLOW).assertIsDisplayed().performClick()

        composeRule.onAllNodesWithTag(MedicationFormTestTags.USAGE_HISTORY).assertCountEquals(1)
    }

    @Test
    fun theOverflowActionIsATouchTargetWithASpokenLabel() {
        showForm(detailsState())

        composeRule.onNodeWithTag(MedicationFormTestTags.OVERFLOW)
            .assertContentDescriptionEquals("More options")
            .assertWidthIsAtLeast(Sizes.minTouchTarget)
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
    }

    @Test
    fun theMenuItemOpensTheUsageHistory() {
        var opened = 0
        showForm(detailsState(), onOpenUsageHistory = { opened++ })

        composeRule.onNodeWithTag(MedicationFormTestTags.OVERFLOW).performClick()
        composeRule.onNodeWithTag(MedicationFormTestTags.USAGE_HISTORY).performClick()

        assertEquals(1, opened)
    }

    private fun detailsState() = MedicationFormUiState(
        mode = MedicationFormMode.Edit(MedicationId(1)),
        name = "Metoprolol",
        doseText = "40",
        doseUnit = DoseUnit.MILLIGRAM,
        usedSince = LocalDate.of(2026, 9, 14),
        schedules = listOf(
            ScheduleRowState(
                index = 0,
                summary = ScheduleSummary.EveryNHours(12),
                amount = Quantity.of("40", DoseUnit.MILLIGRAM),
            ),
        ),
    )

    private fun showForm(uiState: MedicationFormUiState, onOpenUsageHistory: () -> Unit = {}) {
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
                    onOpenUsageHistory = onOpenUsageHistory,
                )
            }
        }
    }
}
