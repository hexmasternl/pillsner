package nl.hexmaster.pillsner.domain.backup

import nl.hexmaster.pillsner.domain.reset.ReminderHistoryReset
import nl.hexmaster.pillsner.domain.reset.ReminderRefresh
import nl.hexmaster.pillsner.domain.reset.ReminderTeardown

/**
 * Restores a backup: replace the whole dataset, then settle what hung off the old one
 * (`data-backup-restore`).
 *
 * The same order as [nl.hexmaster.pillsner.domain.reset.EraseAllData], and for the same reason. The
 * replace goes first and is all-or-nothing, so a failure there throws before anything else is
 * touched and the user still has exactly what they had. Only once it has landed are the reminders
 * on screen taken down, the evidence about the old doses forgotten, and the alarms and the watch
 * brought in line with the new data, through the same ports a reset uses.
 *
 * The caller has already decrypted the file and had the user confirm; this does no checking of its
 * own beyond what [BackupSnapshot] and the store enforce.
 */
class ImportBackup(
    private val store: BackupDataStore,
    private val teardown: ReminderTeardown,
    private val history: ReminderHistoryReset,
    private val refresh: ReminderRefresh,
) {

    suspend operator fun invoke(snapshot: BackupSnapshot) {
        store.replaceAll(snapshot)
        teardown.cancelAll()
        history.forget()
        refresh.refresh()
    }
}
