package nl.hexmaster.pillsner.ui.settings.reset

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.domain.reset.EraseAllData
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The confirmation across a configuration change (spec: app-reset; design D7).
 *
 * The tick lives in the view model rather than in the dialog, so a rotation can neither untick it
 * silently nor — worse — leave it ticked over a dialog that was recreated without it. This drives a
 * real [ResetViewModel] through a real activity recreation, which is what a rotation is.
 */
@RunWith(AndroidJUnit4::class)
class ResetRotationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<FragmentActivity>()

    private var erases = 0

    private val factory = viewModelFactory {
        initializer { ResetViewModel(EraseAllData({ erases++ }, { }, { })) }
    }

    @Test
    fun rotatingWithTheBoxTicked_keepsTheDialogOpenTickedAndConfirmable() {
        composeRule.setContent {
            val viewModel: ResetViewModel = viewModel(factory = factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PillsnerTheme {
                if (state.dialogVisible) {
                    ResetAppDialog(
                        accepted = state.confirmationAccepted,
                        confirmEnabled = state.canConfirm,
                        onAcceptedChange = viewModel::onConfirmationToggled,
                        onConfirm = viewModel::onConfirmed,
                        onDismiss = viewModel::onDismiss,
                    )
                } else {
                    DangerZoneSection(onResetTapped = viewModel::onResetTapped)
                }
            }
        }

        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsEnabled()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ResetDialogTestTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).assertIsOn()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsEnabled()
        assertEquals("And nothing has been erased along the way", 0, erases)
    }
}
