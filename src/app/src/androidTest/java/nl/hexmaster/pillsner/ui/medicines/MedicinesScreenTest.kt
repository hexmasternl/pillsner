package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: medicine-overview sections, tiles, empty states and the add button. */
@RunWith(AndroidJUnit4::class)
class MedicinesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val mg400 = Quantity.of("400", DoseUnit.MILLIGRAM)
    private val mg500 = Quantity.of("500", DoseUnit.MILLIGRAM)
    private val oneTablet = Quantity.of("1", DoseUnit.TABLET)
    private val ml25 = Quantity.of("2.5", DoseUnit.MILLILITRE)

    private val ibuprofen = MedicineTileState(
        MedicationId(1),
        "Ibuprofen",
        listOf(ScheduleLine(ScheduleSummary.TimesPerDay(2), mg400)),
        isActive = true,
    )
    private val amoxicillin = MedicineTileState(
        MedicationId(2),
        "Amoxicillin",
        listOf(ScheduleLine(ScheduleSummary.EveryNHours(8), mg500)),
        isActive = true,
    )
    private val methotrexate = MedicineTileState(
        MedicationId(3),
        "Methotrexate",
        listOf(
            ScheduleLine(
                ScheduleSummary.TimesPerDayOnDays(
                    1,
                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                ),
                oneTablet,
            ),
        ),
        isActive = false,
    )
    private val vitaminD = MedicineTileState(
        MedicationId(4),
        "Vitamin D",
        listOf(ScheduleLine(ScheduleSummary.TimesEveryOtherDay(1), ml25)),
        isActive = false,
    )
    private val metoprolol = MedicineTileState(
        MedicationId(5),
        "Metoprolol",
        listOf(
            ScheduleLine(ScheduleSummary.EveryNHours(12), Quantity.of("40", DoseUnit.MILLIGRAM)),
            ScheduleLine(
                ScheduleSummary.TimesPerDayOnDays(1, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
                Quantity.of("20", DoseUnit.MILLIGRAM),
            ),
        ),
        isActive = true,
    )

    @Test
    fun noMedicines_showsEmptyStateAndNoHeaders() {
        setScreen(MedicinesUiState(isLoading = false))

        composeRule.onNodeWithTag(MedicinesScreenTestTags.EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithText("No medicines yet").assertIsDisplayed()
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.ACTIVE_HEADER).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.INACTIVE_HEADER).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).assertCountEquals(0)
    }

    @Test
    fun activeOnly_showsOneHeaderAndNoInactiveHeader() {
        setScreen(MedicinesUiState(active = listOf(amoxicillin, ibuprofen), isLoading = false))

        composeRule.onNodeWithTag(MedicinesScreenTestTags.ACTIVE_HEADER).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.INACTIVE_HEADER).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).assertCountEquals(2)
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.EMPTY_STATE).assertCountEquals(0)
    }

    @Test
    fun mixed_showsBothHeaders() {
        setScreen(
            MedicinesUiState(
                active = listOf(amoxicillin, ibuprofen),
                inactive = listOf(methotrexate),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(MedicinesScreenTestTags.ACTIVE_HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.INACTIVE_HEADER).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).assertCountEquals(3)
    }

    @Test
    fun inactiveOnly_showsTheActiveEmptyStateAboveTheInactiveHeader() {
        setScreen(MedicinesUiState(inactive = listOf(methotrexate, vitaminD), isLoading = false))

        composeRule.onNodeWithTag(MedicinesScreenTestTags.ACTIVE_HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.ACTIVE_EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithText("No active medicines").assertIsDisplayed()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.INACTIVE_HEADER).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).assertCountEquals(2)

        val activeHeaderTop = composeRule.onNodeWithTag(MedicinesScreenTestTags.ACTIVE_HEADER)
            .fetchSemanticsNode().positionInRoot.y
        val inactiveHeaderTop = composeRule.onNodeWithTag(MedicinesScreenTestTags.INACTIVE_HEADER)
            .fetchSemanticsNode().positionInRoot.y
        assertEquals(true, activeHeaderTop < inactiveHeaderTop)
    }

    @Test
    fun activeTile_isOneNodeHoldingNameAndDescription() {
        setScreen(MedicinesUiState(active = listOf(ibuprofen), isLoading = false))

        composeRule.onNodeWithTag(MedicinesScreenTestTags.TILE)
            .assertContentDescriptionContains("Ibuprofen, 400 mg twice a day")
    }

    @Test
    fun inactiveTile_announcesInactiveAndShowsTheChip() {
        setScreen(MedicinesUiState(inactive = listOf(vitaminD), isLoading = false))

        val tile = composeRule.onNodeWithTag(MedicinesScreenTestTags.TILE)
        tile.assertContentDescriptionContains("Vitamin D, 2.5 ml once every other day")
        tile.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Inactive"))
        composeRule.onNodeWithTag(MedicinesScreenTestTags.INACTIVE_CHIP, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun aTileWithTwoSchedules_showsOneLinePerScheduleInOrder() {
        setScreen(MedicinesUiState(active = listOf(metoprolol), isLoading = false))

        composeRule.onNodeWithTag(MedicinesScreenTestTags.TILE)
            .assertContentDescriptionContains(
                "Metoprolol, 40 mg every 12 hours, 20 mg once a day on Mon, Thu",
            )
    }

    @Test
    fun addButton_isPresentWithItsContentDescriptionEvenWhenEmpty() {
        setScreen(MedicinesUiState(isLoading = false))

        composeRule.onNodeWithTag(MedicinesScreenTestTags.ADD_FAB).assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Add medicine")).assertIsDisplayed()
    }

    @Test
    fun addButton_reportsTaps() {
        var taps = 0
        setScreen(MedicinesUiState(isLoading = false), onAddMedicine = { taps++ })

        composeRule.onNodeWithTag(MedicinesScreenTestTags.ADD_FAB).performClick()

        assertEquals(1, taps)
    }

    @Test
    fun whileLoading_neitherTilesNorEmptyStateAreShown() {
        setScreen(MedicinesUiState(isLoading = true))

        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.EMPTY_STATE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).assertCountEquals(0)
    }

    @Test
    fun tiles_renderTheirScheduleDescription() {
        setScreen(MedicinesUiState(active = listOf(amoxicillin), isLoading = false))

        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("500 mg every 8 hours").assertIsDisplayed()
    }

    private fun setScreen(uiState: MedicinesUiState, onAddMedicine: () -> Unit = {}) {
        composeRule.setContent {
            PillsnerTheme {
                Surface { MedicinesScreen(uiState = uiState, onAddMedicine = onAddMedicine) }
            }
        }
    }
}
