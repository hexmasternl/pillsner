package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.ui.PillsnerApp
import nl.hexmaster.pillsner.ui.medicines.MedicinesScreenTestTags
import nl.hexmaster.pillsner.ui.medicines.schedule.ScheduleEditorTestTags
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceFixture
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The add-medicine flow end to end, through the real navigation host so the shared draft, the
 * editor round trip and the save-then-appear behaviour are all exercised together
 * (spec: medicine-add, schedule-editor, app-navigation).
 *
 * The repository is in memory so the test never touches the device's real database.
 */
@RunWith(AndroidJUnit4::class)
class MedicationFormFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private val repository = InMemoryMedicationRepository()
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        val container = AppContainer(composeRule.activity, medicationRepository = repository)
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        // The app-scoped container resolves the lock state at process start; one built for a test
        // has to be asked, or it stays Loading and nothing below the lock gate is ever composed.
        container.resolveLockState()
        // The add button is gated on the legal documents (app-legal-information design D5); these
        // tests are about navigation, so they start from a user who has already accepted.
        LegalAcceptanceFixture.accept(container)
        val biometricAuthenticator = BiometricAuthenticator(composeRule.activity)
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
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
        openForm()
    }

    @Test
    fun theFormOpensWithItsFieldsAndAnEmptySchedulesSection() {
        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.DOSE_PREFIX + QuantityFieldTestTags.AMOUNT)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.USED_SINCE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.USE_UNTIL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.PRESCRIBER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.NO_SCHEDULES).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).assertIsEnabled()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertDoesNotExist()
    }

    @Test
    fun savingWithoutANameShowsTheRequiredError() {
        composeRule.onNodeWithTag(MedicationFormTestTags.DOSE_PREFIX + QuantityFieldTestTags.AMOUNT)
            .performTextInput("40")
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()

        composeRule.onNodeWithText("Enter a name.").assertIsDisplayed()
        assertEquals(emptyList<Any>(), runBlocking { repository.observeAll().first() })
    }

    @Test
    fun aDoseThatIsNotANumberShowsAnError() {
        fillName()
        composeRule.onNodeWithTag(MedicationFormTestTags.DOSE_PREFIX + QuantityFieldTestTags.AMOUNT)
            .performTextInput("abc")
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()

        composeRule.onNodeWithText("Enter the amount as a number.").assertIsDisplayed()
    }

    @Test
    fun backOnAnUntouchedFormReturnsToMedicinesWithoutAsking() {
        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
    }

    @Test
    fun backWithEditsAsksAndKeepEditingKeepsTheDraft() {
        fillName()

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.onNodeWithTag(DiscardDialogTestTags.KEEP).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Metoprolol").assertIsDisplayed()
    }

    @Test
    fun backWithEditsAndDiscardLeavesWithoutSaving() {
        fillName()

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.onNodeWithTag(DiscardDialogTestTags.DISCARD).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        assertEquals(emptyList<Any>(), runBlocking { repository.observeAll().first() })
    }

    @Test
    fun aScheduleBuiltInTheEditorAppearsOnTheForm() {
        fillValidFields()
        addEveryTwelveHoursSchedule()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("40 mg every 12 hours").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScheduleRowTestTags.ROW).assertCountEquals(1)
    }

    @Test
    fun editingAScheduleReplacesItsRow() {
        fillValidFields()
        addEveryTwelveHoursSchedule()

        composeRule.onAllNodesWithTag(ScheduleRowTestTags.ROW).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ScheduleEditorTestTags.AMOUNT_PREFIX + QuantityFieldTestTags.AMOUNT)
            .performTextReplacement("20")
        composeRule.onNodeWithTag(ScheduleEditorTestTags.DONE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("20 mg every 12 hours").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScheduleRowTestTags.ROW).assertCountEquals(1)
    }

    @Test
    fun removingAScheduleEmptiesTheSection() {
        fillValidFields()
        addEveryTwelveHoursSchedule()

        composeRule.onAllNodesWithTag(ScheduleRowTestTags.REMOVE).onFirst().performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(ScheduleRowTestTags.ROW).assertCountEquals(0)
        composeRule.onNodeWithTag(MedicationFormTestTags.NO_SCHEDULES).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun savingAMedicineWithTwoSchedulesShowsItUnderActiveWithBothDescriptions() {
        fillValidFields()
        addEveryTwelveHoursSchedule()
        addEveryTwelveHoursSchedule(amount = "10")

        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.ACTIVE_HEADER).assertIsDisplayed()
        composeRule.onNodeWithText("Metoprolol").assertIsDisplayed()
        composeRule.onNodeWithText("40 mg every 12 hours").assertIsDisplayed()
        composeRule.onNodeWithText("10 mg every 12 hours").assertIsDisplayed()
    }

    @Test
    fun savingWithoutSchedulesListsTheMedicineAsNeeded() {
        fillValidFields()

        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("40 mg as needed").assertIsDisplayed()
    }

    @Test
    fun theEditorRefusesAnEmptyTimesListAndSaysWhy() {
        fillValidFields()
        composeRule.onNodeWithTag(MedicationFormTestTags.ADD_SCHEDULE).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Add at least one time.").assertIsDisplayed()
        composeRule.onNodeWithTag(ScheduleEditorTestTags.DONE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ScheduleEditorTestTags.TITLE).assertIsDisplayed()
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun openForm() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.ADD_FAB).performClick()
        composeRule.waitForIdle()
    }

    private fun fillName() {
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performTextInput("Metoprolol")
    }

    private fun fillValidFields() {
        fillName()
        composeRule.onNodeWithTag(MedicationFormTestTags.DOSE_PREFIX + QuantityFieldTestTags.AMOUNT)
            .performTextInput("40")
    }

    private fun addEveryTwelveHoursSchedule(amount: String = "40") {
        composeRule.onNodeWithTag(MedicationFormTestTags.ADD_SCHEDULE).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ScheduleEditorTestTags.AMOUNT_PREFIX + QuantityFieldTestTags.AMOUNT)
            .performTextReplacement(amount)
        composeRule.onNodeWithTag(
            ScheduleEditorTestTags.PATTERN_PREFIX + "EVERY_N_HOURS",
        ).performClick()
        composeRule.onNodeWithTag(ScheduleEditorTestTags.DONE).performClick()
        composeRule.waitForIdle()
    }
}
