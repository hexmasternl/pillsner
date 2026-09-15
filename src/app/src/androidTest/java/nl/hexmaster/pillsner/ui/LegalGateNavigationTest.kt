package nl.hexmaster.pillsner.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.medicines.MedicinesScreenTestTags
import nl.hexmaster.pillsner.ui.medicines.form.MedicationFormTestTags
import nl.hexmaster.pillsner.ui.navigation.LegalDocumentRoute
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.legal.AcceptLegalTestTags
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceFixture
import nl.hexmaster.pillsner.ui.settings.legal.LegalDocumentTestTags
import nl.hexmaster.pillsner.ui.settings.legal.LegalSectionTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: app-legal "Acceptance is required before a medicine is added", "Acceptance never blocks
 * existing data or reminders"; app-navigation "Legal acceptance destination", "Legal document
 * destination".
 *
 * Each test says for itself whether the documents are accepted, because acceptance is stored on the
 * device and would otherwise leak from one test into the next.
 */
@RunWith(AndroidJUnit4::class)
class LegalGateNavigationTest {

    // Home asks for the notification permission on a fresh install, and the system dialog that
    // follows covers the app, so every gesture after it fails with an empty semantics tree.
    // Granting it up front keeps this test about the gate and nothing else.
    @get:Rule(order = 0)
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private val metoprolol = Medication(
        id = MedicationId(1),
        name = "Metoprolol",
        defaultDose = Quantity.of("40", DoseUnit.MILLIGRAM),
        usedSince = LocalDate.of(2026, 9, 14),
        useUntil = null,
        prescribedBy = Prescriber.GENERAL_PRACTITIONER,
        schedules = emptyList(),
        isActive = true,
    )

    private lateinit var repository: InMemoryMedicationRepository
    private lateinit var navController: TestNavHostController
    private lateinit var container: AppContainer

    private fun start(accepted: Boolean, medicines: List<Medication> = emptyList()) {
        repository = InMemoryMedicationRepository(medicines)
        container = AppContainer(composeRule.activity, medicationRepository = repository)
        if (accepted) {
            LegalAcceptanceFixture.accept(container)
        } else {
            LegalAcceptanceFixture.clear(composeRule.activity)
        }
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        // The app-scoped container resolves the lock state at process start; one built for a test
        // has to be asked, or it stays Loading and nothing below the lock gate is ever composed.
        container.resolveLockState()
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
    }

    @Test
    fun notAccepted_theAddButtonOpensTheGateAndNotTheForm() {
        start(accepted = false)

        tapAdd()

        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertDoesNotExist()
        // Not a top-level destination: the bottom navigation bar is gone.
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertDoesNotExist()
    }

    @Test
    fun accepted_theAddButtonOpensTheFormDirectly() {
        start(accepted = true)

        tapAdd()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertDoesNotExist()
    }

    @Test
    fun accepting_reachesTheFormAndBackFromItReturnsToMedicines() {
        start(accepted = false)
        tapAdd()

        accept()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertIsSelected()
        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertDoesNotExist()
    }

    @Test
    fun accepting_recordsBothDocumentsAtTheirCurrentVersions() {
        start(accepted = false)
        tapAdd()

        accept()

        val acceptance = requireNotNull(runBlocking { container.legalRepository.observeAcceptance().first() }) {
            "Accepting must record an acceptance"
        }
        assertEquals(1, acceptance.disclaimerVersion)
        assertEquals(1, acceptance.termsVersion)
    }

    @Test
    fun backingOutOfTheGate_recordsNothingAndAddsNoMedicine() {
        start(accepted = false)
        tapAdd()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertIsSelected()
        assertNull(runBlocking { container.legalRepository.observeAcceptance().first() })
        assertTrue("No medicine was added", runBlocking { repository.observeAll().first() }.isEmpty())
    }

    @Test
    fun openingAnExistingMedicine_isNeverGated() {
        start(accepted = false, medicines = listOf(metoprolol))

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.TILE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertDoesNotExist()
    }

    @Test
    fun theTermsOpenedFromTheGate_returnToItWithItsScrollPositionIntact() {
        start(accepted = false)
        tapAdd()
        composeRule.onNodeWithTag(AcceptLegalTestTags.TERMS_LINK).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(LegalDocumentTestTags.TITLE).assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // Back on the gate, and still scrolled far enough down that the terms link is on screen:
        // the position survived the visit.
        composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).assertIsDisplayed()
        composeRule.onNodeWithTag(AcceptLegalTestTags.TERMS_LINK).assertIsDisplayed()
    }

    @Test
    fun theDocumentScreenCarriesWhichDocumentInItsRoute() {
        start(accepted = true)
        openTermsFromSettings()

        // The argument is what a restored process rebuilds the screen from, so the Terms come back
        // as the Terms and not as the Disclaimer.
        val destination = navController.currentBackStackEntry
        assertTrue(destination?.destination?.hasRoute(LegalDocumentRoute::class) == true)
        assertEquals(
            LegalDocumentId.TERMS,
            destination?.toRoute<LegalDocumentRoute>()?.document,
        )
    }

    @Test
    fun backFromADocumentOpenedFromSettings_returnsToSettings() {
        start(accepted = true)
        openTermsFromSettings()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsSelected()
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun tapAdd() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.ADD_FAB).performClick()
        composeRule.waitForIdle()
    }

    /**
     * Reads the disclaimer the way a user does, and then accepts. Swipes rather than jumping,
     * because the point of the gate is that the action is dead until the text has actually gone by.
     */
    private fun accept() {
        repeat(MAX_SWIPES) {
            if (acceptIsEnabled()) {
                composeRule.onNodeWithTag(AcceptLegalTestTags.ACCEPT).performClick()
                composeRule.waitForIdle()
                return
            }
            composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        throw AssertionError("The accept action never became enabled after $MAX_SWIPES swipes")
    }

    private fun acceptIsEnabled(): Boolean = composeRule
        .onNodeWithTag(AcceptLegalTestTags.ACCEPT)
        .fetchSemanticsNode()
        .config
        .contains(SemanticsProperties.Disabled)
        .not()

    private fun openTermsFromSettings() {
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag(LegalSectionTestTags.TERMS_ROW))
        composeRule.onNodeWithTag(LegalSectionTestTags.TERMS_ROW).performClick()
        composeRule.waitForIdle()
    }

    private companion object {
        const val MAX_SWIPES = 30
    }
}
