package nl.hexmaster.pillsner.data.labelscan

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Recognizes text from a photographed medicine label entirely on-device
 * (medicine-add-label-scan design D1), using ML Kit's bundled Latin-script model. No network call
 * is ever made: the model ships inside the app, unlike the Play-services-backed variant this
 * project deliberately does not depend on.
 *
 * Boundary contract with the caller (the Medicines screen's capture flow, built separately): this
 * class takes an already-decoded, already-upright [Bitmap] and returns text only. It never reads a
 * `Uri` or a file itself, and it never writes anything — creating the temporary capture file (for
 * the camera path), decoding it or a picked photo into a [Bitmap], and deleting that file
 * immediately afterwards, on every outcome, are all the caller's responsibility.
 */
class LabelTextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * The raw recognized text, or null when nothing could be read.
     *
     * Never throws: an ML Kit failure is treated the same as "nothing recognized"
     * (medicine-label-scan spec, "Nothing usable is recognized"), since a failed scan should open
     * the form at its normal empty defaults rather than surface a technical error.
     *
     * @param rotationDegrees the clockwise rotation, in multiples of 90, needed to make [bitmap]
     *   upright. Defaults to 0 because the caller is expected to have already corrected for EXIF
     *   orientation before decoding.
     */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): String? {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        return runCatching { recognizer.process(image).await().text }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
    }
}
