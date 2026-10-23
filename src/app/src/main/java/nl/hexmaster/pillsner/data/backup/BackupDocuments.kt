package nl.hexmaster.pillsner.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes the one document the user picked (`data-backup-restore`).
 *
 * A location is the `content://` uri the Storage Access Framework handed back, as a string, so the
 * view model never holds an Android type. The app gets access to exactly that document for as long
 * as the picker's grant lasts, and asks for no storage permission of any kind.
 */
interface BackupDocuments {

    /**
     * Makes [bytes] the contents of the document at [location], or leaves it as it was.
     *
     * The create-document picker can hand back an existing document the user chose to overwrite,
     * such as their previous backup, and the Storage Access Framework has no atomic replace: a
     * write truncates first. So a failure partway must not cost the user what was there. See
     * [replaceKeepingPrevious] for exactly what happens on failure.
     *
     * @throws Exception when the new contents could not be written, after the rollback.
     */
    suspend fun replace(location: String, bytes: ByteArray)

    /** The whole document at [location]. */
    suspend fun read(location: String): ByteArray
}

/**
 * The rollback around a non-atomic overwrite, kept free of Android types so it can be tested.
 *
 * [readPrevious] runs first and is held in memory until the new contents are safely written. When
 * it fails, nothing is written at all and its failure is thrown: a write truncates first, and
 * contents that could not be read could not be put back. If [write] then fails, a document that
 * held something gets those contents written back.
 *
 * Nothing is ever deleted: the picker does not say whether it created the document or handed back
 * an existing one, and an empty document could be either, so the only safe rule is that a picked
 * document stays where the user put it. The cost is an empty file left behind by a failed export,
 * which the user named and was told about.
 *
 * The rollback is best effort and never hides the original failure, which is rethrown.
 *
 * @param readPrevious the current contents; throws when they cannot be read.
 */
internal suspend fun replaceKeepingPrevious(
    bytes: ByteArray,
    readPrevious: suspend () -> ByteArray,
    write: suspend (ByteArray) -> Unit,
) {
    val previous = readPrevious()
    try {
        write(bytes)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        try {
            if (previous.isNotEmpty()) write(previous)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The original failure is what the caller reports; a failed rollback adds nothing it can act on.
        }
        throw failure
    }
}

/** The wired [BackupDocuments], over the content resolver. */
class ContentResolverBackupDocuments(private val resolver: ContentResolver) : BackupDocuments {

    override suspend fun replace(location: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val uri = Uri.parse(location)
        replaceKeepingPrevious(
            bytes = bytes,
            readPrevious = { readBytes(uri) },
            write = { contents -> writeBytes(uri, contents) },
        )
    }

    override suspend fun read(location: String): ByteArray = withContext(Dispatchers.IO) {
        readBytes(Uri.parse(location))
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        // "wt" truncates, so the new contents never keep a longer old file's tail.
        val output = resolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
        output.use { it.write(bytes) }
    }

    private fun readBytes(uri: Uri): ByteArray {
        val input = resolver.openInputStream(uri) ?: throw IOException("No input stream")
        return input.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                // A real backup is compressed and far below this; anything larger is not one, and
                // reading on would only fill the heap.
                if (out.size() > BackupFileCodec.MAX_PAYLOAD_BYTES) throw BackupReadException.NotABackup("File too large")
            }
            out.toByteArray()
        }
    }

    private companion object {
        const val BUFFER_BYTES = 8 * 1024
    }
}
