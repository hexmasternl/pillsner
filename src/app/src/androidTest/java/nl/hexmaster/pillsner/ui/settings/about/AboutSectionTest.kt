package nl.hexmaster.pillsner.ui.settings.about

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.ui.AppLockUiState
import nl.hexmaster.pillsner.applock.ui.BiometricResult
import nl.hexmaster.pillsner.applock.ui.SecuritySectionTestTags
import nl.hexmaster.pillsner.applock.ui.VerifyIdentityCallbacks
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.settings.SettingsScreen
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionState
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceState
import nl.hexmaster.pillsner.ui.settings.reset.ResetUiState
import nl.hexmaster.pillsner.ui.settings.theme.ThemeSectionState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The About row on Settings (app-about-screen task 7.1, spec "About section on Settings").
 *
 * Tested through the whole [SettingsScreen] rather than the section alone, because "last section,
 * below Security" is a claim about the list, not about the composable on its own.
 */
@RunWith(AndroidJUnit4::class)
class AboutSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var aboutTapped = 0

    private fun setSettings() {
        composeRule.setContent {
            PillsnerTheme {
                SettingsScreen(
                    languageState = LanguageSectionState(selected = AppLanguage.ENGLISH),
                    onLanguageSelected = {},
                    themeState = ThemeSectionState(),
                    onThemeSelected = {},
                    appLockUiState = AppLockUiState(
                        pinLockEnabled = true,
                        biometricStatus = BiometricStatus.Available,
                    ),
                    appLockEvents = emptyFlow(),
                    securityEffects = emptyFlow(),
                    onSecuritySectionAppeared = {},
                    onEnablePinLockRequested = {},
                    onLockDisableRequested = {},
                    onChangePinTapped = {},
                    onStartPinChange = {},
                    onBiometricEnabled = {},
                    onBiometricDisableRequested = {},
                    verifyCallbacks = VerifyIdentityCallbacks({}, {}, {}, {}, {}),
                    authenticateWithBiometric = { BiometricResult.Cancelled },
                    legalState = LegalAcceptanceState.NeverAccepted,
                    onOpenLegalDocument = {},
                    appInfo = PreviewAppInfo,
                    onAboutTapped = { aboutTapped++ },
                    resetState = ResetUiState(),
                    resetEffects = emptyFlow(),
                    onResetTapped = {},
                    onResetConfirmationToggled = {},
                    onResetConfirmed = {},
                    onResetDismissed = {},
                )
            }
        }
    }

    private fun scrollToAboutRow() {
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag(AboutSectionTestTags.ABOUT_ROW))
    }

    @Test
    fun aboutRowIsShownAndStatesTheInstalledVersion() {
        setSettings()

        scrollToAboutRow()

        composeRule.onNodeWithTag(AboutSectionTestTags.ABOUT_ROW).assertIsDisplayed()
        composeRule.onNodeWithText("Version 0.1.0 (1)").assertIsDisplayed()
    }

    @Test
    fun aboutSectionComesAfterSecurity() {
        setSettings()

        // Scrolling down to About must pass Security: if About were first, the Security row would
        // not be above it in the list order the semantics tree reports.
        scrollToAboutRow()
        val securityTop = composeRule.onNodeWithTag(SecuritySectionTestTags.CHANGE_PIN_ROW)
            .fetchSemanticsNode().positionInRoot.y
        val aboutTop = composeRule.onNodeWithTag(AboutSectionTestTags.ABOUT_ROW)
            .fetchSemanticsNode().positionInRoot.y

        assert(aboutTop > securityTop) {
            "Expected the About row below the Security section, but About was at $aboutTop and " +
                "Change PIN at $securityTop"
        }
    }

    @Test
    fun aboutRowAnnouncesItselfAsAButton() {
        setSettings()

        scrollToAboutRow()

        composeRule.onNodeWithTag(AboutSectionTestTags.ABOUT_ROW)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun tappingTheRowAsksToOpenAbout() {
        setSettings()

        scrollToAboutRow()
        composeRule.onNodeWithTag(AboutSectionTestTags.ABOUT_ROW).performClick()

        assertEquals(1, aboutTapped)
    }
}
