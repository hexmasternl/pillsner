package nl.hexmaster.pillsner.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec: data-backup-restore. The wired document access, through the real content resolver.
 *
 * A `file://` uri goes through the same `openInputStream` and `openOutputStream(uri, "wt")` calls
 * a picked `content://` document does; what cannot be staged here is a provider failing mid-write,
 * which `ReplaceKeepingPreviousTest` covers on the JVM.
 */
@RunWith(AndroidJUnit4::class)
class ContentResolverBackupDocumentsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val documents = ContentResolverBackupDocuments(context.contentResolver)
    private val file = File(context.cacheDir, "documents-test.pill")
    private val location = Uri.fromFile(file).toString()

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun replacingALongerDocument_leavesExactlyTheNewContents() = runBlocking {
        file.writeBytes(ByteArray(64) { 7 })

        documents.replace(location, byteArrayOf(1, 2, 3))

        // "wt" truncates, so no tail of the longer old file survives.
        assertArrayEquals(byteArrayOf(1, 2, 3), file.readBytes())
        assertArrayEquals(byteArrayOf(1, 2, 3), documents.read(location))
    }

    @Test
    fun replacingAnEmptyDocument_writesIt() = runBlocking {
        file.writeBytes(ByteArray(0))

        documents.replace(location, byteArrayOf(4, 5))

        assertTrue(file.exists())
        assertArrayEquals(byteArrayOf(4, 5), file.readBytes())
    }
}
