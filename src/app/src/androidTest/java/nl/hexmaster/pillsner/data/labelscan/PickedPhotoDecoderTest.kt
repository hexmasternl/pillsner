package nl.hexmaster.pillsner.data.labelscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The picked-photo read path against real files (design D3, D6): one bounded read through the
 * content resolver, sampled down, converted row by row. The files live in the test's own cache and
 * are removed again; the decoder itself writes nothing.
 */
@RunWith(AndroidJUnit4::class)
class PickedPhotoDecoderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val decoder = PickedPhotoDecoder(context.contentResolver)
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    @Test
    fun aLargeJpegIsSampledDownToTheLongSideBoundAndConvertedToGrey() {
        val uri = jpeg(width = 4_000, height = 3_000, left = Color.WHITE, right = Color.BLACK)

        val frame = decoder.decode(uri)

        assertNotNull(frame)
        assertTrue("long side at most 2000, was ${frame!!.width}", frame.width <= PickedPhotoDecoderLimits.MAX_LONG_SIDE)
        assertEquals(frame.width * 3 / 4, frame.height)
        // White on the left, black on the right, whatever the JPEG did to the exact values.
        assertTrue(frame.pixel(frame.width / 8, frame.height / 2) > 200)
        assertTrue(frame.pixel(frame.width * 7 / 8, frame.height / 2) < 60)
    }

    @Test
    fun aSmallJpegKeepsItsSize() {
        val uri = jpeg(width = 640, height = 480, left = Color.GRAY, right = Color.GRAY)

        val frame = decoder.decode(uri)

        assertEquals(640, frame?.width)
        assertEquals(480, frame?.height)
    }

    @Test
    fun aFileThatIsNotAnImageIsUnreadableRatherThanAnError() {
        val file = File(context.cacheDir, "not-a-photo.jpg").apply { writeText("plain text"); files += this }

        assertNull(decoder.decode(Uri.fromFile(file)))
    }

    @Test
    fun aMissingFileIsUnreadableRatherThanAnError() {
        assertNull(decoder.decode(Uri.fromFile(File(context.cacheDir, "missing.jpg"))))
    }

    private fun jpeg(width: Int, height: Int, left: Int, right: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(left)
            val paint = android.graphics.Paint().apply { color = right }
            drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        val file = File(context.cacheDir, "picked-${width}x$height.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        files += file
        return Uri.fromFile(file)
    }
}

/** The decoder's bound, repeated here because the decoder keeps its constants private. */
private object PickedPhotoDecoderLimits {
    const val MAX_LONG_SIDE = 2_000
}
