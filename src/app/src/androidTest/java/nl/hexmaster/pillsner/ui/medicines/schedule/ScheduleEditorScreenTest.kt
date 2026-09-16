package nl.hexmaster.pillsner.ui.medicines.schedule

import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.validation.SchedulePattern
import nl.hexmaster.pillsner.ui.locale.AppLocale
import nl.hexmaster.pillsner.ui.medicines.form.ScheduleEditorUiState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The pattern selector stays aligned regardless of how its translated labels wrap (spec:
 * schedule-editor "Pattern selection", GitHub issue #10).
 *
 * Covers the two locales `app/build.gradle.kts` actually packages (`resourceConfigurations`):
 * English, where every label fits on one line, and Dutch, where the outer two wrap to a second
 * line while the middle one does not. A third, unpackaged locale would silently fall back to the
 * English strings here, so it would not exercise anything this test doesn't already cover.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theThreeSegmentsShareOneHeightInDutchWhereLabelsWrap() {
        assertSegmentsShareOneHeight(AppLanguage.DUTCH, Locale.forLanguageTag("nl"))
    }

    @Test
    fun theThreeSegmentsShareOneHeightInEnglishWhereLabelsFitOnOneLine() {
        assertSegmentsShareOneHeight(AppLanguage.ENGLISH, Locale.ENGLISH)
    }

    private fun assertSegmentsShareOneHeight(language: AppLanguage, locale: Locale) {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppLocale.apply(language, LocaleList(locale))
        val localizedContext = AppLocale.wrap(context)

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
                PillsnerTheme {
                    ScheduleEditorScreen(
                        uiState = ScheduleEditorUiState(amountText = "40", amountUnit = DoseUnit.MILLIGRAM),
                        onAmountTextChange = {},
                        onAmountUnitChange = {},
                        onPatternChange = {},
                        onIntervalDaysChange = {},
                        onIntervalHoursChange = {},
                        onFirstDoseAtChange = {},
                        onDayToggled = {},
                        onTimeAdded = {},
                        onTimeRemoved = {},
                        onDone = {},
                        onBack = {},
                    )
                }
            }
        }

        val heights = SchedulePattern.entries.map { pattern ->
            composeRule.onNodeWithTag(ScheduleEditorTestTags.PATTERN_PREFIX + pattern.name)
                .fetchSemanticsNode().size.height
        }

        assertEquals("all three segments should share one height", 1, heights.distinct().size)
    }
}
