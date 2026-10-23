package nl.hexmaster.pillsner.data.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream
import java.io.IOException
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

    /** Replaces the contents of the document at [location] with [bytes]. */
    suspend fun write(location: String, bytes: ByteArray)

    /** The whole document at [location]. */
    suspend fun read(location: String): ByteArray

    /** Removes a document this app created and could not finish; best effort, never throws. */
    suspend fun discard(location: String)
}

/** The wired [BackupDocuments], over the content resolver. */
class ContentResolverBackupDocuments(private val resolver: ContentResolver) : BackupDocuments {

    override suspend fun write(location: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        // "wt" truncates, so writing over an older backup the user picked never leaves its tail.
        val output = resolver.openOutputStream(Uri.parse(location), "wt") ?: throw IOException("No output stream")
        output.use { it.write(bytes) }
    }

    override suspend fun read(location: String): ByteArray = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(Uri.parse(location)) ?: throw IOException("No input stream")
        input.use { stream ->
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

    override suspend fun discard(location: String) {
        withContext(Dispatchers.IO) {
            runCatching { DocumentsContract.deleteDocument(resolver, Uri.parse(location)) }
        }
    }

    private companion object {
        const val BUFFER_BYTES = 8 * 1024
    }
}
