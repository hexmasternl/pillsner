package nl.hexmaster.pillsner.ui.settings.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.emptyFlow
import nl.hexmaster.pillsner.applock.ui.AppLockUiState
import nl.hexmaster.pillsner.applock.ui.BiometricResult
import nl.hexmaster.pillsner.applock.ui.VerifyIdentityCallbacks
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.ui.settings.SettingsScreen
import nl.hexmaster.pillsner.ui.settings.about.PreviewAppInfo
import nl.hexmaster.pillsner.ui.settings.language.LanguageSectionState
import nl.hexmaster.pillsner.ui.settings.legal.LegalAcceptanceState
import nl.hexmaster.pillsner.ui.settings.reset.DangerZoneTestTags
import nl.hexmaster.pillsner.ui.settings.reset.ResetUiState
import nl.hexmaster.pillsner.ui.settings.theme.ThemeSectionState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Your data section and its dialogs, through the whole Settings screen (spec:
 * data-backup-restore). The state is driven the way [BackupViewModel] drives it.
 */
@RunWith(AndroidJUnit4::class)
class BackupSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var exportTaps = 0
    private var importTaps = 0
    private var exportsConfirmed = 0
    private var importsConfirmed = 0

    @Test
    fun bothActionsSitDirectlyAboveTheDangerZone() {
        setSettings(BackupUiState())
        scrollTo(DangerZoneTestTags.HEADING)

        composeRule.onNodeWithTag(BackupSectionTestTags.HEADING).assert(isHeading())
        composeRule.onNodeWithText("Export data").assertIsDisplayed()
        composeRule.onNodeWithText("Import data").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupSectionTestTags.EXPORT).assertHasClickAction().performClick()
        composeRule.onNodeWithTag(BackupSectionTestTags.IMPORT).assertHasClickAction().performClick()
        assertEquals(1, exportTaps)
        assertEquals(1, importTaps)

        val importTop = composeRule.onNodeWithTag(BackupSectionTestTags.IMPORT).fetchSemanticsNode().positionInRoot.y
        val dangerTop = composeRule.onNodeWithTag(DangerZoneTestTags.HEADING).fetchSemanticsNode().positionInRoot.y
        assert(importTop < dangerTop) { "Your data must come before the danger zone" }
    }

    @Test
    fun whileWorking_bothActionsAreDisabledAndProgressIsNamed() {
        setSettings(BackupUiState(work = BackupWork.EXPORTING))
        scrollTo(BackupSectionTestTags.PROGRESS)

        composeRule.onNodeWithTag(BackupSectionTestTags.EXPORT).assertIsNotEnabled()
        composeRule.onNodeWithTag(BackupSectionTestTags.IMPORT).assertIsNotEnabled()
        composeRule.onNodeWithText("Exporting your data…").assertIsDisplayed()
    }

    @Test
    fun theExportDialog_onlyContinuesOnceBothEntriesMatch() {
        setSettings(BackupUiState(dialog = BackupDialog.ExportPassphrase()))

        composeRule.onNodeWithTag(BackupDialogTestTags.EXPORT_CONTINUE).assertIsNotEnabled()

        composeRule.onNodeWithTag(BackupDialogTestTags.EXPORT_PASSPHRASE).performTextInput("secret")
        composeRule.onNodeWithTag(BackupDialogTestTags.EXPORT_CONFIRMATION).performTextInput("secrex")
        composeRule.onNodeWithText("The two passphrases are different. Type the same one twice.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupDialogTestTags.EXPORT_CONTINUE).assertIsNotEnabled()

        composeRule.onNodeWithTag(BackupDialogTestTags.EXPORT_CONFIRMATION).performTextReplacement("secret")
        composeRule.onNodeWithTag(BackupDialogTestTags.EXPORT_CONTINUE).assertIsEnabled().performClick()
        assertEquals(1, exportsConfirmed)
    }

    @Test
    fun aRejectedPassphrase_saysSoInTheDialog() {
        setSettings(BackupUiState(dialog = BackupDialog.ImportPassphrase(rejected = true)))

        composeRule.onNodeWithTag(BackupDialogTestTags.IMPORT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("This passphrase does not open the backup", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupDialogTestTags.IMPORT_OPEN).assertIsNotEnabled()
    }

    @Test
    fun theImportConfirmation_namesWhatIsReplacedAndIsGatedByTheTick() {
        setSettings(BackupUiState(dialog = BackupDialog.ConfirmImport()))

        composeRule.onNodeWithText("will be replaced by the backup", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupDialogTestTags.CONFIRM_REPLACE).assertIsNotEnabled().performClick()
        assertEquals("No replace without the tick", 0, importsConfirmed)

        composeRule.onNodeWithTag(BackupDialogTestTags.CONFIRM_CHECKBOX).performClick()
        composeRule.onNodeWithTag(BackupDialogTestTags.CONFIRM_REPLACE).assertIsEnabled().performClick()
        assertEquals(1, importsConfirmed)
    }

    private fun setSettings(initial: BackupUiState) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            PillsnerTheme {
                SettingsScreen(
                    languageState = LanguageSectionState(selected = AppLanguage.ENGLISH),
                    onLanguageSelected = {},
                    themeState = ThemeSectionState(),
                    onThemeSelected = {},
                    appLockUiState = AppLockUiState(),
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
                    onReminderLogTapped = {},
                    resetState = ResetUiState(),
                    resetEffects = emptyFlow(),
                    onResetTapped = {},
                    onResetConfirmationToggled = {},
                    onResetConfirmed = {},
                    onResetDismissed = {},
                    backupState = state,
                    backupCallbacks = BackupCallbacks(
                        onExportTapped = { exportTaps++ },
                        onExportPassphraseChange = { value ->
                            (state.dialog as? BackupDialog.ExportPassphrase)?.let { state = state.copy(dialog = it.copy(passphrase = value)) }
                        },
                        onExportConfirmationChange = { value ->
                            (state.dialog as? BackupDialog.ExportPassphrase)?.let { state = state.copy(dialog = it.copy(confirmation = value)) }
                        },
                        onExportPassphraseConfirmed = { exportsConfirmed++ },
                        onImportTapped = { importTaps++ },
                        onImportPassphraseChange = {},
                        onImportPassphraseConfirmed = {},
                        onImportConfirmationToggled = { accepted -> state = state.copy(dialog = BackupDialog.ConfirmImport(accepted)) },
                        onImportConfirmed = {
                            if ((state.dialog as? BackupDialog.ConfirmImport)?.accepted == true) importsConfirmed++
                        },
                        onDialogDismissed = { state = state.copy(dialog = null) },
                    ),
                )
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }
}
