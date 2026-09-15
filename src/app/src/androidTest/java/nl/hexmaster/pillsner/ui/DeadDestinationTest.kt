package nl.hexmaster.pillsner.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.ui.medicines.form.MedicationFormTestTags
import nl.hexmaster.pillsner.ui.navigation.MedicationFormGraph
import nl.hexmaster.pillsner.ui.navigation.MedicineHistory
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceFixture
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A destination opened for a medicine that is no longer there (spec: app-reset, design D9).
 *
 * This is the state a reset leaves on the Medicines back stack: the medicine an id-keyed
 * destination was opened for has been erased. Every such destination must close itself and return
 * to the list rather than crash or sit on an empty form. A medicine id that never existed is the
 * same situation, and needs no reset to arrange.
 */
@RunWith(AndroidJUnit4::class)
class DeadDestinationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        val container = AppContainer(composeRule.activity)
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        // The app-scoped container resolves the lock state at process start; one built for a test
        // has to be asked, or it stays Loading and nothing below the lock gate is ever composed.
        container.resolveLockState()
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
    }

    @Test
    fun theFormForAMedicineThatIsGone_closesBackToTheList() {
        openMedicines()

        composeRule.runOnUiThread { navController.navigate(MedicationFormGraph(ERASED_MEDICINE)) }

        assertTheFlowCloses()
    }

    @Test
    fun theHistoryForAMedicineThatIsGone_closesBackToTheList() {
        openMedicines()

        composeRule.runOnUiThread {
            navController.navigate(MedicationFormGraph(ERASED_MEDICINE))
            navController.navigate(MedicineHistory(ERASED_MEDICINE))
        }

        // Both destinations unwind, because both view models report a medicine they cannot read
        // rather than rendering an empty screen for it.
        assertTheFlowCloses()
    }

    private fun openMedicines() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()
    }

    private fun assertTheFlowCloses() {
        composeRule.waitUntil(timeoutMillis = 5_000) { nodeCount(MedicationFormTestTags.NAME) == 0 }
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
    }

    private fun nodeCount(tag: String): Int =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private companion object {
        /** An id no medicine has, which is what every id on the back stack becomes after a reset. */
        const val ERASED_MEDICINE = 99_999L
    }
}
