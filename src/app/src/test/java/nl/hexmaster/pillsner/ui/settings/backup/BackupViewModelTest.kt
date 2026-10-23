package nl.hexmaster.pillsner.ui.settings.backup

import java.io.IOException
import java.nio.ByteBuffer
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.data.backup.BackupDocuments
import nl.hexmaster.pillsner.data.backup.BackupFileCodec
import nl.hexmaster.pillsner.domain.backup.BackupDataStore
import nl.hexmaster.pillsner.domain.backup.BackupFixtures
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import nl.hexmaster.pillsner.domain.backup.ExportBackup
import nl.hexmaster.pillsner.domain.backup.ImportBackup
import nl.hexmaster.pillsner.domain.backup.StoredDataset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec: data-backup-restore, the export and import flows and every way out of them. */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC)
    private val codec = BackupFileCodec(iterations = 100_000)

    /** The device's data, before and after; starts with something so a replace is visible. */
    private var onDevice: BackupSnapshot = BackupFixtures.populated()
    private var replaces = 0
    private var failReplace = false
    private val reconciled = mutableListOf<String>()

    private val store = object : BackupDataStore {
        override suspend fun readAll() = StoredDataset(emptyList(), emptyList(), emptyList())
        override suspend fun replaceAll(snapshot: BackupSnapshot) {
            if (failReplace) error("rolled back")
            replaces++
            onDevice = snapshot
        }
    }

    private val documents = object : BackupDocuments {
        val files = mutableMapOf<String, ByteArray>()
        val discarded = mutableListOf<String>()
        var failWrites = false

        override suspend fun write(location: String, bytes: ByteArray) {
            if (failWrites) throw IOException("disk full")
            files[location] = bytes
        }

        override suspend fun read(location: String) = files[location] ?: throw IOException("gone")

        override suspend fun discard(location: String) {
            discarded += location
            files.remove(location)
        }
    }

    private lateinit var viewModel: BackupViewModel
    private val messages = mutableListOf<BackupMessage>()
    private val pickers = mutableListOf<BackupPickerRequest>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = BackupViewModel(
            exportBackup = ExportBackup(store),
            importBackup = ImportBackup(
                store = store,
                teardown = { reconciled += "teardown" },
                history = { reconciled += "forget" },
                refresh = { reconciled += "refresh" },
            ),
            codec = codec,
            documents = documents,
            clock = clock,
            cryptoDispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect { messages += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.pickerRequests.collect { pickers += it } }
    }

    private fun enterExportPassphrase(passphrase: String, confirmation: String = passphrase) {
        viewModel.onExportTapped()
        viewModel.onExportPassphraseChange(passphrase)
        viewModel.onExportConfirmationChange(confirmation)
    }

    // --- Export -------------------------------------------------------------------------------

    @Test
    fun `an empty passphrase opens no picker`() = runTest(dispatcher) {
        collect()
        enterExportPassphrase("")

        viewModel.onExportPassphraseConfirmed()
        advanceUntilIdle()

        assertTrue(pickers.isEmpty())
        assertTrue(viewModel.uiState.value.dialog is BackupDialog.ExportPassphrase)
    }

    @Test
    fun `a mismatched confirmation shows a mismatch and opens no picker`() = runTest(dispatcher) {
        collect()
        enterExportPassphrase("secret", "secrex")

        viewModel.onExportPassphraseConfirmed()
        advanceUntilIdle()

        val dialog = viewModel.uiState.value.dialog as BackupDialog.ExportPassphrase
        assertTrue(dialog.mismatch)
        assertFalse(dialog.canProceed)
        assertTrue(pickers.isEmpty())
    }

    @Test
    fun `a confirmation still being typed is not yet a mismatch`() {
        assertFalse(BackupDialog.ExportPassphrase("secret", "sec").mismatch)
        assertFalse(BackupDialog.ExportPassphrase("secret", "").mismatch)
    }

    @Test
    fun `matching passphrases open the create-document picker with a dated name`() = runTest(dispatcher) {
        collect()
        enterExportPassphrase("secret")

        viewModel.onExportPassphraseConfirmed()
        advanceUntilIdle()

        assertEquals(listOf(BackupPickerRequest.CreateDocument("pillsner-backup-2026-10-23.pill")), pickers)
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `cancelling the picker writes nothing`() = runTest(dispatcher) {
        collect()
        enterExportPassphrase("secret")
        viewModel.onExportPassphraseConfirmed()

        viewModel.onExportDocumentChosen(null)
        advanceUntilIdle()

        assertTrue(documents.files.isEmpty())
        assertEquals(listOf(BackupMessage.EXPORT_CANCELLED), messages)
    }

    @Test
    fun `a chosen document receives a backup that opens with the passphrase`() = runTest(dispatcher) {
        collect()
        enterExportPassphrase("secret")
        viewModel.onExportPassphraseConfirmed()

        viewModel.onExportDocumentChosen("content://doc/1")
        advanceUntilIdle()

        val written = documents.files.getValue("content://doc/1")
        assertEquals(BackupSnapshot.EMPTY, codec.decode(written, "secret".toCharArray()))
        assertEquals(listOf(BackupMessage.EXPORT_DONE), messages)
        assertNull("Progress is cleared", viewModel.uiState.value.work)
    }

    @Test
    fun `a write that fails removes the half-made file and says so`() = runTest(dispatcher) {
        collect()
        documents.failWrites = true
        enterExportPassphrase("secret")
        viewModel.onExportPassphraseConfirmed()

        viewModel.onExportDocumentChosen("content://doc/1")
        advanceUntilIdle()

        assertEquals(listOf("content://doc/1"), documents.discarded)
        assertEquals(listOf(BackupMessage.EXPORT_FAILED), messages)
    }

    @Test
    fun `a picker result with no passphrase waiting discards the empty file`() = runTest(dispatcher) {
        // What a process death while the picker was open looks like to the new view model.
        collect()

        viewModel.onExportDocumentChosen("content://doc/1")
        advanceUntilIdle()

        assertEquals(listOf("content://doc/1"), documents.discarded)
        assertEquals(listOf(BackupMessage.EXPORT_FAILED), messages)
    }

    // --- Import -------------------------------------------------------------------------------

    private fun backupFile(snapshot: BackupSnapshot = BackupSnapshot.EMPTY, passphrase: String = "secret"): String {
        documents.files["content://backup"] = codec.encode(snapshot, passphrase.toCharArray())
        return "content://backup"
    }

    private fun TestScope.pickAndUnlock(passphrase: String) {
        viewModel.onImportDocumentChosen("content://backup")
        advanceUntilIdle()
        viewModel.onImportPassphraseChange(passphrase)
        viewModel.onImportPassphraseConfirmed()
        advanceUntilIdle()
    }

    @Test
    fun `import starts with the open-document picker`() = runTest(dispatcher) {
        collect()

        viewModel.onImportTapped()
        advanceUntilIdle()

        assertEquals(listOf(BackupPickerRequest.OpenDocument), pickers)
    }

    @Test
    fun `cancelling the open picker changes nothing`() = runTest(dispatcher) {
        collect()
        val before = onDevice

        viewModel.onImportDocumentChosen(null)
        advanceUntilIdle()

        assertEquals(listOf(BackupMessage.IMPORT_CANCELLED), messages)
        assertEquals(before, onDevice)
    }

    @Test
    fun `a file that is not a backup is refused before any passphrase is asked for`() = runTest(dispatcher) {
        collect()
        documents.files["content://backup"] = "hello".toByteArray()

        viewModel.onImportDocumentChosen("content://backup")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialog)
        assertEquals(listOf(BackupMessage.NOT_A_BACKUP), messages)
    }

    @Test
    fun `a newer format is refused by name, before any passphrase is asked for`() = runTest(dispatcher) {
        collect()
        backupFile()
        ByteBuffer.wrap(documents.files.getValue("content://backup")).putInt(4, 9)

        viewModel.onImportDocumentChosen("content://backup")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialog)
        assertEquals(listOf(BackupMessage.NEWER_VERSION), messages)
    }

    @Test
    fun `a file that cannot be read says so`() = runTest(dispatcher) {
        collect()

        viewModel.onImportDocumentChosen("content://missing")
        advanceUntilIdle()

        assertEquals(listOf(BackupMessage.READ_FAILED), messages)
    }

    @Test
    fun `the wrong passphrase keeps the dialog open, marked, and changes nothing`() = runTest(dispatcher) {
        collect()
        backupFile()
        val before = onDevice

        pickAndUnlock("wrong")

        assertEquals(BackupDialog.ImportPassphrase(passphrase = "", rejected = true), viewModel.uiState.value.dialog)
        assertEquals(before, onDevice)
        assertTrue(messages.isEmpty())

        // And the same file can be tried again without picking it again.
        viewModel.onImportPassphraseChange("secret")
        viewModel.onImportPassphraseConfirmed()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.dialog is BackupDialog.ConfirmImport)
    }

    @Test
    fun `the right passphrase leads to the confirmation, still changing nothing`() = runTest(dispatcher) {
        collect()
        backupFile()
        val before = onDevice

        pickAndUnlock("secret")

        assertEquals(BackupDialog.ConfirmImport(accepted = false), viewModel.uiState.value.dialog)
        assertEquals(before, onDevice)
    }

    @Test
    fun `confirming without the tick replaces nothing`() = runTest(dispatcher) {
        collect()
        backupFile()
        pickAndUnlock("secret")

        viewModel.onImportConfirmed()
        advanceUntilIdle()

        assertEquals(0, replaces)
    }

    @Test
    fun `cancelling the confirmation replaces nothing`() = runTest(dispatcher) {
        collect()
        backupFile()
        pickAndUnlock("secret")
        viewModel.onImportConfirmationToggled(true)

        viewModel.onDialogDismissed()
        advanceUntilIdle()

        assertEquals(0, replaces)
        assertNull(viewModel.uiState.value.dialog)
        assertEquals(listOf(BackupMessage.IMPORT_CANCELLED), messages)
    }

    @Test
    fun `a confirmed import replaces everything once and reconciles reminders and the watch`() = runTest(dispatcher) {
        collect()
        backupFile(BackupSnapshot.EMPTY)
        pickAndUnlock("secret")
        viewModel.onImportConfirmationToggled(true)

        viewModel.onImportConfirmed()
        viewModel.onImportConfirmed()
        advanceUntilIdle()

        assertEquals("A double tap imports once", 1, replaces)
        assertEquals(BackupSnapshot.EMPTY, onDevice)
        assertEquals(listOf("teardown", "forget", "refresh"), reconciled)
        assertEquals(listOf(BackupMessage.IMPORT_DONE), messages)
        assertEquals(BackupUiState(), viewModel.uiState.value)
    }

    @Test
    fun `a replace that fails reports it and reconciles nothing`() = runTest(dispatcher) {
        collect()
        failReplace = true
        backupFile()
        pickAndUnlock("secret")
        viewModel.onImportConfirmationToggled(true)

        viewModel.onImportConfirmed()
        advanceUntilIdle()

        assertEquals(listOf(BackupMessage.IMPORT_FAILED), messages)
        assertTrue(reconciled.isEmpty())
    }

    @Test
    fun `nothing starts while a dialog is open`() = runTest(dispatcher) {
        collect()
        viewModel.onExportTapped()

        viewModel.onImportTapped()
        advanceUntilIdle()

        assertTrue(pickers.isEmpty())
    }
}
