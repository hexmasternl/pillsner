package nl.hexmaster.pillsner.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.ui.home.WelcomeScreenTestTags
import nl.hexmaster.pillsner.ui.medicines.MedicinesScreenTestTags
import nl.hexmaster.pillsner.ui.medicines.form.MedicationFormTestTags
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.about.AboutScreenTestTags
import nl.hexmaster.pillsner.ui.settings.about.AboutSectionTestTags
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceFixture
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation shell behaviour in a compact window (the default phone emulator). Hosted on
 * [FragmentActivity] (declared for tests in `src/debug/AndroidManifest.xml`), not a bare
 * `ComponentActivity`, because [PillsnerApp] now needs a real [BiometricAuthenticator]
 * (app-login design D6).
 */
@RunWith(AndroidJUnit4::class)
class PillsnerAppNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private lateinit var navController: TestNavHostController
    private lateinit var appLockViewModel: AppLockViewModel

    @Before
    fun setUp() {
        // Built once, outside the composition: a container created per recomposition would resolve
        // the lock state again on every frame and hand the shell a new view model each time.
        val container = AppContainer(composeRule.activity)
        appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
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
        // The app lock resolves its initial state asynchronously (app-login design D2); the lock
        // is disabled by default, so every navigation test waits past that resolution once here.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            appLockViewModel.uiState.value.lockState != LockState.Loading
        }
    }

    @Test
    fun onLaunch_homeIsSelectedAndWelcomeScreenShown() {
        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsSelected()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertIsNotSelected()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsNotSelected()
    }

    @Test
    fun tapMedicines_showsOverviewAndSelectsItem() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertIsSelected()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsNotSelected()
    }

    @Test
    fun tapSettings_showsPlaceholderAndSelectsItem() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).performClick()

        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsSelected()
    }

    @Test
    fun tapHomeFromSettings_returnsToWelcomeScreen() {
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).performClick()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).performClick()

        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsSelected()
    }

    @Test
    fun reTappingSelectedItem_doesNotGrowTheBackStack() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()
        val depthAfterFirstTap = navController.currentBackStack.value.size

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()

        assertEquals(depthAfterFirstTap, navController.currentBackStack.value.size)
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
    }

    @Test
    fun backFromMedicines_returnsToHome() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsSelected()
    }

    @Test
    fun tapAddOnMedicines_showsAddMedicineAndHidesTheBottomBar() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.ADD_FAB).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicationFormTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertDoesNotExist()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertDoesNotExist()
    }

    @Test
    fun backFromAddMedicine_returnsToMedicinesWithTheBottomBarVisible() {
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.onNodeWithTag(MedicinesScreenTestTags.ADD_FAB).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MedicationFormTestTags.BACK).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertIsSelected()
    }

    /** Settings' About row is the last thing in its list, so it has to be scrolled to. */
    private fun openAboutFromSettings() {
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag(AboutSectionTestTags.ABOUT_ROW))
        composeRule.onNodeWithTag(AboutSectionTestTags.ABOUT_ROW).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun tapAboutOnSettings_showsAboutAndHidesTheBottomBar() {
        openAboutFromSettings()

        composeRule.onNodeWithTag(AboutScreenTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertDoesNotExist()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertDoesNotExist()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertDoesNotExist()
    }

    @Test
    fun backFromAbout_returnsToSettingsWithTheBottomBarVisible() {
        openAboutFromSettings()

        composeRule.onNodeWithTag(AboutScreenTestTags.BACK).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsSelected()
    }

    @Test
    fun systemBackFromAbout_returnsToSettings() {
        openAboutFromSettings()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsSelected()
    }

    @Test
    fun backTwiceFromAbout_reachesHome() {
        openAboutFromSettings()

        repeat(2) {
            composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsSelected()
    }

    @Test
    fun aboutIsNotANavigationItem() {
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).performClick()
        composeRule.waitForIdle()

        // The bar still shows exactly the three top-level destinations.
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsDisplayed()
        composeRule.onAllNodesWithTag(AboutScreenTestTags.TITLE).assertCountEquals(0)
    }

    @Test
    fun backFromHome_finishesTheActivity() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertTrue(composeRule.activity.isFinishing)
    }
}
