package nl.hexmaster.pillsner.data.backup

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec: data-backup-restore. Overwriting a picked document never costs the user what was there,
 * because the Storage Access Framework truncates before it writes and has no atomic replace.
 */
class ReplaceKeepingPreviousTest {

    private val newBackup = byteArrayOf(9, 9, 9)
    private val oldBackup = byteArrayOf(1, 2, 3)

    /** A document as the platform behaves: a write truncates first, then may fail. */
    private inner class Document(var contents: ByteArray?, private var failingWrites: Int = 0) {
        var deleted = false
        val writes = mutableListOf<ByteArray>()

        suspend fun replace(readable: Boolean = true) = replaceKeepingPrevious(
            bytes = newBackup,
            readPrevious = { if (readable) contents else null },
            write = { bytes ->
                writes += bytes
                contents = ByteArray(0)
                if (failingWrites > 0) {
                    failingWrites--
                    throw failure
                }
                contents = bytes
            },
            delete = {
                deleted = true
                contents = null
            },
        )
    }

    private val failure = IOException("disk full")

    @Test
    fun `a write that succeeds replaces the contents`() = runBlocking {
        val document = Document(oldBackup)

        document.replace()

        assertArrayEquals(newBackup, document.contents)
        assertFalse(document.deleted)
    }

    @Test
    fun `a failed write over an earlier backup puts it back, and still reports the failure`() {
        val document = Document(oldBackup, failingWrites = 1)

        val thrown = assertThrows(IOException::class.java) { runBlocking { document.replace() } }

        assertSame("The caller hears about the original failure", failure, thrown)
        assertArrayEquals("The old backup is back, byte for byte", oldBackup, document.contents)
        assertFalse(document.deleted)
    }

    @Test
    fun `a failed write into the picker's fresh empty document removes it`() {
        val document = Document(ByteArray(0), failingWrites = 1)

        assertThrows(IOException::class.java) { runBlocking { document.replace() } }

        assertTrue("Nothing of the user's was there, so nothing is left behind", document.deleted)
    }

    @Test
    fun `a document whose contents could not be read is never deleted`() {
        val document = Document(oldBackup, failingWrites = 1)

        assertThrows(IOException::class.java) { runBlocking { document.replace(readable = false) } }

        assertFalse("Not shown to be this export's own, so not ours to delete", document.deleted)
        assertEquals("And nothing is written back that was never read", 1, document.writes.size)
    }

    @Test
    fun `a rollback that fails too still reports the original failure`() {
        val document = Document(oldBackup, failingWrites = 2)

        val thrown = assertThrows(IOException::class.java) { runBlocking { document.replace() } }

        assertSame(failure, thrown)
        assertEquals("The restore was attempted", 2, document.writes.size)
        assertFalse("Nothing deleted a document that held something", document.deleted)
    }
}
