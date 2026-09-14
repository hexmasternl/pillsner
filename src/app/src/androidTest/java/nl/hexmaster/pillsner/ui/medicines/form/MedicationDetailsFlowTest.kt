package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.ui.PillsnerApp
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opening a medicine from its tile and editing it, through the real navigation host
 * (spec: medicine-details).
 */
@RunWith(AndroidJUnit4::class)
class MedicationDetailsFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)
    private val mg20 = Quantity.of("20", DoseUnit.MILLIGRAM)

    private val repository = InMemoryMedicationRepository(
        listOf(
            medication(
                id = 1,
                name = "Metoprolol",
                schedules = listOf(
                    Schedule.EveryNHours(mg40, 12, LocalTime.of(8, 0)),
                    Schedule.EveryNDays(mg20, 1, listOf(LocalTime.of(9, 0))),
                ),
            ),
            medication(id = 2, name = "Zolpidem", schedules = emptyList(), isActive = false),
        ),
    )

    @Before
    fun setUp() {
        val container = AppContainer(composeRule.activity, medicationRepository = repository)
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        val biometricAuthenticator = BiometricAuthenticator(composeRule.activity)
        composeRule.setContent {
            val navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            PillsnerTheme {
                PillsnerApp(
                    viewModelFactory = container.viewModelFactory,
                    appLockViewModel = appLockViewModel,
                    biometricAuthenticator = biometricAuthenticator,
                    appInfo = container.appInfo,
                    navController = navController,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            appLockViewModel.uiState.value.lockState != LockState.Loading
        }
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun tappingATileOpensTheMedicineWithEverythingFilledIn() {
        openMetoprolol()

        composeRule.onNodeWithText("Medicine details").assertIsDisplayed()
        composeRule.onNodeWithText("Metoprolol").assertIsDisplayed()
        composeRule.onNodeWithText("40").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScheduleRowTestTags.ROW).assertCountEquals(2)
        composeRule.onNodeWithText("40 mg every 12 hours").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("20 mg once a day").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.ACTIVE_SWITCH).performScrollTo().assertIsOn()
    }

    @Test
    fun anInactiveMedicineOpensWithItsSwitchOff_andTurningItOnMovesTheTile() {
        openTileWithText("Zolpidem")

        val switch = composeRule.onNodeWithTag(MedicationFormTestTags.ACTIVE_SWITCH)
        switch.performScrollTo().assertIsOff()
        switch.performClick()
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        assertEquals(true, stored(2).isActive)
        assertEquals(listOf("Metoprolol", "Zolpidem"), tileNames())
    }

    @Test
    fun renamingAMedicineKeepsOneTileAtItsNewAlphabeticalPlace() {
        openMetoprolol()

        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performTextReplacement("Amlodipine")
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()
        composeRule.waitForIdle()

        assertEquals("Amlodipine", stored(1).name)
        assertEquals(listOf("Amlodipine", "Zolpidem"), tileNames())
        composeRule.onAllNodesWithText("Metoprolol").assertCountEquals(0)
    }

    @Test
    fun removingASchedulePutsOnlyTheOtherDescriptionOnTheTile() {
        openMetoprolol()

        composeRule.onAllNodesWithTag(ScheduleRowTestTags.REMOVE).onFirst().performClick()
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()
        composeRule.waitForIdle()

        assertEquals(1, stored(1).schedules.size)
        composeRule.onNodeWithText("20 mg once a day").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("40 mg every 12 hours").assertCountEquals(0)
    }

    @Test
    fun backWithoutEditsLeavesStraightAway() {
        openMetoprolol()

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        assertEquals("Metoprolol", stored(1).name)
    }

    @Test
    fun backAfterAnEditAsksAndDiscardingChangesNothing() {
        openMetoprolol()
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performTextReplacement("Something else")

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.onNodeWithTag(DiscardDialogTestTags.DISCARD).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        assertEquals("Metoprolol", stored(1).name)
    }

    @Test
    fun keepEditingPreservesTheEdit() {
        openMetoprolol()
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performTextReplacement("Something else")

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.onNodeWithTag(DiscardDialogTestTags.KEEP).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Something else").assertIsDisplayed()
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun openMetoprolol() = openTileWithText("Metoprolol")

    private fun openTileWithText(name: String) {
        composeRule.onNode(hasText(name, substring = true)).performClick()
        composeRule.waitForIdle()
    }

    private fun tileNames(): List<String> = repository.let {
        runBlocking { it.observeAll().first() }
            .sortedBy { medication -> medication.name.lowercase() }
            .map { medication -> medication.name }
    }

    private fun stored(id: Long): Medication =
        runBlocking { repository.observeAll().first() }.first { it.id == MedicationId(id) }

    private fun medication(
        id: Long,
        name: String,
        schedules: List<Schedule>,
        isActive: Boolean = true,
    ) = Medication(
        id = MedicationId(id),
        name = name,
        defaultDose = mg40,
        usedSince = today,
        useUntil = null,
        prescribedBy = Prescriber.SPECIALIST,
        schedules = schedules,
        isActive = isActive,
    )
}
