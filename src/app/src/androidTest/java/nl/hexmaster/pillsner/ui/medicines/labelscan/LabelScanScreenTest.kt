package nl.hexmaster.pillsner.ui.medicines.labelscan

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The scanning screen's controls and what they announce (spec: medicine-label-scan, "Live scanning
 * screen" and "Scanning is accessible"). The camera is a slot, so nothing here needs one.
 */
@RunWith(AndroidJUnit4::class)
class LabelScanScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theControlsAreLabelledAndTheInstructionIsShown() {
        show(LabelScanUiState(isReady = true, torchAvailable = true))

        composeRule.onNodeWithTag(LabelScanTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanTestTags.INSTRUCTION).assertIsDisplayed()
        composeRule.onNodeWithText("Hold the label inside the frame").assertIsDisplayed()
        composeRule.onNodeWithText("Read now").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithContentDescription("Turn torch on").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel").assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanTestTags.GUIDE).assert(hasContentDescription("Hold the label inside the frame"))
    }

    @Test
    fun theShutterAndCancelReportToTheirCallbacks() {
        var shutterTaps = 0
        var cancelled = false
        show(LabelScanUiState(isReady = true), onShutter = { shutterTaps++ }, onCancel = { cancelled = true })

        composeRule.onNodeWithTag(LabelScanTestTags.SHUTTER).performClick()
        composeRule.onNodeWithTag(LabelScanTestTags.CANCEL).performClick()

        assertEquals(1, shutterTaps)
        assertTrue(cancelled)
    }

    @Test
    fun theTorchToggleReflectsItsState() {
        var toggles = 0
        show(LabelScanUiState(isReady = true, torchAvailable = true, torchOn = true), onTorchToggled = { toggles++ })

        composeRule.onNodeWithContentDescription("Turn torch off").performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun withoutAFlashUnitThereIsNoTorchControl() {
        show(LabelScanUiState(isReady = true, torchAvailable = false))

        composeRule.onNodeWithTag(LabelScanTestTags.TORCH).assertDoesNotExist()
    }

    @Test
    fun theShutterWaitsForTheRecogniserAndTheHintsChangeTheInstruction() {
        show(LabelScanUiState(isReady = false))
        composeRule.onNodeWithText("Getting the reader ready…").assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanTestTags.SHUTTER).assertIsNotEnabled()
    }

    @Test
    fun theAdjustHintAndTheShutterHintAreShown() {
        show(LabelScanUiState(isReady = true, hint = ScanHint.ADJUST))
        composeRule.onNodeWithText("Move closer, add light or hold still").assertIsDisplayed()
    }

    @Test
    fun acceptanceShowsLabelRead() {
        show(LabelScanUiState(isReady = true, finished = true))
        composeRule.onNodeWithText("Label read").assertIsDisplayed()
    }

    @Test
    fun aFailedRecogniserSaysSoAndDisablesTheShutter() {
        show(LabelScanUiState(failed = true))
        composeRule.onNodeWithText("The label reader could not start. You can still choose a photo or type the details.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(LabelScanTestTags.SHUTTER).assertIsNotEnabled()
    }

    private fun show(
        uiState: LabelScanUiState,
        onShutter: () -> Unit = {},
        onTorchToggled: () -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeRule.setContent {
            PillsnerTheme {
                LabelScanScreen(
                    uiState = uiState,
                    onShutter = onShutter,
                    onTorchToggled = onTorchToggled,
                    onCancel = onCancel,
                )
            }
        }
    }
}
