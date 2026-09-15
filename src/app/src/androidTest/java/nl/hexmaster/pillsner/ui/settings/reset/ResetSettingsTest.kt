package nl.hexmaster.pillsner.ui.settings.reset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.ui.AppLockUiState
import nl.hexmaster.pillsner.applock.ui.BiometricResult
import nl.hexmaster.pillsner.applock.ui.SecuritySectionTestTags
import nl.hexmaster.pillsner.applock.ui.VerifyIdentityCallbacks
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.settings.SettingsScreen
import nl.hexmaster.pillsner.ui.settings.SettingsScreenTestTags
import nl.hexmaster.pillsner.ui.settings.about.AboutSectionTestTags
import nl.hexmaster.pillsner.ui.settings.about.PreviewAppInfo
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionState
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceState
import nl.hexmaster.pillsner.ui.settings.theme.ThemeSectionState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The danger zone and its confirmation, through the whole Settings screen (spec: app-reset).
 *
 * Through the screen rather than the two composables alone, because "last section" and "the dialog
 * gates the button" are claims about the screen, not about either composable on its own. The state
 * is driven the way the view model drives it, so what is asserted here is the contract the screen
 * offers and not a mock of it.
 */
@RunWith(AndroidJUnit4::class)
class ResetSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirms = 0

    @Test
    fun theDangerZoneIsAHeadingAndAButton_belowAbout() {
        setSettings()
        scrollToDangerZone()

        composeRule.onNodeWithTag(DangerZoneTestTags.HEADING)
            .assertIsDisplayed()
            .assert(isHeading())
        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeRule.onNodeWithText("Reset app").assertIsDisplayed()

        val aboutTop = composeRule.onNodeWithTag(AboutSectionTestTags.ABOUT_ROW)
            .fetchSemanticsNode().positionInRoot.y
        val dangerTop = composeRule.onNodeWithTag(DangerZoneTestTags.HEADING)
            .fetchSemanticsNode().positionInRoot.y
        assert(dangerTop > aboutTop) {
            "The danger zone must be the last section; About was at $aboutTop and it at $dangerTop"
        }
    }

    @Test
    fun theSectionAloneErasesNothing() {
        setSettings()
        scrollToDangerZone()

        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).performClick()

        // It opens the confirmation. That is the whole job of the section.
        assertEquals(0, confirms)
    }

    @Test
    fun theConfirmButtonIsGatedOnTheCheckbox() {
        setSettings(dialogVisible = true)

        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsNotEnabled()

        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsEnabled()

        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun theWholeCheckboxRowToggles_andAnnouncesItselfAsACheckbox() {
        setSettings(dialogVisible = true)

        val row = composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        row.assertIsOff()

        // Tapping the label, not the box: the label is part of the target, so the whole row is one
        // node with one announced state.
        composeRule.onNodeWithText("I understand all data will be erased permanently").performClick()

        row.assertIsOn()
        composeRule.onAllNodesWithTag(ResetDialogTestTags.CHECKBOX).assertCountEquals(1)
    }

    @Test
    fun cancelClosesTheDialogAndErasesNothing() {
        setSettings(dialogVisible = true)
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()

        composeRule.onNodeWithTag(ResetDialogTestTags.CANCEL).performClick()

        assertEquals("Cancel erases nothing", 0, confirms)
        composeRule.onAllNodesWithTag(ResetDialogTestTags.DIALOG).assertCountEquals(0)
    }

    @Test
    fun systemBackClosesTheDialogAndErasesNothing() {
        setSettings(dialogVisible = true)
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()

        // The same `onDismissRequest` a tap outside the dialog arrives through, which is why the
        // three ways out are one path and none of them erases anything.
        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals("Back erases nothing", 0, confirms)
        composeRule.onAllNodesWithTag(ResetDialogTestTags.DIALOG).assertCountEquals(0)
    }

    @Test
    fun reopeningAfterCancelStartsUnticked() {
        setSettings()
        scrollToDangerZone()
        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CANCEL).performClick()

        scrollToDangerZone()
        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).performClick()

        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).assertIsOff()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun confirmingWhileTickedErasesOnce() {
        setSettings(dialogVisible = true)

        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).performClick()

        assertEquals(1, confirms)
    }

    @Test
    fun afterAResetTheUserIsStillOnSettingsAndIsTold() {
        setSettings(resetEffects = flowOf(ResetEffect.Erased))

        composeRule.onNodeWithTag(SettingsScreenTestTags.SNACKBAR).assertExists()
        composeRule.onNodeWithText("Everything has been erased").assertIsDisplayed()
        // Still Settings: the screen never navigates, and Home and Medicines empty themselves.
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    /**
     * Drives the screen the way `ResetViewModel` does, so the assertions above are about the
     * contract the screen offers rather than about a stub.
     */
    private fun setSettings(
        dialogVisible: Boolean = false,
        resetEffects: Flow<ResetEffect> = emptyFlow(),
    ) {
        composeRule.setContent {
            var state by androidx.compose.runtime.remember {
                mutableStateOf(ResetUiState(dialogVisible = dialogVisible))
            }
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
                    onAboutTapped = {},
                    resetState = state,
                    resetEffects = resetEffects,
                    onResetTapped = {
                        state = state.copy(dialogVisible = true, confirmationAccepted = false)
                    },
                    onResetConfirmationToggled = { state = state.copy(confirmationAccepted = it) },
                    onResetConfirmed = {
                        if (state.canConfirm) {
                            confirms++
                            state = ResetUiState()
                        }
                    },
                    onResetDismissed = {
                        state = state.copy(dialogVisible = false, confirmationAccepted = false)
                    },
                )
            }
        }
    }

    private fun scrollToDangerZone() {
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag(DangerZoneTestTags.BUTTON))
    }
}
