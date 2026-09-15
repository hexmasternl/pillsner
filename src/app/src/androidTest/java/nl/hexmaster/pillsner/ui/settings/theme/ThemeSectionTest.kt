package nl.hexmaster.pillsner.ui.settings.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionTestTags
import nl.hexmaster.pillsner.ui.theme.DarkColorScheme
import nl.hexmaster.pillsner.ui.theme.LightColorScheme
import nl.hexmaster.pillsner.ui.theme.LocalPillsnerDarkTheme
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-theme section, and that the choice reaches the rendered scheme. */
@RunWith(AndroidJUnit4::class)
class ThemeSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDropdownShowsWhatTheUserChose() {
        setSection(ThemeSectionState(selected = AppTheme.DARK))

        composeRule.onNodeWithTag(ThemeSectionTestTags.DROPDOWN).assertIsDisplayed()
        composeRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun aUserWhoHasChosenNothingSeesSystemDefault() {
        setSection(ThemeSectionState())

        composeRule.onNodeWithText("System default").assertIsDisplayed()
    }

    @Test
    fun theMenuOffersThePhoneThenLightThenDark() {
        setSection(ThemeSectionState())

        composeRule.onNodeWithTag(ThemeSectionTestTags.DROPDOWN).performClick()

        listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK).forEach { option ->
            composeRule.onNodeWithTag(ThemeSectionTestTags.OPTION_PREFIX + option.name)
                .assertIsDisplayed()
        }
        composeRule.onNodeWithText("Light").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun theMenuOffersThreeOptionsAndNoMore() {
        setSection(ThemeSectionState())

        composeRule.onNodeWithTag(ThemeSectionTestTags.DROPDOWN).performClick()

        composeRule.onAllNodes(anOption).assertCountEquals(3)
    }

    @Test
    fun choosingTheDarkThemeReportsIt() {
        val chosen = mutableListOf<AppTheme>()
        setSection(ThemeSectionState(), onThemeSelected = { chosen += it })

        composeRule.onNodeWithTag(ThemeSectionTestTags.DROPDOWN).performClick()
        composeRule.onNodeWithTag(ThemeSectionTestTags.OPTION_PREFIX + AppTheme.DARK.name)
            .performClick()

        assertEquals(listOf(AppTheme.DARK), chosen)
    }

    @Test
    fun choosingToFollowThePhoneReportsIt() {
        val chosen = mutableListOf<AppTheme>()
        setSection(ThemeSectionState(selected = AppTheme.DARK), onThemeSelected = { chosen += it })

        composeRule.onNodeWithTag(ThemeSectionTestTags.DROPDOWN).performClick()
        composeRule.onNodeWithTag(ThemeSectionTestTags.OPTION_PREFIX + AppTheme.SYSTEM.name)
            .performClick()

        assertEquals(listOf(AppTheme.SYSTEM), chosen)
    }

    @Test
    fun thereIsNoRestartNotice() {
        setSection(ThemeSectionState(selected = AppTheme.DARK))

        // The language section needs one; a colour scheme takes effect on the spot (design D3).
        composeRule.onAllNodesWithTag(LanguageSectionTestTags.RESTART_NOTICE).assertCountEquals(0)
    }

    @Test
    fun choosingDarkOnALightPhoneRendersTheDarkScheme() {
        var dark = false
        var surface: Color? = null
        composeRule.setContent {
            PillsnerTheme(darkTheme = AppTheme.DARK.isDark(systemInDarkTheme = false)) {
                dark = LocalPillsnerDarkTheme.current
                surface = MaterialTheme.colorScheme.surface
            }
        }

        assertTrue(dark)
        assertEquals(DarkColorScheme.surface, surface)
    }

    @Test
    fun choosingLightOnADarkPhoneRendersTheLightScheme() {
        var dark = true
        var surface: Color? = null
        composeRule.setContent {
            PillsnerTheme(darkTheme = AppTheme.LIGHT.isDark(systemInDarkTheme = true)) {
                dark = LocalPillsnerDarkTheme.current
                surface = MaterialTheme.colorScheme.surface
            }
        }

        assertFalse(dark)
        assertEquals(LightColorScheme.surface, surface)
    }

    /** Any option row in the open menu, whatever theme it stands for. */
    private val anOption = SemanticsMatcher("an option of the theme menu") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)
            ?.startsWith(ThemeSectionTestTags.OPTION_PREFIX) == true
    }

    private fun setSection(
        state: ThemeSectionState,
        onThemeSelected: (AppTheme) -> Unit = {},
    ) {
        composeRule.setContent {
            PillsnerTheme {
                Surface { ThemeSection(state = state, onThemeSelected = onThemeSelected) }
            }
        }
    }
}
