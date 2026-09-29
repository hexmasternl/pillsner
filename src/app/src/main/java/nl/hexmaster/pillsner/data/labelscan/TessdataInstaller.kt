package nl.hexmaster.pillsner.data.labelscan

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File
import java.io.IOException

/**
 * Puts the bundled trained data where Tesseract can read it (medicine-label-photo-prefill design
 * D1): `filesDir/ocr/tessdata/eng.traineddata`, copied out of the app's own assets before the
 * first scan. A version marker next to it carries the app's version code, so an update whose
 * bundled data differs replaces the copy on the next scan.
 *
 * The directory holds no user data and is excluded from backup and device transfer in both backup
 * rule files: any device recreates it from the assets.
 */
class TessdataInstaller(context: Context, private val versionCode: Int) {

    private val context = context.applicationContext

    /** The directory to hand to the recogniser: it contains the `tessdata` folder. */
    val dataPath: File get() = File(context.filesDir, DATA_DIR)

    /**
     * Copies the trained data when it is missing or belongs to another app version.
     *
     * @return [dataPath], ready for `TessBaseAPI.init`.
     */
    /**
     * @throws IllegalStateException when the data cannot be read from the assets or written to the
     *   files directory (a full disk, for one); the same contract as `FrameRecogniser.open`, so both
     *   scan paths show their "could not start" state instead of crashing.
     */
    @WorkerThread
    fun install(): File {
        val tessdata = File(dataPath, TESSDATA_DIR)
        val marker = File(dataPath, VERSION_MARKER)
        val target = File(tessdata, TRAINED_DATA_FILE)
        try {
            val installedVersion = marker.takeIf { it.isFile }?.readText()?.trim()
            if (target.isFile && installedVersion == versionCode.toString()) return dataPath

            tessdata.mkdirs()
            // Written next to the target and renamed, so a copy cut short by process death never
            // leaves a half file that looks installed.
            val temporary = File(tessdata, "$TRAINED_DATA_FILE.tmp")
            context.assets.open("$TESSDATA_DIR/$TRAINED_DATA_FILE").use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "Could not install the trained data" }
            marker.writeText(versionCode.toString())
        } catch (failure: IOException) {
            throw IllegalStateException("Could not install the trained data", failure)
        }
        Log.d(TAG, "Trained data installed")
        return dataPath
    }

    companion object {
        /** The Tesseract language the app bundles; the only one, for now (design D1). */
        const val LANGUAGE = "eng"

        private const val TAG = "LabelScan"
        private const val DATA_DIR = "ocr"
        private const val TESSDATA_DIR = "tessdata"
        private const val TRAINED_DATA_FILE = "$LANGUAGE.traineddata"
        private const val VERSION_MARKER = "version"
    }
}
