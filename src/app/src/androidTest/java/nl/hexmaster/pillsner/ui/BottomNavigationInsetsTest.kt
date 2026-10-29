package nl.hexmaster.pillsner.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.Dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.ui.AppLockViewModel
import nl.hexmaster.pillsner.applock.ui.BiometricAuthenticator
import nl.hexmaster.pillsner.data.RoomDoseRepository
import nl.hexmaster.pillsner.data.RoomMedicationRepository
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.data.stock.RoomStockBatchRepository
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.home.WelcomeScreenTestTags
import nl.hexmaster.pillsner.ui.insets.EdgeToEdgeHost
import nl.hexmaster.pillsner.ui.insets.InsetPreset
import nl.hexmaster.pillsner.ui.insets.assertInsideSafeDrawingArea
import nl.hexmaster.pillsner.ui.insets.unclippedBoundsInWindow
import nl.hexmaster.pillsner.ui.medicines.MedicinesScreenTestTags
import nl.hexmaster.pillsner.ui.navigation.NavigationTestTags
import nl.hexmaster.pillsner.ui.settings.reset.DangerZoneTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: app-theme, "Content stays clear of system bars, cutouts and the keyboard": a top-level
 * list scrolls its last item fully above the bottom navigation bar, and no more than its own
 * content padding away from it (design D4).
 *
 * Hosted through the app shell rather than a screen on its own, because what is tested is how
 * `NavigationSuiteScaffold` and each destination share the bottom inset. The data lives in an
 * in-memory database, so the device's own data is neither read nor changed.
 */
@RunWith(AndroidJUnit4::class)
class BottomNavigationInsetsTest {

    @get:Rule
    val host = EdgeToEdgeHost(FragmentActivity::class.java)

    private val composeRule get() = host.composeRule

    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        PillsnerDatabase::class.java,
    ).build()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun medicines_theLastTileScrollsFullyAboveTheBottomNavigation() {
        showApp {
            repeat(MEDICINE_COUNT) { index -> medicationRepository.add(medication("Medicine ${index + 1}")) }
        }

        composeRule.onNodeWithTag(NavigationTestTags.MEDICINES).performClick()
        composeRule.waitForIdle()

        // The list's bottom padding keeps the last tile clear of the floating action button.
        assertScrollsFullyAboveTheBottomNavigation(
            list = composeRule.onNodeWithTag(MedicinesScreenTestTags.LIST),
            contentPadding = Sizes.fab + Spacing.lg,
        )
    }

    @Test
    fun home_theLastDoseScrollsFullyAboveTheBottomNavigation() {
        // Home shows at most five doses, which only fill a phone screen at a large font.
        showApp(fontScale = 2f) {
            val id = medicationRepository.add(medication("Metoprolol"))
            val now = Instant.now()
            val doses = (1..DOSE_COUNT).map { hour ->
                PlannedDose(id, "Metoprolol", AMOUNT, now.plus(Duration.ofHours(hour.toLong())))
            }
            doseRepository.insertPlanned(doses, plannedAt = now)
        }

        assertScrollsFullyAboveTheBottomNavigation(
            list = composeRule.onNodeWithTag(WelcomeScreenTestTags.DOSE_LIST),
            contentPadding = Spacing.xxl,
        )
    }

    @Test
    fun settings_resetAppAndTheLastRowScrollFullyAboveTheBottomNavigation() {
        showApp()

        composeRule.onNodeWithTag(NavigationTestTags.SETTINGS).performClick()
        composeRule.waitForIdle()
        val list = composeRule.onNode(hasScrollAction())
        list.performScrollToNode(hasTestTag(DangerZoneTestTags.BUTTON))
        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).assertInsideSafeDrawingArea(host)
        assertTrue(
            "Reset app should end above the bottom navigation bar",
            composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).unclippedBoundsInWindow().bottom <=
                bottomNavigationTop() + host.tolerancePx,
        )

        assertScrollsFullyAboveTheBottomNavigation(list = list, contentPadding = Spacing.xxl)
    }

    /**
     * Scrolls [list] to its end, then checks that its last item ends at or above the bottom
     * navigation bar and that the band between them is no wider than [contentPadding]: a wider
     * band means the navigation bar's inset was applied twice.
     */
    private fun assertScrollsFullyAboveTheBottomNavigation(list: SemanticsNodeInteraction, contentPadding: Dp) {
        scrollToEnd(list)
        val scrollRange = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("The list should be long enough to scroll", scrollRange.value() > 0f)

        val lastItemBottom = list.onChildren().fetchSemanticsNodes().maxOf { node ->
            node.positionInWindow.y + node.size.height
        }
        val barTop = bottomNavigationTop()
        val tolerance = host.tolerancePx
        assertTrue(
            "The last item ends at $lastItemBottom px, below the bottom navigation bar's top at $barTop px",
            lastItemBottom <= barTop + tolerance,
        )
        val band = (barTop - lastItemBottom) / host.density
        assertTrue(
            "$band dp separates the last item from the bottom navigation bar, more than the list's " +
                "$contentPadding content padding: an inset applied twice?",
            band <= contentPadding.value + TOLERANCE_DP,
        )
    }

    /** Scrolls a screen at a time until the list stops moving: a lazy list's length is an estimate. */
    private fun scrollToEnd(list: SemanticsNodeInteraction) {
        fun position() = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        var previous = position()
        repeat(MAX_SCROLL_STEPS) {
            list.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, host.activity.window.decorView.height.toFloat())
            }
            composeRule.waitForIdle()
            val current = position()
            if (current == previous) return
            previous = current
        }
        throw AssertionError("The list did not reach its end in $MAX_SCROLL_STEPS screens")
    }

    /** The top of the bottom navigation bar, which holds the destination items. */
    private fun bottomNavigationTop(): Float =
        composeRule.onNodeWithTag(NavigationTestTags.HOME).onParent().unclippedBoundsInWindow().top

    private fun showApp(fontScale: Float = 1f, seed: suspend AppContainer.() -> Unit = {}) {
        grantNotificationPermission()
        val activity = host.launch(InsetPreset.THREE_BUTTON)
        val container = AppContainer(
            activity,
            medicationRepository = RoomMedicationRepository(database.medicationDao()),
            doseRepository = RoomDoseRepository(database.doseDao()),
            stockBatchRepository = RoomStockBatchRepository(database.stockBatchDao()),
        )
        runBlocking { container.seed() }
        val appLockViewModel =
            container.viewModelFactory.create(AppLockViewModel::class.java, CreationExtras.Empty)
        container.resolveLockState()
        val biometricAuthenticator = BiometricAuthenticator(activity)
        host.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                PillsnerTheme {
                    PillsnerApp(
                        viewModelFactory = container.viewModelFactory,
                        appLockViewModel = appLockViewModel,
                        biometricAuthenticator = biometricAuthenticator,
                        appInfo = container.appInfo,
                        navController = TestNavHostController(LocalContext.current).apply {
                            navigatorProvider.addNavigator(ComposeNavigator())
                        },
                    )
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            appLockViewModel.uiState.value.lockState != LockState.Loading
        }
        composeRule.waitForIdle()
    }

    /**
     * Home asks for the notification permission on a fresh install, and the system dialog would
     * cover the screen under test.
     */
    private fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    private fun medication(name: String) = NewMedication(
        name = name,
        defaultDose = AMOUNT,
        usedSince = LocalDate.of(2026, 9, 13),
        useUntil = null,
        prescribedBy = Prescriber.GENERAL_PRACTITIONER,
        schedules = emptyList(),
    )

    private companion object {
        const val MEDICINE_COUNT = 12
        const val DOSE_COUNT = 5
        const val TOLERANCE_DP = 0.5f
        const val MAX_SCROLL_STEPS = 20
        val AMOUNT = Quantity.of("40", DoseUnit.MILLIGRAM)
    }
}
