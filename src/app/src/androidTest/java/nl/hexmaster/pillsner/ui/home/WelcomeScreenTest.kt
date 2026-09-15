package nl.hexmaster.pillsner.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertContentDescriptionContains
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

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now: Instant = Instant.parse("2026-09-11T10:00:00Z")
    private val formatter = UpcomingDoseTimeFormatter(ZoneOffset.UTC, Locale.UK, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun header_showsLogoAndTitleWithHeadingSemantics() {
        setScreen(HomeUiState(isLoading = false))

        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onNode(hasText("Pillsner") and isHeading()).assertIsDisplayed()
        // The two-colour wordmark must stay one node: TalkBack and text matchers see "Pillsner", not "Pills" + "ner".
        composeRule.onAllNodes(hasText("Pillsner")).assertCountEquals(1)
        composeRule.onAllNodes(hasText("Pills", substring = false)).assertCountEquals(0)
        composeRule.onNode(hasContentDescription("Pillsner logo")).assertIsDisplayed()
    }

    @Test
    fun noDoses_showsEmptyStateAndNoTiles() {
        setScreen(HomeUiState(upcomingDoses = emptyList(), isLoading = false))

        composeRule.onNodeWithTag(WelcomeScreenTestTags.EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithText("Nothing due right now").assertIsDisplayed()
        composeRule.onNodeWithText("Your next dose will appear here.").assertIsDisplayed()
        composeRule.onAllNodesWithTag(WelcomeScreenTestTags.DOSE_TILE).assertCountEquals(0)
    }

    @Test
    fun eightDoses_showFiveTilesAndNoEmptyState() {
        // The view model caps the list; the screen simply renders what it is given, so hand it five.
        setScreen(HomeUiState(upcomingDoses = doses(5), isLoading = false))

        composeRule.onAllNodesWithTag(WelcomeScreenTestTags.DOSE_TILE).assertCountEquals(5)
        composeRule.onAllNodesWithTag(WelcomeScreenTestTags.EMPTY_STATE).assertCountEquals(0)
    }

    @Test
    fun tile_isOneNodeDescribingNameAmountStatusAndTime() {
        val dose = UpcomingDose(DoseId(1), "Ibuprofen", Quantity.of("1", DoseUnit.TABLET), now.plus(Duration.ofHours(10)))
        setScreen(HomeUiState(upcomingDoses = listOf(dose), isLoading = false))

        val tile = composeRule.onAllNodesWithTag(WelcomeScreenTestTags.DOSE_TILE)[0]
        tile.assertIsDisplayed()
        tile.assertContentDescriptionContains("Ibuprofen", substring = true)
        tile.assertContentDescriptionContains("1 tablet", substring = true)
        tile.assertContentDescriptionContains("Due", substring = true)
        tile.assertContentDescriptionContains("20:00", substring = true)
    }

    @Test
    fun tileForTomorrow_mentionsTomorrow() {
        val dose = UpcomingDose(DoseId(1), "Amoxicillin", Quantity.of("500", DoseUnit.MILLIGRAM), now.plus(Duration.ofHours(22)))
        setScreen(HomeUiState(upcomingDoses = listOf(dose), isLoading = false))

        composeRule.onAllNodesWithTag(WelcomeScreenTestTags.DOSE_TILE)[0]
            .assertContentDescriptionContains("Tomorrow", substring = true)
    }

    @Test
    fun tile_isTheTapTargetThatOpensItsOwnDose() {
        val opened = mutableListOf<DoseId>()
        setScreen(HomeUiState(upcomingDoses = doses(3), isLoading = false)) { opened += it }

        val tiles = composeRule.onAllNodesWithTag(WelcomeScreenTestTags.DOSE_TILE)
        tiles[2].assertHasClickAction()
        tiles[2].performClick()

        // The third tile, not the first: a tile opens its own dose, not whichever was rendered first.
        assertEquals(listOf(DoseId(3)), opened)
    }

    @Test
    fun loading_showsNeitherTilesNorEmptyState() {
        setScreen(HomeUiState(isLoading = true))

        composeRule.onNodeWithTag(WelcomeScreenTestTags.HEADER).assertIsDisplayed()
        composeRule.onAllNodesWithTag(WelcomeScreenTestTags.DOSE_TILE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(WelcomeScreenTestTags.EMPTY_STATE).assertCountEquals(0)
    }

    private fun setScreen(state: HomeUiState, onOpenDose: (DoseId) -> Unit = {}) {
        composeRule.setContent {
            PillsnerTheme {
                WelcomeScreen(uiState = state, timeFormatter = formatter, onOpenDose = onOpenDose)
            }
        }
    }

    private fun doses(count: Int): List<UpcomingDose> = (1..count).map { index ->
        UpcomingDose(
            DoseId(index.toLong()),
            "Medicine $index",
            Quantity.of("1", DoseUnit.TABLET),
            now.plus(Duration.ofHours(index.toLong())),
        )
    }
}
