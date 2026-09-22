package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.medicines.history.MedicineHistoryEffect
import nl.hexmaster.pillsner.ui.medicines.history.MedicineHistoryScreen
import nl.hexmaster.pillsner.ui.medicines.history.MedicineHistoryViewModel
import nl.hexmaster.pillsner.ui.medicines.schedule.ScheduleEditorScreen
import nl.hexmaster.pillsner.ui.navigation.MedicationForm
import nl.hexmaster.pillsner.ui.navigation.MedicationFormGraph
import nl.hexmaster.pillsner.ui.navigation.MedicineHistory
import nl.hexmaster.pillsner.ui.navigation.EditSchedule

/**
 * The add-medicine flow: the form and the schedule editor around one shared draft (design D4).
 *
 * Both destinations take their [MedicationFormViewModel] from the graph's own back-stack entry, so
 * a schedule built in the editor lands straight in the form's list, and the whole draft survives
 * rotation and process death for as long as the flow is on the back stack.
 */
fun NavGraphBuilder.medicationFormGraph(
    navController: NavHostController,
    viewModelFactory: ViewModelProvider.Factory,
    onOpenFailed: () -> Unit = {},
) {
    navigation<MedicationFormGraph>(startDestination = MedicationForm) {
        composable<MedicationForm> { entry ->
            val viewModel = entry.sharedViewModel(navController, viewModelFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val saveFailedMessage = stringResource(R.string.medicine_save_failed)

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        MedicationFormEffect.Saved -> navController.closeFlow()
                        MedicationFormEffect.SaveFailed -> snackbarHostState.showSnackbar(saveFailedMessage)
                        MedicationFormEffect.OpenFailed -> {
                            onOpenFailed()
                            navController.closeFlow()
                        }
                    }
                }
            }

            MedicationFormScreen(
                uiState = uiState,
                onNameChange = viewModel::onNameChange,
                onDoseTextChange = viewModel::onDoseTextChange,
                onDoseUnitChange = viewModel::onDoseUnitChange,
                onUsedSinceChange = viewModel::onUsedSinceChange,
                onUseUntilChange = viewModel::onUseUntilChange,
                onPrescriberChange = viewModel::onPrescriberChange,
                onAddSchedule = { navController.navigate(EditSchedule()) },
                onEditSchedule = { index -> navController.navigate(EditSchedule(index)) },
                onRemoveSchedule = viewModel::removeSchedule,
                onSave = viewModel::save,
                onActiveChanged = viewModel::onActiveChanged,
                onBack = { if (viewModel.onBackRequested()) navController.popBackStack() },
                onDiscard = {
                    viewModel.onDiscardDialogDismissed()
                    navController.closeFlow()
                },
                onKeepEditing = viewModel::onDiscardDialogDismissed,
                snackbarHostState = snackbarHostState,
                onOpenUsageHistory = {
                    // Navigating forward is not leaving the form, so the discard dialog stays out
                    // of it and the draft waits on the back stack.
                    (uiState.mode as? MedicationFormMode.Edit)
                        ?.let { navController.navigate(MedicineHistory(it.id.value)) }
                },
                onAddStockClicked = viewModel::onAddStockClicked,
                onAddStockDismissed = viewModel::onAddStockDismissed,
                onStockQuantityTextChange = viewModel::onStockQuantityTextChange,
                onStockUnitChange = viewModel::onStockUnitChange,
                onStockStrengthTextChange = viewModel::onStockStrengthTextChange,
                onStockExpiryDateChange = viewModel::onStockExpiryDateChange,
                onSaveStockBatch = viewModel::onSaveStockBatch,
                onRemoveStockBatchClicked = viewModel::onRemoveStockBatchClicked,
                onRemoveStockBatchCancelled = viewModel::onRemoveStockBatchCancelled,
                onRemoveStockBatchConfirmed = viewModel::onRemoveStockBatchConfirmed,
            )
        }

        composable<MedicineHistory> {
            // Its own view model, not the flow's: nothing the history does can reach the draft.
            val viewModel: MedicineHistoryViewModel = viewModel(factory = viewModelFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        MedicineHistoryEffect.OpenFailed -> {
                            onOpenFailed()
                            navController.popBackStack()
                        }
                    }
                }
            }

            MedicineHistoryScreen(
                uiState = uiState,
                onPeriodSelected = viewModel::onPeriodSelected,
                onBack = { navController.popBackStack() },
            )
        }

        composable<EditSchedule> { entry ->
            val viewModel = entry.sharedViewModel(navController, viewModelFactory)
            val index = entry.toRoute<EditSchedule>().index
            val uiState by viewModel.editorState.collectAsStateWithLifecycle()

            // Loads the schedule being edited once per visit, not on every recomposition.
            LaunchedEffect(index) { viewModel.openSchedule(index) }

            ScheduleEditorScreen(
                uiState = uiState,
                onAmountTextChange = viewModel::onAmountTextChange,
                onAmountUnitChange = viewModel::onAmountUnitChange,
                onPatternChange = viewModel::onPatternChange,
                onIntervalDaysChange = viewModel::onIntervalDaysChange,
                onIntervalHoursChange = viewModel::onIntervalHoursChange,
                onFirstDoseAtChange = viewModel::onFirstDoseAtChange,
                onDayToggled = viewModel::onDayToggled,
                onTimeAdded = viewModel::onTimeAdded,
                onTimeRemoved = viewModel::onTimeRemoved,
                onDone = { if (viewModel.commitSchedule()) navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Leaves the whole flow, whichever medicine it was opened on. */
private fun NavHostController.closeFlow() {
    popBackStack(route = MedicationFormGraph::class, inclusive = true)
}

/** The view model scoped to the whole flow rather than to one destination. */
@Composable
private fun androidx.navigation.NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
    viewModelFactory: ViewModelProvider.Factory,
): MedicationFormViewModel {
    val graphEntry = remember(this) { navController.getBackStackEntry<MedicationFormGraph>() }
    return viewModel(viewModelStoreOwner = graphEntry, factory = viewModelFactory)
}
