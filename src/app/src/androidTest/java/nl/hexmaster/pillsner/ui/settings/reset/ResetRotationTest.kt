package nl.hexmaster.pillsner.ui.settings.reset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.pillsner.domain.reset.EraseAllData
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The confirmation when its composables are thrown away and built again (design D7).
 *
 * This is what a rotation does to the dialog: every composable in it is discarded and recreated,
 * while the view model — which a configuration change does not touch — stays. The design rests on
 * the tick living in the view model rather than in the dialog, so that a rotation can neither
 * untick it silently nor, worse, leave it ticked over a dialog that was rebuilt without it.
 *
 * The composition is discarded with a changing [key] rather than by recreating the activity,
 * because `setContent` does not run again for a recreated activity and the screen would simply be
 * blank — which would prove nothing.
 */
@RunWith(AndroidJUnit4::class)
class ResetRotationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var erases = 0
    private val viewModel = ResetViewModel(EraseAllData({ erases++ }, { }, { }, { }))
    private val generation = mutableIntStateOf(0)

    @Test
    fun rebuildingTheDialog_keepsItOpenTickedAndConfirmable() {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PillsnerTheme {
                // Everything inside is discarded and composed afresh when the key changes.
                key(generation.intValue) {
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
        }

        composeRule.onNodeWithTag(DangerZoneTestTags.BUTTON).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).performClick()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsEnabled()

        composeRule.runOnUiThread { generation.intValue++ }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ResetDialogTestTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ResetDialogTestTags.CHECKBOX).assertIsOn()
        composeRule.onNodeWithTag(ResetDialogTestTags.CONFIRM).assertIsEnabled()
        assertEquals("And nothing has been erased along the way", 0, erases)
    }
}
