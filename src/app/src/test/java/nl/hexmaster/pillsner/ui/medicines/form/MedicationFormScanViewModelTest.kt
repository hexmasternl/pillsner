package nl.hexmaster.pillsner.ui.medicines.form

import androidx.lifecycle.SavedStateHandle
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.data.labelscan.PhotoScanner
import nl.hexmaster.pillsner.domain.labelscan.LabelInterpretation
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.ui.medicines.AmountParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The form's side of a label scan (spec: medicine-label-scan, "Interpretation pre-fills the form"). */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicationFormScanViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 9, 29)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val repository = InMemoryMedicationRepository()
    private val scanner = FakePhotoScanner()

    private val interpretation = LabelInterpretation(
        name = "Zorvalex",
        defaultDose = Quantity.of("50", DoseUnit.MILLIGRAM),
        schedules = listOf(
            Schedule.EveryNDays(Quantity.of("1", DoseUnit.TABLET), 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))),
        ),
        usedSince = LocalDate.of(2026, 9, 27),
        useUntil = LocalDate.of(2026, 10, 6),
        rawText = "ZORVALEX 50 MG\nTake 1 tablet twice daily",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Applying -------------------------------------------------------------------------

    @Test
    fun `an untouched draft takes the interpretation at once and shows the banner`() {
        val viewModel = viewModel()

        viewModel.onInterpretationReceived(interpretation)

        val state = viewModel.uiState.value
        assertEquals("Zorvalex", state.name)
        assertEquals("50", state.doseText)
        assertEquals(DoseUnit.MILLIGRAM, state.doseUnit)
        assertEquals(1, state.schedules.size)
        assertEquals(ScheduleSummary.TimesPerDay(2), state.schedules.single().summary)
        assertEquals(LocalDate.of(2026, 9, 27), state.usedSince)
        assertEquals(LocalDate.of(2026, 10, 6), state.useUntil)
        assertEquals(Prescriber.GENERAL_PRACTITIONER, state.prescribedBy)
        assertTrue(state.showScanBanner)
        assertEquals(interpretation.rawText, state.scanRawText)
        assertNull(state.pendingInterpretation)
        assertTrue(state.hasEdits)
    }

    @Test
    fun `an edited draft asks first and keeps what was typed until then`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Typed name")

        viewModel.onInterpretationReceived(interpretation)

        val state = viewModel.uiState.value
        assertNotNull(state.pendingInterpretation)
        assertEquals("Typed name", state.name)
        assertFalse(state.showScanBanner)
    }

    @Test
    fun `keep leaves the draft as typed`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Typed name")
        viewModel.onInterpretationReceived(interpretation)

        viewModel.onReplaceDeclined()

        val state = viewModel.uiState.value
        assertNull(state.pendingInterpretation)
        assertEquals("Typed name", state.name)
        assertFalse(state.showScanBanner)
    }

    @Test
    fun `replace applies the interpretation`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Typed name")
        viewModel.onInterpretationReceived(interpretation)

        viewModel.onReplaceConfirmed()

        val state = viewModel.uiState.value
        assertNull(state.pendingInterpretation)
        assertEquals("Zorvalex", state.name)
        assertTrue(state.showScanBanner)
    }

    @Test
    fun `the prescriber never changes, even through replace`() {
        val viewModel = viewModel()
        viewModel.onPrescriberChange(Prescriber.SPECIALIST)
        viewModel.onInterpretationReceived(interpretation)

        viewModel.onReplaceConfirmed()

        assertEquals(Prescriber.SPECIALIST, viewModel.uiState.value.prescribedBy)
        assertEquals("Zorvalex", viewModel.uiState.value.name)
    }

    @Test
    fun `absent fields are left as they are`() {
        val viewModel = viewModel()
        val nameOnly = LabelInterpretation(name = "Zorvalex", usedSince = today)

        viewModel.onInterpretationReceived(nameOnly)

        val state = viewModel.uiState.value
        assertEquals("Zorvalex", state.name)
        assertEquals("", state.doseText)
        assertEquals(DoseUnit.MILLIGRAM, state.doseUnit)
        assertTrue(state.schedules.isEmpty())
        assertEquals(today, state.usedSince)
        assertNull(state.useUntil)
    }

    @Test
    fun `an empty interpretation leaves the draft and says nothing was readable`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onInterpretationReceived(LabelInterpretation.empty(today))

        assertEquals(listOf(MedicationFormEffect.NothingReadable), effects)
        assertEquals("", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.hasEdits)
        assertFalse(viewModel.uiState.value.showScanBanner)
    }

    @Test
    fun `a pre-filled amount still goes through the ordinary validation on save`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onInterpretationReceived(interpretation)

        viewModel.onDoseTextChange("abc")
        viewModel.save()

        assertTrue(viewModel.uiState.value.showErrors)
        assertNotNull(viewModel.uiState.value.doseError)
        assertTrue(repository.observeAllNow().isEmpty())
    }

    // --- Banner ---------------------------------------------------------------------------

    @Test
    fun `the banner flag and the text survive a saved-state round trip until dismissed`() {
        val handle = SavedStateHandle()
        viewModel(savedStateHandle = handle).onInterpretationReceived(interpretation)

        val restored = viewModel(savedStateHandle = handle)
        assertTrue(restored.uiState.value.showScanBanner)
        assertEquals(interpretation.rawText, restored.uiState.value.scanRawText)
        assertEquals("Zorvalex", restored.uiState.value.name)

        restored.onScanBannerDismissed()
        val afterDismiss = viewModel(savedStateHandle = handle)

        assertFalse(afterDismiss.uiState.value.showScanBanner)
    }

    @Test
    fun `the banner survives a draft edit and the text sheet opens and closes`() {
        val viewModel = viewModel()
        viewModel.onInterpretationReceived(interpretation)

        viewModel.onNameChange("Zorvalex 50")
        assertTrue(viewModel.uiState.value.showScanBanner)

        viewModel.onShowScanText()
        assertTrue(viewModel.uiState.value.showScanText)
        viewModel.onScanTextDismissed()
        assertFalse(viewModel.uiState.value.showScanText)
    }

    // --- Picked photo -----------------------------------------------------------------------

    @Test
    fun `a readable photo is applied like a camera scan`() = runTest(dispatcher) {
        scanner.result = interpretation
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://photos/1")

        assertFalse(viewModel.uiState.value.isScanning)
        assertEquals("Zorvalex", viewModel.uiState.value.name)
        assertEquals("content://photos/1", scanner.lastUri)
    }

    @Test
    fun `a photo that cannot be read says so and leaves the draft`() = runTest(dispatcher) {
        scanner.result = null
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onPhotoPicked("content://photos/1")

        assertEquals(listOf(MedicationFormEffect.PhotoUnreadable), effects)
        assertFalse(viewModel.uiState.value.isScanning)
        assertEquals("", viewModel.uiState.value.name)
    }

    @Test
    fun `a scanner failure is the same as an unreadable photo`() = runTest(dispatcher) {
        scanner.failure = IllegalStateException("decoder")
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onPhotoPicked("content://photos/1")

        assertEquals(listOf(MedicationFormEffect.PhotoUnreadable), effects)
    }

    @Test
    fun `backing out of the picker does nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onPhotoPicked(null)

        assertTrue(effects.isEmpty())
        assertFalse(viewModel.uiState.value.isScanning)
        assertNull(scanner.lastUri)
    }

    @Test
    fun `cancel while reading stops the scanner and clears the reading state`() = runTest(dispatcher) {
        scanner.hang = true
        val viewModel = viewModel()
        viewModel.onPhotoPicked("content://photos/1")
        assertTrue(viewModel.uiState.value.isScanning)

        viewModel.cancelScan()

        assertFalse(viewModel.uiState.value.isScanning)
        assertTrue(scanner.stopped)
        assertEquals("", viewModel.uiState.value.name)
    }

    // --- Entry point and permission ---------------------------------------------------------

    @Test
    fun `scanning is offered on a new medicine and not on an existing one`() = runTest(dispatcher) {
        assertTrue(viewModel().uiState.value.canScanLabel)

        val id = repository.add(
            NewMedication("Zorvalex", Quantity.of("50", DoseUnit.MILLIGRAM), today, null, Prescriber.SELF, emptyList()),
        )
        val editing = viewModel(savedStateHandle = SavedStateHandle(mapOf("medicationId" to id.value)))

        assertFalse(editing.uiState.value.canScanLabel)
    }

    @Test
    fun `the option sheet opens and closes, and the camera availability is what the container said`() {
        val viewModel = viewModel(cameraAvailable = false)

        viewModel.onScanLabelClicked()
        assertTrue(viewModel.uiState.value.showScanOptions)
        assertFalse(viewModel.uiState.value.cameraAvailable)

        viewModel.onScanOptionsDismissed()
        assertFalse(viewModel.uiState.value.showScanOptions)
    }

    @Test
    fun `scan with camera opens the screen at once when the permission is granted`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        viewModel.onScanLabelClicked()

        viewModel.onScanWithCameraChosen(permissionGranted = true)

        assertEquals(listOf(MedicationFormEffect.OpenLabelScan), effects)
        assertFalse(viewModel.uiState.value.showScanOptions)
        assertFalse(viewModel.uiState.value.showCameraRationale)
    }

    @Test
    fun `without the permission the rationale comes before any system prompt`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        viewModel.onScanLabelClicked()

        viewModel.onScanWithCameraChosen(permissionGranted = false)
        assertTrue(viewModel.uiState.value.showCameraRationale)
        assertTrue(effects.isEmpty())

        viewModel.onRationaleContinue()

        assertFalse(viewModel.uiState.value.showCameraRationale)
        assertEquals(listOf(MedicationFormEffect.RequestCameraPermission), effects)
    }

    @Test
    fun `not now closes the rationale and changes nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        viewModel.onScanWithCameraChosen(permissionGranted = false)

        viewModel.onRationaleDismissed()

        assertFalse(viewModel.uiState.value.showCameraRationale)
        assertTrue(effects.isEmpty())
        assertFalse(viewModel.uiState.value.hasEdits)
    }

    @Test
    fun `a granted permission opens the screen and a denied one says the camera is unavailable`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)

        viewModel.onCameraPermissionResult(granted = true, permanentlyDenied = false)
        viewModel.onCameraPermissionResult(granted = false, permanentlyDenied = false)
        viewModel.onCameraPermissionResult(granted = false, permanentlyDenied = true)

        assertEquals(
            listOf(
                MedicationFormEffect.OpenLabelScan,
                MedicationFormEffect.CameraUnavailable(permanentlyDenied = false),
                MedicationFormEffect.CameraUnavailable(permanentlyDenied = true),
            ),
            effects,
        )
        assertFalse(viewModel.uiState.value.hasEdits)
    }

    @Test
    fun `choose a photo closes the sheet and opens the picker`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = collect(viewModel)
        viewModel.onScanLabelClicked()

        viewModel.onChoosePhotoChosen()

        assertEquals(listOf(MedicationFormEffect.PickPhoto), effects)
        assertFalse(viewModel.uiState.value.showScanOptions)
    }

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        cameraAvailable: Boolean = true,
    ) = MedicationFormViewModel(
        repository = repository,
        savedStateHandle = savedStateHandle,
        amountParser = AmountParser(Locale.UK),
        clock = clock,
        photoScanner = scanner,
        cameraAvailable = cameraAvailable,
    )

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: MedicationFormViewModel): List<MedicationFormEffect> {
        val effects = mutableListOf<MedicationFormEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        return effects
    }

    private fun InMemoryMedicationRepository.observeAllNow() = runBlocking { observeAll().first() }

    /** Answers with [result], throws [failure], or hangs until cancelled. */
    private class FakePhotoScanner : PhotoScanner {
        var result: LabelInterpretation? = null
        var failure: RuntimeException? = null
        var hang = false
        var stopped = false
        var lastUri: String? = null

        override suspend fun scan(uri: String): LabelInterpretation? {
            lastUri = uri
            if (hang) awaitCancellation()
            failure?.let { throw it }
            return result
        }

        override fun stop() {
            stopped = true
        }
    }
}
