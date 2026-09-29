package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
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
import nl.hexmaster.pillsner.ui.medicines.labelscan.CameraPermission
import nl.hexmaster.pillsner.ui.medicines.labelscan.LabelScanCamera
import nl.hexmaster.pillsner.ui.medicines.labelscan.LabelScanEffect
import nl.hexmaster.pillsner.ui.medicines.labelscan.LabelScanScreen
import nl.hexmaster.pillsner.ui.medicines.labelscan.LabelScanViewModel
import nl.hexmaster.pillsner.ui.medicines.labelscan.rememberCameraPermissionRequest
import nl.hexmaster.pillsner.ui.medicines.schedule.ScheduleEditorScreen
import nl.hexmaster.pillsner.ui.navigation.MedicationForm
import nl.hexmaster.pillsner.ui.navigation.MedicationFormGraph
import nl.hexmaster.pillsner.ui.navigation.MedicineHistory
import nl.hexmaster.pillsner.ui.navigation.EditSchedule
import nl.hexmaster.pillsner.ui.navigation.LabelScan

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
            val context = LocalContext.current
            val cameraUnavailableMessage = stringResource(R.string.label_scan_camera_unavailable)
            val openSettingsLabel = stringResource(R.string.label_scan_open_settings)
            val nothingReadableMessage = stringResource(R.string.label_scan_nothing_readable)
            val photoUnreadableMessage = stringResource(R.string.label_scan_photo_unreadable)
            // Registered here rather than in the screen, so the result reaches the view model even
            // after a configuration change while the system prompt or the picker was showing.
            val requestCameraPermission = rememberCameraPermissionRequest(viewModel::onCameraPermissionResult)
            val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                viewModel.onPhotoPicked(uri?.toString())
            }

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        MedicationFormEffect.Saved -> navController.closeFlow()
                        MedicationFormEffect.SaveFailed -> snackbarHostState.showSnackbar(saveFailedMessage)
                        MedicationFormEffect.OpenFailed -> {
                            onOpenFailed()
                            navController.closeFlow()
                        }
                        MedicationFormEffect.OpenLabelScan -> navController.navigate(LabelScan)
                        MedicationFormEffect.RequestCameraPermission -> requestCameraPermission()
                        MedicationFormEffect.PickPhoto -> pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                        is MedicationFormEffect.CameraUnavailable -> {
                            val result = snackbarHostState.showSnackbar(
                                message = cameraUnavailableMessage,
                                actionLabel = openSettingsLabel.takeIf { effect.permanentlyDenied },
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                context.startActivity(CameraPermission.appSettingsIntent(context))
                            }
                        }
                        MedicationFormEffect.NothingReadable -> snackbarHostState.showSnackbar(nothingReadableMessage)
                        MedicationFormEffect.PhotoUnreadable -> snackbarHostState.showSnackbar(photoUnreadableMessage)
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
                onSecondaryDetailsToggled = viewModel::onSecondaryDetailsToggled,
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
                onScanLabelClicked = viewModel::onScanLabelClicked,
                onScanOptionsDismissed = viewModel::onScanOptionsDismissed,
                onScanWithCameraChosen = { viewModel.onScanWithCameraChosen(CameraPermission.isGranted(context)) },
                onChoosePhotoChosen = viewModel::onChoosePhotoChosen,
                onRationaleContinue = viewModel::onRationaleContinue,
                onRationaleDismissed = viewModel::onRationaleDismissed,
                onCancelScan = viewModel::cancelScan,
                onReplaceConfirmed = viewModel::onReplaceConfirmed,
                onReplaceDeclined = viewModel::onReplaceDeclined,
                onScanBannerDismissed = viewModel::onScanBannerDismissed,
                onShowScanText = viewModel::onShowScanText,
                onScanTextDismissed = viewModel::onScanTextDismissed,
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

        composable<LabelScan> { entry ->
            val formViewModel = entry.sharedViewModel(navController, viewModelFactory)
            // Its own view model for the camera session (design D2); the result lands in the shared draft.
            val scanViewModel: LabelScanViewModel = viewModel(factory = viewModelFactory)
            val uiState by scanViewModel.uiState.collectAsStateWithLifecycle()
            val surfaceRequest by scanViewModel.surfaceRequest.collectAsStateWithLifecycle()
            val haptics = LocalHapticFeedback.current
            // The viewport that keeps the preview and the analysis stream on the same region needs the
            // viewfinder's aspect ratio, which only layout knows.
            var viewfinderSize by remember { mutableStateOf(IntSize.Zero) }

            LaunchedEffect(scanViewModel) {
                scanViewModel.effects.collect { effect ->
                    when (effect) {
                        is LabelScanEffect.Finished -> {
                            if (effect.accepted) {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                // Long enough for the live region to announce "Label read" (design D3).
                                delay(ACCEPTANCE_DWELL_MILLIS)
                            }
                            formViewModel.onInterpretationReceived(effect.interpretation)
                            navController.popBackStack()
                        }
                    }
                }
            }

            LabelScanCamera(scanViewModel, viewfinderSize)
            LabelScanScreen(
                uiState = uiState,
                onShutter = scanViewModel::onShutter,
                onTorchToggled = scanViewModel::onTorchToggled,
                onCancel = {
                    scanViewModel.onCancel()
                    navController.popBackStack()
                },
                guide = scanViewModel.guide,
                onViewfinderSizeChanged = { viewfinderSize = it },
                viewfinder = { viewfinderModifier ->
                    surfaceRequest?.let { request -> CameraXViewfinder(request, viewfinderModifier) }
                },
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

/** How long "Label read" stays on screen before the form returns (design D3). */
private const val ACCEPTANCE_DWELL_MILLIS = 600L

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
