package nl.hexmaster.pillsner.ui.medicines.form

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.model.isWholePill
import nl.hexmaster.pillsner.domain.model.summarize
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository
import nl.hexmaster.pillsner.domain.scheduling.currentDates
import nl.hexmaster.pillsner.domain.stock.AddStockBatch
import nl.hexmaster.pillsner.domain.stock.ProjectWeeklyUsage
import nl.hexmaster.pillsner.domain.stock.StockState
import nl.hexmaster.pillsner.domain.stock.stockState
import nl.hexmaster.pillsner.domain.validation.MedicationFieldError
import nl.hexmaster.pillsner.domain.validation.MedicationFormValidator
import nl.hexmaster.pillsner.domain.validation.ScheduleDraftValidator
import nl.hexmaster.pillsner.domain.validation.SchedulePattern
import nl.hexmaster.pillsner.data.labelscan.PhotoScanner
import nl.hexmaster.pillsner.domain.labelscan.LabelInterpretation
import nl.hexmaster.pillsner.ui.medicines.AmountParser

/**
 * Owns the one draft that the add-medicine form and the schedule editor both edit (design D8).
 *
 * The two screens share this view model through the flow's navigation graph entry, which is what
 * lets a schedule built in the editor land in the form's list without a round trip through the
 * database. Nothing here logs the name or any amount: they are the user's medical data.
 *
 * @param dates today's date, re-emitted when it changes, so the Stock section's expiry heads-up and
 *   weekly projection move on at midnight while the form stays open.
 * @param photoScanner reads a picked photo into an interpretation (medicine-label-photo-prefill D5).
 * @param cameraAvailable whether the device has a camera, so the option sheet can offer it.
 */
class MedicationFormViewModel(
    private val repository: MedicationRepository,
    private val savedStateHandle: SavedStateHandle,
    private val amountParser: AmountParser = AmountParser(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val stockBatchRepository: StockBatchRepository? = null,
    private val addStockBatch: AddStockBatch? = null,
    private val dates: Flow<LocalDate> = currentDates(clock),
    private val photoScanner: PhotoScanner? = null,
    private val cameraAvailable: Boolean = true,
) : ViewModel() {

    /** The date the form opened on: only the starting "used since" of a new draft. */
    private val today: LocalDate = LocalDate.now(clock)
    private val projectWeeklyUsage = ProjectWeeklyUsage()

    /** Set once the medicine is loaded; the Stock section reads its unit and schedules. */
    private var loadedMedication: Medication? = null

    /** Which entrance the user came through; the route carries the medicine, or nothing. */
    private val mode: MedicationFormMode =
        savedStateHandle.get<Long>(MEDICATION_ID_ARG)
            ?.let { MedicationFormMode.Edit(MedicationId(it)) }
            ?: MedicationFormMode.Add

    private var draft: MedicationFormDraft = DraftSaver.restore(savedStateHandle, today)

    /**
     * What the form opened with. "Has the user changed anything" is this compared with [draft], so
     * reverting an edit by hand leaves the form untouched again and back does not ask.
     */
    private var initialDraft: MedicationFormDraft =
        DraftSaver.restoreInitial(savedStateHandle) ?: MedicationFormDraft(usedSince = today)

    private var scheduleDraft: ScheduleDraft = ScheduleDraft()

    private val _uiState = MutableStateFlow(draft.toUiState(showErrors = false))
    val uiState: StateFlow<MedicationFormUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow(ScheduleEditorUiState())
    val editorState: StateFlow<ScheduleEditorUiState> = _editorState.asStateFlow()

    // A channel, not a shared flow: the form can fail to open before the screen has started
    // collecting, and that message must still arrive rather than be dropped.
    private val _effects = Channel<MedicationFormEffect>(Channel.BUFFERED)
    val effects: Flow<MedicationFormEffect> = _effects.receiveAsFlow()

    init {
        val editing = mode as? MedicationFormMode.Edit
        when {
            editing == null -> DraftSaver.saveInitial(savedStateHandle, initialDraft)
            // A half-edited form must never snap back to the stored medicine after a rotation or
            // process death, but the stored medicine is still what the Stock section works on.
            DraftSaver.hasSavedDraft(savedStateHandle) -> load(editing.id, keepDraft = true)
            else -> load(editing.id, keepDraft = false)
        }
    }

    /**
     * Loads the medicine being edited and starts observing its stock.
     *
     * @param keepDraft true when a saved draft was restored: the draft's fields stay exactly as the
     *   user left them, and only the stored medicine behind the Stock section is loaded.
     */
    private fun load(id: MedicationId, keepDraft: Boolean) {
        if (!keepDraft) _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val medication = runCatching { repository.get(id) }.getOrNull()
            if (medication == null) {
                // Medicines are never removed, so this can only be a defect. The id is safe to
                // log; the name never is.
                Log.d(TAG, "No medicine with id ${id.value} to open")
                _effects.trySend(MedicationFormEffect.OpenFailed)
                return@launch
            }
            if (!keepDraft) {
                draft = MedicationFormDraft.from(medication, amountParser.format(medication.defaultDose.value))
                initialDraft = draft
                DraftSaver.save(savedStateHandle, draft)
                DraftSaver.saveInitial(savedStateHandle, initialDraft)
                _uiState.value = draft.toUiState(showErrors = false)
            }
            loadedMedication = medication
            observeStock(medication)
        }
    }

    /**
     * Keeps the Stock section live: re-emits whenever the medicine's batches change or the date
     * does, not only when the form itself is edited (`medicine-stock-tracking`'s "Tile and
     * details-screen heads-up is a live read" decision).
     */
    private fun observeStock(medication: Medication) {
        val repo = stockBatchRepository ?: return
        viewModelScope.launch {
            combine(repo.observeBatches(medication.id), dates, ::Pair).collect { (batches, date) ->
                val rows = batches
                    .sortedWith(compareBy({ it.expiryDate }, { it.addedAt }))
                    .map { StockBatchRowState(it.id, it.usableRemaining, it.unit, it.strengthPerUnit, it.expiryDate) }
                val state = batches.takeIf { it.isNotEmpty() }
                    ?.let { stockState(it, medication, date, clock.zone, projectWeeklyUsage) }
                // Every batch's strength is relative to the stored default dose unit, so that unit
                // cannot change on this form while any batch exists.
                val lockedDoseUnit = medication.defaultDose.unit.takeIf { batches.isNotEmpty() }
                _uiState.update { it.copy(stockBatches = rows, stockState = state, lockedDoseUnit = lockedDoseUnit) }
            }
        }
    }

    // --- Stock events (medicine-stock-tracking) --------------------------------------------

    fun onAddStockClicked() {
        val medication = loadedMedication ?: return
        _uiState.update {
            it.copy(
                addStockState = AddStockUiState(
                    defaultDoseUnit = medication.defaultDose.unit,
                    unit = medication.defaultDose.unit,
                ),
            )
        }
    }

    fun onAddStockDismissed() {
        _uiState.update { it.copy(addStockState = null) }
    }

    fun onStockQuantityTextChange(value: String) = updateAddStock { it.copy(quantityText = value) }

    fun onStockUnitChange(value: DoseUnit) = updateAddStock { it.copy(unit = value) }

    fun onStockStrengthTextChange(value: String) = updateAddStock { it.copy(strengthText = value) }

    fun onStockExpiryDateChange(value: LocalDate) = updateAddStock {
        it.copy(expiryDate = value, expiryPastWarning = value.isBefore(LocalDate.now(clock)))
    }

    fun onSaveStockBatch() {
        val current = _uiState.value.addStockState ?: return
        val validated = current.copy(
            showErrors = true,
            quantityError = validateStockAmount(current.quantityText, wholePillsOnly = current.unit.isWholePill),
            strengthError = if (current.needsStrength) validateStockAmount(current.strengthText) else null,
        )
        _uiState.update { it.copy(addStockState = validated) }
        if (!validated.canSave) return

        val amount = amountParser.parse(current.quantityText) ?: return
        val strength = if (current.needsStrength) {
            amountParser.parse(current.strengthText) ?: return
        } else {
            java.math.BigDecimal.ONE
        }
        val expiryDate = current.expiryDate ?: return
        val medicationId = (loadedMedication ?: return).id
        _uiState.update { it.copy(addStockState = validated.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                addStockBatch?.invoke(medicationId, Quantity(amount, current.unit), strength, expiryDate)
            }
            _uiState.update { it.copy(addStockState = null) }
        }
    }

    private fun updateAddStock(transform: (AddStockUiState) -> AddStockUiState) {
        _uiState.update { state ->
            val current = state.addStockState ?: return@update state
            state.copy(addStockState = transform(current))
        }
    }

    /**
     * [wholePillsOnly] rejects a fractional amount, for a quantity counted in tablets or capsules
     * (`medicine-stock-tracking`'s "Whole-pill units" requirement).
     */
    private fun validateStockAmount(text: String, wholePillsOnly: Boolean = false): MedicationFieldError? {
        if (text.isBlank()) return MedicationFieldError.DOSE_REQUIRED
        val value = amountParser.parse(text) ?: return MedicationFieldError.DOSE_NOT_A_NUMBER
        if (value <= java.math.BigDecimal.ZERO) return MedicationFieldError.DOSE_NOT_POSITIVE
        if (wholePillsOnly && value.stripTrailingZeros().scale() > 0) return MedicationFieldError.STOCK_NOT_WHOLE_PILLS
        return null
    }

    /** Opens the removal confirmation dialog for one batch (`medicine-stock-tracking`). */
    fun onRemoveStockBatchClicked(batchId: StockBatchId) {
        _uiState.update { it.copy(pendingStockRemoval = batchId) }
    }

    /** Cancelling, or dismissing the dialog any other way, leaves every batch untouched. */
    fun onRemoveStockBatchCancelled() {
        _uiState.update { it.copy(pendingStockRemoval = null) }
    }

    fun onRemoveStockBatchConfirmed() {
        val batchId = _uiState.value.pendingStockRemoval ?: return
        _uiState.update { it.copy(pendingStockRemoval = null) }
        viewModelScope.launch {
            runCatching { stockBatchRepository?.removeBatch(batchId) }
        }
    }

    // --- Form events ----------------------------------------------------------------------

    fun onActiveChanged(value: Boolean) = updateDraft { it.copy(isActive = value) }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }

    fun onDoseTextChange(value: String) = updateDraft { it.copy(doseText = value) }

    fun onDoseUnitChange(value: DoseUnit) = updateDraft { it.copy(doseUnit = value) }

    fun onUsedSinceChange(value: LocalDate) = updateDraft { it.copy(usedSince = value) }

    fun onUseUntilChange(value: LocalDate?) = updateDraft { it.copy(useUntil = value) }

    fun onPrescriberChange(value: Prescriber) = updateDraft { it.copy(prescribedBy = value) }

    fun removeSchedule(index: Int) = updateDraft { current ->
        current.copy(schedules = current.schedules.filterIndexed { position, _ -> position != index })
    }

    /** Back was pressed: a touched form asks first, an untouched one may leave straight away. */
    fun onBackRequested(): Boolean {
        val touched = draft != initialDraft
        if (touched) _uiState.update { it.copy(showDiscardDialog = true) }
        return !touched
    }

    fun onDiscardDialogDismissed() = _uiState.update { it.copy(showDiscardDialog = false) }

    /**
     * Opens or closes the secondary details panel. Pure presentation: the flag lives next to the
     * draft in the saved state so it survives rotation and process death, but it is not part of
     * the draft, so toggling never counts as an edit (`medicine-details`, "Secondary details
     * toggle").
     */
    fun onSecondaryDetailsToggled() = setSecondaryDetailsExpanded(!_uiState.value.secondaryDetailsExpanded)

    private fun setSecondaryDetailsExpanded(expanded: Boolean) {
        savedStateHandle[SECONDARY_DETAILS_EXPANDED_KEY] = expanded
        _uiState.update { it.copy(secondaryDetailsExpanded = expanded) }
    }

    fun save() {
        val state = _uiState.value.copy(showErrors = true)
        _uiState.value = state
        if (!state.canSave) {
            // An error the user cannot see is no feedback at all: when a field folded away behind
            // the secondary details toggle is what stops the save, the panel opens on its own.
            if (state.hasSecondaryDetailsError) setSecondaryDetailsExpanded(true)
            return
        }

        val amount = amountParser.parse(draft.doseText) ?: return
        val defaultDose = Quantity(amount, draft.doseUnit)

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val effect = runCatching {
                when (val current = mode) {
                    MedicationFormMode.Add -> repository.add(
                        NewMedication(
                            name = draft.name.trim(),
                            defaultDose = defaultDose,
                            usedSince = draft.usedSince,
                            useUntil = draft.useUntil,
                            prescribedBy = draft.prescribedBy,
                            schedules = draft.schedules,
                        ),
                    )

                    is MedicationFormMode.Edit ->
                        repository.update(draft.toMedication(current.id, defaultDose))
                }
            }.fold(
                onSuccess = { MedicationFormEffect.Saved },
                // Deliberately no detail: the exception could carry the medicine's name.
                onFailure = { MedicationFormEffect.SaveFailed },
            )
            _uiState.update { it.copy(isSaving = false) }
            _effects.trySend(effect)
        }
    }

    // --- Editor events --------------------------------------------------------------------

    /** Opens the editor on the schedule at [index], or on a new one when [index] is null. */
    fun openSchedule(index: Int?) {
        val existing = index?.let { draft.schedules.getOrNull(it) }
        scheduleDraft = if (existing == null) {
            ScheduleDraft(amountText = draft.doseText, amountUnit = draft.doseUnit)
        } else {
            ScheduleDraft.from(index, existing, amountParser.format(existing.amount.value))
        }
        publishEditor()
    }

    fun onAmountTextChange(value: String) = updateSchedule { it.copy(amountText = value) }

    fun onAmountUnitChange(value: DoseUnit) = updateSchedule { it.copy(amountUnit = value) }

    /** Switching pattern keeps the amount and the times; only the inputs on screen change. */
    fun onPatternChange(value: SchedulePattern) = updateSchedule { it.copy(pattern = value) }

    fun onIntervalDaysChange(value: Int) = updateSchedule {
        it.copy(intervalDays = value.coerceIn(1, Schedule.MAX_INTERVAL_DAYS))
    }

    fun onIntervalHoursChange(value: Int) = updateSchedule { it.copy(intervalHours = value) }

    fun onFirstDoseAtChange(value: LocalTime) = updateSchedule { it.copy(firstDoseAt = value) }

    fun onDayToggled(day: DayOfWeek) = updateSchedule { current ->
        current.copy(days = if (day in current.days) current.days - day else current.days + day)
    }

    /** Adds [time] unless it is already listed; the list stays in ascending order. */
    fun onTimeAdded(time: LocalTime) {
        if (time in scheduleDraft.times) {
            _editorState.update { it.copy(duplicateTimeRejected = true) }
            return
        }
        updateSchedule { it.copy(times = (it.times + time).sorted()) }
    }

    fun onTimeRemoved(time: LocalTime) = updateSchedule { it.copy(times = it.times - time) }

    fun onDuplicateTimeMessageShown() = _editorState.update { it.copy(duplicateTimeRejected = false) }

    /**
     * Validates and puts the schedule into the draft, appending a new one or replacing the one
     * being edited.
     *
     * @return true when the editor may close.
     */
    fun commitSchedule(): Boolean {
        val amount = amountParser.parse(scheduleDraft.amountText)
        val schedule = scheduleDraft.toSchedule(amount)
        if (schedule == null) {
            updateSchedule { it.copy(showErrors = true) }
            return false
        }
        val index = scheduleDraft.index
        updateDraft { current ->
            val schedules = if (index == null) {
                current.schedules + schedule
            } else {
                current.schedules.toMutableList().also { it[index] = schedule }
            }
            current.copy(schedules = schedules)
        }
        return true
    }

    // --- Label scan (medicine-label-photo-prefill design D5) --------------------------------

    private var scanJob: Job? = null

    fun onScanLabelClicked() = _uiState.update { it.copy(showScanOptions = true) }

    fun onScanOptionsDismissed() = _uiState.update { it.copy(showScanOptions = false) }

    /**
     * "Scan with camera" was chosen. With the permission already granted the scanning screen opens
     * at once; otherwise the in-app rationale comes first, and only its Continue shows the system
     * prompt (design D2). The permission is never requested anywhere else.
     */
    fun onScanWithCameraChosen(permissionGranted: Boolean) {
        _uiState.update { it.copy(showScanOptions = false, showCameraRationale = !permissionGranted) }
        if (permissionGranted) _effects.trySend(MedicationFormEffect.OpenLabelScan)
    }

    fun onRationaleContinue() {
        _uiState.update { it.copy(showCameraRationale = false) }
        _effects.trySend(MedicationFormEffect.RequestCameraPermission)
    }

    /** "Not now": no system prompt, and the form exactly as it was. */
    fun onRationaleDismissed() = _uiState.update { it.copy(showCameraRationale = false) }

    fun onCameraPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        val effect = if (granted) {
            MedicationFormEffect.OpenLabelScan
        } else {
            MedicationFormEffect.CameraUnavailable(permanentlyDenied)
        }
        _effects.trySend(effect)
    }

    fun onChoosePhotoChosen() {
        _uiState.update { it.copy(showScanOptions = false) }
        _effects.trySend(MedicationFormEffect.PickPhoto)
    }

    /**
     * The picker returned. Null means the user backed out of it. Otherwise the photo is read once,
     * off the main thread, behind the modal "Reading the photo" state, and never copied (design D6).
     *
     * @param uri the picked photo's content URI as text, so this stays free of Android types.
     */
    fun onPhotoPicked(uri: String?) {
        if (uri == null) return
        val scanner = photoScanner ?: return
        _uiState.update { it.copy(isScanning = true) }
        scanJob = viewModelScope.launch {
            val interpretation = try {
                scanner.scan(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: RuntimeException) {
                // Deliberately no detail: a decoder message could carry the file's name.
                Log.d(TAG, "Picked photo scan failed")
                null
            }
            if (interpretation == null) {
                _effects.trySend(MedicationFormEffect.PhotoUnreadable)
            } else {
                onInterpretationReceived(interpretation)
            }
        }.also { job ->
            // Whatever the outcome, cancellation included, the reading state ends only once the job,
            // and with it the recogniser session it owns, has fully completed.
            job.invokeOnCompletion {
                if (scanJob === job) scanJob = null
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    /** Cancel on the "Reading the photo" state: stops the read and leaves the form unchanged. */
    fun cancelScan() {
        // Interrupt the native recognition first, so the cancelled job reaches its close() quickly.
        // The modal stays up until that close has run (see the completion handler in onPhotoPicked):
        // the recogniser is shared with the live scan, and a new session must not open against an
        // engine the cancelled one is still about to release.
        photoScanner?.stop()
        scanJob?.cancel()
    }

    /**
     * The scanning screen or the picked-photo path finished (design D5). An untouched draft takes
     * the result at once; an edited one is asked first; an empty result changes nothing and says so.
     */
    fun onInterpretationReceived(interpretation: LabelInterpretation) {
        when {
            interpretation.isEmpty -> _effects.trySend(MedicationFormEffect.NothingReadable)
            draft == initialDraft -> applyInterpretation(interpretation)
            else -> _uiState.update { it.copy(pendingInterpretation = interpretation) }
        }
    }

    fun onReplaceConfirmed() {
        val pending = _uiState.value.pendingInterpretation ?: return
        _uiState.update { it.copy(pendingInterpretation = null) }
        applyInterpretation(pending)
    }

    /** "Keep": the interpretation is discarded and the draft stays as typed. */
    fun onReplaceDeclined() = _uiState.update { it.copy(pendingInterpretation = null) }

    /** Dismissed for good: the flag lives in the saved state, so rotation does not bring it back. */
    fun onScanBannerDismissed() {
        savedStateHandle[SCAN_BANNER_KEY] = false
        _uiState.update { it.copy(showScanBanner = false) }
    }

    fun onShowScanText() = _uiState.update { it.copy(showScanText = true) }

    fun onScanTextDismissed() = _uiState.update { it.copy(showScanText = false) }

    /**
     * Sets exactly the fields the interpretation carries and leaves every other one, prescriber
     * included. An empty schedule list and a null use-until mean "absent", not "clear". Goes
     * through [updateDraft], so the pre-fill is saved like any edit and survives rotation.
     */
    private fun applyInterpretation(interpretation: LabelInterpretation) {
        savedStateHandle[SCAN_BANNER_KEY] = true
        savedStateHandle[SCAN_RAW_TEXT_KEY] = interpretation.rawText
        updateDraft { current ->
            current.copy(
                name = interpretation.name ?: current.name,
                doseText = interpretation.defaultDose?.let { amountParser.format(it.value) } ?: current.doseText,
                doseUnit = interpretation.defaultDose?.unit ?: current.doseUnit,
                schedules = interpretation.schedules.ifEmpty { current.schedules },
                usedSince = interpretation.usedSince,
                useUntil = interpretation.useUntil ?: current.useUntil,
            )
        }
    }

    // --- Plumbing -------------------------------------------------------------------------

    private fun updateDraft(transform: (MedicationFormDraft) -> MedicationFormDraft) {
        draft = transform(draft)
        DraftSaver.save(savedStateHandle, draft)
        val previous = _uiState.value
        _uiState.value = draft.toUiState(
            showErrors = previous.showErrors,
            isSaving = previous.isSaving,
            showDiscardDialog = previous.showDiscardDialog,
            stockBatches = previous.stockBatches,
            stockState = previous.stockState,
            lockedDoseUnit = previous.lockedDoseUnit,
            addStockState = previous.addStockState,
            pendingStockRemoval = previous.pendingStockRemoval,
            showScanOptions = previous.showScanOptions,
            showCameraRationale = previous.showCameraRationale,
            isScanning = previous.isScanning,
            pendingInterpretation = previous.pendingInterpretation,
            showScanText = previous.showScanText,
        )
    }

    private fun updateSchedule(transform: (ScheduleDraft) -> ScheduleDraft) {
        scheduleDraft = transform(scheduleDraft)
        publishEditor()
    }

    private fun publishEditor() {
        val amount = amountParser.parse(scheduleDraft.amountText)
        val errors = ScheduleDraftValidator.validate(
            pattern = scheduleDraft.pattern,
            amountText = scheduleDraft.amountText,
            amountValue = amount,
            times = scheduleDraft.times,
            days = scheduleDraft.days,
        )
        val schedule = scheduleDraft.toSchedule(amount)
        _editorState.value = ScheduleEditorUiState(
            isNew = scheduleDraft.index == null,
            amountText = scheduleDraft.amountText,
            amountUnit = scheduleDraft.amountUnit,
            pattern = scheduleDraft.pattern,
            intervalDays = scheduleDraft.intervalDays,
            intervalHours = scheduleDraft.intervalHours,
            days = scheduleDraft.days,
            times = scheduleDraft.times,
            firstDoseAt = scheduleDraft.firstDoseAt,
            dailyDoseTimes = Schedule.EveryNHours(
                amount = amount?.takeIf { it > java.math.BigDecimal.ZERO }
                    ?.let { Quantity(it, scheduleDraft.amountUnit) }
                    ?: PLACEHOLDER_AMOUNT,
                intervalHours = scheduleDraft.intervalHours,
                firstDoseAt = scheduleDraft.firstDoseAt,
            ).dailyDoseTimes(),
            preview = schedule?.summarize(),
            previewAmount = schedule?.amount,
            errors = errors,
            showErrors = scheduleDraft.showErrors,
            duplicateTimeRejected = false,
        )
    }

    private fun MedicationFormDraft.toUiState(
        showErrors: Boolean,
        isSaving: Boolean = false,
        showDiscardDialog: Boolean = false,
        stockBatches: List<StockBatchRowState> = emptyList(),
        stockState: StockState? = null,
        lockedDoseUnit: DoseUnit? = null,
        addStockState: AddStockUiState? = null,
        pendingStockRemoval: StockBatchId? = null,
        showScanOptions: Boolean = false,
        showCameraRationale: Boolean = false,
        isScanning: Boolean = false,
        pendingInterpretation: LabelInterpretation? = null,
        showScanText: Boolean = false,
    ): MedicationFormUiState {
        val validation = MedicationFormValidator.validate(
            name = name,
            doseText = doseText,
            doseValue = amountParser.parse(doseText),
            usedSince = usedSince,
            useUntil = useUntil,
        )
        return MedicationFormUiState(
            name = name,
            doseText = doseText,
            doseUnit = doseUnit,
            usedSince = usedSince,
            useUntil = useUntil,
            prescribedBy = prescribedBy,
            schedules = schedules.mapIndexed { index, schedule ->
                ScheduleRowState(index, schedule.summarize(), schedule.amount)
            },
            mode = mode,
            isActive = isActive,
            nameError = validation.name,
            doseError = validation.defaultDose,
            useUntilError = validation.useUntil,
            showErrors = showErrors,
            isSaving = isSaving,
            hasEdits = this != initialDraft,
            showDiscardDialog = showDiscardDialog,
            stockBatches = stockBatches,
            stockState = stockState,
            lockedDoseUnit = lockedDoseUnit,
            addStockState = addStockState,
            pendingStockRemoval = pendingStockRemoval,
            cameraAvailable = cameraAvailable,
            showScanOptions = showScanOptions,
            showCameraRationale = showCameraRationale,
            isScanning = isScanning,
            pendingInterpretation = pendingInterpretation,
            showScanText = showScanText,
            // Both live in the saved state, so the banner survives rotation and process death until
            // the user dismisses it, and never comes back once they have (design D5).
            showScanBanner = savedStateHandle.get<Boolean>(SCAN_BANNER_KEY) ?: false,
            scanRawText = savedStateHandle.get<String>(SCAN_RAW_TEXT_KEY),
            // The saved state is the one source of truth for the panel, so a state rebuilt after a
            // draft edit and a new view model after process death both read the same flag.
            secondaryDetailsExpanded = savedStateHandle[SECONDARY_DETAILS_EXPANDED_KEY] ?: false,
        )
    }

    private companion object {
        const val TAG = "MedicineForm"

        /** The name navigation gives the route argument of [MedicationFormGraph]. */
        const val MEDICATION_ID_ARG = "medicationId"

        /** Where the secondary details panel's open/closed flag lives, next to the draft. */
        const val SECONDARY_DETAILS_EXPANDED_KEY = "secondary_details_expanded"

        /** The review banner shown flag, next to the draft (medicine-label-photo-prefill D5). */
        const val SCAN_BANNER_KEY = "scan_banner_shown"

        /** The recognised text behind the banner action "Show text", next to the draft. */
        const val SCAN_RAW_TEXT_KEY = "scan_raw_text"

        /**
         * The daily dose times depend only on the interval and the first dose, but the shape needs
         * an amount to exist, so an invalid amount borrows this one. It never reaches the draft.
         */
        val PLACEHOLDER_AMOUNT = Quantity.of("1", DoseUnit.UNIT)
    }
}
