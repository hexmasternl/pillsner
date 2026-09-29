package nl.hexmaster.pillsner.data.labelscan

import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import com.googlecode.tesseract.android.TessBaseAPI
import nl.hexmaster.pillsner.domain.labelscan.RecognisedLine

/**
 * Reads text off a greyscale frame with Tesseract (medicine-label-photo-prefill design D3).
 *
 * One native `TessBaseAPI` lives for one scan session: [open] once when the scanning screen (or
 * the picked-photo read) starts, [recognise] as many frames as arrive, [close] when it ends.
 * [stop] may be called from any thread to interrupt a recognition in progress, so closing the
 * screen never waits on a slow frame.
 *
 * Nothing here logs what was read. The only log lines are timings and counts.
 */
class LabelTextRecogniser(private val installer: TessdataInstaller) : FrameRecogniser {

    @Volatile
    private var api: TessBaseAPI? = null

    override val isOpen: Boolean get() = api != null

    /**
     * Installs the trained data if needed and initialises the engine. Blocking, and slow enough
     * (it loads the model) that it belongs off the main thread.
     *
     * @throws IllegalStateException when Tesseract cannot initialise from the installed data.
     */
    @WorkerThread
    @Synchronized
    override fun open() {
        if (api != null) return
        val started = SystemClock.elapsedRealtime()
        val dataPath = installer.install()
        val tess = TessBaseAPI()
        if (!tess.init(dataPath.absolutePath, TessdataInstaller.LANGUAGE)) {
            tess.recycle()
            throw IllegalStateException("Tesseract could not initialise")
        }
        tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
        api = tess
        Log.d(TAG, "Recogniser opened in ${SystemClock.elapsedRealtime() - started} ms")
    }

    /**
     * Recognises one frame and returns its text lines in reading order with their confidence.
     * Returns nothing when the recogniser is not open or was stopped mid-frame.
     */
    @WorkerThread
    @Synchronized
    override fun recognise(frame: GreyFrame): List<RecognisedLine> {
        val tess = api ?: return emptyList()
        val started = SystemClock.elapsedRealtime()
        tess.setImage(frame.pixels, frame.width, frame.height, BYTES_PER_PIXEL, frame.width)
        // Runs the recognition; the iterator below reads the result at text-line level.
        tess.utF8Text ?: return emptyList()
        val lines = mutableListOf<RecognisedLine>()
        val iterator = tess.resultIterator ?: return emptyList()
        try {
            iterator.begin()
            do {
                val text = iterator.getUTF8Text(LEVEL)?.trim()
                if (!text.isNullOrEmpty()) lines += RecognisedLine(text, iterator.confidence(LEVEL))
            } while (iterator.next(LEVEL))
        } finally {
            iterator.delete()
        }
        Log.d(TAG, "Frame recognised in ${SystemClock.elapsedRealtime() - started} ms, ${lines.size} lines")
        return lines
    }

    /** Interrupts a recognition in progress, from any thread. The recogniser stays open. */
    override fun stop() {
        api?.stop()
    }

    /** Releases the native engine. Waits for a frame in progress, so call [stop] first to hurry it. */
    @Synchronized
    override fun close() {
        api?.recycle()
        api = null
        Log.d(TAG, "Recogniser closed")
    }

    private companion object {
        const val TAG = "LabelScan"

        /** The frame is greyscale, one byte per pixel, rows packed without padding. */
        const val BYTES_PER_PIXEL = 1
        const val LEVEL = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
    }
}
