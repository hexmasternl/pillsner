package nl.hexmaster.pillsner.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import nl.hexmaster.pillsner.ui.dose.DoseDetailTestTags
import nl.hexmaster.pillsner.ui.home.WelcomeScreenTestTags
import nl.hexmaster.pillsner.ui.navigation.DoseDetail
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The dose detail destination inside the app shell (spec: app-navigation).
 *
 * The tile that opens it is covered by `WelcomeScreenTest`; what matters here is what happens once
 * the destination is on the stack — that it is not a top-level one, so the navigation suite hides
 * itself, and that every way out returns to Home with Home selected.
 *
 * The dose is not in the device's database, so the screen settles into its "no longer scheduled"
 * state. That is the right state to assert on: it needs no fixture data and it is the one a real
 * user reaches when a refresh withdraws the dose they were looking at.
 */
@RunWith(AndroidJUnit4::class)
class DoseDetailNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        val container = AppContainer(composeRule.activity)
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
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
    fun openingADose_hidesTheNavigationSuite() {
        openDose()

        composeRule.onNodeWithTag(DoseDetailTestTags.TITLE).assertIsDisplayed()
        composeRule.onAllNodesWithTag(NavigationTestTags.HOME).assertCountEquals(0)
        composeRule.onAllNodesWithTag(NavigationTestTags.MEDICINES).assertCountEquals(0)
        composeRule.onAllNodesWithTag(NavigationTestTags.SETTINGS).assertCountEquals(0)
    }

    @Test
    fun systemBack_returnsToHomeWithHomeSelected() {
        openDose()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertBackOnHome()
    }

    @Test
    fun close_returnsToHomeWithHomeSelected() {
        openDose()

        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).performClick()
        composeRule.waitForIdle()

        assertBackOnHome()
    }

    @Test
    fun theBackArrow_returnsToHomeWithHomeSelected() {
        openDose()

        composeRule.onNodeWithTag(DoseDetailTestTags.BACK).performClick()
        composeRule.waitForIdle()

        assertBackOnHome()
    }

    @Test
    fun backFromHomeAfterLeaving_doesNotReturnToTheDose() {
        openDose()
        composeRule.onNodeWithTag(DoseDetailTestTags.CLOSE).performClick()
        composeRule.waitForIdle()

        // Home is the start destination, so back from it leaves the app rather than going forward
        // into a destination the user has already answered and left.
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(DoseDetailTestTags.TITLE).assertCountEquals(0)
    }

    private fun openDose() {
        composeRule.runOnUiThread { navController.navigate(DoseDetail(1L)) }
        composeRule.waitForIdle()
    }

    private fun assertBackOnHome() {
        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.HOME).assertIsSelected()
        composeRule.onAllNodesWithTag(DoseDetailTestTags.TITLE).assertCountEquals(0)
    }
}
