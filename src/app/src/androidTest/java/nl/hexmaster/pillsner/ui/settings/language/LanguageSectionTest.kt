package nl.hexmaster.pillsner.ui.settings.language

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.locale.AppLocale
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-language section, its options and the restart notice. */
@RunWith(AndroidJUnit4::class)
class LanguageSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDropdownShowsWhatTheUserChose() {
        setSection(LanguageSectionState(selected = AppLanguage.DUTCH))

        composeRule.onNodeWithTag(LanguageSectionTestTags.DROPDOWN).assertIsDisplayed()
        composeRule.onNodeWithText("Nederlands").assertIsDisplayed()
    }

    @Test
    fun aUserWhoHasChosenNothingSeesSystemDefault() {
        setSection(LanguageSectionState())

        composeRule.onNodeWithText("System default").assertIsDisplayed()
    }

    @Test
    fun theMenuOffersThePhoneThenEveryLanguageUnderItsOwnName() {
        setSection(LanguageSectionState())

        composeRule.onNodeWithTag(LanguageSectionTestTags.DROPDOWN).performClick()

        listOf(
            AppLanguage.SYSTEM,
            AppLanguage.ENGLISH,
            AppLanguage.DUTCH,
            AppLanguage.GERMAN,
            AppLanguage.FRENCH,
            AppLanguage.SPANISH,
            AppLanguage.PORTUGUESE,
        ).forEach { option ->
            composeRule.onNodeWithTag(LanguageSectionTestTags.OPTION_PREFIX + option.name)
                .assertIsDisplayed()
        }
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("Nederlands").assertIsDisplayed()
        composeRule.onNodeWithText("Deutsch").assertIsDisplayed()
        composeRule.onNodeWithText("Français").assertIsDisplayed()
        composeRule.onNodeWithText("Español").assertIsDisplayed()
        composeRule.onNodeWithText("Português").assertIsDisplayed()
    }

    @Test
    fun choosingALanguageReportsIt() {
        val chosen = mutableListOf<AppLanguage>()
        setSection(LanguageSectionState(), onLanguageSelected = { chosen += it })

        composeRule.onNodeWithTag(LanguageSectionTestTags.DROPDOWN).performClick()
        composeRule.onNodeWithTag(LanguageSectionTestTags.OPTION_PREFIX + AppLanguage.DUTCH.name)
            .performClick()

        assertEquals(listOf(AppLanguage.DUTCH), chosen)
    }

    @Test
    fun aPendingRestartIsSaidPlainly() {
        setSection(LanguageSectionState(selected = AppLanguage.DUTCH, restartRequired = true))

        composeRule.onNodeWithTag(LanguageSectionTestTags.RESTART_NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText("Restart Pillsner to apply the new language").assertIsDisplayed()
    }

    @Test
    fun withNothingPendingThereIsNoNotice() {
        setSection(LanguageSectionState(selected = AppLanguage.ENGLISH))

        composeRule.onAllNodesWithTag(LanguageSectionTestTags.RESTART_NOTICE).assertCountEquals(0)
    }

    @Test
    fun theAppReadsItselfInTheLanguageItResolved() {
        val context: Context = ApplicationProvider.getApplicationContext()

        AppLocale.apply(AppLanguage.DUTCH, android.os.LocaleList(Locale.ENGLISH))
        val dutch = AppLocale.wrap(context)
        assertEquals("Instellingen", dutch.getString(nl.hexmaster.pillsner.R.string.settings_title))
        assertEquals(AppLanguage.DUTCH, AppLocale.inEffect)

        AppLocale.apply(AppLanguage.SYSTEM, android.os.LocaleList(Locale.ENGLISH))
        val english = AppLocale.wrap(context)
        assertEquals("Settings", english.getString(nl.hexmaster.pillsner.R.string.settings_title))
        assertEquals(AppLanguage.ENGLISH, AppLocale.inEffect)
    }

    @Test
    fun aPhoneSpeakingNeitherLanguageReadsEnglish() {
        val context: Context = ApplicationProvider.getApplicationContext()

        AppLocale.apply(AppLanguage.SYSTEM, android.os.LocaleList(Locale.ITALIAN))

        assertEquals(AppLanguage.ENGLISH, AppLocale.inEffect)
        assertEquals(
            "Settings",
            AppLocale.wrap(context).getString(nl.hexmaster.pillsner.R.string.settings_title),
        )
    }

    @Test
    fun aDutchPhoneReadsDutchWithoutAnyChoice() {
        val context: Context = ApplicationProvider.getApplicationContext()

        AppLocale.apply(AppLanguage.SYSTEM, android.os.LocaleList(Locale.forLanguageTag("nl-BE")))

        assertEquals(AppLanguage.DUTCH, AppLocale.inEffect)
        assertEquals(
            "Instellingen",
            AppLocale.wrap(context).getString(nl.hexmaster.pillsner.R.string.settings_title),
        )
    }

    @Test
    fun aGermanPhoneReadsGermanWithoutAnyChoice() {
        val context: Context = ApplicationProvider.getApplicationContext()

        AppLocale.apply(AppLanguage.SYSTEM, android.os.LocaleList(Locale.GERMAN))

        assertEquals(AppLanguage.GERMAN, AppLocale.inEffect)
        assertEquals(
            "Einstellungen",
            AppLocale.wrap(context).getString(nl.hexmaster.pillsner.R.string.settings_title),
        )
    }

    @Test
    fun theRestartNoticeIsTranslatedIntoANewLanguage() {
        val context: Context = ApplicationProvider.getApplicationContext()

        AppLocale.apply(AppLanguage.GERMAN, android.os.LocaleList(Locale.ENGLISH))

        assertEquals(
            "Starte Pillsner neu, um die neue Sprache zu verwenden",
            AppLocale.wrap(context)
                .getString(nl.hexmaster.pillsner.R.string.settings_language_restart_notice),
        )
    }

    private fun setSection(
        state: LanguageSectionState,
        onLanguageSelected: (AppLanguage) -> Unit = {},
    ) {
        composeRule.setContent {
            PillsnerTheme {
                Surface { LanguageSection(state = state, onLanguageSelected = onLanguageSelected) }
            }
        }
    }
}
