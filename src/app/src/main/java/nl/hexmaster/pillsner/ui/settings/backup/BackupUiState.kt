package nl.hexmaster.pillsner.ui.settings.backup

/**
 * What the backup section and its dialogs are showing (`data-backup-restore`).
 *
 * @property dialog the dialog in front of the user, or null.
 * @property work what is running in the background, or null. Both actions are disabled while
 *   anything runs, so a double tap starts one export, not two.
 */
data class BackupUiState(
    val dialog: BackupDialog? = null,
    val work: BackupWork? = null,
) {
    val actionsEnabled: Boolean get() = work == null && dialog == null
}

/** The long-running steps, each shown as indeterminate progress with its own label. */
enum class BackupWork { EXPORTING, READING, IMPORTING }

sealed interface BackupDialog {

    /**
     * Choosing the export passphrase. Typed twice, because there is no recovering a backup whose
     * passphrase was mistyped once.
     */
    data class ExportPassphrase(val passphrase: String = "", val confirmation: String = "") : BackupDialog {
        /** Shown as soon as the confirmation can no longer become the passphrase, not on every keystroke. */
        val mismatch: Boolean get() = confirmation.isNotEmpty() && !passphrase.startsWith(confirmation)
        val canProceed: Boolean get() = passphrase.isNotEmpty() && passphrase == confirmation
    }

    /**
     * The passphrase for the picked file.
     *
     * @property rejected whether the last attempt did not open it, which stays on screen so the
     *   user can try again without picking the file again.
     */
    data class ImportPassphrase(val passphrase: String = "", val rejected: Boolean = false) : BackupDialog {
        val canProceed: Boolean get() = passphrase.isNotEmpty()
    }

    /** The last gate before the replace, with a tick, as the reset has. */
    data class ConfirmImport(val accepted: Boolean = false) : BackupDialog
}

/** Asks the root of the app to open a system picker; see `PillsnerApp` for why it lives there. */
sealed interface BackupPickerRequest {
    data class CreateDocument(val suggestedName: String) : BackupPickerRequest
    data object OpenDocument : BackupPickerRequest
}

/** A one-shot message for the Settings snackbar; never part of the state, so never replayed. */
enum class BackupMessage {
    EXPORT_DONE,
    EXPORT_CANCELLED,
    EXPORT_FAILED,
    IMPORT_DONE,
    IMPORT_CANCELLED,
    IMPORT_FAILED,
    NOT_A_BACKUP,
    NEWER_VERSION,
    READ_FAILED,
}
