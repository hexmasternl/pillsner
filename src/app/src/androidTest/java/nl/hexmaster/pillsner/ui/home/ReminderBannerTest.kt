package nl.hexmaster.pillsner.ui.home

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: welcome-screen reminder readiness, and dose-tile status (design system 2.3, 8.9). */
@RunWith(AndroidJUnit4::class)
class ReminderBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now: Instant = Instant.parse("2026-09-14T10:00:00Z")
    private val formatter = UpcomingDoseTimeFormatter(ZoneOffset.UTC, Locale.UK, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun withNotificationsOff_theBannerSaysSo() {
        setScreen(HomeUiState(isLoading = false, notificationsAllowed = false, now = now))

        composeRule.onNodeWithTag(ReminderBannerTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("Pillsner cannot show reminders because notifications are turned off.")
            .assertIsDisplayed()
    }

    @Test
    fun withInexactAlarms_theBannerSaysRemindersMayBeLate() {
        setScreen(HomeUiState(isLoading = false, alarmsAreExact = false, now = now))

        composeRule.onNodeWithTag(ReminderBannerTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Reminders may arrive up to ten minutes late because exact alarms are turned off.",
        ).assertIsDisplayed()
    }

    @Test
    fun withEverythingAllowed_thereIsNoBanner() {
        setScreen(HomeUiState(isLoading = false, now = now))

        composeRule.onAllNodesWithTag(ReminderBannerTestTags.BANNER).assertCountEquals(0)
    }

    @Test
    fun theBannerOffersAWayToFixIt() {
        var taps = 0
        setScreen(HomeUiState(isLoading = false, notificationsAllowed = false, now = now)) { taps++ }

        val action = composeRule.onNodeWithTag(ReminderBannerTestTags.ACTION)
        action.assertIsDisplayed()
        action.assertHasClickAction()
        composeRule.onNodeWithText("Open settings").assertIsDisplayed()

        action.performClick()

        assertEquals(1, taps)
    }

    @Test
    fun aDoseWhoseMomentHasPassed_isAnnouncedAsOverdue() {
        val overdue = UpcomingDose(
            DoseId(1),
            "Ibuprofen",
            Quantity.of("40", DoseUnit.MILLIGRAM),
            now.minus(Duration.ofHours(1)),
            isOverdue = true,
        )
        setScreen(HomeUiState(upcomingDoses = listOf(overdue), isLoading = false, now = now))

        composeRule.onNodeWithTag(WelcomeScreenTestTags.DOSE_TILE)
            .assertContentDescriptionContains("Overdue", substring = true)
    }

    @Test
    fun aPostponedDose_isAnnouncedAsSnoozed() {
        val snoozed = UpcomingDose(
            DoseId(1),
            "Ibuprofen",
            Quantity.of("40", DoseUnit.MILLIGRAM),
            now.minus(Duration.ofMinutes(5)),
            snoozedUntil = now.plus(Duration.ofMinutes(10)),
        )
        setScreen(HomeUiState(upcomingDoses = listOf(snoozed), isLoading = false, now = now))

        composeRule.onNodeWithTag(WelcomeScreenTestTags.DOSE_TILE)
            .assertContentDescriptionContains("Snoozed", substring = true)
    }

    @Test
    fun aDoseStillAhead_isAnnouncedAsDue() {
        val due = UpcomingDose(
            DoseId(1),
            "Ibuprofen",
            Quantity.of("40", DoseUnit.MILLIGRAM),
            now.plus(Duration.ofHours(2)),
        )
        setScreen(HomeUiState(upcomingDoses = listOf(due), isLoading = false, now = now))

        composeRule.onNodeWithTag(WelcomeScreenTestTags.DOSE_TILE)
            .assertContentDescriptionContains("Due", substring = true)
    }

    private fun setScreen(state: HomeUiState, onOpenSettings: () -> Unit = {}) {
        composeRule.setContent {
            PillsnerTheme {
                Surface {
                    WelcomeScreen(
                        uiState = state,
                        timeFormatter = formatter,
                        onOpenReminderSettings = onOpenSettings,
                    )
                }
            }
        }
    }
}
