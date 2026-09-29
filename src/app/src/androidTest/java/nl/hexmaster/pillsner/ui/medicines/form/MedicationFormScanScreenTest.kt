package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.labelscan.LabelInterpretation
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.ui.medicines.labelscan.LabelScanFormTestTags
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The label-scan parts of the Add medicine form (spec: medicine-add "Scan a label entry point",
 * medicine-label-scan "Camera permission" and "Interpretation pre-fills the form").
 */
@RunWith(AndroidJUnit4::class)
class MedicationFormScanScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val filled = MedicationFormUiState(
        name = "Zorvalex",
        doseText = "50",
        doseUnit = DoseUnit.MILLIGRAM,
        usedSince = LocalDate.of(2026, 9, 27),
        useUntil = LocalDate.of(2026, 10, 6),
        schedules = listOf(
            ScheduleRowState(0, ScheduleSummary.TimesPerDay(2), Quantity.of("1", DoseUnit.TABLET)),
        ),
    )

    @Test
    fun theScanIconSitsAtTheTrailingEndOfTheNameFieldInAddMode() {
        var taps = 0
        show(MedicationFormUiState(), onScanLabelClicked = { taps++ })

        composeRule.onNodeWithTag(LabelScanFormTestTags.SCAN_ACTION).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Scan a label").assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.SCAN_ACTION).performClick()
        assertEquals(1, taps)

        val icon = composeRule.onNodeWithTag(LabelScanFormTestTags.SCAN_ACTION).fetchSemanticsNode().boundsInRoot
        val name = composeRule.onNodeWithTag(MedicationFormTestTags.NAME).fetchSemanticsNode().boundsInRoot
        assertTrue("The scan icon lies inside the name field", icon.top >= name.top && icon.bottom <= name.bottom)
        assertTrue("The scan icon is at the trailing end", icon.right <= name.right && icon.left > name.left + name.width / 2)
    }

    @Test
    fun theScanIconIsAbsentInEditMode() {
        show(MedicationFormUiState(mode = MedicationFormMode.Edit(MedicationId(1)), name = "Zorvalex"))

        composeRule.onNodeWithTag(LabelScanFormTestTags.SCAN_ACTION).assertDoesNotExist()
    }

    @Test
    fun theOptionSheetOffersBothWaysWithACamera() {
        show(MedicationFormUiState(showScanOptions = true, cameraAvailable = true))
        composeRule.onNodeWithTag(LabelScanFormTestTags.OPTION_CAMERA).assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.OPTION_PHOTO).assertIsDisplayed()
    }

    @Test
    fun withoutACameraOnlyThePhotoOptionIsOffered() {
        show(MedicationFormUiState(showScanOptions = true, cameraAvailable = false))
        composeRule.onNodeWithTag(LabelScanFormTestTags.OPTION_CAMERA).assertDoesNotExist()
        composeRule.onNodeWithTag(LabelScanFormTestTags.OPTION_PHOTO).assertIsDisplayed()
    }

    @Test
    fun theRationaleShowsBeforeTheSystemPromptWithContinueAndNotNow() {
        var continued = 0
        var dismissed = 0
        show(
            MedicationFormUiState(showCameraRationale = true),
            onRationaleContinue = { continued++ },
            onRationaleDismissed = { dismissed++ },
        )

        composeRule.onNodeWithTag(LabelScanFormTestTags.RATIONALE).assertIsDisplayed()
        composeRule.onNodeWithText("Pillsner uses the camera only to read the label in front of it. Nothing is saved or sent anywhere.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.RATIONALE_NOT_NOW).performClick()
        composeRule.onNodeWithTag(LabelScanFormTestTags.RATIONALE_CONTINUE).performClick()

        assertEquals(1, dismissed)
        assertEquals(1, continued)
    }

    @Test
    fun theBannerAndItsActionsArePresentAfterAnAppliedInterpretation() {
        var showText = 0
        var dismissed = 0
        show(
            filled.copy(showScanBanner = true, scanRawText = "ZORVALEX 50 MG"),
            onShowScanText = { showText++ },
            onScanBannerDismissed = { dismissed++ },
        )

        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("Filled in from your label scan. Check every field before saving.").assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER_SHOW_TEXT).performClick()
        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER_DISMISS).performClick()

        assertEquals(1, showText)
        assertEquals(1, dismissed)
    }

    @Test
    fun withoutTheTextInMemoryTheBannerOffersOnlyDismiss() {
        // After process death the flag is restored but the recognised text is not (design D5).
        show(filled.copy(showScanBanner = true, scanRawText = null))

        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER_SHOW_TEXT).assertDoesNotExist()
        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER_DISMISS).assertIsDisplayed()
    }

    @Test
    fun theRecognisedTextSheetShowsTheTextAndNothingElse() {
        show(filled.copy(showScanBanner = true, scanRawText = "ZORVALEX 50 MG\nTake 1 tablet twice daily", showScanText = true))

        composeRule.onNodeWithTag(LabelScanFormTestTags.TEXT_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("ZORVALEX 50 MG\nTake 1 tablet twice daily").assertIsDisplayed()
    }

    @Test
    fun theReplaceDialogOffersKeepAndReplace() {
        var replaced = 0
        var kept = 0
        show(
            filled.copy(pendingInterpretation = LabelInterpretation(name = "Other", usedSince = LocalDate.of(2026, 9, 29))),
            onReplaceConfirmed = { replaced++ },
            onReplaceDeclined = { kept++ },
        )

        composeRule.onNodeWithTag(LabelScanFormTestTags.REPLACE).assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.REPLACE_KEEP).performClick()
        composeRule.onNodeWithTag(LabelScanFormTestTags.REPLACE_CONFIRM).performClick()

        assertEquals(1, kept)
        assertEquals(1, replaced)
    }

    @Test
    fun theReadingStateIsModalWithCancel() {
        var cancelled = 0
        show(MedicationFormUiState(isScanning = true), onCancelScan = { cancelled++ })

        composeRule.onNodeWithTag(LabelScanFormTestTags.READING).assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.READING_CANCEL).performClick()

        assertEquals(1, cancelled)
    }

    @Test
    fun theFormScrollsFullyAtTheLargestFontScaleWithTheBannerShown() {
        show(filled.copy(showScanBanner = true, scanRawText = "ZORVALEX 50 MG"), fontScale = 2f)

        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.BANNER_DISMISS).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.NAME).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanFormTestTags.SCAN_ACTION).assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.USED_SINCE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.USE_UNTIL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.PRESCRIBER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.ADD_SCHEDULE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(MedicationFormTestTags.SAVE).assertIsDisplayed()
    }

    private fun show(
        uiState: MedicationFormUiState,
        fontScale: Float = 1f,
        onScanLabelClicked: () -> Unit = {},
        onRationaleContinue: () -> Unit = {},
        onRationaleDismissed: () -> Unit = {},
        onCancelScan: () -> Unit = {},
        onReplaceConfirmed: () -> Unit = {},
        onReplaceDeclined: () -> Unit = {},
        onScanBannerDismissed: () -> Unit = {},
        onShowScanText: () -> Unit = {},
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PillsnerTheme {
                    MedicationFormScreen(
                        uiState = uiState,
                        onNameChange = {},
                        onDoseTextChange = {},
                        onDoseUnitChange = {},
                        onUsedSinceChange = {},
                        onUseUntilChange = {},
                        onPrescriberChange = {},
                        onActiveChanged = {},
                        onAddSchedule = {},
                        onEditSchedule = {},
                        onRemoveSchedule = {},
                        onSave = {},
                        onBack = {},
                        onDiscard = {},
                        onKeepEditing = {},
                        snackbarHostState = remember { SnackbarHostState() },
                        onScanLabelClicked = onScanLabelClicked,
                        onRationaleContinue = onRationaleContinue,
                        onRationaleDismissed = onRationaleDismissed,
                        onCancelScan = onCancelScan,
                        onReplaceConfirmed = onReplaceConfirmed,
                        onReplaceDeclined = onReplaceDeclined,
                        onScanBannerDismissed = onScanBannerDismissed,
                        onShowScanText = onShowScanText,
                    )
                }
            }
        }
    }
}
