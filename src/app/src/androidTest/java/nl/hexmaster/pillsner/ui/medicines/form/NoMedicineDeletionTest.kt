package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A medicine can never be removed (spec: medicine-details).
 *
 * The details screen is where a delete would most naturally be added, so the screen is checked to
 * offer nothing of the sort. `MedicationDaoContractTest` guards the other end, the database.
 */
@RunWith(AndroidJUnit4::class)
class NoMedicineDeletionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theOverflowMenuOffersNoWayToRemoveAMedicine() {
        showDetailsScreen()

        composeRule.onNodeWithTag(MedicationFormTestTags.OVERFLOW).performClick()

        composeRule.onNodeWithTag(MedicationFormTestTags.USAGE_HISTORY).assertIsDisplayed()
        listOf("Delete", "Archive", "Remove medicine").forEach { word ->
            composeRule.onAllNodesWithText(word, substring = true, ignoreCase = true)
                .assertCountEquals(0)
        }
    }

    @Test
    fun theDetailsScreenOffersNoWayToRemoveAMedicine() {
        showDetailsScreen()

        // "Remove schedule" is allowed: a schedule is not the medicine.
        listOf("Delete", "Archive", "Remove medicine").forEach { word ->
            composeRule.onAllNodesWithText(word, substring = true, ignoreCase = true)
                .assertCountEquals(0)
        }
    }

    private fun showDetailsScreen() {
        composeRule.setContent {
            PillsnerTheme {
                MedicationFormScreen(
                    uiState = MedicationFormUiState(
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
                    ),
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
