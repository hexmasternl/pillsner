package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: medicine-overview swipe to reveal, activate and deactivate. */
@RunWith(AndroidJUnit4::class)
class MedicinesSwipeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = InMemoryMedicationRepository(
        listOf(
            medication(1, "Ibuprofen", isActive = true),
            medication(2, "Metoprolol", isActive = true),
            medication(3, "Amoxicillin", isActive = false),
        ),
    )

    @Test
    fun anActiveTileRevealsDeactivate_andTappingItMovesTheTile() {
        setScreen()

        firstTile().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Deactivate").assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineTileActionTestTags.ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(false, stored(1).isActive)
        // Alphabetically Ibuprofen now sits after Amoxicillin in the inactive section.
        assertEquals(listOf("Metoprolol", "Amoxicillin", "Ibuprofen"), tileNamesInOrder())
    }

    @Test
    fun anInactiveTileRevealsActivate_andTappingItMovesTheTileUp() {
        setScreen()

        tileWithName("Amoxicillin").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Activate").assertIsDisplayed()
        composeRule.onNodeWithTag(MedicineTileActionTestTags.ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(true, stored(3).isActive)
        assertEquals(listOf("Amoxicillin", "Ibuprofen", "Metoprolol"), tileNamesInOrder())
    }

    @Test
    fun revealingASecondTileClosesTheFirst() {
        setScreen()

        firstTile().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(MedicineTileActionTestTags.ACTION).assertCountEquals(1)

        tileWithName("Metoprolol").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(MedicineTileActionTestTags.ACTION).assertCountEquals(1)
        composeRule.onNodeWithText("Deactivate").assertIsDisplayed()
    }

    @Test
    fun aSwipeTowardsTheEndEdgeDoesNothing() {
        setScreen()

        firstTile().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(MedicineTileActionTestTags.ACTION).assertCountEquals(0)
    }

    @Test
    fun closedTilesHideTheirActionFromAScreenReader() {
        setScreen()

        composeRule.onAllNodesWithTag(MedicineTileActionTestTags.ACTION).assertCountEquals(0)
    }

    @Test
    fun swipingWithoutTappingChangesNothing() {
        setScreen()

        firstTile().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(true, stored(1).isActive)
        assertEquals(listOf("Ibuprofen", "Metoprolol", "Amoxicillin"), tileNamesInOrder())
    }

    @Test
    fun underRightToLeftTheActionStillAppears() {
        composeRule.setContent {
            PillsnerTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface { Screen() }
                }
            }
        }

        firstTile().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Deactivate").assertIsDisplayed()
    }

    @Test
    fun activatingTheLastInactiveMedicineRemovesTheInactiveHeader() {
        setScreen()

        tileWithName("Amoxicillin").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MedicineTileActionTestTags.ACTION).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.INACTIVE_HEADER).assertCountEquals(0)
    }

    @Test
    fun aFailingUpdateShowsTheMessageAndLeavesTheTileInPlace() {
        val failing = FailingRepository(repository)
        composeRule.setContent {
            PillsnerTheme { Surface { Screen(repository = failing) } }
        }

        firstTile().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MedicineTileActionTestTags.ACTION).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not update medicine").assertIsDisplayed()
        assertEquals(listOf("Ibuprofen", "Metoprolol", "Amoxicillin"), tileNamesInOrder())
    }

    @Test
    fun everyTileOffersItsActionWithoutASwipe() {
        setScreen()

        val labels = composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE)
            .fetchSemanticsNodes()
            .map { node ->
                val actions = node.config.firstOrNull { it.key == SemanticsActions.CustomActions }
                    ?.let { it.value as? List<*> }
                    .orEmpty()
                assertEquals("Each tile offers exactly one activation action", 1, actions.size)
                (actions.single() as CustomAccessibilityAction).label
            }

        assertEquals(listOf("Deactivate", "Deactivate", "Activate"), labels)
    }

    @Test
    fun performingTheAccessibilityActionMovesTheTile() {
        setScreen()

        val action = firstTile().fetchSemanticsNode().config
            .first { it.key == SemanticsActions.CustomActions }
            .let { (it.value as List<*>).single() as CustomAccessibilityAction }
        composeRule.runOnUiThread { action.action() }
        composeRule.waitForIdle()

        assertEquals(false, stored(1).isActive)
        assertEquals(listOf("Metoprolol", "Amoxicillin", "Ibuprofen"), tileNamesInOrder())
    }

    @Test
    fun anInactiveTileStillAnnouncesThatItIsInactive() {
        setScreen()

        tileWithName("Amoxicillin")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Inactive"))
    }

    @Test
    fun tappingAClosedTileOpensThatMedicine() {
        val opened = mutableListOf<MedicationId>()
        setScreen(onOpen = { opened += it })

        firstTile().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(MedicationId(1)), opened)
    }

    @Test
    fun tappingARevealedTileClosesItInsteadOfOpeningIt() {
        val opened = mutableListOf<MedicationId>()
        setScreen(onOpen = { opened += it })
        firstTile().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(MedicineTileActionTestTags.ACTION).assertCountEquals(1)

        firstTile().performClick()
        composeRule.waitForIdle()

        assertEquals("A finger that has just swiped should not navigate", emptyList<MedicationId>(), opened)
        composeRule.onAllNodesWithTag(MedicineTileActionTestTags.ACTION).assertCountEquals(0)
    }

    @Test
    fun aScreenReaderDefaultActionOpensTheMedicine() {
        val opened = mutableListOf<MedicationId>()
        setScreen(onOpen = { opened += it })

        val open = firstTile().fetchSemanticsNode().config
            .first { it.key == SemanticsActions.OnClick }
            .let { it.value as AccessibilityAction<*> }
        assertEquals("Open medicine details", open.label)

        // The activation action is still one menu away, next to the default one.
        val custom = firstTile().fetchSemanticsNode().config
            .first { it.key == SemanticsActions.CustomActions }
            .let { (it.value as List<*>).single() as CustomAccessibilityAction }
        assertEquals("Deactivate", custom.label)
    }

    // --- Helpers --------------------------------------------------------------------------

    @androidx.compose.runtime.Composable
    private fun Screen(
        repository: MedicationRepository = this.repository,
        onOpen: (MedicationId) -> Unit = {},
    ) {
        val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
            MedicinesViewModel(repository, Locale.UK)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        MedicinesScreen(
            uiState = uiState,
            onAddMedicine = {},
            onSetActive = viewModel::onSetActive,
            onOpenMedication = onOpen,
            effects = viewModel.effects,
        )
    }

    private fun setScreen(onOpen: (MedicationId) -> Unit = {}) {
        composeRule.setContent { PillsnerTheme { Surface { Screen(onOpen = onOpen) } } }
    }

    private fun firstTile() = composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE).onFirst()

    private fun tileWithName(name: String) = composeRule.onNode(
        hasTestTag(MedicinesScreenTestTags.TILE) and
            androidx.compose.ui.test.hasContentDescriptionExactly(*descriptionsFor(name)),
    )

    private fun descriptionsFor(name: String): Array<String> = arrayOf("$name, 40 mg once a day")

    private fun tileNamesInOrder(): List<String> =
        composeRule.onAllNodesWithTag(MedicinesScreenTestTags.TILE)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                node.config.firstOrNull { it.key == SemanticsProperties.ContentDescription }
                    ?.let { (it.value as? List<*>)?.firstOrNull() as? String }
                    ?.substringBefore(",")
            }

    private fun stored(id: Long): Medication =
        runBlocking { repository.observeAll().first() }.first { it.id == MedicationId(id) }

    /** Reads through to the real repository but refuses every write. */
    private class FailingRepository(private val delegate: MedicationRepository) : MedicationRepository {
        override fun observeAll(): Flow<List<Medication>> = delegate.observeAll()
        override suspend fun get(id: MedicationId): Medication? = null
        override suspend fun update(medication: Medication): Unit = error("no database")
        override suspend fun add(medication: NewMedication): MedicationId = error("no database")
        override suspend fun setActive(id: MedicationId, isActive: Boolean): Unit = error("no database")
    }

    private companion object {
        fun medication(id: Long, name: String, isActive: Boolean) = Medication(
            id = MedicationId(id),
            name = name,
            defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
            usedSince = LocalDate.of(2026, 9, 14),
            useUntil = null,
            prescribedBy = Prescriber.SELF,
            schedules = listOf(
                Schedule.EveryNDays(
                    Quantity.of("40", DoseUnit.MILLIGRAM),
                    intervalDays = 1,
                    times = listOf(LocalTime.of(8, 0)),
                ),
            ),
            isActive = isActive,
        )
    }
}
