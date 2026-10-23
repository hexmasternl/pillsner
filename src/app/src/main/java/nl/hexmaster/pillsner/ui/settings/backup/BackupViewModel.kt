package nl.hexmaster.pillsner.ui.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.hexmaster.pillsner.data.backup.BackupDocuments
import nl.hexmaster.pillsner.data.backup.BackupFileCodec
import nl.hexmaster.pillsner.data.backup.BackupReadException
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import nl.hexmaster.pillsner.domain.backup.ExportBackup
import nl.hexmaster.pillsner.domain.backup.ImportBackup

/**
 * Export and import, step by step (`data-backup-restore`).
 *
 * Export: passphrase twice, then the create-document picker, then write. Import: the open-document
 * picker, a header check that needs no passphrase, the passphrase, the decrypt, a confirmation with
 * a tick, and only then the replace. Each step can be backed out of, and backing out changes
 * nothing on the device.
 *
 * Scoped to the activity rather than to Settings: the picker sends the app to the background, which
 * relocks it and takes Settings off screen, and the result still has to arrive somewhere. A picked
 * document is then held, untouched, until [appOpen] says the user has unlocked again: nothing is
 * read from the database or the document behind the lock screen. Whatever the user was in the
 * middle of carries on from there.
 *
 * The passphrase and the decrypted snapshot are held only in memory, only between the steps that
 * need them, and dropped the moment the flow ends either way.
 *
 * @param appOpen true while the app's content may be shown: unlocked, or no lock set.
 * @param cryptoDispatcher where PBKDF2 and AES run: hundreds of milliseconds of CPU, never on main.
 */
class BackupViewModel(
    private val exportBackup: ExportBackup,
    private val importBackup: ImportBackup,
    private val codec: BackupFileCodec,
    private val documents: BackupDocuments,
    private val clock: Clock,
    private val appOpen: Flow<Boolean>,
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    // Channels, as for the reset: a picker must open once, and a snackbar must not come back on a
    // configuration change as though a second export had happened.
    private val _pickerRequests = Channel<BackupPickerRequest>(Channel.BUFFERED)
    val pickerRequests: Flow<BackupPickerRequest> = _pickerRequests.receiveAsFlow()

    private val _messages = Channel<BackupMessage>(Channel.BUFFERED)
    val messages: Flow<BackupMessage> = _messages.receiveAsFlow()

    private var exportPassphrase: CharArray? = null
    private var importFile: ByteArray? = null
    private var importSnapshot: BackupSnapshot? = null

    // --- Export -------------------------------------------------------------------------------

    fun onExportTapped() {
        if (!_uiState.value.actionsEnabled) return
        _uiState.update { it.copy(dialog = BackupDialog.ExportPassphrase()) }
    }

    fun onExportPassphraseChange(value: String) = updateExportDialog { it.copy(passphrase = value) }

    fun onExportConfirmationChange(value: String) = updateExportDialog { it.copy(confirmation = value) }

    /** Opens the picker, and only when the two entries match; the passphrase waits here meanwhile. */
    fun onExportPassphraseConfirmed() {
        val dialog = _uiState.value.dialog as? BackupDialog.ExportPassphrase ?: return
        if (!dialog.canProceed) return
        exportPassphrase = dialog.passphrase.toCharArray()
        _uiState.update { it.copy(dialog = null, pickerOpen = true) }
        _pickerRequests.trySend(BackupPickerRequest.CreateDocument(suggestedName()))
    }

    /** @param location the picked document, or null when the user cancelled the picker. */
    fun onExportDocumentChosen(location: String?) {
        val passphrase = exportPassphrase
        exportPassphrase = null
        if (location == null) {
            _uiState.update { it.copy(pickerOpen = false) }
            passphrase?.fill(' ')
            _messages.trySend(BackupMessage.EXPORT_CANCELLED)
            return
        }

        viewModelScope.launch {
            awaitAppOpen()
            if (passphrase == null) {
                // The process died while the picker was open and took the passphrase with it. The
                // picked document is left as it is: nothing here can tell a placeholder the picker
                // just made from a file the user already had.
                _messages.send(BackupMessage.EXPORT_FAILED)
                return@launch
            }
            _uiState.update { it.copy(work = BackupWork.EXPORTING) }
            try {
                // The whole file exists in memory before the document is opened, so an encoding
                // failure never touches it, and the replace rolls a failed write back itself.
                val bytes = withContext(cryptoDispatcher) { codec.encode(exportBackup(), passphrase) }
                documents.replace(location, bytes)
                _messages.send(BackupMessage.EXPORT_DONE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Never the exception's text: it is not for the user, and nothing about the data
                // should reach a log either.
                _messages.send(BackupMessage.EXPORT_FAILED)
            } finally {
                passphrase.fill(' ')
                _uiState.update { it.copy(work = null) }
            }
        }
    }

    // --- Import -------------------------------------------------------------------------------

    fun onImportTapped() {
        if (!_uiState.value.actionsEnabled) return
        _uiState.update { it.copy(pickerOpen = true) }
        _pickerRequests.trySend(BackupPickerRequest.OpenDocument)
    }

    /**
     * Reads the picked file and checks its header, before asking for anything. A file that is not a
     * backup, or one from a newer Pillsner, is refused without the user typing a passphrase for it.
     *
     * @param location the picked document, or null when the user cancelled the picker.
     */
    fun onImportDocumentChosen(location: String?) {
        if (location == null) {
            _uiState.update { it.copy(pickerOpen = false) }
            _messages.trySend(BackupMessage.IMPORT_CANCELLED)
            return
        }
        viewModelScope.launch {
            awaitAppOpen()
            _uiState.update { it.copy(work = BackupWork.READING) }
            try {
                val bytes = documents.read(location)
                codec.inspect(bytes)
                importFile = bytes
                _uiState.update { it.copy(dialog = BackupDialog.ImportPassphrase()) }
            } catch (error: BackupReadException) {
                _messages.send(error.toMessage())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _messages.send(BackupMessage.READ_FAILED)
            } finally {
                _uiState.update { it.copy(work = null) }
            }
        }
    }

    fun onImportPassphraseChange(value: String) {
        _uiState.update { state ->
            val dialog = state.dialog as? BackupDialog.ImportPassphrase ?: return@update state
            state.copy(dialog = dialog.copy(passphrase = value))
        }
    }

    /**
     * Decrypts. A wrong passphrase keeps the dialog open, emptied and marked, so the user can try
     * again; anything else ends the flow with its message. Nothing on the device changes here.
     */
    fun onImportPassphraseConfirmed() {
        val dialog = _uiState.value.dialog as? BackupDialog.ImportPassphrase ?: return
        val file = importFile ?: return
        if (!dialog.canProceed || _uiState.value.work != null) return
        val passphrase = dialog.passphrase.toCharArray()

        viewModelScope.launch {
            _uiState.update { it.copy(work = BackupWork.READING) }
            try {
                importSnapshot = withContext(cryptoDispatcher) { codec.decode(file, passphrase) }
                importFile = null
                _uiState.update { it.copy(dialog = BackupDialog.ConfirmImport()) }
            } catch (_: BackupReadException.CannotDecrypt) {
                _uiState.update { it.copy(dialog = BackupDialog.ImportPassphrase(rejected = true)) }
            } catch (error: BackupReadException) {
                endImport()
                _messages.send(error.toMessage())
            } finally {
                passphrase.fill(' ')
                _uiState.update { it.copy(work = null) }
            }
        }
    }

    fun onImportConfirmationToggled(accepted: Boolean) {
        _uiState.update { state ->
            if (state.dialog !is BackupDialog.ConfirmImport) return@update state
            state.copy(dialog = BackupDialog.ConfirmImport(accepted))
        }
    }

    /** The replace itself: only with the box ticked, and only once however often it is tapped. */
    fun onImportConfirmed() {
        val dialog = _uiState.value.dialog as? BackupDialog.ConfirmImport ?: return
        val snapshot = importSnapshot ?: return
        if (!dialog.accepted || _uiState.value.work != null) return
        importSnapshot = null

        viewModelScope.launch {
            _uiState.value = BackupUiState(work = BackupWork.IMPORTING)
            try {
                importBackup(snapshot)
                _messages.send(BackupMessage.IMPORT_DONE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The replace is one transaction, so a failure here left the data as it was.
                _messages.send(BackupMessage.IMPORT_FAILED)
            } finally {
                _uiState.value = BackupUiState()
            }
        }
    }

    // --- Either -------------------------------------------------------------------------------

    /** Cancel, system back and a tap outside, for any of the dialogs. Nothing has changed yet. */
    fun onDialogDismissed() {
        val dialog = _uiState.value.dialog ?: return
        if (_uiState.value.work != null) return
        endImport()
        _uiState.update { it.copy(dialog = null) }
        if (dialog !is BackupDialog.ExportPassphrase) _messages.trySend(BackupMessage.IMPORT_CANCELLED)
    }

    override fun onCleared() {
        exportPassphrase?.fill(' ')
        endImport()
    }

    /**
     * Suspends until the app is unlocked, then lets the picked document go on to be used. The
     * picker counts as open until then, so nothing else can start in between.
     */
    private suspend fun awaitAppOpen() {
        appOpen.first { it }
        _uiState.update { it.copy(pickerOpen = false) }
    }

    private fun endImport() {
        importFile = null
        importSnapshot = null
    }

    private fun updateExportDialog(change: (BackupDialog.ExportPassphrase) -> BackupDialog.ExportPassphrase) {
        _uiState.update { state ->
            val dialog = state.dialog as? BackupDialog.ExportPassphrase ?: return@update state
            state.copy(dialog = change(dialog))
        }
    }

    private fun suggestedName() = "pillsner-backup-${LocalDate.now(clock)}.pill"

    private fun BackupReadException.toMessage(): BackupMessage = when (this) {
        is BackupReadException.NewerVersion -> BackupMessage.NEWER_VERSION
        is BackupReadException.NotABackup, is BackupReadException.CannotDecrypt -> BackupMessage.NOT_A_BACKUP
    }
}
