package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.PillsnerApp
import nl.hexmaster.pillsner.ui.medicines.history.MedicineHistoryTestTags
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opening the usage history from the details overflow and coming back, through the real navigation
 * host (spec: app-navigation, medicine-usage-history).
 *
 * The point of the test is the draft: the history is a destination inside the medicine form flow,
 * so going there is not leaving the form, and back has to return to the edit exactly as it was,
 * with no discard confirmation in between.
 */
@RunWith(AndroidJUnit4::class)
class MedicineHistoryNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val mg40 = Quantity.of("40", DoseUnit.MILLIGRAM)

    private val medications = InMemoryMedicationRepository(
        listOf(
            Medication(
                id = MedicationId(1),
                name = "Metoprolol",
                defaultDose = mg40,
                usedSince = today.minusMonths(6),
                useUntil = null,
                prescribedBy = Prescriber.GENERAL_PRACTITIONER,
                isActive = true,
                schedules = emptyList(),
            ),
        ),
    )

    private val doses = InMemoryDoseRepository(
        listOf(
            dose(1, at(today.minusDays(2)), IntakeOutcome.TAKEN),
            dose(2, at(today.minusDays(1)), IntakeOutcome.TAKEN),
        ),
    )

    @Before
    fun setUp() {
        val container = AppContainer(
            composeRule.activity,
            medicationRepository = medications,
            doseRepository = doses,
        )
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        // The app-scoped container resolves the lock state at process start; one built for a test
        // has to be asked, or it stays Loading and nothing below the lock gate is ever composed.
        container.resolveLockState()
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
    fun theHistoryOpensFromTheOverflowAndBackReturnsToTheFormWithTheEditIntact() {
        composeRule.onNode(hasText("Metoprolol", substring = true)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performTextReplacement("Metoprololum")

        composeRule.onNodeWithTag(MedicationFormTestTags.OVERFLOW).performClick()
        composeRule.onNodeWithTag(MedicationFormTestTags.USAGE_HISTORY).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicineHistoryTestTags.TITLE).assertIsDisplayed()
        // The bottom navigation is hidden on this destination.
        composeRule.onAllNodesWithTag(NavigationTestTags.MEDICINES).assertCountEquals(0)

        composeRule.onNodeWithTag(MedicineHistoryTestTags.BACK).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Medicine details").assertIsDisplayed()
        composeRule.onNodeWithText("Metoprololum").assertIsDisplayed()
        composeRule.onAllNodesWithTag(DiscardDialogTestTags.DISCARD).assertCountEquals(0)
    }

    private fun dose(id: Long, at: Instant, outcome: IntakeOutcome) = Dose(
        id = DoseId(id),
        medicationId = MedicationId(1),
        medicationName = "Metoprolol",
        amount = mg40,
        scheduledAt = at,
        intake = Intake(outcome, at),
    )

    private fun at(date: LocalDate): Instant =
        ZonedDateTime.of(date, LocalTime.of(8, 0), amsterdam).toInstant()
}
