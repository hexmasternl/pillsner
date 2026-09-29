package nl.hexmaster.pillsner.data.labelscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.labelscan.InterpretLabelText
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real Tesseract engine with the bundled trained data, on the device (spec: medicine-label-scan
 * "Recognition runs entirely inside the app"). The label is drawn, not photographed: this proves
 * the install, the initialisation, the byte-frame path and the interpretation fit together offline,
 * not how well a camera frame reads (that is the manual measurement in task 9.4).
 */
@RunWith(AndroidJUnit4::class)
class LabelTextRecogniserTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val installer = TessdataInstaller(context, versionCode = 1)
    private val recogniser = LabelTextRecogniser(installer)

    @After
    fun tearDown() {
        recogniser.close()
    }

    @Test
    fun theTrainedDataInstallsFromAssetsAndIsReplacedWhenTheVersionChanges() {
        File(context.filesDir, "ocr").deleteRecursively()

        val dataPath = installer.install()
        val trainedData = File(dataPath, "tessdata/eng.traineddata")
        assertTrue(trainedData.isFile)
        assertEquals(4_113_088L, trainedData.length())
        assertEquals("1", File(dataPath, "version").readText())

        // Same version: the copy is left alone.
        trainedData.setLastModified(1_000L)
        installer.install()
        assertEquals(1_000L, trainedData.lastModified())

        // New version: the copy is replaced and the marker moves on.
        TessdataInstaller(context, versionCode = 2).install()
        assertTrue(trainedData.lastModified() > 1_000L)
        assertEquals("2", File(dataPath, "version").readText())
    }

    @Test
    fun aDrawnLabelIsRecognisedAndInterpretedWithoutAnyNetwork() {
        val frame = drawLabel(
            "ZORVALEX 50 MG TABLETS",
            "Take 1 tablet twice daily",
            "for 10 days",
        )

        val opened = SystemClock.elapsedRealtime()
        recogniser.open()
        val openMillis = SystemClock.elapsedRealtime() - opened
        assertTrue(recogniser.isOpen)

        val started = SystemClock.elapsedRealtime()
        val lines = recogniser.recognise(frame)
        val recogniseMillis = SystemClock.elapsedRealtime() - started
        // Timings only, never the text (design D3): read them with `adb logcat -s LabelScanTest`.
        Log.i(TAG, "open=${openMillis}ms recognise=${recogniseMillis}ms frame=${frame.width}x${frame.height} lines=${lines.size}")

        assertFalse("Tesseract read nothing from a clean drawn label", lines.isEmpty())
        val interpretation = InterpretLabelText()(lines, LocalDate.of(2026, 9, 29))
        assertEquals("ZORVALEX", interpretation.name)
        assertEquals(Quantity.of("50", DoseUnit.MILLIGRAM), interpretation.defaultDose)
        assertEquals(1, interpretation.schedules.size)
        assertEquals(LocalDate.of(2026, 10, 8), interpretation.useUntil)
    }

    @Test
    fun recognisingAfterCloseReturnsNothingInsteadOfCrashing() {
        recogniser.open()
        recogniser.close()

        assertTrue(recogniser.recognise(drawLabel("ZORVALEX 50 MG")).isEmpty())
        assertFalse(recogniser.isOpen)
    }

    /** Black text on white at roughly the size a label reads at through a 1280-wide frame. */
    private fun drawLabel(vararg lines: String): GreyFrame {
        val width = 1000
        val height = 120 + lines.size * 90
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 56f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        lines.forEachIndexed { index, line -> canvas.drawText(line, 60f, 110f + index * 90f, paint) }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        val grey = ByteArray(pixels.size) { index -> (pixels[index] and 0xFF).toByte() }
        return GreyFrame(grey, width, height)
    }

    private companion object {
        const val TAG = "LabelScanTest"
    }
}
