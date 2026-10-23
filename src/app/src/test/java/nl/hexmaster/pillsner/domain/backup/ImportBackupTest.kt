package nl.hexmaster.pillsner.domain.backup

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Spec: data-backup-restore, what an import does and in what order. */
class ImportBackupTest {

    private val steps = mutableListOf<String>()
    private var replacedWith: BackupSnapshot? = null

    private val store = object : BackupDataStore {
        override suspend fun readAll() = error("An import never reads")
        override suspend fun replaceAll(snapshot: BackupSnapshot) {
            steps += "replace"
            replacedWith = snapshot
        }
    }

    private val importBackup = ImportBackup(
        store = store,
        teardown = { steps += "teardown" },
        history = { steps += "forget" },
        refresh = { steps += "refresh" },
    )

    @Test
    fun `an import replaces, takes the notifications down, forgets what they saw, then re-arms`() = runBlocking {
        importBackup(BackupFixtures.populated())

        // The same order as a reset: the all-or-nothing step first, the corrections after it.
        assertEquals(listOf("replace", "teardown", "forget", "refresh"), steps)
        assertEquals(BackupFixtures.populated(), replacedWith)
    }

    @Test
    fun `an empty backup still replaces, leaving the app empty`() = runBlocking {
        importBackup(BackupSnapshot.EMPTY)

        assertEquals(BackupSnapshot.EMPTY, replacedWith)
        assertEquals(listOf("replace", "teardown", "forget", "refresh"), steps)
    }

    @Test
    fun `a replace that fails touches nothing else`() {
        val failing = ImportBackup(
            store = object : BackupDataStore {
                override suspend fun readAll() = error("unused")
                override suspend fun replaceAll(snapshot: BackupSnapshot) = error("the transaction rolled back")
            },
            teardown = { steps += "teardown" },
            history = { steps += "forget" },
            refresh = { steps += "refresh" },
        )

        assertThrows(IllegalStateException::class.java) { runBlocking { failing(BackupFixtures.populated()) } }

        // The data is as it was, so the reminders and the evidence about it must be too.
        assertEquals(emptyList<String>(), steps)
    }
}

/** Spec: data-backup-restore, a snapshot that cannot be restored whole is refused whole. */
class BackupSnapshotTest {

    private val now = Instant.parse("2026-09-14T06:00:00Z")

    @Test
    fun `a dose pointing at a medication the backup lacks is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupSnapshot(emptyList(), emptyList(), listOf(BackupFixtures.dose("missing", now, null)))
        }
    }

    @Test
    fun `a stock batch pointing at a medication the backup lacks is refused`() {
        val populated = BackupFixtures.populated()
        assertThrows(IllegalArgumentException::class.java) {
            populated.copy(stockBatches = populated.stockBatches + populated.stockBatches.first().copy(medicationBackupId = "z"))
        }
    }

    @Test
    fun `two medications sharing an id are refused`() {
        val populated = BackupFixtures.populated()
        assertThrows(IllegalArgumentException::class.java) {
            populated.copy(medications = populated.medications + populated.medications.first().copy(name = "Twin"))
        }
    }
}
