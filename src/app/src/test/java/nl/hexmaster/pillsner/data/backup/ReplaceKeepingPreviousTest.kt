package nl.hexmaster.pillsner.data.backup

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Spec: data-backup-restore. Overwriting a picked document never costs the user what was there,
 * because the Storage Access Framework truncates before it writes and has no atomic replace, and a
 * picked document is never deleted, because nothing says whether the export created it.
 */
class ReplaceKeepingPreviousTest {

    private val newBackup = byteArrayOf(9, 9, 9)
    private val oldBackup = byteArrayOf(1, 2, 3)
    private val failure = IOException("disk full")
    private val unreadable = IOException("permission denied")

    /** A document as the platform behaves: a write truncates first, then may fail. */
    private inner class Document(var contents: ByteArray, private var failingWrites: Int = 0) {
        val writes = mutableListOf<ByteArray>()

        suspend fun replace(readable: Boolean = true) = replaceKeepingPrevious(
            bytes = newBackup,
            readPrevious = { if (readable) contents else throw unreadable },
            write = { bytes ->
                writes += bytes
                contents = ByteArray(0)
                if (failingWrites > 0) {
                    failingWrites--
                    throw failure
                }
                contents = bytes
            },
        )
    }

    @Test
    fun `a write that succeeds replaces the contents`() = runBlocking {
        val document = Document(oldBackup)

        document.replace()

        assertArrayEquals(newBackup, document.contents)
    }

    @Test
    fun `a failed write over an earlier backup puts it back, and still reports the failure`() {
        val document = Document(oldBackup, failingWrites = 1)

        val thrown = assertThrows(IOException::class.java) { runBlocking { document.replace() } }

        assertSame("The caller hears about the original failure", failure, thrown)
        assertArrayEquals("The old backup is back, byte for byte", oldBackup, document.contents)
    }

    @Test
    fun `a failed write into an empty document leaves it as an empty document`() {
        // The picker's fresh placeholder and a zero-byte file the user already had look the same.
        val document = Document(ByteArray(0), failingWrites = 1)

        assertThrows(IOException::class.java) { runBlocking { document.replace() } }

        // Left in place by construction: the rollback has no way to delete a document at all.
        assertEquals("Nothing is written back, since there was nothing", 1, document.writes.size)
    }

    @Test
    fun `a document whose contents cannot be read is not written to at all`() {
        // A write would truncate it, and what was there could never be put back.
        val document = Document(oldBackup)

        val thrown = assertThrows(IOException::class.java) { runBlocking { document.replace(readable = false) } }

        assertSame("The read failure is what the caller hears", unreadable, thrown)
        assertEquals("Not one write was attempted", 0, document.writes.size)
        assertArrayEquals("The document is exactly as it was", oldBackup, document.contents)
    }

    @Test
    fun `a rollback that fails too still reports the original failure`() {
        val document = Document(oldBackup, failingWrites = 2)

        val thrown = assertThrows(IOException::class.java) { runBlocking { document.replace() } }

        assertSame(failure, thrown)
        assertEquals("The restore was attempted", 2, document.writes.size)
    }
}
